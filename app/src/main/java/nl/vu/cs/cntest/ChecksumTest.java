package nl.vu.cs.cntest;


import junit.framework.TestCase;

import nl.vu.cs.cn.TCPSegment;
import nl.vu.cs.cn.TcpControlBlock;
import nl.vu.cs.cn.util.util;

/**
 * Created by nikos on 28-6-15.
 */
public class ChecksumTest extends TestCase {

    public void testCheckSum (){
        TcpControlBlock tcb = new TcpControlBlock();
        TCPSegment sgmt = new TCPSegment(tcb, util.DATA,new byte[0]);
        assertEquals(0, sgmt.computeChecksum());

    }





}

