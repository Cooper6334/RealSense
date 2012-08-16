package ntu.real.sense;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;

public class Global {
	protected static final String TAG = "WifiDev";

	// final static int SERVER_PORT = 12345;
	// // static String SERVER_IP = "192.168.5.1";
	// static String SERVER_SSID = "Bombaaa";
	// static String SERVER_KEY = "bombplus";
	// static String mMAC = "3C:5A:37:89:3F:38";
	static String mMAC = "";
	final static UUID mUUID = UUID
			.fromString("d4925895-0722-4252-a969-03be18b8ffba");
	final static String nfctag = "text/bombPlus";
	static boolean flagFromNFC = false;
	static boolean flagGamePlayed = false;
	static boolean flagIsServer = true;
	static BluetoothDevice device = null;
	// BombPlusActivity
	protected static final int SHOW_MSG = 1;
	protected static final int DISMISS_MSG = 2;
	protected static final int CHANGE_MSG = 3;
	protected static final int SOCKET_FAILED = 4;
	final static int START_GAME = 5;

	// ButtonHostActivity
	protected static final int ADD_CLIENT = 6;
	protected static final int ADD_OUTPUT = 7;
	protected static final int ADD_INPUT = 8;

	static boolean flagIsPlaying = false;
	static Agent mClientAgent;
	static ServerAgent mServerAgent;

}
