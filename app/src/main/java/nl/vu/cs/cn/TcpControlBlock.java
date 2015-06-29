package nl.vu.cs.cn;

/**
 * Created by nikos on 17-6-15.
 */


//class that contains all the connection information
public class TcpControlBlock {

    enum ConnectionState {
        CLOSED, ESTABLISHED,SYN_SENT, LISTEN, SYN_RCVD, FIN_WAIT1 ,
        FIN_WAIT_2, CLOSE_WAIT, LAST_ACK, CLOSING, TIME_WAIT

    }

    static int TCB_BUF_SIZE = 8192;

    int             tcb_our_ip_address;
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

    public void clear() {
        tcb_our_ip_address = 0;
        tcb_our_sequence_number = 0;
        tcb_our_expected_ack = 0;
        tcb_their_ip_address = 0;
        tcb_their_port = 0;
        tcb_our_port = 0;
        tcb_their_sequence_num = 0;
        tcb_data_left = 0;
    }

}
