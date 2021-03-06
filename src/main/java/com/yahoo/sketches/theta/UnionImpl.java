/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.theta;

import static com.yahoo.sketches.theta.CompactSketch.compactCache;
import static com.yahoo.sketches.theta.CompactSketch.createCompactSketch;
import static com.yahoo.sketches.theta.PreambleUtil.FAMILY_BYTE;
import static com.yahoo.sketches.theta.PreambleUtil.FLAGS_BYTE;
import static com.yahoo.sketches.theta.PreambleUtil.MAX_THETA_LONG_AS_DOUBLE;
import static com.yahoo.sketches.theta.PreambleUtil.ORDERED_FLAG_MASK;
import static com.yahoo.sketches.theta.PreambleUtil.PREAMBLE_LONGS_BYTE;
import static com.yahoo.sketches.theta.PreambleUtil.RETAINED_ENTRIES_INT;
import static com.yahoo.sketches.theta.PreambleUtil.SEED_HASH_SHORT;
import static com.yahoo.sketches.theta.PreambleUtil.SER_VER_BYTE;
import static com.yahoo.sketches.theta.PreambleUtil.THETA_LONG;
import static com.yahoo.sketches.theta.PreambleUtil.UNION_THETA_LONG;
import static java.lang.Math.min;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;

/**
 * @author Lee Rhodes
 * @author Kevin Lang
 */
abstract class UnionImpl extends SetOperation implements Union {
  protected static final Family MY_FAMILY = Family.UNION;
  protected final short seedHash_;
  protected final UpdateSketch gadget_;
  protected long unionThetaLong_;
  
  /**
   * Construct a new Union that can be on-heap or off-heap
   * 
   * @param gadget Configured instance of UpdateSketch.
   */
  UnionImpl(UpdateSketch gadget) {
    gadget_ = gadget;
    seedHash_ = computeSeedHash(gadget_.getSeed());
    unionThetaLong_ = gadget_.getThetaLong();
  }
  
  /**
   * Heapify or Wrap a Union that can be on-heap or off-heap 
   * from a Memory object containing data. 
   * 
   * @param gadget Configured instance of UpdateSketch.
   * @param srcMem The source Memory object.
   * <a href="{@docRoot}/resources/dictionary.html#mem">See Memory</a>
   * @param seed <a href="{@docRoot}/resources/dictionary.html#seed">See seed</a> 
   */
  UnionImpl(UpdateSketch gadget, Memory srcMem, long seed) {
    gadget_ = gadget;
    seedHash_ = computeSeedHash(gadget_.getSeed());
    MY_FAMILY.checkFamilyID(srcMem.getByte(FAMILY_BYTE));
    unionThetaLong_ = srcMem.getLong(UNION_THETA_LONG);
  }
  
  @Override
  public void update(Sketch sketchIn) {
    //UNION Empty Rule: AND the empty states
    
    if ((sketchIn == null)  || sketchIn.isEmpty()) {
      //null/empty is interpreted as (1.0, 0, T).  Nothing changes
      return;
    }
    
    PreambleUtil.checkSeedHashes(seedHash_, sketchIn.getSeedHash());
    long thetaLongIn = sketchIn.getThetaLong();
    
    unionThetaLong_ = min(unionThetaLong_, thetaLongIn); //Theta rule
    
    if(sketchIn.isOrdered()) { //Use early stop
      int curCount = sketchIn.getRetainedEntries(false);
      
      if(sketchIn.isDirect()) {
        Memory skMem = sketchIn.getMemory();
        int preambleLongs = skMem.getByte(PREAMBLE_LONGS_BYTE) & 0X3F;
        for (int i = 0; i < curCount; i++ ) {
          int offsetBytes = (preambleLongs +i) << 3;
          long hashIn = skMem.getLong(offsetBytes);
          if (hashIn >= unionThetaLong_) break; // "early stop"
          gadget_.hashUpdate(hashIn); //backdoor update, hash function is bypassed
        }
      } 
      else { //on Heap
        long[] cacheIn = sketchIn.getCache(); //not a copy!
        for (int i = 0; i < curCount; i++ ) {
          long hashIn = cacheIn[i];
          if (hashIn >= unionThetaLong_) break; // "early stop"
          gadget_.hashUpdate(hashIn); //backdoor update, hash function is bypassed
        }
      }
    } 
    else { //either not-ordered compact or Hash Table.
      long[] cacheIn = sketchIn.getCache(); //if off-heap this will be a copy
      int arrLongs = cacheIn.length;
      for (int i = 0; i < arrLongs; i++ ) {
        long hashIn = cacheIn[i];
        if ((hashIn <= 0L) || (hashIn >= unionThetaLong_)) continue;
        gadget_.hashUpdate(hashIn); //backdoor update, hash function is bypassed
      }
    }
    unionThetaLong_ = min(unionThetaLong_, gadget_.getThetaLong());
  }
  
  @Override
  public void update(Memory skMem) {
    //UNION Rule: AND the empty states
    if (skMem == null) return;
    int cap = (int)skMem.getCapacity();
    int f;
    assert ((f=skMem.getByte(FAMILY_BYTE)) == 3) : "Illegal Family/SketchType byte: "+f;
    int serVer = skMem.getByte(SER_VER_BYTE);
    if (serVer == 1) {
      if (cap <= 24) return; //empty
      processVer1(skMem);
    }
    else if (serVer == 2) {
      if (cap <= 8) return; //empty
      processVer2(skMem);
    }
    else if (serVer == 3) {
      if (cap <= 8) return; //empty
      processVer3(skMem);
    }
    else throw new IllegalArgumentException("SerVer is unknown: "+serVer);
  }
  
  @Override
  public void update(long datum) {
    gadget_.update(datum);
  }
  
  @Override
  public void update(double datum) {
    gadget_.update(datum);
  }
  
  @Override
  public void update(String datum) {
    gadget_.update(datum);
  }
  
  @Override
  public void update(byte[] data) {
    gadget_.update(data);
  }
  
  @Override
  public void update(int[] data) {
    gadget_.update(data);
  }
  
  @Override
  public void update(long[] data) {
    gadget_.update(data);
  }
  
  //must trust seed, no seedhash. No p, can't be empty, can only be compact, ordered, cap > 24
  private void processVer1(Memory skMem) {
    long thetaLongIn = skMem.getLong(THETA_LONG);
    unionThetaLong_ = min(unionThetaLong_, thetaLongIn); //Theta rule
    int curCount = skMem.getInt(RETAINED_ENTRIES_INT);
    int preLongs = 3;
    for (int i = 0; i < curCount; i++ ) {
      int offsetBytes = (preLongs +i) << 3;
      long hashIn = skMem.getLong(offsetBytes);
      if (hashIn >= unionThetaLong_) break; // "early stop"
      gadget_.hashUpdate(hashIn); //backdoor update, hash function is bypassed
    }
    unionThetaLong_ = min(unionThetaLong_, gadget_.getThetaLong());
  }
  
  //has seedhash, p, could have 0 entries & theta, can only be compact, ordered, cap >= 8
  private void processVer2(Memory skMem) {
    PreambleUtil.checkSeedHashes(seedHash_, skMem.getShort(SEED_HASH_SHORT));
    int preLongs = skMem.getByte(PREAMBLE_LONGS_BYTE) & 0X3F;
    int curCount = skMem.getInt(RETAINED_ENTRIES_INT);
    long thetaLongIn;
    if (preLongs == 1) {
      return;
    }
    if (preLongs == 2) {
      assert curCount > 0;
      thetaLongIn = Long.MAX_VALUE;
    } else { //prelongs == 3, curCount may be 0 (e.g., from intersection)
      thetaLongIn = skMem.getLong(THETA_LONG);
    }
    unionThetaLong_ = min(unionThetaLong_, thetaLongIn); //Theta rule
    for (int i = 0; i < curCount; i++ ) {
      int offsetBytes = (preLongs +i) << 3;
      long hashIn = skMem.getLong(offsetBytes);
      if (hashIn >= unionThetaLong_) break; // "early stop"
      gadget_.hashUpdate(hashIn); //backdoor update, hash function is bypassed
    }
    unionThetaLong_ = min(unionThetaLong_, gadget_.getThetaLong());
  }
  
  //has seedhash, p, could have 0 entries & theta, could be unordered, compact, cap >= 8
  private void processVer3(Memory skMem) {
    PreambleUtil.checkSeedHashes(seedHash_, skMem.getShort(SEED_HASH_SHORT));
    int preLongs = skMem.getByte(PREAMBLE_LONGS_BYTE) & 0X3F;
    int curCount = skMem.getInt(RETAINED_ENTRIES_INT);
    long thetaLongIn;
    if (preLongs == 1) {
      return;
    }
    if (preLongs == 2) { //curCount has to be > 0 and exact
      assert curCount > 0;
      thetaLongIn = Long.MAX_VALUE;
    } else { //prelongs == 3, curCount may be 0 (e.g., from intersection)
      thetaLongIn = skMem.getLong(THETA_LONG);
    }
    unionThetaLong_ = min(unionThetaLong_, thetaLongIn); //Theta rule
    boolean ordered = skMem.isAnyBitsSet(FLAGS_BYTE, (byte) ORDERED_FLAG_MASK);
    if (ordered) {
      for (int i = 0; i < curCount; i++ ) {
        int offsetBytes = (preLongs +i) << 3;
        long hashIn = skMem.getLong(offsetBytes);
        if (hashIn >= unionThetaLong_) break; // "early stop"
        gadget_.hashUpdate(hashIn); //backdoor update, hash function is bypassed
      }
    }
    else { //unordered
      for (int i = 0; i < curCount; i++ ) {
        int offsetBytes = (preLongs +i) << 3;
        long hashIn = skMem.getLong(offsetBytes);
        if ((hashIn <= 0L) || (hashIn >= unionThetaLong_)) continue;
        gadget_.hashUpdate(hashIn); //backdoor update, hash function is bypassed
      }
    }
    unionThetaLong_ = min(unionThetaLong_, gadget_.getThetaLong());
  }
  
  @Override
  public CompactSketch getResult(boolean dstOrdered, Memory dstMem) {
    int gadgetCurCount = gadget_.getRetainedEntries(true);
    int k = 1 << gadget_.getLgNomLongs();
    
    if (gadgetCurCount > k) {
      gadget_.rebuild();
    } 
    //curCount <= k; gadget theta could be p < 1.0, but cannot do a quick select
    long thetaLongR = min(gadget_.getThetaLong(), unionThetaLong_);
    double p = gadget_.getP();
    double thetaR = thetaLongR/MAX_THETA_LONG_AS_DOUBLE;
    long[] gadgetCache = gadget_.getCache(); //if Direct, always a copy
    //CurCount must be recounted with a scan using the new theta
    int curCountR = HashOperations.count(gadgetCache, thetaLongR);
    long[] compactCacheR = compactCache(gadgetCache, curCountR, thetaLongR, dstOrdered);
    boolean emptyR = (gadget_.isEmpty() && (p >= thetaR) && (curCountR == 0));
    return createCompactSketch(compactCacheR, emptyR, seedHash_, curCountR, thetaLongR, 
        dstOrdered, dstMem);
  }
  
  @Override
  public CompactSketch getResult() {
    return getResult(true, null);
  }
  
  @Override
  public byte[] toByteArray() {
    byte[] gadgetByteArr = gadget_.toByteArray();
    Memory mem = new NativeMemory(gadgetByteArr);
    mem.putLong(UNION_THETA_LONG, unionThetaLong_); // union theta
    return gadgetByteArr;
  }
  
  @Override
  public void reset() {
    gadget_.reset();
    unionThetaLong_ = gadget_.getThetaLong();
  }
  
}
