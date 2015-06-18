package nl.vu.cs.cn;

import android.util.Log;

import java.nio.ByteBuffer;

import nl.vu.cs.cn.util.util;

/**
 * Created by stavri on 17-6-15.
 */
public class TCPSegment {

    static int PSEUDO_LENGTH = 12;
    static short TCP_PROTOCOL = 6;
    static int SRC_ADDRESS = 0;
    static int DST_ADDRESS = 4;
    static int PRTCL = 8;
    static int LGTH = 10;

    static int SRC_PORT = 0;
    static int DST_PORT = 2;
    static int SEQ_NO = 4;
    static int ACK_NO = 8;
    static int UNUSED1 = 12;
    static int FLAGS = 13;
    static int WIN = 14;
    static int CHECKSUM = 16;
    static int UNUSED2 = 18;
    static int DATA = 20;



    short sourcePort;
    short destinationPort;
    int sequenceNumber;
    int ackNumber;
    byte tcpFlags;
    short checksum;
    int length;
    byte[] data;
    int sourceIP;
    int destinationIP;


    public TCPSegment(){}

    public TCPSegment(IP.Packet pck){
        ByteBuffer buffer = ByteBuffer.wrap(pck.data);
        this.sourcePort = buffer.getShort(SRC_PORT);
        this.destinationPort = buffer.getShort(DST_PORT);
        this.sequenceNumber = buffer.getInt(SEQ_NO);
        this.ackNumber = buffer.getInt(ACK_NO);
        this.tcpFlags = buffer.get(FLAGS);
        this.checksum = buffer.getShort(CHECKSUM);
        this.data = new byte[pck.length-DATA];
        if (this.data.length > 0) {
            buffer.get(this.data, DATA, this.data.length);
        }
        this.length = pck.length;
        this.sourceIP = Integer.reverseBytes(pck.source);
        this.destinationIP = Integer.reverseBytes(pck.destination);
    }

    public TCPSegment(TcpControlBlock tcb, byte tcpFlags,  byte[] data) {

        this.sourcePort = tcb.tcb_our_port;
        this.destinationPort = tcb.tcb_their_port;
        this.sequenceNumber = tcb.tcb_our_sequence_number;
        this.ackNumber = tcb.tcb_their_sequence_num;
        this.tcpFlags = tcpFlags;
        this.checksum = 0;
        this.data = data;
        this.length = DATA + data.length;
        this.sourceIP = tcb.tcb_our_ip_address;
        this.destinationIP = tcb.tcb_their_ip_address;
        this.checksum = computeChecksum();

    }

    public int length(){
        return this.length;
    }

    public void setChecksum(short checksum){
        this.checksum = checksum;
    }

    public short computeChecksum(){

        int total_length =  this.length;
        byte[] raw = new byte[total_length];
        this.toArray(raw, 0);
        ByteBuffer rawBuf = ByteBuffer.wrap(raw);
//        rawBuf.putInt(SRC_ADDRESS,this.sourceIP);
//        rawBuf.putInt(DST_ADDRESS, this.destinationIP);
//        rawBuf.putShort(PRTCL, TCP_PROTOCOL);
//        rawBuf.putShort(LGTH, (short) this.length);

        long sum = 0;

        sum += (this.sourceIP >>> 16) + (this.sourceIP & 0xffff);
        sum += (this.destinationIP >>> 16) + (this.destinationIP & 0xffff);
        sum += (TCP_PROTOCOL);
        sum += (this.length >>> 16) + (this.length & 0xffff);


        for (int i=0 ; i < total_length - 1; i += 2){
            sum += (rawBuf.getShort(i) & 0xffff);
        }

        if (total_length % 2 != 0){
            sum += (rawBuf.get(total_length - 1) & 0xffff) << 8;
        }

        while (sum > 65535){
            sum = (sum>>>16) + (sum & 0xffff);
        }

        sum = ~sum & 0xffff;

        return (short) sum;

    }

    public void toArray(byte[] dst , int offset){

        ByteBuffer buffer = ByteBuffer.allocate(this.length);
        buffer.putShort(SRC_PORT, this.sourcePort);
        buffer.putShort(DST_PORT, this.destinationPort);
        buffer.putInt(SEQ_NO, this.sequenceNumber);
        buffer.putInt(ACK_NO, this.ackNumber);
        buffer.put(UNUSED1, (byte) 0);
        buffer.put(FLAGS, this.tcpFlags);
        buffer.putShort(WIN, (short) 1);
        buffer.putShort(CHECKSUM, this.checksum);
        buffer.putShort(UNUSED2, (short)0);

        System.arraycopy(buffer.array(), 0, dst, offset, this.length);

    }

    public boolean isValid(TcpControlBlock tcb, int expectedFlags){

        boolean check = (this.computeChecksum() == 0)
                && this.tcpFlags == expectedFlags
                && this.destinationPort == tcb.tcb_our_port
                && this.destinationIP == tcb.tcb_our_ip_address;

        switch (expectedFlags){
            case util.SYN:
                if (check){
                    tcb.tcb_their_ip_address = this.sourceIP;
                    tcb.tcb_their_port = this.sourcePort;
                    tcb.tcb_their_sequence_num = this.sequenceNumber;
                }
                break;

            case util.SYNACK:

                check = check
                        && this.ackNumber == tcb.tcb_our_expected_ack
                        && this.sourceIP == tcb.tcb_their_ip_address
                        && this.sourcePort == tcb.tcb_their_port;

                if (check){
                    tcb.tcb_their_sequence_num = this.sequenceNumber;
                }

                break;


            case util.DATA:

                check = check
                        && this.ackNumber == tcb.tcb_our_expected_ack
                        && this.sourceIP == tcb.tcb_their_ip_address
                        && this.sourcePort == tcb.tcb_their_port
                        && this.sequenceNumber == tcb.tcb_their_sequence_num + 1;

                break;




        }

        if(!check){
            Log.i("IP: " + Integer.reverseBytes(tcb.tcb_our_ip_address) , "Invalid Segment: " + this.toString());
        }

        return check;


    }

    public String toString(){
        return "sourcePort: " + this.sourcePort + " destinationPort: " + this.destinationPort +
                " sequenceNumber: " + this.sequenceNumber + " ackNumber: " + this.ackNumber +
                " tcpFlags: " + this.tcpFlags + " checksum: " + this.checksum;
    }
}
