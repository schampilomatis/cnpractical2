package nl.vu.cs.cn;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class Chat extends Activity {


	class Server implements Runnable{
		public void run(){
			try {
				TCP tcp = new TCP(30);
				TCP.Socket serversock = tcp.socket((short) 1000);
				serversock.accept();
				byte[] msgArray = new byte[5];
				serversock.read(msgArray, 0 , 5);
				String msg = new String(msgArray);
				Log.i("Received Msg", msg + "\n");
				msgArray = new byte[2];
				serversock.read(msgArray, 0, 2);
				msg = new String(msgArray);
				Log.i("Received Msg", msg + "\n");


			}catch(Exception e){
				Log.i("exception", e.getMessage());
			}
		}
	}

	class Client implements Runnable{
		public void run(){
			try {

				TCP tcp = new TCP(100);
				TCP.Socket clientsock = tcp.socket((short) 1000);
				clientsock.connect(IP.IpAddress.getAddress("192.168.0.30"),(short)1000);
				String msg = "Nikolas";
				byte [] msgArray = msg.getBytes();
				clientsock.write(msgArray, 0, msgArray.length );
			}catch(Exception e){
				Log.i("exception", e.getMessage());
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Thread serverThread = new Thread(new Server());
		Thread clientThread = new Thread(new Client());

		Log.v("Ip 20", " result:" + IP.IpAddress.getAddress("192.168.0.20").getAddress());
		Log.v("Ip 20 reversed", " result:" + Integer.reverseBytes(IP.IpAddress.getAddress(20).getAddress()));
		Log.v("Ip 10", " result:" + IP.IpAddress.getAddress("192.168.0.10").getAddress());
		Log.v("Ip 10 reversed", " result:" + Integer.reverseBytes(IP.IpAddress.getAddress(10).getAddress()));
		serverThread.start();
		clientThread.start();

		// Connect various GUI components to the networking stack.
	}

}
