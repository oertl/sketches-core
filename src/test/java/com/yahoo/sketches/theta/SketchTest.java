/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.theta;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;
import org.testng.annotations.Test;

import static com.yahoo.sketches.Family.ALPHA;
import static com.yahoo.sketches.Family.QUICKSELECT;
import static com.yahoo.sketches.Util.DEFAULT_NOMINAL_ENTRIES;
import static com.yahoo.sketches.Util.DEFAULT_UPDATE_SEED;
import static com.yahoo.sketches.theta.PreambleUtil.COMPACT_FLAG_MASK;
import static com.yahoo.sketches.theta.PreambleUtil.FLAGS_BYTE;
import static com.yahoo.sketches.theta.ResizeFactor.X1;
import static com.yahoo.sketches.theta.ResizeFactor.X2;
import static com.yahoo.sketches.theta.ResizeFactor.X4;
import static com.yahoo.sketches.theta.ResizeFactor.X8;
import static com.yahoo.sketches.theta.Sketch.getMaxCompactSketchBytes;
import static org.testng.Assert.assertEquals;

/**
 * @author Lee Rhodes
 */
public class SketchTest {
  
  @Test
  public void checkGetMaxBytesWithEntries() {
    assertEquals(getMaxCompactSketchBytes(10), 10*8 + (Family.COMPACT.getMaxPreLongs() << 3) );
  }
  
  @Test
  public void checkGetCurrentBytes() {
    int k = 64;
    int lowQSPreLongs = Family.QUICKSELECT.getMinPreLongs();
    int lowCompPreLongs = Family.COMPACT.getMinPreLongs();
    UpdateSketch sketch = UpdateSketch.builder().build(k); // QS Sketch
    assertEquals(sketch.getCurrentPreambleLongs(false), lowQSPreLongs);
    assertEquals(sketch.getCurrentPreambleLongs(true), 1); //compact form
    assertEquals(sketch.getCurrentDataLongs(false), k*2);
    assertEquals(sketch.getCurrentDataLongs(true), 0); //compact form
    assertEquals(sketch.getCurrentBytes(false), k*2*8 + (lowQSPreLongs << 3));
    assertEquals(sketch.getCurrentBytes(true), lowCompPreLongs << 3);
    
    CompactSketch compSk = sketch.compact(false, null);
    assertEquals(compSk.getCurrentBytes(true), 8);
    assertEquals(compSk.getCurrentBytes(false), 8);
    
    int compPreLongs = Sketch.compactPreambleLongs(sketch.getThetaLong(), sketch.isEmpty());
    assertEquals(compPreLongs, 1);
    
    for (int i=0; i<k; i++) sketch.update(i);
    
    assertEquals(sketch.getCurrentPreambleLongs(false), lowQSPreLongs);
    assertEquals(sketch.getCurrentPreambleLongs(true), 2); //compact form
    assertEquals(sketch.getCurrentDataLongs(false), k*2);
    assertEquals(sketch.getCurrentDataLongs(true), k); //compact form
    assertEquals(sketch.getCurrentBytes(false), k*2*8 + (lowQSPreLongs << 3));
    assertEquals(sketch.getCurrentBytes(true), k*8 + 2*8); //compact form  //FAILS HERE
    
    compPreLongs = Sketch.compactPreambleLongs(sketch.getThetaLong(), sketch.isEmpty());
    assertEquals(compPreLongs, 2);
    
    for (int i=k; i<2*k; i++) sketch.update(i); //go estimation mode
    int curCount = sketch.getRetainedEntries(true);

    assertEquals(sketch.getCurrentPreambleLongs(false), lowQSPreLongs);
    assertEquals(sketch.getCurrentPreambleLongs(true), 3); //compact form
    assertEquals(sketch.getCurrentDataLongs(false), k*2);
    assertEquals(sketch.getCurrentDataLongs(true), curCount); //compact form
    assertEquals(sketch.getCurrentBytes(false), k*2*8 + (lowQSPreLongs << 3));
    assertEquals(sketch.getCurrentBytes(true), curCount*8 + 3*8); //compact form
    
    compPreLongs = Sketch.compactPreambleLongs(sketch.getThetaLong(), sketch.isEmpty());
    assertEquals(compPreLongs, 3);
    
    for (int i=0; i<3; i++) {
      int maxCompBytes = Sketch.getMaxCompactSketchBytes(i);
      assertEquals(maxCompBytes, (Family.COMPACT.getMaxPreLongs() << 3) + i*8);
    }
  }

  @Test
  public void checkBuilder() {
    int k = 2048;
    int lgK = Integer.numberOfTrailingZeros(k);
    long seed = 1021;
    float p = (float)0.5;
    ResizeFactor rf = X4;
    Family fam = Family.ALPHA;
    
    UpdateSketch sk1 = UpdateSketch.builder().setSeed(seed)
        .setP(p).setResizeFactor(rf).setFamily(fam).build(k);
    String nameS1 = sk1.getClass().getSimpleName();
    assertEquals(nameS1, "HeapAlphaSketch");
    assertEquals(sk1.getLgNomLongs(), lgK);
    assertEquals(sk1.getSeed(), seed);
    assertEquals(sk1.getP(), p);
    assertEquals(sk1.getLgResizeFactor(), rf.lg());
    
    //check reset of defaults
    
    sk1 = UpdateSketch.builder().build();
    nameS1 = sk1.getClass().getSimpleName();
    assertEquals(nameS1, "HeapQuickSelectSketch");
    assertEquals(sk1.getLgNomLongs(), Integer.numberOfTrailingZeros(DEFAULT_NOMINAL_ENTRIES));
    assertEquals(sk1.getSeed(), DEFAULT_UPDATE_SEED);
    assertEquals(sk1.getP(), (float)1.0);
    assertEquals(sk1.getLgResizeFactor(), ResizeFactor.X8.lg());
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkBuilderIllegalEntries() {
    int k = 1000;
    UpdateSketch.builder().build(k);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkBuilderIllegalP() {
    float p = (float)1.5;
    UpdateSketch.builder().setP(p).build();
  }
  
  @Test
  public void checkBuilderResizeFactor() {
    ResizeFactor rf;
    rf = X1;
    assertEquals(rf.getValue(), 1);
    assertEquals(rf.lg(), 0);
    assertEquals(ResizeFactor.getRF(0), X1);
    rf = X2;
    assertEquals(rf.getValue(), 2);
    assertEquals(rf.lg(), 1);
    assertEquals(ResizeFactor.getRF(1), X2);
    rf = X4;
    assertEquals(rf.getValue(), 4);
    assertEquals(rf.lg(), 2);
    assertEquals(ResizeFactor.getRF(2), X4);
    rf = X8;
    assertEquals(rf.getValue(), 8);
    assertEquals(rf.lg(), 3);
    assertEquals(ResizeFactor.getRF(3), X8);
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkWrapBadFamily() {
    UpdateSketch sketch = UpdateSketch.builder().setFamily(Family.ALPHA).build(1024);
    byte[] byteArr = sketch.toByteArray();
    Memory srcMem = new NativeMemory(byteArr);
    Sketch sketch2 = Sketch.wrap(srcMem);
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkBadFamily() {
    UpdateSketch sketch = UpdateSketch.builder().setFamily(Family.INTERSECTION).build(1024);
  }
  
  @Test
  public void checkSerVer() {
    UpdateSketch sketch = UpdateSketch.builder().build(1024);
    byte[] sketchArray = sketch.toByteArray();
    Memory mem = new NativeMemory(sketchArray);
    int serVer = Sketch.getSerializationVersion(mem);
    assertEquals(serVer, 3);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkLBlimits0() {
    Sketch.lowerBound(0, Long.MAX_VALUE, 0, true);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkUBlimits0() {
    Sketch.upperBound(0, Long.MAX_VALUE, 0, true);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkLBlimits4() {
    Sketch.lowerBound(4, Long.MAX_VALUE, 0, true);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkUBlimits4() {
    Sketch.upperBound(4, Long.MAX_VALUE, 0, true);
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkHeapifyAlphaCompactExcep() {
    int k = 512;
    Sketch sketch1 = UpdateSketch.builder().setFamily(ALPHA).build(k);
    byte[] byteArray = sketch1.toByteArray();
    Memory mem = new NativeMemory(byteArray);
    //corrupt:
    mem.setBits(FLAGS_BYTE, (byte) COMPACT_FLAG_MASK);
    Sketch sketch2 = Sketch.heapify(mem);
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkHeapifyQSCompactExcep() {
    int k = 512;
    Sketch sketch1 = UpdateSketch.builder().setFamily(QUICKSELECT).build(k);
    byte[] byteArray = sketch1.toByteArray();
    Memory mem = new NativeMemory(byteArray);
    //corrupt:
    mem.setBits(FLAGS_BYTE, (byte) COMPACT_FLAG_MASK);
    Sketch sketch2 = Sketch.heapify(mem);
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkHeapifyNotCompactExcep() {
    int k = 512;
    UpdateSketch sketch1 = UpdateSketch.builder().setFamily(QUICKSELECT).build(k);
    int bytes = Sketch.getMaxCompactSketchBytes(0);
    byte[] byteArray = new byte[bytes];
    Memory mem = new NativeMemory(byteArray);
    CompactSketch comp = sketch1.compact(false, mem);
    //corrupt:
    mem.clearBits(FLAGS_BYTE, (byte) COMPACT_FLAG_MASK);
    Sketch sketch2 = Sketch.heapify(mem);
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkHeapifyFamilyExcep() {
    int k = 512;
    Union union = SetOperation.builder().buildUnion(k);
    byte[] byteArray = union.toByteArray();
    Memory mem = new NativeMemory(byteArray);
    //Improper use
    Sketch sketch2 = Sketch.heapify(mem);
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkWrapAlphaCompactExcep() {
    int k = 512;
    Sketch sketch1 = UpdateSketch.builder().setFamily(ALPHA).build(k);
    byte[] byteArray = sketch1.toByteArray();
    Memory mem = new NativeMemory(byteArray);
    //corrupt:
    mem.setBits(FLAGS_BYTE, (byte) COMPACT_FLAG_MASK);
    Sketch sketch2 = Sketch.wrap(mem);
    
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkWrapQSCompactExcep() {
    int k = 512;
    Sketch sketch1 = UpdateSketch.builder().setFamily(QUICKSELECT).build(k);
    byte[] byteArray = sketch1.toByteArray();
    Memory mem = new NativeMemory(byteArray);
    //corrupt:
    mem.setBits(FLAGS_BYTE, (byte) COMPACT_FLAG_MASK);
    Sketch sketch2 = Sketch.wrap(mem);
  }
  
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkWrapNotCompactExcep() {
    int k = 512;
    UpdateSketch sketch1 = UpdateSketch.builder().setFamily(QUICKSELECT).build(k);
    int bytes = Sketch.getMaxCompactSketchBytes(0);
    byte[] byteArray = new byte[bytes];
    Memory mem = new NativeMemory(byteArray);
    CompactSketch comp = sketch1.compact(false, mem);
    //corrupt:
    mem.clearBits(FLAGS_BYTE, (byte) COMPACT_FLAG_MASK);
    Sketch sketch2 = Sketch.wrap(mem);
  }
  
  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }
  
  /**
   * @param s value to print 
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }
  
}