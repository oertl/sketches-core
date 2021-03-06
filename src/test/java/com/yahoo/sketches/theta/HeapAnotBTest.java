/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.theta;

import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Lee Rhodes
 */
public class HeapAnotBTest {
  
  @Test
  public void checkExactAnotB_AvalidNoOverlap() {
    int k = 512;
    
    UpdateSketch usk1 = UpdateSketch.builder().build(k);
    UpdateSketch usk2 = UpdateSketch.builder().build(k);
    
    for (int i=0; i<k/2; i++) usk1.update(i);
    for (int i=k/2; i<k; i++) usk2.update(i);
    
    AnotB aNb = SetOperation.builder().buildANotB();
    aNb.update(usk1, usk2);
    
    CompactSketch rsk1;
    
    rsk1 = aNb.getResult(false, null);
    assertEquals(rsk1.getEstimate(), (double)(k/2));
    
    aNb.update(usk1, usk2);
    rsk1 = aNb.getResult(true, null);
    assertEquals(rsk1.getEstimate(), (double)(k/2));
    
    //getCurrentBytes( compact )
    int bytes = rsk1.getCurrentBytes(true);
    byte[] byteArray = new byte[bytes];
    Memory mem = new NativeMemory(byteArray);
    
    aNb.update(usk1, usk2);
    rsk1 = aNb.getResult(false, mem);
    assertEquals(rsk1.getEstimate(), (double)(k/2));
    
    aNb.update(usk1, usk2);
    rsk1 = aNb.getResult(true, mem);
    assertEquals(rsk1.getEstimate(), (double)(k/2));
  }
  
  @Test
  public void checkCombinations() {
    int k = 512;
    UpdateSketch aNull = null;
    UpdateSketch bNull = null;
    UpdateSketch aEmpty = UpdateSketch.builder().build(k);
    UpdateSketch bEmpty = UpdateSketch.builder().build(k);
    
    UpdateSketch aHT = UpdateSketch.builder().build(k);
    for (int i=0; i<k; i++) aHT.update(i);
    CompactSketch aC = aHT.compact(false, null);
    CompactSketch aO = aHT.compact(true,  null);
    
    UpdateSketch bHT = UpdateSketch.builder().build(k);
    for (int i=k/2; i<(k+k/2); i++) bHT.update(i); //overlap is k/2
    CompactSketch bC = bHT.compact(false, null);
    CompactSketch bO = bHT.compact(true,  null);
    
    CompactSketch res;
    AnotB aNb;
    boolean ordered = true;
    
    aNb = SetOperation.builder().buildANotB();

    aNb.update(aNull, bNull);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aNull, bEmpty);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aNull, bC);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aNull, bO);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aNull, bHT);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    
    aNb.update(aEmpty, bNull);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aEmpty, bEmpty);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aEmpty, bC);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aEmpty, bO);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aEmpty, bHT);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), 0.0);
    assertTrue(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    
    aNb.update(aC, bNull);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aC, bEmpty);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aC, bC);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aC, bO);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aC, bHT);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    
    aNb.update(aO, bNull);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aO, bEmpty);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aO, bC);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aO, bO);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aO, bHT);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    
    aNb.update(aHT, bNull);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aHT, bEmpty);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aHT, bC);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aHT, bO);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
    
    aNb.update(aHT, bHT);
    res = aNb.getResult(!ordered, null);
    assertEquals(res.getEstimate(), (double) k/2);
    assertFalse(res.isEmpty());
    assertEquals(res.getThetaLong(), Long.MAX_VALUE);
  }
  
  @Test
  public void checkAnotBnotC() {
    int k = 1024;
    boolean ordered = true;
    
    UpdateSketch aU = UpdateSketch.builder().build(k);
    for (int i=0; i<k; i++) aU.update(i);        //All 1024
    
    UpdateSketch bU = UpdateSketch.builder().build(k);
    for (int i=0; i<k/2; i++) bU.update(i);      //512
    
    UpdateSketch cU = UpdateSketch.builder().build(k);
    for (int i=k/2; i<3*k/4; i++) cU.update(i);  //256
    
    int memBytes = Sketch.getMaxUpdateSketchBytes(k);
    CompactSketch r1, r2;
    
    byte[] memArr1 = new byte[memBytes];
    byte[] memArr2 = new byte[memBytes];
    Memory mem1 = new NativeMemory(memArr1);
    Memory mem2 = new NativeMemory(memArr2);
    
    AnotB aNb = SetOperation.builder().buildANotB();
    aNb.update(aU, bU);
    r1 = aNb.getResult(ordered, mem1);
    
    aNb.update(r1, cU);
    r2 = aNb.getResult(ordered, mem2);
    double est = r2.getEstimate();
    println("est: "+est);
    assertEquals(est, k/4, 0.0);
  }
  
  @Test
  public void checkAnotBnotC_sameMemory() {
    int k = 1024;
    boolean ordered = true;
    
    UpdateSketch a = UpdateSketch.builder().build(k);
    for (int i=0; i<k; i++) a.update(i);        //All 1024
    
    UpdateSketch b = UpdateSketch.builder().build(k);
    for (int i=0; i<k/2; i++) b.update(i);      //512
    
    UpdateSketch c = UpdateSketch.builder().build(k);
    for (int i=k/2; i<3*k/4; i++) c.update(i);  //256
    
    int memBytes = Sketch.getMaxCompactSketchBytes(a.getCurrentBytes(true));
    
    byte[] memArr = new byte[memBytes];
    Memory mem = new NativeMemory(memArr);
    
    CompactSketch r;
    AnotB aNb = SetOperation.builder().buildANotB();
    
    //Iterative loop: {
    aNb.update(a, b);
    r = aNb.getResult(ordered, mem);
    
    aNb.update(r, c);
    r = aNb.getResult(ordered, mem);
    //}
    
    double est = r.getEstimate();
    println("est: "+est);
    assertEquals(est, k/4, 0.0);
  }
  
  @Test
  public void checkGetResult() {
    int k = 1024;
    UpdateSketch skA = Sketches.updateSketchBuilder().build();
    UpdateSketch skB = Sketches.updateSketchBuilder().build();
  
    AnotB aNotB = Sketches.setOperationBuilder().buildANotB(k);
    aNotB.update(skA, skB);
    CompactSketch csk = aNotB.getResult();
    assertEquals(csk.getCurrentBytes(true), 8);
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