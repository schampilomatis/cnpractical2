package nl.vu.cs.cntest;

import junit.framework.TestCase;

import java.io.IOException;

import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.TCP;

/**
 * Created by nikos on 28-6-15.
 */
public class CloseTest extends TestCase {

    public static int CLIENT_IP = 1;

    public static int CLIENT_2_IP = 2;

    public static int SERVER_IP = 10;

    public static int SERVER_PORT = 4444;

    public static int BYTES = 10;

    public static int HUGE_BYTES=30000;



    public void testClose() throws InterruptedException{

        class Server implements Runnable {

            public boolean closed;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();

                    closed = server.close();
                } catch (IOException ignored){

                }
            }


        }

        class Client implements Runnable {
            public boolean closed;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    closed = client.close();
                } catch (IOException ignored) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(true, client.closed);
        assertEquals(true, server.closed);

    }

    public void testCloseUnopenedConnection() throws InterruptedException{

        class Server implements Runnable {

            public boolean closed;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);

                    closed = server.close();
                } catch (IOException ignored){

                }
            }


        }

        class Client implements Runnable {
            public boolean closed;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    closed = client.close();
                } catch (IOException ignored) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(false, client.closed);
        assertEquals(false, server.closed);

    }

    public void testCloseClosedConnection() throws InterruptedException{

        class Server implements Runnable {

            public boolean closed;
            public boolean closed2;
            public void run(){
                try {
                    TCP.Socket server = new TCP(SERVER_IP).socket(SERVER_PORT);
                    server.accept();

                    closed = server.close();
                    closed2 = server.close();
                } catch (IOException ignored){

                }
            }


        }

        class Client implements Runnable {
            public boolean closed;
            public boolean closed2;
            public void run() {
                try {
                    TCP.Socket client = new TCP(CLIENT_IP).socket();
                    IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + SERVER_IP);
                    client.connect(serverAddress, SERVER_PORT);
                    closed = client.close();
                    closed2 = client.close();
                } catch (IOException ignored) {

                }
            }
        }
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        Client client = new Client();
        client.run();
        serverThread.join();
        assertEquals(true, client.closed);
        assertEquals(true, server.closed);
        assertEquals(false, client.closed2);
        assertEquals(false, server.closed2);

    }


}
