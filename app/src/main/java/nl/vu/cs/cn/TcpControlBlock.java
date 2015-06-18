package nl.vu.cs.cn;

/**
 * Created by nikos on 17-6-15.
 */



public class TcpControlBlock {

    enum ConnectionState {
        CLOSED, READ_ONLY, WRITE_ONLY, ESTABLISHED,SYN_SENT, LISTEN, SYN_RCVD
    }

    static int TCB_BUF_SIZE = 8192;

    int             tcb_our_ip_address;
    int             tcb_their_ip_address;
    short           tcb_our_port;
    short           tcb_their_port;
    int             tcb_our_sequence_number;
    int             tcb_our_expected_ack;
    int             tcb_their_sequence_num;
    byte[]          tcb_data = new byte[TCB_BUF_SIZE];
    byte            tcb_p_data;
    int             tcb_data_left;
    ConnectionState tcb_state;

}
