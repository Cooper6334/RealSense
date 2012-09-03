package ntu.real.sense;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Agent {
	Socket socket;
	String address;
	BufferedReader reader;
	PrintWriter writer;
	int id;
	int team;

	public Agent(Socket b, String add, int inId) {
		socket = b;
		address = add;
		id = inId;
		try {
			reader = new BufferedReader(new InputStreamReader(
					b.getInputStream()));
			writer = new PrintWriter(b.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void write(String str) {
		//Log.e("age", "write " + str);
		writer.println(str);
		writer.flush();
	}

	public String read() {
		try {
			return reader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//Log.e("age", "read exception");
			return "";
		}

	}

	public void clear() {
		try {
			if (writer != null) {
				writer.close();
			}
			if (reader != null) {
				reader.close();
			}
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

	}

	public void setTeam(int inTeam) {
		team = inTeam;
	}
}
