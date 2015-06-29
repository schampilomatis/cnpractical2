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
        //contains all the information for the socket connection
        private TcpControlBlock tcb;

    	private Socket(short port) {

            tcb = new TcpControlBlock();

            // Initializing tcb with our values
            tcb.tcb_our_ip_address = Integer.reverseBytes(ip.getLocalAddress().getAddress());
            tcb.tcb_our_port = port;
            tcb.tcb_state = TcpControlBlock.ConnectionState.CLOSED;


    	}


		/**
         * Connect this socket to the specified destination and port.
         *
         * @param dst the destination to connect to
         * @param portInt the port to connect to
         * @return true if the connect succeeded.
         */
        public boolean connect(IpAddress dst, int portInt) {


            short port = (short)portInt;

            //Cannot connect from a non-Closed socket
            if (tcb.tcb_state != TcpControlBlock.ConnectionState.CLOSED){
                return false;
            }


            //send SYN - expect SYNACK
            TCPSegment synack = sendSYN(dst, port);

            //synack = null means that sending SYN failed 10 times
            if (synack != null){
                if (sendACK(synack)){
                    tcb.tcb_state = TcpControlBlock.ConnectionState.ESTABLISHED;
                    return true;
                }

                return false;
            }

            return false;
        }


        /**
         * Accept a connection on this socket.
         * This call blocks until a connection is made.
         */
        public void accept() {


            Log.i("IP: " + IP.IpAddress.htoa(Integer.reverseBytes(tcb.tcb_our_ip_address)) , "ACCEPTING CONNECTIONS");

            tcb.tcb_state = TcpControlBlock.ConnectionState.LISTEN;
            try {
                while(true) {
                    //receive a segment without timeout(0) blocking the socket
                    TCPSegment syn = receiveSegment(0);

                    if (syn.isValid(tcb, util.SYN)) {

                        tcb.tcb_state = TcpControlBlock.ConnectionState.SYN_RCVD;

                        //Initializing the values of tcb with those found in the SYN segment
                        tcb.tcb_their_ip_address = syn.sourceIP;
                        tcb.tcb_their_port = syn.sourcePort;
                        tcb.tcb_their_sequence_num = syn.sequenceNumber + 1;




                        //Random int for initial sequence number
                        tcb.tcb_our_sequence_number = new Random().nextInt();
                        tcb.tcb_our_expected_ack = tcb.tcb_our_sequence_number + 1;

                        //send SYNACK segment expecting for an ACK (util.DATA flags)
                        TCPSegment synack = new TCPSegment(tcb, util.SYNACK, new byte[0]);
                        send(synack, util.DATA);


                        tcb.tcb_state = TcpControlBlock.ConnectionState.ESTABLISHED;

                        //increase seq number by one. SYNACK length counts as 1 althought there are no data
                        tcb.tcb_our_sequence_number ++;
                        break;



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

            //Can only read from ESTABLISHED, FIN_WAIT1, FIN_WAIT_2
            if (tcb.tcb_state != TcpControlBlock.ConnectionState.ESTABLISHED
                    && tcb.tcb_state != TcpControlBlock.ConnectionState.FIN_WAIT1
                    && tcb.tcb_state != TcpControlBlock.ConnectionState.FIN_WAIT_2){
                return -1;
            }

            //initializing the start(where to store next) and dataleft(how many more bytes) values
            int start = offset;
            int dataleft = maxlen;

            //moving unread bytes from previous calls of read
            int dataFromBuffer = readFromBuffer(buf, offset, maxlen);
            start += dataFromBuffer;
            dataleft -= dataFromBuffer;


            while (dataleft > 0){

                //receive one segment (10 tries)
                TCPSegment dataSgmt = receiveDataSegment();
                if (dataSgmt != null) {
                    if (dataSgmt.data.length > dataleft) {

                        //extra bytes are copied to the buffer for next calls of read
                        System.arraycopy(dataSgmt.data, 0, buf, start, dataleft);
                        byte[] receivedBuf = new byte[dataSgmt.data.length - dataleft];
                        System.arraycopy(dataSgmt.data, dataleft, receivedBuf, 0, dataSgmt.data.length - dataleft );

                        //the buffer reference is kept inside tcb
                        tcb.tcb_received_data = receivedBuf;
                        dataleft = 0;

                    }else{
                        int readBytesLen = dataSgmt.data.length;
                        System.arraycopy(dataSgmt.data, 0, buf, start, readBytesLen);
                        dataleft -= readBytesLen;
                        start += readBytesLen;
                    }
                }else{// return after 10 fails of receiveDataSegment
                    return maxlen - dataleft;
                }
            }

            return maxlen;


        }



//      function that:
//      transfers the previous read extra bytes to the buf
//      decreases the size of the buffer or it reinitializes it removing the bytes transfered
//      returns the number of bytes transfered
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


//            return -1 in case of wrong state
            if (tcb.tcb_state != TcpControlBlock.ConnectionState.ESTABLISHED
                && tcb.tcb_state != TcpControlBlock.ConnectionState.CLOSE_WAIT){
                return -1;
            }

            tcb.tcb_data_left = len;
            int start = offset;
            while (tcb.tcb_data_left > 0){
                int sgmt_len;

                //the length of a the segment data is the minimum of max data for a segment and the
                //data left to send
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

            // close() called first on our socket
            if (tcb.tcb_state == TcpControlBlock.ConnectionState.ESTABLISHED){
                tcb.tcb_our_expected_ack ++;// increase expected ack by 1 because of FIN
                tcb.tcb_state = TcpControlBlock.ConnectionState.FIN_WAIT1;
                sendFIN(); //send fin 10 times until acknowledged


                if (tcb.tcb_state == TcpControlBlock.ConnectionState.FIN_WAIT1) {
                    tcb.tcb_state = TcpControlBlock.ConnectionState.FIN_WAIT_2;
                    tcb.tcb_our_sequence_number++; //increase our sequence number because of the FIN sent
                }


                //FIN received while waiting for our FIN acknowledgement(simultaneous close)
                else if (tcb.tcb_state == TcpControlBlock.ConnectionState.CLOSING){
                    tcb.tcb_state = TcpControlBlock.ConnectionState.TIME_WAIT;

                    //terminate the connection after TIME_WAIT ms
                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    terminate();
                                }
                            },
                            util.TIME_WAIT
                    );
                }
                return true;


            }

            //close() called first on their socket
            else if (tcb.tcb_state == TcpControlBlock.ConnectionState.CLOSE_WAIT){
                tcb.tcb_our_expected_ack ++;
                sendFIN();
                tcb.tcb_state = TcpControlBlock.ConnectionState.LAST_ACK;
                tcb.tcb_our_sequence_number++;

                //terminate without waiting
                terminate();
                return true;

            }



            return false;
        }


//      function that:
//      sends SYN to an IP address, port
//      retuns SYNACK


        private TCPSegment sendSYN(IP.IpAddress dst, short port){


            tcb.tcb_state = TcpControlBlock.ConnectionState.SYN_SENT;

            // inintialize tcb values
            tcb.tcb_their_ip_address = Integer.reverseBytes(dst.getAddress());
            tcb.tcb_their_port = port;
            tcb.tcb_their_sequence_num = 0;

            //random initial seq number
            int initialSeqNumber = new Random().nextInt();
            tcb.tcb_our_sequence_number = initialSeqNumber;

            //expected ack = seq number + 1 because of SYN segment
            tcb.tcb_our_expected_ack = initialSeqNumber + 1;
            TCPSegment syn = new TCPSegment(tcb, util.SYN , new byte[0]);

            Log.i("IP: " + Integer.reverseBytes(tcb.tcb_our_ip_address), "CONNECTING TO IP: " + Integer.reverseBytes(tcb.tcb_their_ip_address));

            //send SYN 10 times until we receive SYNACK
            TCPSegment synack = send(syn, util.SYNACK);

            if (synack != null){

                tcb.tcb_state = TcpControlBlock.ConnectionState.SYN_RCVD;
                tcb.tcb_our_sequence_number ++;
                //initialize their sequence number
                tcb.tcb_their_sequence_num = synack.sequenceNumber ;
            }

            return synack;

        }

//        function that sends ACK for a given segment
//        returns true for successful sending
//        sets their sequence number
        private boolean sendACK(TCPSegment sgmt){

//          the increase in sequence number is equal to data.length but is equal to 1 for SYNACK and FIN segments
            int offset;
            int datalength = sgmt.data.length;
            if (sgmt.data.length == 0){
                offset = 1;
            }else{
                offset = datalength;
            }

//          increase sequence number or set it back to the same number if the ack was for a previous segment
            tcb.tcb_their_sequence_num = sgmt.sequenceNumber + offset;

//          create and send ACK
            TCPSegment ack = new TCPSegment(tcb, util.DATA, new byte[0]);
            try {
                sendSegment(ack , tcb);
                return true;
            }catch (Exception e){
                return false;
            }


        }


//        Function that sends FIN (up to 10 times) and expects ACK
        private TCPSegment sendFIN(){
            TCPSegment fin = new TCPSegment(tcb, util.FIN, new byte[0]);
            try{
                return  send(fin, util.DATA);
            }catch(Exception e){
                return null;
            }
        }


//        Function called whenever FIN is received
        private void receivedFIN(TCPSegment fin){

//            Received fin in ESTABLISHED(first one to receive)
            if (tcb.tcb_state == TcpControlBlock.ConnectionState.ESTABLISHED) {
                tcb.tcb_state = TcpControlBlock.ConnectionState.CLOSE_WAIT;
                Log.i("IP: " + IP.IpAddress.htoa(Integer.reverseBytes(tcb.tcb_our_ip_address)) , "Received FIN in ESTABLISHED sending ack");
                sendACK(fin);
            }

//            Received fin in FIN_WAIT_2 (second one to receive)
            else if (tcb.tcb_state == TcpControlBlock.ConnectionState.FIN_WAIT_2){
                Log.i("IP: " + IP.IpAddress.htoa(Integer.reverseBytes(tcb.tcb_our_ip_address)), "Received FIN in READ ONLY sending ack");

                sendACK(fin);
                tcb.tcb_state = TcpControlBlock.ConnectionState.TIME_WAIT;

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
//            Received fin in FIN_WAIT_1 (simultaneous close)
            else if (tcb.tcb_state == TcpControlBlock.ConnectionState.FIN_WAIT1) {

                //increasing our sequence number here in order to send the correct sequence number in the ACK segment
                tcb.tcb_our_sequence_number ++;

                //FINACK received
                if ((fin.tcpFlags & util.ACK_BYTE) !=0){
                    tcb.tcb_state = TcpControlBlock.ConnectionState.TIME_WAIT;
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
                sendACK(fin);
                tcb.tcb_state = TcpControlBlock.ConnectionState.CLOSING;

            }
        }





//        Function that sends a segment expecting a response(it can be either ACK or SYNACK)
//        Up to 10 attempts
//        handles receiving previous segments
//        returns the response
        private TCPSegment send(TCPSegment segment, int expectedFlags){

            int attempts = 0;
            //variable that determines when the segment has to be resent
            //the segment is resent in case of invalid ckecksum or no response
            boolean resend = true;
            while (attempts < util.MAX_ATTEMPTS) {
                try {
                    if (!tcb.ackReceivedfromRead) {

                        if (resend){
                            //send the Segment
                            sendSegment(segment, tcb);
                            resend = false;

                        }

                        try {

                            TCPSegment receivedSegment = receiveSegment(util.TIMEOUT);
                            if (receivedSegment.hasValidChecksum()) { //check checksum
                                if (receivedSegment.isFIN(tcb)) { //received fin instead of ACK/SYNACK
                                    receivedFIN(receivedSegment);
                                }else if (receivedSegment.isValid(tcb, expectedFlags)) { //valid response
                                    return receivedSegment;
                                }else if (isPreviousNonSYN(receivedSegment)){ //previous non SYN segment
                                    sendACK(receivedSegment);
                                }else if (receivedSegment.isPreviousSYN(tcb)){
                                    resend = true; //the only way to be here is when sending SYNACK
                                    attempts++;
                                }
                            }else{

                                resend = true;
                                attempts++;
                            }

                        } catch (Exception e) {

                            //have to refresh the segment after each resend because we might have received a FIN
                            segment.refresh_SEQ_ACK(tcb);
                            segment.refresh_SEQ_ACK(tcb);
                            resend = true;
                            attempts++;
                        }
                    }
                    else{
                        // we might receive ack for a segment sent in READ if we simultaneously read.
                        // in that case we return a plain ACK.
                        tcb.ackReceivedfromRead = false;
                        return new TCPSegment(tcb, util.DATA, new byte[0]);
                    }

                } catch (IOException e) {
                    attempts++;
                }
            }

            return null;
        }

//        function that receives a Data segment
//        returns the segment received
//        handles receiving past segments
//        sends acknowledgements for the segments received
//        receives a segment with maximum 10 failures

        private TCPSegment receiveDataSegment(){

            int attempts = 0;
            while (attempts < util.MAX_ATTEMPTS
                    &&(tcb.tcb_state== TcpControlBlock.ConnectionState.FIN_WAIT1
                    || (tcb.tcb_state == TcpControlBlock.ConnectionState.FIN_WAIT_2)
                    || tcb.tcb_state == TcpControlBlock.ConnectionState.ESTABLISHED)) {
                try {
                    TCPSegment datasgmt = receiveSegment(util.TIMEOUT);
                    if (datasgmt.hasValidChecksum()){ //validate checksum

                        if (datasgmt.isFIN(tcb)){ //FIN instead of ACK
                            receivedFIN(datasgmt);
                            attempts++;
                        }else if(datasgmt.isValid(tcb, util.DATA)){
                            if (datasgmt.data.length == 0){ // if data length is 0 we assume that we received an expected ACK
                                tcb.ackReceivedfromRead = true;
                                attempts++;
                            }else { //Valid Segment
                                sendACK(datasgmt);
                                return datasgmt;
                            }
                        }else if (isPreviousNonSYN(datasgmt)){//previous segment received
                            sendACK(datasgmt);
                            attempts++;
                        }


                    };
                }catch (Exception e){
                    attempts++;
                }

            }
            return null;
        }


//      Function that checks for previous segments(SYN has to be handled seperately)
        private boolean isPreviousNonSYN(TCPSegment sgmt){
            return sgmt.isPreviousData(tcb)
                || (sgmt.isPreviousSYNACK(tcb))
                || (sgmt.isPreviousFin(tcb));
        }
//      Terminate the connection
        private void terminate(){
            tcb.tcb_state = TcpControlBlock.ConnectionState.CLOSED;
            tcb.clear();
        }



    }

//  Convert a TCPSegment into IP packet and send
    private int sendSegment(TCPSegment tcpSeg,TcpControlBlock tcb) throws IOException{

        Log.i("IP: " + IP.IpAddress.htoa(Integer.reverseBytes(tcb.tcb_our_ip_address)), "send segment: " + tcpSeg.toString());
        int dstAddress = Integer.reverseBytes(tcb.tcb_their_ip_address);
        int packetID = new Random().nextInt();
        byte[] data = new byte[tcpSeg.length];
        tcpSeg.toArray(data, 0);
        IP.Packet pck = new IP.Packet(dstAddress,IP. TCP_PROTOCOL, packetID, data, tcpSeg.length);


        return ip.ip_send(pck);

    }


//  Receive an IP packet and convert to TCP Segment
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
        int port = Math.abs(new Random().nextInt(util.MAX_PORT - util.MIN_PORT)) + 1024;
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
