package nl.vu.cs.cn;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Chat extends Activity {

	final int MSG_LNGTH = 50;
	final int UPPER_IP = 1;
	final int UPPER_PORT = 2000;
	final int LOWER_IP = 2;

	TextView upperTextView, lowerTextView;
	EditText upperEditText, lowerEditText;
	Button upperSend, lowerSend;

	//Reader Runnable used in both sockets
	//Both sockets read and write at the same time
	class Reader implements Runnable{

		private TCP.Socket socket;
		private byte[] buffer;
		private TextView dst;


		public Reader(TCP.Socket sck, TextView dst){
			super();
			this.socket = sck;
			this.buffer = new byte[MSG_LNGTH];
			this.dst = dst;
		}
		// Reads data from a given socket
		public void run() {

            class Writer implements Runnable{
                private String msg;
                public Writer(String msg){
                    this.msg = msg;
                }
                public void run(){
                    dst.append(msg);
                }
            }

			int bytes_received;
			do{

				bytes_received = this.socket.read(this.buffer, 0, MSG_LNGTH);
				if (bytes_received > 0){

                    String msg = (new String(this.buffer)).substring(0,bytes_received) + "\n";

                    runOnUiThread(new Writer(msg));




				}

			}while(bytes_received>=0);
		}
	}


	//function that converts a msg to byte array and sends it through a given socket used in both sockets
	private int writeToSock(TCP.Socket sck, String msg){
		byte[] buffer = new byte[MSG_LNGTH];
		byte[] msgArray = msg.getBytes();
		int length = msgArray.length;
		if (msgArray.length > MSG_LNGTH){
			length = MSG_LNGTH;
		}
		System.arraycopy(msgArray, 0, buffer, 0, length);

		return sck.write(buffer, 0, MSG_LNGTH);
	}


	// Server socket accepts connections and starts the reader
	class Upper implements Runnable{
		TCP.Socket uppersock;
		TextView upperTextView;

		public void run(){
			try{
				TCP tcp = new TCP(UPPER_IP);
				uppersock = tcp.socket(UPPER_PORT);
				uppersock.accept();
				new Thread(new Reader(uppersock, upperTextView)).start();

			}catch(Exception e){
				Log.i("exception", e.getMessage());
			}
		}



		public Upper(TextView upperTextView) {
			super();
			this.upperTextView = upperTextView;
		}


		public int write(String msg){
			return writeToSock(uppersock, msg);
		}

		public void close(){
			this.uppersock.close();
		}

	}

	//Client socket connects to server and starts the reader
	class Lower implements Runnable{

		TCP.Socket lowersock;
		TextView lowerTextView;
		public void run(){
			try{
				TCP tcp = new TCP(LOWER_IP);
				lowersock = tcp.socket();
				lowersock.connect(IP.IpAddress.getAddress("192.168.0." + UPPER_IP), UPPER_PORT);
                new Thread(new Reader(lowersock, lowerTextView)).start();
			}catch(Exception e){
				Log.i("exception", e.getMessage());
			}
		}

		public Lower(TextView lowerTextView){

			this.lowerTextView = lowerTextView;

		}

		public int write(String msg){
			return writeToSock(lowersock, msg);
		}
		public void close(){
			this.lowersock.close();
		}
	}

	Upper upper;
	Lower lower;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		upperTextView = (TextView) findViewById(R.id.upperTextView);
		lowerTextView = (TextView) findViewById(R.id.lowerTextView);
		upperEditText = (EditText) findViewById(R.id.upperEditText);
		lowerEditText = (EditText) findViewById(R.id.lowerEditText);
		upperSend = (Button) findViewById(R.id.upperSend);
		lowerSend = (Button) findViewById(R.id.lowerSend);
		upperTextView.setMovementMethod(new ScrollingMovementMethod());
		lowerTextView.setMovementMethod(new ScrollingMovementMethod());
		upper = new Upper(upperTextView);
		lower = new Lower(lowerTextView);




		upperSend.setOnClickListener(
				new View.OnClickListener() {

					public void onClick(View arg0) {
						String msg = upperEditText.getText().toString();
						upper.write(msg);
					}
				}

		);

		lowerSend.setOnClickListener(
				new View.OnClickListener() {

					public void onClick(View arg0) {
						String msg = lowerEditText.getText().toString();
						lower.write(msg);
					}
				}

		);


	}


	@Override
	public void onPause(){
		super.onPause();
		(new Thread(new Runnable() {
			public void run() {
				upper.close();
			}
		})).run();
		(new Thread(new Runnable() {
			public void run() {
				lower.close();
			}
		})).run();

	}


	//Starts sockets on Resume in order to reconnect after each activity resume
	@Override
	public void onResume(){
		super.onResume();

		Thread upperThread = new Thread(upper);
		Thread lowerThread = new Thread(lower);
		upperThread.start();
		lowerThread.start();

	}

}
