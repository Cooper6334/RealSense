package ntu.real.sense;

import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.text.format.Time;

public class Global {
	protected static final String TAG = "WifiDev";

	// final static int SERVER_PORT = 12345;
	// // static String SERVER_IP = "192.168.5.1";
	// static String SERVER_SSID = "Bombaaa";
	// static String SERVER_KEY = "bombplus";
	// static String mMAC = "3C:5A:37:89:3F:38";

	static int selectWay;
	static String name;
	static final String[] userName = { "Allen", "Bob", "Cooper", "David",
			"Eric", "Frank", "Grace", "Herry" };
	static final int[] userColor = { 0xffdda628, 0xff9bba3b, 0xff5bb6b9,
			0xffc9544b, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.DKGRAY };

	// static String mMAC = "";
	// final static UUID mUUID = UUID
	// .fromString("d4925895-0722-4252-a969-03be18b8ffba");
	static String IP = "192.168.5.1";
	static int PORT = 6334;
	static int FILE_PORT = 6335;

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

	// client端接收
	protected static final int SERVER_SEND_FILE_START = 0x106;
	protected static final int SERVER_SEND_FILE_COMPLETED = 0x108;

	// server端傳送
	protected static final int CLIENT_RECEIVE_START = 0x109;
	protected static final int CLIENT_RECEIVE_COMPLETED = 0x107;

	// client端傳送
	protected static final int SERVER_RECEIVE_FILE_COMPLETED = 0x110;

	// server、client端接收(來自client)
	protected static final int CLIENT_SEND_FILE_START = 0x111;
	protected static final int CLIENT_SEND_FILE_COMPLETED = 0x112;
	// 來自對方client說自己傳完了
	protected static final int CLIENT_SEND_FILE_COMPLETED_REMOTE = 0x113;

	static boolean flagIsPlaying = false;
	static boolean flagIsReceiving = false;
	static Agent mClientAgent;
	static ServerAgent mServerAgent;

	static Time startTime;
	static Time endTime;
	static long startTimeMs;
	static long endTimeMs;
	static Time now;
	static ArrayList<Target> storedDegree;
	static int userNumber;
	static ListAllPath demoTest;

}
