package nl.vu.cs.cn;

import android.util.Log;

import java.nio.ByteBuffer;

import nl.vu.cs.cn.util.util;

/**
 * Created by stavri on 17-6-15.
 */
public class TCPSegment {

    static short TCP_PROTOCOL = 6;

    static int SRC_PORT = 0;
    static int DST_PORT = 2;
    static int SEQ_NO = 4;
    static int ACK_NO = 8;
    static int HLEN = 12;
    static int FLAGS = 13;
    static int WIN = 14;
    static int CHECKSUM = 16;
    static int URG = 18;
    static int DATA = 20;



    short sourcePort;
    short destinationPort;
    int sequenceNumber;
    int ackNumber;
    byte dataOffset;
    byte tcpFlags;
    short window;
    short checksum;
    short urgentPointer;
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
        this.dataOffset = buffer.get(HLEN);
        this.tcpFlags = buffer.get(FLAGS);
        this.window = buffer.getShort(WIN);
        this.checksum = buffer.getShort(CHECKSUM);
        this.urgentPointer = buffer.getShort(URG);
        this.data = new byte[pck.length-DATA];
        if (this.data.length > 0) {
            System.arraycopy(pck.data, DATA, this.data, 0, this.data.length);
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
        this.dataOffset = (byte)80;
        this.tcpFlags = tcpFlags;
        this.window = (short)1;
        this.checksum = 0;
        this.urgentPointer = (short)0;
        this.data = data;
        this.length = DATA + data.length;
        this.sourceIP = tcb.tcb_our_ip_address;
        this.destinationIP = tcb.tcb_their_ip_address;
        this.checksum = computeChecksum();

    }

    public int getCheckSum(){
        return this.checksum;
    }

    public int length(){
        return this.length;
    }

    public void refresh_SEQ_ACK(TcpControlBlock tcb){
        this.sequenceNumber = tcb.tcb_our_sequence_number;
        this.ackNumber = tcb.tcb_their_sequence_num;
        this.checksum = 0;
        this.checksum = computeChecksum();
    }

    public short computeChecksum(){

        int total_length =  this.length;
        byte[] raw = new byte[total_length];
        this.toArray(raw, 0);
        ByteBuffer rawBuf = ByteBuffer.wrap(raw);


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
        buffer.put(HLEN, this.dataOffset);
        buffer.put(FLAGS, this.tcpFlags);
        buffer.putShort(WIN, this.window);
        buffer.putShort(CHECKSUM, this.checksum);
        buffer.putShort(URG, this.urgentPointer);

        System.arraycopy(this.data,0,dst,DATA+offset,this.data.length);
        System.arraycopy(buffer.array(), 0, dst, offset, this.length);
        if (this.data.length > 0){
            System.arraycopy(this.data, 0, dst, offset + DATA, this.data.length);
        }
    }

    public boolean hasValidChecksum(){
        boolean checksumIsValid = this.computeChecksum() == 0;
        if (!checksumIsValid){
            Log.i("IP: " + IP.IpAddress.htoa(Integer.reverseBytes(this.destinationIP)), "Invalid Checksum: " + this.computeChecksum());
        }
        return checksumIsValid;
    }

    public boolean isPreviousData(TcpControlBlock tcb){
        return  this.destinationPort == tcb.tcb_our_port
                && this.destinationIP == tcb.tcb_our_ip_address
                && this.ackNumber == tcb.tcb_our_expected_ack
                && this.sourceIP == tcb.tcb_their_ip_address
                && this.sourcePort == tcb.tcb_their_port
                && this.sequenceNumber == tcb.tcb_their_sequence_num - length
                &&(this.tcpFlags & util.ACK_BYTE)!=0;
    }

    public boolean isFIN(TcpControlBlock tcb){
        return this.destinationPort == tcb.tcb_our_port
                && this.destinationIP == tcb.tcb_our_ip_address
                && this.sourceIP == tcb.tcb_their_ip_address
                && this.sourcePort == tcb.tcb_their_port
                && this.sequenceNumber == tcb.tcb_their_sequence_num
                && (this.tcpFlags & util.FIN_BYTE) != 0;
    }

    public boolean isPreviousSYNACK(TcpControlBlock tcb){
        return this.destinationPort == tcb.tcb_our_port
                && this.destinationIP == tcb.tcb_our_ip_address
                && this.sourceIP == tcb.tcb_their_ip_address
                && this.sourcePort == tcb.tcb_their_port
                && this.sequenceNumber == tcb.tcb_their_sequence_num -1
                && (this.tcpFlags & util.SYN) != 0
                && (this.tcpFlags & util.ACK_BYTE) !=0;
    }

    public boolean isValid(TcpControlBlock tcb, int expectedFlags){

        boolean check = this.destinationPort == tcb.tcb_our_port
                && this.destinationIP == tcb.tcb_our_ip_address;

        switch (expectedFlags){
            case util.SYN:
                check = check
                        &&(this.tcpFlags & util.SYN_BYTE)!=0;
                break;

            case util.SYNACK:

                check = check
                        &&(this.tcpFlags & util.SYN_BYTE)!=0
                        &&(this.tcpFlags & util.ACK_BYTE)!=0
                        && this.ackNumber == tcb.tcb_our_expected_ack
                        && this.sourceIP == tcb.tcb_their_ip_address
                        && this.sourcePort == tcb.tcb_their_port;
                break;


            case util.DATA:

                check = check
                        && this.ackNumber == tcb.tcb_our_expected_ack
                        && this.sourceIP == tcb.tcb_their_ip_address
                        && this.sourcePort == tcb.tcb_their_port
                        && this.sequenceNumber == tcb.tcb_their_sequence_num
                        &&(this.tcpFlags & util.ACK_BYTE)!=0;

                break;




        }

        if(!check){
            Log.i("IP: " + IP.IpAddress.htoa(Integer.reverseBytes(tcb.tcb_our_ip_address)), "Invalid Segment: " + this.toString() +
                    " expected: seqNo: " + tcb.tcb_their_sequence_num + ", ackNo: " + tcb.tcb_our_expected_ack + ", flags: " + expectedFlags);
        }

        return check;


    }

    public String toString(){
//        return "sourcePort: " + IP.IpAddress.htoa(this.sourcePort) + " destinationPort: " + IP.IpAddress.htoa(this.destinationPort) +

        return "\n" +
                this.sourcePort + " " + this.destinationPort + "\n" +
                this.sequenceNumber + "\n" +
                this.ackNumber + "\n" +
                this.dataOffset + " " + this.tcpFlags + " " + this.window + "\n" +
                this.checksum + " " + this.urgentPointer + "\n" +
                new String(this.data);
    }
}
