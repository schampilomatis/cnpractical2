package nl.vu.cs.cn;

import java.util.Random;

/**
 * Created by nikos on 17-6-15.
 */



public class TcpControlBlock {

    enum ConnectionState {
        CLOSED, ESTABLISHED,SYN_SENT, LISTEN, SYN_RCVD, FIN_WAIT1 ,
        FIN_WAIT_2, CLOSE_WAIT, LAST_ACK, CLOSING, TIME_WAIT

    }

    static int TCB_BUF_SIZE = 8192;

    public int             tcb_our_ip_address;
    int             tcb_their_ip_address;
    short           tcb_our_port;
    short           tcb_their_port;
    int             tcb_our_sequence_number;
    int             tcb_our_expected_ack;
    int             tcb_their_sequence_num;
    byte[]          tcb_received_data = new byte[0];
    int             tcb_data_left;
    boolean         ackReceivedfromRead = false;
    ConnectionState tcb_state;

    public void clear(){
        tcb_our_ip_address = 0;
        tcb_our_sequence_number = 0;
        tcb_our_expected_ack = 0;
        tcb_their_ip_address = 0;
        tcb_their_port = 0;
        tcb_our_port = 0;
        tcb_their_sequence_num = 0;
        tcb_data_left = 0;
    }
    public void randomTCB(){

        Random rnd = new Random();
        tcb_our_ip_address = rnd.nextInt();
        tcb_our_sequence_number = rnd.nextInt();
        tcb_our_expected_ack = rnd.nextInt();
        tcb_their_ip_address = tcb_our_ip_address;
        tcb_their_port = (short)rnd.nextInt(65535);
        tcb_our_port = tcb_their_port;
        tcb_their_sequence_num = rnd.nextInt();
        tcb_data_left = 0;
    }

}
