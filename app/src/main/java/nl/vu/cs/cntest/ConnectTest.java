package nl.vu.cs.cntest;

import junit.framework.TestCase;

import java.io.IOException;

import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.TCP;

/**
 * Created by nikos on 28-6-15.
 */
public class ConnectTest extends TestCase {

    public static int CLIENT_IP = 1;

    public static int CLIENT_2_IP = 2;

    public static int SERVER_IP = 10;

    public static int SERVER_PORT = 4444;

    public class Server implements Runnable {

        TCP.Socket server;
        public void run() {


            server.accept();
        }



        public Server(int ip,int port) throws IOException {
            this.server = new TCP(ip).socket(port);
        }
    }

    public class Client implements Runnable {
        TCP.Socket client;
        public boolean connectSuccess;
        int serverIP, serverPort;

        public void run() {
            IP.IpAddress serverAddress = IP.IpAddress.getAddress("192.168.0." + serverIP);
            connectSuccess = client.connect(serverAddress, serverPort);
        }



        public Client(int ip, int serverIP, int serverPort) throws IOException {
            this.client = new TCP(ip).socket();
            this.serverIP = serverIP;
            this.serverPort = serverPort;
        }
    }




    public void testConnect() throws IOException, InterruptedException{



        Server serverTask = new Server(SERVER_IP,SERVER_PORT);
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
        Client clientTask = new Client(CLIENT_IP,SERVER_IP,SERVER_PORT);
        clientTask.run();
        serverThread.join();

        assertEquals(true, clientTask.connectSuccess);


    }

    public void testConnectToNonExistentServer() throws  IOException{

        Client clientTask = new Client(CLIENT_IP,SERVER_IP,SERVER_PORT);
        clientTask.run();

        assertEquals(false,clientTask.connectSuccess);

    }

    public void testConnectToBoundServer() throws IOException ,InterruptedException {

        Server serverTask = new Server(SERVER_IP,SERVER_PORT);
        Client clientTask1  = new Client(CLIENT_IP,SERVER_IP,SERVER_PORT);
        Client clientTask2 = new Client(CLIENT_2_IP,SERVER_IP,SERVER_PORT);

        Thread serverThread = new Thread(serverTask);
        serverThread.start();

        clientTask1.run();
        clientTask2.run();

        serverThread.join();

        assertEquals(false, clientTask2.connectSuccess);
        assertEquals(true,clientTask1.connectSuccess);


    }

    public void testConnectToWrongPort () throws IOException , InterruptedException {

        Server serverTask = new Server(SERVER_IP,SERVER_PORT);
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
        Client clientTask = new Client(CLIENT_IP,SERVER_IP,SERVER_PORT+1);
        clientTask.run();
        Client clientTask2 = new Client(CLIENT_IP,SERVER_IP,SERVER_PORT);
        clientTask2.run();
        serverThread.join();

        assertEquals(false, clientTask.connectSuccess);
        assertEquals(true,clientTask2.connectSuccess);
    }







}
