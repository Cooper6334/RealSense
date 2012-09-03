package ntu.real.sense;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class ServerActivity extends Activity implements SensorEventListener {
	int imgBtnSize = 150;
	int imgMargin = 5;

	RealSurface surface;
	RelativeLayout layout;
	int CurrentButtonNumber = 0; // CurrentButtonNumber流水號 設定物件ID

	ServerAgent msa = Global.mServerAgent;
	SensorManager sensorManager;

	int users;
	int degs[];
	int sId;
	Thread[] readClientThread;

	// 儲存每張圖片資訊的arraylist
	ListAllPath demoTest;
	UriRelatedOperation UriRelatedOperation;

	// 接收來自某client(ReadClientThread傳來的)或server的自己的角度轉變等msg，並作處理(廣播給所有人之類的)
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			// 改變角度時廣播給所有client
			case 0x101:

				surface.target.get(m.arg1).degree = m.arg2;
				msa.writeAll("setdeg");
				msa.writeAll("" + m.arg1);
				msa.writeAll("" + m.arg2);
				break;

			// 點擊
			case 0x102:
				Toast.makeText(ServerActivity.this, "click", Toast.LENGTH_SHORT)
						.show();
				break;

			// 由自己傳遞照片(server傳給client，不需重新建立連線)
			case 0x103:
				ArrayList<Integer> sendTargetList = (ArrayList<Integer>) m.obj;
				Integer picNumber = sendTargetList
						.remove(sendTargetList.size() - 1);// 要傳的圖片id
				String toastTemp = "The selected picture: " + picNumber
						+ ". The selected nominances: ";
				// 要傳的圖片檔案
				File tmpFile = new File(demoTest.file_list.get(picNumber));
				Uri outputFileUri = Uri.fromFile(tmpFile);

				// 一個一個target送出
				Integer i;
				Integer t;
				for (i = 0; i < sendTargetList.size(); i++) {
					t = sendTargetList.get(i);
					toastTemp += t + ",";
					Log.e("houpan", "開始傳摟到:" + t);
					new Thread(new ServerFileOutputTransferThread(t,
							outputFileUri)).start();

				}
				toastTemp += "\n圖片實際位址：" + outputFileUri.toString();
				Toast.makeText(ServerActivity.this, toastTemp,
						Toast.LENGTH_SHORT).show();

				// 開socket，傳IP跟port，讓另外一端連上來

				break;
			// 傳照片給client且結束了
			case Global.CLIENT_RECEIVE_COMPLETED:
				Toast.makeText(ServerActivity.this, "傳輸完成", Toast.LENGTH_SHORT)
						.show();
				break;

			// 接收由client而來的傳輸
			case Global.CLIENT_SEND_FILE_START:
				if (((String) m.obj).split("_").length == 1) {// 直接傳到server
					// 開始接收
					Log.e("houpan", "收到來自client的訊息(toServer)");
					Toast.makeText(ServerActivity.this, "開始接收",
							Toast.LENGTH_SHORT).show();
					new Thread(new ServerFileInputTransferThread()).start();

				} else {// 經server傳到另一client
					int sourceId = Integer
							.parseInt(((String) m.obj).split("_")[0]);
					int targetId = Integer
							.parseInt(((String) m.obj).split("_")[1]);
					Log.e("houpan", "收到來自client的訊息(toClient):" + sourceId + ","
							+ targetId);
					msa.writeToId("ClinetSendFile_start_" + sourceId, targetId);
					Toast.makeText(ServerActivity.this, "轉傳至" + targetId,
							Toast.LENGTH_SHORT).show();
				}
				break;

			case Global.CLIENT_SEND_FILE_COMPLETED:
				Toast.makeText(ServerActivity.this, "接收完成", Toast.LENGTH_SHORT)
						.show();
				break;

			}

		}
	};

	// 直接收client的資料
	public class ServerFileInputTransferThread implements Runnable {
		Integer sourceId;// 為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;

		@Override
		public void run() {

			Message tempMessage = new Message();
			tempMessage.what = Global.CLIENT_SEND_FILE_COMPLETED;// 傳完了
			handler.sendMessage(tempMessage);

		}
	}

	//
	public class ServerFileOutputTransferThread implements Runnable {
		Integer targetId;// 為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;

		ServerFileOutputTransferThread(Integer i, Uri outputFileUri) {
			this.targetId = i;
			this.outputFileUri = outputFileUri;
		}

		@Override
		public void run() {
			Socket socket = null;
			Log.e("houpan", "why");

			try {
				ServerSocket serverSocket = new ServerSocket(6666);
				socket = serverSocket.accept();
				Log.e("ip", serverSocket.getLocalSocketAddress().toString());

				msa.writeToId("ServerSendFile_start", targetId);

				String dn = socket.getInetAddress().toString();
				serverSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block

				e1.printStackTrace();
			}
			//
			//
			if (socket != null) {

				InputStream inputUriStream;
				OutputStream outputDataStream;
				Log.e("houpan", "傳L1");
				ContentResolver cr = getContentResolver();
				// 要傳出去的圖片 InputStream inputUriStream = null; //要收下的client

				try {
					inputUriStream = cr.openInputStream(outputFileUri);
					outputDataStream = socket.getOutputStream();

					ntu.real.sense.UriRelatedOperation.copyFile(inputUriStream,
							outputDataStream);
					Message m = new Message();
					m.what = Global.CLIENT_RECEIVE_COMPLETED;
					handler.sendMessage(m);
					Log.e("houpan", "傳L2");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

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

		RelativeLayout RL_temp = new RelativeLayout(this);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.FILL_PARENT,
				RelativeLayout.LayoutParams.FILL_PARENT);
		// params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		params.setMargins(dm.widthPixels / 10, dm.widthPixels / 10,
				dm.widthPixels / 10, dm.widthPixels / 10);
		RL_temp.setLayoutParams(params);
		layout.addView(RL_temp);
		// 注意顯示太多照片會out of memory
		int index = demoTest.file_list.size();
		if (index > 13) {
			index = 13;
		}

		for (int i = 1; i < index; i++) {

			Log.e("圖片網址：", demoTest.file_list.get(i));
			Bitmap bitmap = decodeBitmap(demoTest.file_list.get(i));

			ImageButton image_temp = new ImageButton(this);
			image_temp.setImageBitmap(bitmap);
			image_temp.setBackgroundColor(Color.BLUE);
			Log.e("oriID", Integer.toString(image_temp.getId()));
			image_temp.setId(i); // ID不能是零，不然會爛掉！
			Log.e("newID", Integer.toString(image_temp.getId()));
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
		surface = new RealSurface(this, dm.widthPixels, dm.heightPixels);
		this.addContentView(surface, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		// 加入使用者名單到RealSense
		sId = msa.getCount();// Server的id
		users = sId + 1;// 總共的user數
		// 沒有意外的話，users的最後一個表server

		for (int i = 0; i < users; i++) {
			Log.e("HP", "id:" + i + ". is" + Global.userName[i]);
			surface.target.add(new Target(Global.userName[i], 0,
					Global.userColor[i]));
		}

		degs = new int[sId + 1];

		readClientThread = new Thread[sId];
		for (int i = 0; i < sId; i++) {
			// 傳某client的id與總使用者數量給那個client
			msa.writeToId("init", i);
			msa.writeToId("" + i, i);
			msa.writeToId("" + users, i);
			// 對每一個client都用一個thread來管控它
			// 此thread會一直去讀client有沒有要送什麼訊息server
			// ReadClientThread拿到的handler可以用來送server
			readClientThread[i] = new Thread(new ReadClientThread(i, handler));
			readClientThread[i].start();
		}

		// 設定繪圖與傳遞照片之Thread
		// server自己處理自己的事情
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
						// String show = "Send to:";
						for (Target t : surface.selected) {
							// 先把target拿出來，比對id是不是自己，如果是的話就不加入要傳的list中
							for (i = 0; i < Global.userName.length; i++) {// 不知為何順序是反的，用查的比較安全
								if (t.name.equals(Global.userName[i])) {
									break;
								}
							}
							if (sId != i) {
								sendTargetList.add(i);
							}

							// temp=surface.showTarget.indexOf(t);
							// Log.e("HP",surface.showTarget.get(temp).name);

							// show += (t.name +
							// "  whose id is"+surface.showTarget.indexOf(t));
							// Log.e("HP",surface.showTarget.get(0).name); 可以拿名字
							// server不用考慮經過第三者傳遞的問題，可以直接開socket傳
						}
						// show=". The selected pic:"+Integer.toString(surface.selectedPhoto);

						// sendTargetList最後一格用來放要傳的圖片的id
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

	}

	@Override
	public void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
		Global.flagIsPlaying = false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

		if (surface.myDeg - event.values[0] > 10
				|| event.values[0] - surface.myDeg > 10) {
			surface.myDeg = (int) event.values[0];
			Message m = new Message();
			m.what = 0x101;
			m.arg1 = sId;
			m.arg2 = (int) event.values[0];
			handler.sendMessage(m);
		}

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
