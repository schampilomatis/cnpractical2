package nl.vu.cs.cn;


import android.util.Log;

import java.io.IOException;
import java.util.Random;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.util.util;
/**
 * This class represents a TCP stack. It should be built on top of the IP stack
 * which is bound to a given IP address.
 */
public class TCP {

	/** The underlying IP stack for this TCP stack. */
    private IP ip;

    /**
     * This class represents a TCP socket.
     *
     */
    public class Socket {



    	/* Hint: You probably need some socket specific data. */

    	/**
    	 * Construct a client socket.
    	 */

        private TcpControlBlock tcb;

    	private Socket(short port) {

            tcb = new TcpControlBlock();
            tcb.tcb_our_ip_address = Integer.reverseBytes(ip.getLocalAddress().getAddress());
            tcb.tcb_our_port = port;
            tcb.tcb_state = TcpControlBlock.ConnectionState.CLOSED;


    	}


		/**
         * Connect this socket to the specified destination and port.
         *
         * @param dst the destination to connect to
         * @param port the port to connect to
         * @return true if the connect succeeded.
         */
        public boolean connect(IpAddress dst, short port) {

            // Implement the connection side of the three-way handshake here.

            if (tcb.tcb_state != TcpControlBlock.ConnectionState.CLOSED){
                return false;
            }



            TCPSegment synack = sendSYN(dst, port);

            if (synack != null){
                if (sendACK(synack)){
                    tcb.tcb_state = TcpControlBlock.ConnectionState.ESTABLISHED;
                    return true;
                };

                return false;
            }

            return false;
        }


        /**
         * Accept a connection on this socket.
         * This call blocks until a connection is made.
         */
        public void accept() {

            // Implement the receive side of the three-way handshake here.
            Log.i("IP: " + Integer.reverseBytes(tcb.tcb_our_ip_address) , "ACCEPTING CONNECTIONS");

            tcb.tcb_state = TcpControlBlock.ConnectionState.LISTEN;
            try {
                while(true) {

                    TCPSegment syn = receiveSegment(0);

                    if (syn.isValid(tcb, util.SYN)) {

                        tcb.tcb_state = TcpControlBlock.ConnectionState.SYN_RCVD;
                        tcb.tcb_their_ip_address = syn.sourceIP;
                        tcb.tcb_their_port = syn.sourcePort;
                        tcb.tcb_our_sequence_number = new Random().nextInt();
                        tcb.tcb_their_sequence_num = syn.sequenceNumber + 1;
                        tcb.tcb_our_expected_ack = tcb.tcb_our_sequence_number + 1;
                        TCPSegment synack = new TCPSegment(tcb, util.SYNACK, new byte[0]);
                        TCPSegment ack = send(synack, util.DATA);
                        if (ack != null) {
                            tcb.tcb_state = TcpControlBlock.ConnectionState.ESTABLISHED;
                            tcb.tcb_our_sequence_number ++;
                            break;
                        }
                    }

                }



            }catch(Exception e){
                Log.i("socket error", e.toString());
            }
        }

        /**
         * Reads bytes from the socket into the buffer.
         * This call is not required to return maxlen bytes
         * every time it returns.
         *
         * @param buf the buffer to read into
         * @param offset the offset to begin reading data into
         * @param maxlen the maximum number of bytes to read
         * @return the number of bytes read, or -1 if an error occurs.
         */
        public int read(byte[] buf, int offset, int maxlen) {

            if (tcb.tcb_state != TcpControlBlock.ConnectionState.ESTABLISHED
                    && tcb.tcb_state != TcpControlBlock.ConnectionState.READ_ONLY){
                return -1;
            }
            int start = offset;
            int dataleft = maxlen;
            int dataFromBuffer = readFromBuffer(buf, offset, maxlen);
            start += dataFromBuffer;
            dataleft -= dataFromBuffer;

            while (dataleft > 0){
                TCPSegment dataSgmt = receiveDataSegment();
                if (dataSgmt != null) {
                    if (dataSgmt.data.length > dataleft) {
                        System.arraycopy(dataSgmt.data, 0, buf, start, dataleft);
                        byte[] receivedBuf = new byte[dataSgmt.data.length - dataleft];
                        System.arraycopy(dataSgmt.data, dataleft, receivedBuf, 0, dataSgmt.data.length - dataleft );
                        tcb.tcb_received_data = receivedBuf;
                        dataleft = 0;

                    }else{
                        int readBytesLen = dataSgmt.data.length;
                        System.arraycopy(dataSgmt.data, 0, buf, start, readBytesLen);
                        dataleft -= readBytesLen;
                        start += readBytesLen;
                    }
                }else{
                    return maxlen - dataleft;
                }
            }

            return maxlen;


        }

        private int readFromBuffer(byte[] buf, int offset, int maxlen){

            int undeliveredDataLength = tcb.tcb_received_data.length;
            if (undeliveredDataLength > 0){
                int dataFromBuffer = undeliveredDataLength;
                if (undeliveredDataLength > maxlen){
                    dataFromBuffer = maxlen;
                }
                System.arraycopy( tcb.tcb_received_data, 0, buf, offset, dataFromBuffer);
                int newReceivedBufLen = undeliveredDataLength - dataFromBuffer;
                byte[] newReceivedBuf = new byte[newReceivedBufLen];
                if (newReceivedBufLen > 0){
                    System.arraycopy(tcb.tcb_received_data, dataFromBuffer, newReceivedBuf, 0, newReceivedBufLen);
                }
                tcb.tcb_received_data = newReceivedBuf;
                return dataFromBuffer;


            }else{
                return 0;
            }
        }

        /**
         * Writes to the socket from the buffer.
         *
         * @param buf the buffer to
         * @param offset the offset to begin writing data from
         * @param len the number of bytes to write
         * @return the number of bytes written or -1 if an error occurs.
         */
        public int write(byte[] buf, int offset, int len) {

            if (tcb.tcb_state != TcpControlBlock.ConnectionState.ESTABLISHED
                && tcb.tcb_state != TcpControlBlock.ConnectionState.WRITE_ONLY){
                return -1;
            }
            tcb.tcb_data_left = len;
            int start = offset;
            while (tcb.tcb_data_left > 0){
                int sgmt_len;
                if (tcb.tcb_data_left > util.MAX_DATA_LEN){
                    sgmt_len = util.MAX_DATA_LEN;
                }else{
                    sgmt_len = tcb.tcb_data_left;
                }

                byte[] sgmt_data = new byte[sgmt_len];
                System.arraycopy(buf, start, sgmt_data, 0, sgmt_len);
                tcb.tcb_our_expected_ack += sgmt_len;
                TCPSegment sgmt = new TCPSegment(tcb, util.DATA, sgmt_data);
                TCPSegment ack = send(sgmt, util.DATA);

                if (ack != null){
                    tcb.tcb_our_sequence_number = tcb.tcb_our_expected_ack;
                    start = start + sgmt_len;
                    tcb.tcb_data_left = tcb.tcb_data_left - sgmt_len;
                }else{
                    return start - offset;
                }




            }
            return start - offset;

        }

        /**
         * Closes the connection for this socket.
         * Blocks until the connection is closed.
         *
         * @return true unless no connection was open.
         */
        public boolean close() {

            if (tcb.tcb_state == TcpControlBlock.ConnectionState.ESTABLISHED){

                TCPSegment ack = sendFIN();
//                while (ack == null){
//                    ack = sendFIN();
//                }

                tcb.tcb_state = TcpControlBlock.ConnectionState.READ_ONLY;
                tcb.tcb_our_sequence_number++;

                return true;


            }else if (tcb.tcb_state == TcpControlBlock.ConnectionState.WRITE_ONLY){
                TCPSegment ack = sendFIN();
//                while (ack == null){
//                    ack = sendFIN();
//                }

                tcb.tcb_our_sequence_number++;

                terminate();
                return true;

            }

            return false;
        }

        private TCPSegment sendSYN(IP.IpAddress dst, short port){
            tcb.tcb_their_ip_address = Integer.reverseBytes(dst.getAddress());
            tcb.tcb_their_port = port;
            tcb.tcb_state = TcpControlBlock.ConnectionState.SYN_SENT;
            int initialSeqNumber = new Random().nextInt();
            tcb.tcb_our_sequence_number = initialSeqNumber;
            tcb.tcb_their_sequence_num = 0;
            tcb.tcb_our_expected_ack = initialSeqNumber + 1;
            TCPSegment syn = new TCPSegment(tcb, util.SYN , new byte[0]);

            Log.i("IP: " + Integer.reverseBytes(tcb.tcb_our_ip_address), "CONNECTING TO IP: " + Integer.reverseBytes(tcb.tcb_their_ip_address));

            TCPSegment synack = send(syn, util.SYNACK);

            if (synack != null){
                tcb.tcb_state = TcpControlBlock.ConnectionState.SYN_RCVD;
                tcb.tcb_our_sequence_number ++;
                tcb.tcb_their_sequence_num = synack.sequenceNumber ;
            }

            return synack;

        }


        private boolean sendACK(TCPSegment sgmt){
            int offset = sgmt.data.length == 0 ? 1: sgmt.data.length;
            tcb.tcb_their_sequence_num = sgmt.sequenceNumber + offset;
            TCPSegment ack = new TCPSegment(tcb, util.DATA, new byte[0]);
            try {
                sendSegment(ack , tcb);
                return true;
            }catch (Exception e){
                return false;
            }


        }

        private TCPSegment sendFIN(){
            TCPSegment fin = new TCPSegment(tcb, util.FIN, new byte[0]);
            try{
                TCPSegment ack =  send(fin, util.DATA);
                return ack;
            }catch(Exception e){
                return null;
            }
        }

        private void receivedFIN(TCPSegment fin){
            if (tcb.tcb_state == TcpControlBlock.ConnectionState.ESTABLISHED) {
                tcb.tcb_state = TcpControlBlock.ConnectionState.WRITE_ONLY;
                sendACK(fin);
            }else if (tcb.tcb_state == TcpControlBlock.ConnectionState.READ_ONLY){
                sendACK(fin);

                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                terminate();
                            }
                        },
                        5000
                );

            }
        }



        private TCPSegment send(TCPSegment segment, int expectedFlags){

            int attempts = 0;

            while (attempts < util.MAX_ATTEMPTS) {
                try {

                    sendSegment(segment, tcb);
                    try {

                        TCPSegment receivedSegment = receiveSegment(util.TIMEOUT);

                        if (receivedSegment.hasValidChecksum() && receivedSegment.isValid(tcb, expectedFlags)){
                            return receivedSegment;
                        }else{
                            attempts++;
                        }

                    }catch(Exception e){
                        attempts++;
                    }

                } catch (IOException e) {
                    attempts++;
                }
            }

            return null;
        }

        private TCPSegment receiveDataSegment(){
            int attempts = 0;
            while (attempts < util.MAX_ATTEMPTS) {
                try {
                    TCPSegment datasgmt = receiveSegment(util.TIMEOUT);
                    if (datasgmt.hasValidChecksum()){

                        if (datasgmt.isValid(tcb, util.DATA)){
                            sendACK(datasgmt);
                            return datasgmt;
                        }else if(datasgmt.isPreviousData(tcb)){
                            sendACK(datasgmt);
                            attempts ++;
                        }else if(datasgmt.isPreviousSYNACK(tcb)){
                            sendACK(datasgmt);
                            attempts++;
                        }else if(datasgmt.isFIN(tcb)){
                            receivedFIN(datasgmt);
                            attempts++;
                        };


                    };
                }catch (Exception e){
                    attempts++;
                }

            }
            return null;
        }

        private void terminate(){
            tcb.tcb_state = TcpControlBlock.ConnectionState.CLOSED;
            tcb.clear();
        }

    }


    private int sendSegment(TCPSegment tcpSeg,TcpControlBlock tcb) throws IOException{

        Log.i("IP: " + IP.IpAddress.htoa(Integer.reverseBytes(tcb.tcb_our_ip_address)), "send segment: " + tcpSeg.toString());
        int dstAddress = Integer.reverseBytes(tcb.tcb_their_ip_address);
        int packetID = new Random().nextInt();
        byte[] data = new byte[tcpSeg.length()];
        tcpSeg.toArray(data, 0);
        IP.Packet pck = new IP.Packet(dstAddress,IP. TCP_PROTOCOL, packetID, data, tcpSeg.length());


        return ip.ip_send(pck);

    }

    private TCPSegment receiveSegment(int timeout) throws IOException , InterruptedException{

        IP.Packet pck = new IP.Packet();
        ip.ip_receive_timeout(pck, timeout);
        TCPSegment result = new TCPSegment(pck);
        Log.i("IP: " + IP.IpAddress.htoa(pck.destination) , "receive segment: " + result.toString());
        return new TCPSegment(pck);

    }





    /**
     * Constructs a TCP stack for the given virtual address.
     * The virtual address for this TCP stack is then
     * 192.168.0.address.
     *Log.i("IP: " + Integer.reverseBytes(tcb.tcb_our_ip_address) , "1");
     * @param address The last octet of the virtual IP address 1-254.
     * @throws IOException if the IP stack fails to initialize.
     */
    public TCP(int address) throws IOException {
        ip = new IP(address);
    }

    /**
     * @return a new socket for this stack
     */
    public Socket socket() {
        int port = new Random().nextInt(util.MAX_PORT - util.MIN_PORT) + 1024;
        return new Socket((short)port);
    }

    /**
     * @return a new server socket for this stack bound to the given port
     * @param port the port to bind the socket to.
     */
    public Socket socket(int port) {
        return new Socket((short)port);
    }



}
