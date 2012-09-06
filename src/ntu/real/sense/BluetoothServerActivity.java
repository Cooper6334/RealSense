package ntu.real.sense;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class BluetoothServerActivity extends Activity {

	// private BluetoothAdapter mBluetoothAdapter;
	ServerAgent msa;
	Agent mca;
	// String title = "";
	boolean flagGameStart = false;
	boolean flagGamePrepare = false;
	boolean flagExit = false;
	ServerSocket serverSocket;
	ListView lv;
	ArrayAdapter<String> adapter;
	WifiManager mWifiManager;
	Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case Msg.joined:
				// adapter.add(title);
				adapter.add(m.obj + "");
				// BluetoothActivity.this.setTitle(msa.getCount() + title);
				// Log.e("ba", "now devices=" + msa.getCount());
				break;
			case Msg.remove:
				String title4 = (String) m.obj;

				for (int i = 0; i < adapter.getCount(); i++) {
					if (adapter.getItem(i).equals(title4)) {
						adapter.remove(adapter.getItem(i));
						break;
					}
				}

				break;
			case Msg.gameprepare:

				if (Global.flagIsServer && !flagGameStart) {

					flagGameStart = true;
					try {
						Thread.sleep(1000);
						if (serverSocket != null) {
							serverSocket.close();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();

					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Log.e("ba", "write all gamestart");
					msa.writeAll("gamestart");

					sendEmptyMessage(Msg.gamestart);
				} else if (!Global.flagIsServer && mca != null) {
					mca.write("gameprepare");
				}
				break;
			case Msg.gamestart:

				Log.e("ba", "get flag gamestart");
				Intent intent = new Intent();
				if (Global.flagIsServer) {
					Global.mServerAgent = msa;
					intent.setClass(BluetoothServerActivity.this,
							ServerActivity.class);
				} else {
					if (!flagGameStart) {
						Global.mClientAgent = mca;
						intent.setClass(BluetoothServerActivity.this,
								ClientActivity.class);
					}
				}

				startActivity(intent);
				BluetoothServerActivity.this.finish();
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host);
		bindViews();
		initBluetooth();

		if (Global.flagIsServer) {
			startServer();
		} else {
			searchClient();
		}

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		flagExit = true;
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (msa != null) {
			msa.clear();
			msa = null;
		}
		if (mca != null) {
			mca.clear();
			mca = null;
		}
		Log.e("ba", "back press");
	}
	/*
	public void onDestroyed(){
		super.onDestroy();
		flagExit = true;
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (msa != null) {
			msa.clear();
			msa = null;
		}
		if (mca != null) {
			mca.clear();
			mca = null;
		}
	}*/

	void bindViews() {
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		lv = (ListView) findViewById(R.id.listView1);
		lv.setAdapter(adapter);

		Button b = (Button) findViewById(R.id.start);
		b.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.e("ba", flagGamePrepare + ":" + adapter.getCount());
				if (!flagGamePrepare) {
					flagGamePrepare = true;
					myHandler.sendEmptyMessage(Msg.gameprepare);
				}
			}
		});
	}

	void initBluetooth() {
		// mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// if (mBluetoothAdapter == null) {
		// Toast.makeText(this, "There is no Bluetooth", Toast.LENGTH_SHORT)
		// .show();
		// }
		// if (!mBluetoothAdapter.isEnabled()) {
		// mBluetoothAdapter.enable();
		// while (!mBluetoothAdapter.isEnabled()) {
		//
		// }
		// }

		// title = mBluetoothAdapter.getName();
		mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

		Message m = new Message();
		m.what = Msg.joined;
		if (Global.flagIsServer) {
			m.obj = "Server";
		} else {
			m.obj = "Client";
		}
		myHandler.sendMessage(m);
		// myHandler.sendEmptyMessage(Msg.joined);
		msa = new ServerAgent();

	}

	void startServer() {
		new Thread(new AcceptThread()).start();
	}

	void searchClient() {
		new Thread(new ConnectThread()).start();
	}

	private class AcceptThread extends Thread {

		public void run() {
			Socket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				Log.e("ba", "server finding");

				try {
					serverSocket = new ServerSocket(Global.PORT);
					Log.e("ip", serverSocket.getLocalSocketAddress().toString());
					socket = serverSocket.accept();
					String dn = socket.getInetAddress().toString();
					serverSocket.close();
					Log.e("ser", "accept " + dn);
					if (socket != null) {
						Log.e("ba", "" + adapter.getCount());
						Agent a = msa.addClient(socket, dn);
						for (int i = 0; i < adapter.getCount(); i++) {
							a.write("readname");
							a.write(adapter.getItem(i));
						}
						msa.writeWithoutId("readname", a.id);
						msa.writeWithoutId(dn, a.id);
						// title = dn;
						// myHandler.sendEmptyMessage(Msg.joined);
						Message m = new Message();
						m.what = Msg.joined;
						m.obj = dn;
						myHandler.sendMessage(m);

						new Thread(new ReadThread(a)).start();
					}
				} catch (IOException e) {
					Log.e("ba", "accept thread end");

					return;
				}
			}
		}
	}

	private class ConnectThread extends Thread {

		public void run() {
			Log.e("bsa", "start client thread");
			boolean flagInPair = false;

			// Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
			// .getBondedDevices();
			Socket mmSocket = null;
			Log.e("ba", "connect in paired");
			flagInPair = true;
			while (true) {

				try {
					// mmSocket = device
					// .createRfcommSocketToServiceRecord(Global.mUUID);

					DhcpInfo mDhcpInfo = mWifiManager.getDhcpInfo();
					int ipadd = mDhcpInfo.gateway;
					Global.IP = ((ipadd & 0xFF) + "." + (ipadd >> 8 & 0xFF)
							+ "." + (ipadd >> 16 & 0xFF) + "." + (ipadd >> 24 & 0xFF));
					Log.e("ip", Global.IP);
					mmSocket = new Socket(Global.IP, Global.PORT);

					mca = new Agent(mmSocket, mmSocket.getInetAddress()
							.toString(), 0);
					Log.e("bc", "connect ok");
					new Thread(new ReadThread(mca)).start();
					return;
					// Global.mClientAgent = new Agent(mmSocket);
				} catch (IOException connectException) {
					try {
						if (flagExit) {
							return;
						}
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Log.e("ba", "client exception ");
					try {
						if (mmSocket != null) {
							mmSocket.close();
						}
					} catch (IOException closeException) {
					}

				}

			}

		}
	}

	private class ReadThread extends Thread {
		Agent mca;

		ReadThread(Agent ca) {
			mca = ca;
		}

		public void run() {

			while (true) {

				String r = mca.read();

				if (r.equals("gameprepare")) {
					myHandler.sendEmptyMessage(Msg.gameprepare);

				} else if (r.equals("gamestart")) {
					if (!Global.flagIsServer) {
						mca.write("gamestart");
						myHandler.sendEmptyMessage(Msg.gamestart);
					}

					return;
				} else if (r.equals("readname")) {
					Message m = new Message();
					m.what = Msg.joined;
					m.obj = mca.read();
					myHandler.sendMessage(m);
					// title = mca.read();
					// myHandler.sendEmptyMessage(Msg.joined);
				} else if (r.equals("remove")) {
					Log.e("bsa", "remove device");
					Message m6 = new Message();
					m6.what = Msg.remove;
					m6.obj = mca.read();
					myHandler.sendMessage(m6);
				} else {
					Log.e("bsa", "read else");
					if (Global.flagIsServer) {

						Message m6 = new Message();
						m6.what = Msg.remove;
						m6.obj = mca.socket.getInetAddress().toString();

						if (msa != null && mca != null) {
							Log.e("bsa", "remove id:" + mca.id);
							msa.remove(mca.id);
						}
						msa.writeAll("remove");
						msa.writeAll(m6.obj + "");
						myHandler.sendMessage(m6);
					} else {
						BluetoothServerActivity.this.finish();
					}
					return;
				}

			}
		}
	}

}
