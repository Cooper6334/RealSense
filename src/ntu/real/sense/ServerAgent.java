package ntu.real.sense;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;


import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.ArrayAdapter;

public class ServerAgent {
	// ServerSocket socket;
	ArrayList<Agent> clients;
	int ii = 0;
	boolean flagEnded = false;
	int maxid = 0;

	public ServerAgent() {
		clients = new ArrayList<Agent>();
	}

	public Agent addClient(BluetoothSocket b, String add) {
		Log.e("sa", clients.size()+":id");
//		for (Agent a : clients) {
//			if (a.address.equals(add)) {
//				Log.e("sag", "return null");
//				return null;
//			}
//		}
		Agent ag = new Agent(b, add, maxid);
		clients.add(ag);
		maxid++;
		return ag;
	}

	public void addClient(Agent age) {

		clients.add(age);
		maxid++;
	}

	int getCount() {
		return clients.size();
	}

	public void writeAll(String str) {
		ii++;
		Log.e("sa", ii + " write all " + str);
		for (Agent b : clients) {
			b.write(str);
		}
	}

	public void writeWithoutId(String str, int id) {

		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).id != id) {
				clients.get(i).write(str);
			}
		}
	}

	public void writeToId(String str, int id) {
		Log.e("sa", "write " + str);
		if (id < clients.size()) {
			clients.get(id).write(str);
		}
	}

	public String readFromId(int id) {
		if (id < clients.size()) {
			return clients.get(id).read();
		}
		return "";
	}

	public void clear() {
		for (Agent b : clients) {
			b.clear();
		}
		clients.clear();
	}

	// public void writeWithout(String str, int id) {
	// for (Agent b : clients) {
	// if (b.id != id) {
	// b.write(str);
	// }
	// }
	// }
	//
	public void clearWithout(int id) {
		for (Agent b : clients) {
			if (b.id != id) {
				b.clear();
			}
		}
	}

	public void remove(int id) {
		for (Agent b : clients) {
			if (b.id == id) {
			
				b.clear();
			}
			clients.remove(b);
			break;
		}
	}

}
