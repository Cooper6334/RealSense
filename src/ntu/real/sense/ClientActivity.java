package ntu.real.sense;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import ntu.real.sense.ServerActivity.ServerFileOutputTransferThread;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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

public class ClientActivity extends Activity implements SensorEventListener {
	int imgBtnSize = 150;
	int imgMargin = 5;

	SensorManager sensorManager;

	RealSurface surface;
	RelativeLayout layout;
	int CurrentButtonNumber = 0; // CurrentButtonNumber流水號 設定物件ID

	ListAllPath demoTest;
	UriRelatedOperation UriRelatedOperation;
	
	Agent mca = Global.mClientAgent;
	int cId;
	int users;
	//用來handle client本身要傳的訊息
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {

			// 傳遞照片(可能是往server或往client)
			case 0x103:
				
				ArrayList<Integer> sendTargetList = (ArrayList<Integer>) m.obj;
				Integer picNumber=sendTargetList.remove(sendTargetList.size()-1);//要傳的圖片id
				String toastTemp="The selected picture: "+picNumber+". The selected nominances: ";
				//要傳的圖片檔案
				File tmpFile = new File(demoTest.file_list.get(picNumber));
				Uri outputFileUri = Uri.fromFile(tmpFile);
				
				
				//一個一個target送出
				Integer i;
				Integer t;
				for (i=0;i<sendTargetList.size();i++){
					
					
					t=sendTargetList.get(i);
					toastTemp+=t+",";
					Log.e("houpan","開始傳摟到:"+t);
					if(t==(users-1)){//對象是server
						Log.e("houpan","對象是server");
						new Thread(new ClientFileOutputTransferThread_toServer(t, outputFileUri)).start();
					}else{
						new Thread(new ClientFileOutputTransferThread_toClient(t, outputFileUri)).start();
					}
					
					
				}
				toastTemp+="\n圖片實際位址："+outputFileUri.toString();
				Toast.makeText(ClientActivity.this, toastTemp, Toast.LENGTH_SHORT)
						.show();

				
				
				//開socket，傳IP跟port，讓另外一端連上來
				
				break;
				
			
			case Global.SERVER_SEND_FILE_START://由server而來的傳輸
				Global.flagIsReceiving=true;
				Toast.makeText(ClientActivity.this, "收到圖片，開始傳遞", Toast.LENGTH_SHORT)
				.show();
				
				//要送出去的檔案
				Log.e("houpan","要收摟");
				new Thread(new ClinetFileInputTransferThread_fromServer()).start();
				break;
				
			case Global.SERVER_SEND_FILE_COMPLETED://傳完囉
				Global.flagIsReceiving=false;
				Toast.makeText(ClientActivity.this, "接收完成", Toast.LENGTH_SHORT)
				.show();
				//要送出去的檔案
				Log.e("houpan","收結束");
				
				break;

			case Global.SERVER_RECEIVE_FILE_COMPLETED://直接傳給server結束了
				Toast.makeText(ClientActivity.this, "傳輸完成(toServer)", Toast.LENGTH_SHORT)
				.show();
				//要送出去的檔案
				Log.e("houpan","收結束");
				break;
				
				
			case Global.CLIENT_SEND_FILE_START://收到其他client的資料
				Global.flagIsReceiving=true;
				Toast.makeText(ClientActivity.this, "收到圖片，開始傳遞", Toast.LENGTH_SHORT)
				.show();
				
				//要送出去的檔案
				Log.e("houpan","要收摟");
				
				new Thread(new ClinetFileInputTransferThread_fromClient(Integer.parseInt((String) m.obj),null)).start();
				break;
				
			case Global.CLIENT_SEND_FILE_COMPLETED://收到其他client的資料
				Global.flagIsReceiving=false;
				Toast.makeText(ClientActivity.this, "接收完成(fromClient)", Toast.LENGTH_SHORT)
				.show();
				
				break;
				
			case Global.CLIENT_SEND_FILE_COMPLETED_REMOTE://傳給client結束了(收到對方的回報)
				Toast.makeText(ClientActivity.this, "傳輸完成(toClient，ACK)", Toast.LENGTH_SHORT)
				.show();
				//要送出去的檔案
				Log.e("houpan","收結束");
				break;
			}

		}
	};
	
	public class ClientFileOutputTransferThread_toServer implements Runnable {
		Integer targetId;//為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;
		
		
		ClientFileOutputTransferThread_toServer(Integer i,Uri outputFileUri){
			this.targetId=i;
			this.outputFileUri=outputFileUri;
		}
		@Override
		public void run() {
			Socket socket = null;
			Log.e("houpan","why");
			
			mca.write("ClientSend_start_server");
				
			
			Message m = new Message();
			m.what = Global.SERVER_RECEIVE_FILE_COMPLETED;
			handler.sendMessage(m);
		}
	}
	
	public class ClientFileOutputTransferThread_toClient implements Runnable {
		Integer targetId;//為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;
		
		
		ClientFileOutputTransferThread_toClient(Integer i,Uri outputFileUri){
			this.targetId=i;
			this.outputFileUri=outputFileUri;
		}
		@Override
		public void run() {
			Socket socket = null;
			Log.e("houpan","why");
			
			mca.write("ClientSend_start_"+Global.mClientAgent.id+"_"+targetId);
				
			
			Message m = new Message();
			m.what = Global.SERVER_RECEIVE_FILE_COMPLETED;
			handler.sendMessage(m);
		}
	}
	
	public class ClinetFileInputTransferThread_fromServer implements Runnable {
		Integer targetId;//為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;
		
		
		@Override
		public void run() {
			/*
			Socket mmSocket = null;
			while(mmSocket!=null){
				WifiManager mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
				DhcpInfo mDhcpInfo = mWifiManager.getDhcpInfo();
				int ipadd = mDhcpInfo.gateway;
				Global.IP = ((ipadd & 0xFF) + "." + (ipadd >> 8 & 0xFF)
						+ "." + (ipadd >> 16 & 0xFF) + "." + (ipadd >> 24 & 0xFF));
				Log.e("ip", Global.IP);
				try {
					mmSocket = new Socket(Global.IP, Global.FILE_PORT);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			Log.e("houpan","收L1");
			File fileOutputStream= new File("/sdcard/DCIM/wifihpshared-" + System.currentTimeMillis()	+ ".jpg");
			InputStream inputstream =null;//client自己的socket input處
			
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
 				 Log.e("houpan","收L2");
 			  } catch (IOException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			  }
			try {
				Log.e("houpan","收L3");
				ntu.real.sense.UriRelatedOperation.copyFile(inputstream, new FileOutputStream(fileOutputStream));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.e("houpan","收L4");
			
			*/
			Message tempMessage = new Message();
			tempMessage.what = Global.SERVER_SEND_FILE_COMPLETED;//傳完了
			handler.sendMessage(tempMessage);
			
			mca.write("ClientReceive_completed_server");	
		}
	}
	

	public class ClinetFileInputTransferThread_fromClient implements Runnable {
		Integer sourceId;//為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;
		
		ClinetFileInputTransferThread_fromClient(Integer i,Uri outputFileUri){
			this.sourceId=i;
			//this.outputFileUri=outputFileUri;
		}
		
		@Override
		public void run() {

			Message tempMessage = new Message();
			tempMessage.what = Global.CLIENT_SEND_FILE_COMPLETED;//傳完了
			handler.sendMessage(tempMessage);
			
			mca.write("ClientReceive_completed_client_"+sourceId);	
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

		InputStream inputStream = null;
		RelativeLayout RL_temp = new RelativeLayout(this);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
//		params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		params.setMargins(dm.widthPixels / 10, dm.widthPixels / 10, dm.widthPixels / 10, dm.widthPixels / 10);
		RL_temp.setLayoutParams(params);
		layout.addView(RL_temp);
		// 注意顯示太多照片會out of memory
		int index = demoTest.file_list.size();
		if (index > 15) {
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
		surface = new RealSurface(this, dm.widthPixels, dm.heightPixels, index);
		this.addContentView(surface, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		// 設定繪圖與傳遞照片之Thread
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (Global.flagIsPlaying) {
					surface.drawView();
					if (surface.flagCanSend) {
						surface.flagCanSend = false;
						
						//要送的目標的id們
						ArrayList<Integer> sendTargetList=new ArrayList<Integer>();
						int i;
						Log.e("houpan","自己id:"+Global.mClientAgent.id);
						for (Target t : surface.selected) {
							//先把target拿出來，比對id是不是自己，如果是的話就不加入要傳的list中
							Log.e("houpan","要找的是是"+t.name);
							for (i=0;i<Global.userName.length;i++){//不知為何順序是反的，用查的比較安全
								Log.e("houpan","找"+Global.userName[i]);
								if(t.name.equals(Global.userName[i])){
									
									break;
								}
							}
							if(Global.mClientAgent.id!=i){
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
					if(!Global.flagIsReceiving){
						String m = mca.read();
						if ("init".equals(m)) {
							cId = Integer.parseInt(mca.read());
							users = Integer.parseInt(mca.read());
							//cId表client自己的id
							Log.e("init", cId + ":" + users);
							Global.mClientAgent.id=cId;
							for (int i = 0; i < users; i++) {
								surface.target.add(new Target(Global.userName[i],
										0, Global.userColor[i]));
							}
						}else if ("ServerSendFile_start".equals(m)) {//server傳檔案過來
							
							Message tempMessage = new Message();
							tempMessage.what = Global.SERVER_SEND_FILE_START;//server傳檔案
							handler.sendMessage(tempMessage);
							
						}else if(m.startsWith("ClinetSendFile_start_")){
							
							Message tempMessage = new Message();
							tempMessage.what = Global.CLIENT_SEND_FILE_START;//client傳檔案
							tempMessage.obj=m.split("_")[2];
							Log.e("houpan","client("+tempMessage.obj+")送檔案過來");
							handler.sendMessage(tempMessage);
						}else if("ClientReceive_completed_client".equals(m)){
							Message tempMessage = new Message();
							tempMessage.what = Global.CLIENT_SEND_FILE_COMPLETED_REMOTE;//從對面的client得到資訊：他收完了
							handler.sendMessage(tempMessage);
							
						}else if ("setdeg".equals(m)) {
							int who = Integer.parseInt(mca.read());
							int deg = Integer.parseInt(mca.read());
							surface.target.get(who).degree = deg;
						}
	
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

		surface.myDeg = (int) event.values[0];
		mca.write("setdeg");
		mca.write("" + surface.myDeg);

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
