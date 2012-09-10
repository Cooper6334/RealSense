package ntu.real.sense;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class ClientActivity extends Activity implements SensorEventListener {
	int imgBtnSize = 150;
	int imgMargin = 5;
	boolean separationLine = false;

	SensorManager sensorManager;

	RealSurface surface;
	RelativeLayout layout;
	int CurrentButtonNumber = 0; // CurrentButtonNumber流水號 設定物件ID

	ListAllPath demoTest;
	UriRelatedOperation UriRelatedOperation;

	RelativeLayout RL_temp;
	Agent mca = Global.mClientAgent;
	int cId;
	int users;
	int picCycling;// 1~12，看新進來的圖片要取代哪個thumbnails

	ServerSocket serverSocket = null;
	Socket socket = null;

	ImageButton image_temp;// 暫存之後要更新的ImageButton
	Bitmap bitmap;
	// 用來handle client本身要傳的訊息
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {

			// 傳遞照片(可能是往server或往client)
			case 0x103:

				ArrayList<Integer> sendTargetList = (ArrayList<Integer>) m.obj;
				Integer picNumber = sendTargetList
						.remove(sendTargetList.size() - 1);// 要傳的圖片id
				String toastTemp = "The selected picture: " + picNumber
						+ ". The selected nominances: ";
				if (demoTest.file_list.size() <= picNumber) {
					return;
				}
				// 要傳的圖片檔案
				File tmpFile = new File(demoTest.file_list.get(picNumber));
				Uri outputFileUri = Uri.fromFile(tmpFile);

				Global.endTime = new Time();
				Global.endTime.setToNow();
				Global.endTimeMs = System.currentTimeMillis();
				try {
					FileWriter ct = new FileWriter("/sdcard/ClientTimeLog.txt",
							true);
					BufferedWriter bwCT = new BufferedWriter(ct); // 將BufferedWeiter與FileWrite物件做連結
					if (separationLine == false) {
						bwCT.write("-\tSeparation\tLine\t-\n");
						separationLine = true;
					}
					Global.now = new Time();
					Global.now.setToNow();
					bwCT.write("<<\t" + Global.now + "\t>>\n");
					bwCT.write("start\t" + Global.startTime + "\n");
					bwCT.write("end\t" + Global.endTime + "\n");
					bwCT.write("startMs\t" + Global.startTimeMs + "\n");
					bwCT.write("endMs\t" + Global.endTimeMs + "\n\n");
					bwCT.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Global.endTime = new Time();
				// Global.endTime.setToNow();
				// Log.e("WeiChen", Global.startTime + " start");
				// Log.e("WeiChen", Global.endTime + " end");

				// 一個一個target送出
				Integer i;
				Integer t;
				for (i = 0; i < sendTargetList.size(); i++) {

					t = sendTargetList.get(i);
					toastTemp += t + ",";
					Log.e("houpan", "開始傳摟到:" + t);
					if (t == (users - 1)) {// 對象是server
						Log.e("houpan", "對象是server");
						add_ClientFileOutputTransferThread_toServer(outputFileUri);
						// new Thread(new
						// ClientFileOutputTransferThread_toServer(outputFileUri)).start();//處理network不能在mainThread上，所以要搬出去處理
					} else {
						add_ClientFileOutputTransferThread_toClient(t,
								outputFileUri);
						// 傳到其他client
						// new ClientFileOutputTransferThread_toClient(t,
						// outputFileUri).run();
					}

				}
				toastTemp += "\n圖片實際位址：" + outputFileUri.toString();
				Toast.makeText(ClientActivity.this, toastTemp,
						Toast.LENGTH_SHORT).show();

				// 開socket，傳IP跟port，讓另外一端連上來

				break;

			case Global.SERVER_SEND_FILE_START:// 由server而來的傳輸
				Global.flagIsReceiving = true;
				Toast.makeText(ClientActivity.this, "收到圖片，開始傳遞",
						Toast.LENGTH_SHORT).show();

				// 要送出去的檔案
				Log.e("houpan", "要收摟");
				new Thread(new ClinetFileInputTransferThread_fromServer())
						.start();
				break;

			case Global.SERVER_SEND_FILE_COMPLETED:// 傳完囉
				Global.flagIsReceiving = false;
				Toast.makeText(ClientActivity.this, "接收完成", Toast.LENGTH_SHORT)
						.show();
				// 要送出去的檔案
				Log.e("houpan", "收結束");

				// 重新設定view
				demoTest.file_list.setElementAt((String) m.obj, picCycling);
				image_temp = (ImageButton) RL_temp.findViewById(picCycling);
				Log.e("houpan", "picCycling:" + picCycling);
				Bitmap bitmap = decodeBitmap(demoTest.file_list.get(picCycling));
				image_temp.setImageBitmap(bitmap);
				picCycling = (picCycling - 6) % 6 + 7;
				Log.e("houpan", "picCycling:" + picCycling);

				break;

			case Global.SERVER_RECEIVE_FILE_COMPLETED:// 直接傳給server結束了
				Toast.makeText(ClientActivity.this, "傳輸完成(toServer)",
						Toast.LENGTH_SHORT).show();
				// 要送出去的檔案
				Log.e("houpan", "收結束");
				break;

			case Global.CLIENT_RECEIVE_COMPLETED:// 傳給某client結束了
				Toast.makeText(ClientActivity.this, "傳輸完成(toClient)",
						Toast.LENGTH_SHORT).show();
				// 要送出去的檔案
				Log.e("houpan", "收結束");
				break;

			case Global.CLIENT_SEND_FILE_START:// 收到其他client的資料
				Global.flagIsReceiving = true;
				Toast.makeText(ClientActivity.this, "收到圖片，開始傳遞",
						Toast.LENGTH_SHORT).show();

				// 要送出去的檔案
				Log.e("houpan", "要收摟");

				new Thread(new ClinetFileInputTransferThread_fromClient(
						(String) m.obj)).start();
				break;

			case Global.CLIENT_SEND_FILE_COMPLETED:// 收到其他client的資料
				Global.flagIsReceiving = false;
				Toast.makeText(ClientActivity.this, "接收完成(fromClient)",
						Toast.LENGTH_SHORT).show();

				// 重新設定view
				demoTest.file_list.setElementAt((String) m.obj, picCycling);
				ImageButton image_temp = (ImageButton) RL_temp
						.findViewById(picCycling);
				Log.e("houpan", "picCycling:" + picCycling);
				bitmap = decodeBitmap(demoTest.file_list.get(picCycling));
				image_temp.setImageBitmap(bitmap);
				picCycling = (picCycling - 6) % 6 + 7;
				Log.e("houpan", "picCycling:" + picCycling);

				break;

			case Global.CLIENT_SEND_FILE_COMPLETED_REMOTE:// 傳給client結束了(收到對方的回報)
				Toast.makeText(ClientActivity.this, "傳輸完成(toClient，ACK)",
						Toast.LENGTH_SHORT).show();
				// 要送出去的檔案
				Log.e("houpan", "收結束");
				break;

			case 0x114:
				String s = (String) m.obj;
				mca.write(s);
				break;
			case 0x115:
				mca.write("setdeg");
				mca.write("" + m.arg1);
				break;
			case 0x116:
				String s2 = (String) m.obj;
				mca.write(s2);
				Thread thr=new Thread(
				new ClientFileOutputTransferThread_toClient(
						toClient_i.remove(0),
						toClient_outputFileUri.remove(0)));
				thr.start();
				break;
			case 0x117:
				String s3 = (String) m.obj;
				mca.write(s3);
				Thread t2=new Thread(
				new ClientFileOutputTransferThread_toServer(
						toServer_outputFileUri.remove(0)));
				t2.start();
				break;
			}

		}
	};

	// 儲存parameter方便之後用
	Vector<Uri> toClient_outputFileUri;
	Vector<Integer> toClient_i;
	Vector<Uri> toServer_outputFileUri;
	Vector<Integer> operationQueue;// 1:toServer, 2:toClient

	public void add_ClientFileOutputTransferThread_toServer(Uri outputFileUri) {
		toServer_outputFileUri.add(outputFileUri);
		operationQueue.add(1);
	}

	public void add_ClientFileOutputTransferThread_toClient(Integer i,
			Uri outputFileUri) {
		toClient_outputFileUri.add(outputFileUri);
		toClient_i.add(i);
		operationQueue.add(2);
	}

	// 用Queue管理傳輸
	public class ClientFileOutputTransferThread_manager implements Runnable {

		ClientFileOutputTransferThread_manager() {
			toClient_outputFileUri = new Vector<Uri>();
			toClient_i = new Vector<Integer>();
			toServer_outputFileUri = new Vector<Uri>();
			operationQueue = new Vector<Integer>();
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				if (operationQueue.size() != 0) {// 做下一個該做的operation
					switch (operationQueue.remove(0)) {
					case 1:
						
						Message m = new Message();
						m.what = 0x117;
						m.obj = "ClientSend_start_" + cId;
						handler.sendMessage(m);
//						new ClientFileOutputTransferThread_toServer(
//								toServer_outputFileUri.remove(0)).run();
						break;
					case 2:
						Message m2 = new Message();
						m2.what = 0x116;
						m2.obj = "ClientSend_start_" + Global.mClientAgent.id + "_"
								+ toClient_i.get(0);
						handler.sendMessage(m2);
//						new ClientFileOutputTransferThread_toClient(
//								toClient_i.remove(0),
//								toClient_outputFileUri.remove(0)).run();

						break;
					}
				} else {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

		}

	}

	public class ClientFileOutputTransferThread_toServer implements Runnable {
		Uri outputFileUri;

		ClientFileOutputTransferThread_toServer(Uri outputFileUri) {
			this.outputFileUri = outputFileUri;
		}

		@Override
		public void run() {

//			Message m=new Message();
//			m.what=0x114;
//			m.obj="ClientSend_start_" + cId;
//			handler.sendMessage(m);
//			 mca.write("ClientSend_start_" + cId);

			Socket socket = null;

			while (socket == null) {
				try {
					socket = serverSocket.accept();
					Thread.sleep(1000);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			Log.e("houpan", "傳L1");
			ContentResolver cr = getContentResolver();
			// 要傳出去的圖片
			InputStream inputUriStream = null;
			// 要收下的client
			OutputStream outputDataStream = null;

			try {
				inputUriStream = cr.openInputStream(outputFileUri);
				Log.e("houpan", "傳L2");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Log.e("houpan", "傳L2");
				outputDataStream = socket.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.e("houpan", "傳L3");
			ntu.real.sense.UriRelatedOperation.copyFile(inputUriStream,
					outputDataStream);
			Log.e("houpan", "傳L4");

			try {
				socket.close();
				// serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Message m2= new Message();
			m2.what = Global.SERVER_RECEIVE_FILE_COMPLETED;
			handler.sendMessage(m2);
		}
	}

	public class ClientFileOutputTransferThread_toClient implements Runnable {
		Integer targetId;// 為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;

		ClientFileOutputTransferThread_toClient(Integer i, Uri outputFileUri) {
			this.targetId = i;
			this.outputFileUri = outputFileUri;
		}

		@Override
		public void run() {
//			Message m = new Message();
//			m.what = 0x114;
//			m.obj = "ClientSend_start_" + Global.mClientAgent.id + "_"
//					+ targetId;
//			handler.sendMessage(m);
			// mca.write("ClientSend_start_" + Global.mClientAgent.id + "_"
			// + targetId);

			Socket socket = null;
			while (socket == null) {
				try {
					socket = serverSocket.accept();
					Thread.sleep(1000);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			Log.e("houpan", "傳L1");
			ContentResolver cr = getContentResolver();
			// 要傳出去的圖片
			InputStream inputUriStream = null;
			// 要收下的client
			OutputStream outputDataStream = null;

			try {
				inputUriStream = cr.openInputStream(outputFileUri);
				Log.e("houpan", "傳L2");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Log.e("houpan", "傳L2");
				outputDataStream = socket.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.e("houpan", "傳L3");
			ntu.real.sense.UriRelatedOperation.copyFile(inputUriStream,
					outputDataStream);
			Log.e("houpan", "傳L4");

			try {
				socket.close();
				// serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Message m2 = new Message();
			m2.what = Global.CLIENT_RECEIVE_COMPLETED;
			handler.sendMessage(m2);
		}
	}

	public class ClinetFileInputTransferThread_fromServer implements Runnable {

		@Override
		public void run() {

			Socket socket = null;

			while (socket == null) {
				try {
					socket = serverSocket.accept();
					Thread.sleep(1000);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			Log.e("houpan", "收L1");
			String fileAbsolutePath = "/sdcard/DCIM/Camera/wifihpshared_"
					+ System.currentTimeMillis() + ".jpg";
			File fileOutputStream = new File(fileAbsolutePath);
			InputStream inputstream = null;// client自己的socket input處

			File dirs = new File(fileOutputStream.getParent());

			if (!dirs.exists())
				dirs.mkdirs();
			try {
				fileOutputStream.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				inputstream = socket.getInputStream();
				Log.e("houpan", "收L2");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Log.e("houpan", "收L3");
				ntu.real.sense.UriRelatedOperation.copyFile(inputstream,
						new FileOutputStream(fileOutputStream));

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.e("houpan", "收L4");
			// serverSocket.close();
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.e("houpan", "收L5");

			// 更新圖示

			Message tempMessage = new Message();
			tempMessage.what = Global.SERVER_SEND_FILE_COMPLETED;// 傳完了
			tempMessage.obj = fileAbsolutePath;
			handler.sendMessage(tempMessage);

			Message m = new Message();
			m.what = 0x114;
			m.obj = "ClientReceive_completed_server";
			handler.sendMessage(m);
			// mca.write("ClientReceive_completed_server");
		}
	}

	public class ClinetFileInputTransferThread_fromClient implements Runnable {
		Integer sourceId;// 為了把內thread不能改動外變數的錯誤濾掉
		String sourceIP;

		ClinetFileInputTransferThread_fromClient(String inputString) {
			this.sourceId = Integer.parseInt(inputString.split("_")[0]);
			this.sourceIP = inputString.split("_")[1];
			// this.outputFileUri=outputFileUri;
		}

		@Override
		public void run() {

			Socket mmSocket = null;
			while (mmSocket == null) {
				try {
					Log.e("houpan", "IP是" + sourceIP);
					mmSocket = new Socket(sourceIP, Global.FILE_PORT);
					Thread.sleep(1000);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			Log.e("houpan", "收L1");
			String fileAbsolutePath = "/sdcard/DCIM/Camera/wifihpshared_"
					+ System.currentTimeMillis() + ".jpg";
			File fileOutputStream = new File(fileAbsolutePath);
			InputStream inputstream = null;// client自己的socket input處

			File dirs = new File(fileOutputStream.getParent());

			if (!dirs.exists())
				dirs.mkdirs();
			try {
				fileOutputStream.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				inputstream = mmSocket.getInputStream();
				Log.e("houpan", "收L2");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Log.e("houpan", "收L3");
				ntu.real.sense.UriRelatedOperation.copyFile(inputstream,
						new FileOutputStream(fileOutputStream));

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.e("houpan", "收L4");
			try {
				mmSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.e("houpan", "收L5");

			Message tempMessage = new Message();
			tempMessage.what = Global.CLIENT_SEND_FILE_COMPLETED;// 傳完了
			tempMessage.obj = fileAbsolutePath;
			handler.sendMessage(tempMessage);

			Message m = new Message();
			m.what = 0x114;
			m.obj = "ClientReceive_completed_client_" + sourceId;
			handler.sendMessage(m);
			// mca.write("ClientReceive_completed_client_" + sourceId);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		imgBtnSize = dm.widthPixels / 4;
		imgMargin = dm.widthPixels / 100;
		Log.e("123", dm.widthPixels + "" + dm.heightPixels);

		// 隱藏title bar&notifiaction bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// 設定顯示照片的layout
		layout = new RelativeLayout(this);
		layout.setBackgroundColor(Color.BLACK);
		setContentView(layout);
		// 讀取照片
		demoTest = new ListAllPath();
		File rootFile = new File("/sdcard/DCIM");
		demoTest.print(rootFile, 0);

		RL_temp = new RelativeLayout(this);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.FILL_PARENT,
				RelativeLayout.LayoutParams.FILL_PARENT);
		params.setMargins(dm.widthPixels / 10, dm.widthPixels / 10,
				dm.widthPixels / 10, dm.widthPixels / 10);

		params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		RL_temp.setLayoutParams(params);
		layout.addView(RL_temp);
		// 注意顯示太多照片會out of memory
		int index = demoTest.file_list.size();
		picCycling = 7;// 把cycling先設在下一個要補的地方
		if (index > 7) {
			index = 7;
		}

		for (int i = 1; i < 13; i++) {

			ImageButton image_temp = new ImageButton(this);
			if (i < index) {// 實際上方格裡面有東西的狀態
				Log.e("圖片網址：", demoTest.file_list.get(i));
				Bitmap bitmap = decodeBitmap(demoTest.file_list.get(i));
				image_temp.setImageBitmap(bitmap);
			} else {
				demoTest.file_list.add(null);
			}
			if (i <= 6) {
				image_temp.setBackgroundColor(Color.RED);
			} else {
				image_temp.setBackgroundColor(Color.BLUE);
			}

			image_temp.setId(i); // ID不能是零，不然會爛掉！
			image_temp.setLayoutParams(params);
			params = new RelativeLayout.LayoutParams(imgBtnSize, imgBtnSize);
			params.setMargins(imgMargin, imgMargin, imgMargin, imgMargin);
			if (i == 1) {

			}
			if (i > 3) {
				Log.e("在誰的下面：", Integer.toString(i - 3));
				params.addRule(RelativeLayout.BELOW, (i - 3));
			}
			if (i % 3 != 1) {// 非列首的條件，要margin
				Log.e("在誰的右邊：", Integer.toString(i - 1));
				params.addRule(RelativeLayout.RIGHT_OF, (i - 1));
			}

			image_temp.setLayoutParams(params);
			RL_temp.addView(image_temp);
		}

		Global.flagIsPlaying = true;
		// 註冊orientation sensor
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sensors.size() > 0) {
			sensorManager.registerListener(this, sensors.get(0),
					SensorManager.SENSOR_DELAY_NORMAL);
		}

		// 加入RealSense
		surface = new RealSurface(this, dm.widthPixels, dm.heightPixels, index);
		this.addContentView(surface, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		// 設定serverSocket
		try {
			serverSocket = new ServerSocket(Global.FILE_PORT);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// 設定繪圖與傳遞照片之Thread
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (Global.flagIsPlaying) {
					surface.drawView();
					if (surface.flagCanSend) {
						surface.flagCanSend = false;

						// 要送的目標的id們
						ArrayList<Integer> sendTargetList = new ArrayList<Integer>();
						int i;
						Log.e("houpan", "自己id:" + Global.mClientAgent.id);
						for (Target t : surface.selected) {
							// 先把target拿出來，比對id是不是自己，如果是的話就不加入要傳的list中
							Log.e("houpan", "要找的是是" + t.name);
							for (i = 0; i < Global.userName.length; i++) {// 不知為何順序是反的，用查的比較安全
								Log.e("houpan", "找" + Global.userName[i]);
								if (t.name.equals(Global.userName[i])) {

									break;
								}
							}
							if (Global.mClientAgent.id != i) {
								sendTargetList.add(i);
							}
						}
						sendTargetList.add(surface.selectedPhoto);

						Message m = new Message();
						m.what = 0x103;
						m.obj = sendTargetList;
						handler.sendMessage(m);
						surface.selected.clear();
					}
					if (surface.flagClick) {
						surface.flagClick = false;
						Message m = new Message();
						m.what = 0x102;
						handler.sendMessage(m);
					}

					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();

		// 從server接訊息的thread
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (Global.flagIsPlaying) {
					if (!Global.flagIsReceiving) {
						String m = mca.read();
						if (m != null) {
							if ("init".equals(m)) {
								cId = Integer.parseInt(mca.read());
								users = Integer.parseInt(mca.read());
								// cId表client自己的id
								Log.e("init", cId + ":" + users);
								Global.mClientAgent.id = cId;
								for (int i = 0; i < users; i++) {
									surface.target.add(new Target(
											Global.userName[i], 0,
											Global.userColor[i]));
								}
							} else if ("ServerSendFile_start".equals(m)) {// server傳檔案過來

								Message tempMessage = new Message();
								tempMessage.what = Global.SERVER_SEND_FILE_START;// server傳檔案
								handler.sendMessage(tempMessage);

							} else if (m.startsWith("ClinetSendFile_start_")) {

								Message tempMessage = new Message();
								tempMessage.what = Global.CLIENT_SEND_FILE_START;// client傳檔案
								tempMessage.obj = m.split("_", 3)[2];
								Log.e("houpan", "client(" + tempMessage.obj
										+ ")送檔案過來");
								handler.sendMessage(tempMessage);
							} else if ("ClientReceive_completed_client"
									.equals(m)) {
								Message tempMessage = new Message();
								tempMessage.what = Global.CLIENT_SEND_FILE_COMPLETED_REMOTE;// 從對面的client得到資訊：他收完了
								handler.sendMessage(tempMessage);

							} else if ("setdeg".equals(m)) {
								int who = Integer.parseInt(mca.read());
								int deg = Integer.parseInt(mca.read());
								surface.target.get(who).degree = deg;
							}

						} else {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					}

				}
			}
		}).start();

		new Thread(new ClientFileOutputTransferThread_manager()).start();// 開啟傳送檔案管理者的thread
	}

	@Override
	public void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
		Global.flagIsPlaying = false;
		/*
		 * if (serverSocket != null) { try { serverSocket.close(); } catch
		 * (IOException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } }
		 * 
		 * if (mca != null) { mca.clear(); mca = null; }
		 */
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

		surface.myDeg = (int) event.values[0];

		Message m = new Message();
		m.what = 0x115;
		m.arg1 = surface.myDeg;
		handler.sendMessage(m);
		// mca.write("setdeg");
		// mca.write("" + surface.myDeg);

	}

	private Bitmap decodeBitmap(String path) {
		BitmapFactory.Options op = new BitmapFactory.Options();
		op.inJustDecodeBounds = true;
		op.inSampleSize = 4;
		Bitmap bmp = BitmapFactory.decodeFile(path, op);
		op.inJustDecodeBounds = false;
		bmp = BitmapFactory.decodeFile(path, op);
		return bmp;
	}
}
