package nl.vu.cs.cntest;

import junit.framework.TestCase;

import java.io.IOException;

import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.TCP;

/**
 * Created by nikos on 28-6-15.
 */
public class ReadWriteTest extends TestCase {

    public static int CLIENT_IP = 1;

    public static int CLIENT_2_IP = 2;

    public static int SERVER_IP = 10;

    public static int SERVER_PORT = 4444;

    public static int BYTES = 10;

    public static int HUGE_BYTES=30000;



    public void testReadWrite () throws InterruptedException{

        class Server implements Runnable {

            public byte[] buf;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();
                    buf= new byte[BYTES];
                    server.read(buf, 0, BYTES);
                } catch (IOException e){

                }
            }


        }

        class Client implements Runnable {
            public int bytesWritten;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    bytesWritten = client.write("1234512345".getBytes(),0,BYTES);
                } catch (IOException e) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(BYTES,client.bytesWritten);
        assertEquals("1234512345",new String(server.buf));

    }

    public void testWriteTwoSegments () throws InterruptedException {

        class Server implements Runnable {

            public byte[] buf;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();
                    buf= new byte[BYTES];
                    server.read(buf, 0, BYTES);
                } catch (IOException e){

                }
            }


        }

        class Client implements Runnable {
            public int bytesSeg1,bytesSeg2;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    bytesSeg1 = client.write("12345".getBytes(),0,BYTES/2);
                    bytesSeg2 = client.write("12345".getBytes(),0,BYTES/2);
                } catch (IOException e) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(BYTES,client.bytesSeg1+client.bytesSeg2);
        assertEquals("1234512345",new String(server.buf));
    }

    public void testReadBufferedData () throws InterruptedException{

        class Server implements Runnable {

            public byte[] buf;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();
                    buf= new byte[BYTES];
                    server.read(buf, 0, BYTES/2);
                    server.read(buf,BYTES/2,BYTES/2);
                } catch (IOException e){

                }
            }


        }

        class Client implements Runnable {
            public int bytesWritten;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    bytesWritten = client.write("1234512345".getBytes(),0,BYTES);
                } catch (IOException e) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(BYTES,client.bytesWritten);
        assertEquals("1234512345",new String(server.buf));

    }

    public void testReadMoreThanWritten () throws InterruptedException{

        class Server implements Runnable {

            public byte[] buf;
            public int bytesRead;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();
                    buf= new byte[BYTES];
                    bytesRead = server.read(buf, 0, BYTES*2);
                } catch (IOException e){

                }
            }


        }

        class Client implements Runnable {
            public int bytesWritten;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    bytesWritten = client.write("1234512345".getBytes(),0,BYTES);
                } catch (IOException e) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(BYTES, client.bytesWritten);
        assertEquals(BYTES,server.bytesRead);
        assertEquals("1234512345",new String(server.buf));

    }

    public void testReadWithoutConnection () throws InterruptedException{

        class Server implements Runnable {

            public byte[] buf;
            public int bytesRead;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);

                    buf= new byte[BYTES];
                    bytesRead = server.read(buf, 0, BYTES);
                } catch (IOException e){

                }
            }


        }


        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();

        serverThread.join();
        assertEquals(-1,server.bytesRead);

    }

    public void testWriteWithoutConnection () throws InterruptedException{

        class Client implements Runnable {
            public int bytesWritten;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    bytesWritten = client.write("1234512345".getBytes(),0,BYTES);
                } catch (IOException e) {

                }
            }
        }

        Client client = new Client();
        client.run();
        assertEquals(-1, client.bytesWritten);

    }

    public void testReadWriteHugeSegment () throws InterruptedException{

        class Server implements Runnable {

            public byte[] buf;
            public int bytesRead;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();
                    buf= new byte[HUGE_BYTES];
                    bytesRead = server.read(buf, 0, HUGE_BYTES);
                } catch (IOException e){

                }
            }


        }

        class Client implements Runnable {
            public int bytesWritten;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    bytesWritten = client.write(new byte[HUGE_BYTES],0,HUGE_BYTES);
                } catch (IOException e) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(HUGE_BYTES, client.bytesWritten);
        assertEquals(HUGE_BYTES,server.bytesRead);

    }

    public void testWriteAfterClose () throws InterruptedException{

        class Server implements Runnable {

            public byte[] buf;
            public int bytesRead;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();
                    buf= new byte[BYTES];
                    bytesRead = server.read(buf, 0, BYTES);
                } catch (IOException e){

                }
            }


        }

        class Client implements Runnable {
            public int bytesWritten;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    client.close();
                    bytesWritten = client.write("1234512345".getBytes(),0,BYTES);

                } catch (IOException e) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(-1,client.bytesWritten);
        assertEquals(0,server.bytesRead);

    }
    public void testReadAfterClose() throws InterruptedException{

        class Server implements Runnable {

            public byte[] buf;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();
                    buf= new byte[BYTES];
                    server.close();
                    server.read(buf, 0, BYTES);
                } catch (IOException e){

                }
            }


        }

        class Client implements Runnable {
            public int bytesWritten;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    bytesWritten = client.write("1234512345".getBytes(),0,BYTES);
                } catch (IOException e) {

                }
            }
        }
        Client client = new Client();
        Thread clientThread = new Thread(client);
        clientThread.start();
        Server server = new Server();
        server.run();
        clientThread.join();
        assertEquals(BYTES,client.bytesWritten);
        assertEquals("1234512345",new String(server.buf));

    }


}
