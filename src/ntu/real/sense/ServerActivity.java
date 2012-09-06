package ntu.real.sense;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

public class ServerActivity extends Activity implements SensorEventListener {
	int imgBtnSize=150;
	int imgMargin=5;
	boolean separationLine = false;
	boolean separationLineClient = false;

	RealSurface surface;
	RelativeLayout layout;
	int CurrentButtonNumber = 0; // CurrentButtonNumber流水號 設定物件ID

	ServerAgent msa = Global.mServerAgent;
	SensorManager sensorManager;

	int users;
	int degs[];
	int sId;
	Thread[] readClientThread;
	int picCycling;//1~12，看新進來的圖片要取代哪個thumbnails
	RelativeLayout RL_temp;
	
	//儲存每張圖片資訊的arraylist
	ListAllPath demoTest;
	UriRelatedOperation UriRelatedOperation;
	
	//接收來自某client(ReadClientThread傳來的)或server的自己的角度轉變等msg，並作處理(廣播給所有人之類的)
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
				Integer picNumber=sendTargetList.remove(sendTargetList.size()-1);//要傳的圖片id
				String toastTemp="The selected picture: "+picNumber+". The selected nominances: ";
				//要傳的圖片檔案
				File tmpFile = new File(demoTest.file_list.get(picNumber));
				Uri outputFileUri = Uri.fromFile(tmpFile);
				
				Global.endTime = new Time();
				Global.endTime.setToNow();
				Global.endTimeMs = System.currentTimeMillis();
				try{
			        FileWriter st = new FileWriter("/sdcard/ServerTimeLog.txt", true);
			        BufferedWriter bwST = new BufferedWriter(st); //將BufferedWeiter與FileWrite物件做連結
			        FileWriter ssd = new FileWriter("/sdcard/ServerSendDegree.txt", true);
			        BufferedWriter bwSSD = new BufferedWriter(ssd);
			        if(separationLine == false){
			        		bwST.write("-\tSeparation\tLine\t-\n");
			        		bwSSD.write("-\tSeparation\tLine\t-\n");
			        		separationLine = true;
			        		}
			        Global.now = new Time();
			        Global.now.setToNow();
			        bwST.write("<<\t" + Global.now + "\t>>\n");
			        bwST.write("start\t" + Global.startTime + "\n");
			        bwST.write("end\t" + Global.endTime + "\n");
			        bwST.write("startMs\t" + Global.startTimeMs + "\n");
			        bwST.write("endMs\t" + Global.endTimeMs + "\n\n");
			        bwSSD.write("<<\t" + Global.now + "\t>>\n");
			        for(Target t : Global.storedDegree){
		        			bwSSD.write("Name:\t" + t.name + "\t" + t.degree + "\n");
			        }bwSSD.newLine();
					bwST.close();
					bwSSD.close();
			    }catch(IOException e){
			       e.printStackTrace();
			    }
				
//				Log.e("WeiChen", Global.startTime + " start");
//				Log.e("WeiChen", Global.endTime + " end");
//				for(Target t : Global.storedDegree){
//					Log.e("WeiChen" , t.degree + "Name: " + t.name);
//				}
				
				//一個一個target送出
				Integer i;
				Integer t;
				for (i=0;i<sendTargetList.size();i++){
					t=sendTargetList.get(i);
					toastTemp+=t+",";
					Log.e("houpan","開始傳摟到:"+t);
					add_ServerFileOutputTransferThread(t,outputFileUri);
					//new Thread(new ServerFileOutputTransferThread(t, outputFileUri)).start();//不用thread的方式執行，而是單純序列化
					
				}
				toastTemp+="\n圖片實際位址："+outputFileUri.toString();
				Toast.makeText(ServerActivity.this, toastTemp, Toast.LENGTH_SHORT)
						.show();

				
				
				//開socket，傳IP跟port，讓另外一端連上來
				
				break;
				//傳照片給client且結束了
				case Global.CLIENT_RECEIVE_COMPLETED:
					
					
					Toast.makeText(ServerActivity.this, "傳輸完成", Toast.LENGTH_SHORT)
					.show();
				break;

				
				//接收由client而來的傳輸
				case Global.CLIENT_SEND_FILE_START:
					if(Global.storedDegree!=null){
						try{
					        FileWriter csd = new FileWriter("/sdcard/ClientSendDegree.txt", true);
					        BufferedWriter bwCSD = new BufferedWriter(csd);
					        if(separationLineClient == false){
				        			bwCSD.write("-\tSeparation\tLine\t-\n");
				        		separationLineClient = true;
				        		}
					        Global.now = new Time();
					        Global.now.setToNow();
					        bwCSD.write("<<\t" + Global.now + "\t>>\n");
					        for(Target tg : Global.storedDegree){
					        		bwCSD.write("Name:\t" + tg.name + "\t" + tg.degree + "\n");
					        }bwCSD.newLine();
					        bwCSD.close();
					    }catch(IOException e){
					       e.printStackTrace();.
					    }	
			        }else{
			        	Global.storedDegree=new ArrayList<Target>(); 
			        }
//					for(Target tg : Global.storedDegree){
//						Log.e("WeiChen" , tg.degree + "Name: " + tg.name);
//					}
					
					if(((String)m.obj).split("_").length==1){//直接傳到server
						//開始接收
						Log.e("houpan","收到來自client的訊息(toServer)");
						Toast.makeText(ServerActivity.this, "開始接收", Toast.LENGTH_SHORT)
						.show();
						new Thread(new ServerFileInputTransferThread(Integer.parseInt(((String)m.obj).split("_")[0]))).start();//傳sourceId進去
						
					}else{//經server傳到另一client
						int sourceId=Integer.parseInt(((String)m.obj).split("_")[0]);
						int targetId=Integer.parseInt(((String)m.obj).split("_")[1]);
						Log.e("houpan","收到來自client的訊息(toClient):"+sourceId+","+targetId);
						Log.e("houpan","ClinetSendFile_start_"+sourceId+"_"+Global.mServerAgent.clients.get(sourceId).address.split("/")[1]);
						
						//server要附上source的IP不然接收端的client會不知道socket要連去哪
						msa.writeToId("ClinetSendFile_start_"+sourceId+"_"+Global.mServerAgent.clients.get(sourceId).address.split("/")[1], targetId);
						Toast.makeText(ServerActivity.this, "轉傳至"+targetId, Toast.LENGTH_SHORT)
						.show();						
					}	
				break;
				
				
				case Global.CLIENT_SEND_FILE_COMPLETED:
					
					//重新設定view
					demoTest.file_list.setElementAt((String)m.obj, picCycling);
					ImageButton image_temp = (ImageButton) RL_temp.findViewById(picCycling);
					Log.e("houpan","picCycling:"+picCycling);
					Bitmap bitmap = decodeBitmap(demoTest.file_list.get(picCycling));
					image_temp.setImageBitmap(bitmap);
					picCycling=(picCycling-6)%6+7;
					Log.e("houpan","picCycling:"+picCycling);
					
					Toast.makeText(ServerActivity.this, "接收完成", Toast.LENGTH_SHORT)
					.show();
				break;
				
				
			}

		}
	};
	
	//以下是用來序列化檔案傳出
	Vector<Integer> outputTransfer_t;
	Vector<Uri> outputTransfer_outputFileUri;
	Vector<Integer> operationQueue;//1:有job
	public void add_ServerFileOutputTransferThread(Integer t, Uri outputFileUri){
		outputTransfer_t.add(t);
		outputTransfer_outputFileUri.add(outputFileUri);
		operationQueue.add(1);
	}
	
	public class ServerFileOutputTransferThread_manager implements Runnable{
		
		ServerFileOutputTransferThread_manager(){
			outputTransfer_t = new Vector<Integer>();
			outputTransfer_outputFileUri= new Vector<Uri>() ;
			operationQueue= new Vector<Integer>() ;//1:有job
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true){
				if(operationQueue.size()!=0){//queue裡面有東西
					operationQueue.remove(0);
					new ServerFileOutputTransferThread(outputTransfer_t.remove(0),outputTransfer_outputFileUri.remove(0)).run();
					
				}else{
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
	
	//直接收client的資料
	public class ServerFileInputTransferThread implements Runnable {
		Integer sourceId;//為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;
				
		ServerFileInputTransferThread(int sourceId){
			this.sourceId=sourceId;
		}
		
		@Override
		public void run() {
			

			Socket mmSocket = null;
			while(mmSocket==null){
				try {
					Log.e("houpan","IP是"+Global.mServerAgent.clients.get(sourceId).address.split("/")[1]);
					mmSocket = new Socket(Global.mServerAgent.clients.get(sourceId).address.split("/")[1], Global.FILE_PORT);
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
		
			
			
			
			
			
			

			Log.e("houpan","收L1");
			String fileAbsolutePath="/sdcard/DCIM/Camera/wifihpshared_" + System.currentTimeMillis()+ ".jpg";
			File fileOutputStream= new File(fileAbsolutePath);
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
			try {
				mmSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Log.e("houpan","收L5");
			
			
			
			
			Message tempMessage = new Message();
			tempMessage.what = Global.CLIENT_SEND_FILE_COMPLETED;//傳完了
			tempMessage.obj=fileAbsolutePath;
			handler.sendMessage(tempMessage);
			
		}
	}
	
	//
	public class ServerFileOutputTransferThread implements Runnable {
		Integer targetId;//為了把內thread不能改動外變數的錯誤濾掉
		Uri outputFileUri;
		
		
		ServerFileOutputTransferThread(Integer i,Uri outputFileUri){
			this.targetId=i;
			this.outputFileUri=outputFileUri;
		}
		@Override
		public void run() {

			msa.writeToId("ServerSendFile_start", targetId);
			
			Socket mmSocket = null;
			while(mmSocket==null){
				try {
					Log.e("houpan","IP是"+Global.mServerAgent.clients.get(targetId).address.split("/")[1]);
					mmSocket = new Socket(Global.mServerAgent.clients.get(targetId).address.split("/")[1], Global.FILE_PORT);
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
		
			Log.e("houpan","傳L1");
			ContentResolver cr = getContentResolver();
		    //要傳出去的圖片
			InputStream inputUriStream = null;
		    //要收下的client
			OutputStream outputDataStream=null;
			
		    try {
				inputUriStream = cr.openInputStream(outputFileUri);
				Log.e("houpan","傳L2");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    try {
		    	Log.e("houpan","傳L2");
				outputDataStream = mmSocket.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		      
		    Log.e("houpan","傳L3");
		    ntu.real.sense.UriRelatedOperation.copyFile(inputUriStream, outputDataStream);
		    Log.e("houpan","傳L4");
		    try {
				mmSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Message m = new Message();
			m.what = Global.CLIENT_RECEIVE_COMPLETED;
			handler.sendMessage(m);
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
		picCycling=7;//把cycling先設在下一個要補的地方
		if (index >= 6) {
			index = 6;
		}
		
		
		for (int i = 1; i < 13; i++) {

			
			ImageButton image_temp = new ImageButton(this);
			if(i<index){//實際上方格裡面有東西的狀態
				Log.e("圖片網址：", demoTest.file_list.get(i));
				Bitmap bitmap = decodeBitmap(demoTest.file_list.get(i));
				image_temp.setImageBitmap(bitmap);
			}else{
				demoTest.file_list.add(null);
			}
			if(i<=6){
				image_temp.setBackgroundColor(Color.RED);
			}else{
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
		surface = new RealSurface(this, dm.widthPixels, dm.heightPixels,index, Global.userName[msa.getCount()]);
		this.addContentView(surface, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		// 加入使用者名單到RealSense
		sId = msa.getCount();//Server的id
		users = sId + 1;//總共的user數
		//沒有意外的話，users的最後一個表server

		for (int i = 0; i < users; i++) {
			Log.e("HP","id:"+i+". is"+Global.userName[i]);
			surface.target.add(new Target(Global.userName[i], 0, Global.userColor[i]));
		}

		degs = new int[sId + 1];

		readClientThread = new Thread[sId];
		for (int i = 0; i < sId; i++) {
			//傳某client的id與總使用者數量給那個client 
			msa.writeToId("init", i);
			msa.writeToId("" + i, i);
			msa.writeToId("" + users, i);
			//對每一個client都用一個thread來管控它
			//此thread會一直去讀client有沒有要送什麼訊息server
			//ReadClientThread拿到的handler可以用來送server
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
						//要送的目標的id們
						ArrayList<Integer> sendTargetList=new ArrayList<Integer>();
						int i;
						//String show = "Send to:";
						for (Target t : surface.selected) {
							//先把target拿出來，比對id是不是自己，如果是的話就不加入要傳的list中
							for (i=0;i<Global.userName.length;i++){//不知為何順序是反的，用查的比較安全
								if(t.name.equals(Global.userName[i])){
									break;
								}
							}
							if(sId!=i){
								sendTargetList.add(i);	
							}
							
							
							//temp=surface.showTarget.indexOf(t);
							//Log.e("HP",surface.showTarget.get(temp).name); 

							
							
							//show += (t.name + "  whose id is"+surface.showTarget.indexOf(t));
							//Log.e("HP",surface.showTarget.get(0).name); 可以拿名字
							//server不用考慮經過第三者傳遞的問題，可以直接開socket傳
						}
						//show=". The selected pic:"+Integer.toString(surface.selectedPhoto);
						
						//sendTargetList最後一格用來放要傳的圖片的id
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
		
		new Thread(new ServerFileOutputTransferThread_manager()).start();
	}

	@Override
	public void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
		Global.flagIsPlaying = false;
		/*		
		if (msa != null) {
			msa.clear();
			msa = null;
		}
*/		
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
