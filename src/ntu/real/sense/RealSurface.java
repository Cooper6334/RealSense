package ntu.real.sense;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class RealSurface extends SurfaceView {
	int selectedPhoto = -1;
	Bitmap selectedPhotoBitmap;
	boolean isBigImage = false;
	int photoNum = 0;
	boolean isLogged = false;
	String serverName = " ";
	boolean flagTouchUp = false;
	boolean flagLongTouch = false;
	boolean flagCanSend = false;
	boolean flagClick = false;
	boolean flagHaveNfc = false;
	int displayWidth = 480;
	int displayHeight = 800;
	int myDeg;
	int tmpDeg;
	int radius = 195;
	int cnt = -1;// 0 for testing mode
	int myId;
	float px, py;
	SurfaceHolder holder;
	ArrayList<Target> target = new ArrayList<Target>();
	ArrayList<Target> showTarget = new ArrayList<Target>();
	Set<Target> selected = new HashSet<Target>();
	Dialog menuDialog;
	Builder nfcDialog;
	AlertDialog ad;
	ListView dlist;
	TouchPoint tp = new TouchPoint();
	float minDeg;
	NfcAdapter nfcAdapter;

	Handler h = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case 0x101:
				if (!flagTouchUp && isBigImage) {

					if (Global.selectWay == 0) {
						setTempTarget();
						logStart();
					} else if (Global.selectWay == 1) {
						setTempTargetNoDeg();
						logStart();
					} else if (Global.selectWay == 2) {
						showTempDialog();
						logStart();
					} else if (Global.selectWay == 3) {
						if (flagHaveNfc) {
							showNfcDialog();
						} else {
							Toast.makeText(RealSurface.this.getContext(),
									"NFC is not avaliable", Toast.LENGTH_LONG)
									.show();
						}
						logStart();
					}

				}
				flagTouchUp = false;
				break;

			}
		}
	};

	public RealSurface(Context context, int num, NfcAdapter a) {
		super(context);
		setZOrderOnTop(true);
		holder = getHolder();
		holder.setFormat(PixelFormat.TRANSPARENT);
		nfcAdapter = a;
		initView();
		// TODO Auto-generated constructor stub
	}

	public RealSurface(Context context, int width, int height, int num,
			NfcAdapter a) {
		super(context);
		setZOrderOnTop(true);
		holder = getHolder();
		holder.setFormat(PixelFormat.TRANSPARENT);
		displayWidth = width;
		displayHeight = height;
		radius = radius * displayWidth / 768;
		photoNum = num;
		nfcAdapter = a;
		initView();
		// TODO Auto-generated constructor stub
	}

	public RealSurface(Context context, int width, int height, int num,
			String name, NfcAdapter a) {
		super(context);
		setZOrderOnTop(true);
		holder = getHolder();
		holder.setFormat(PixelFormat.TRANSPARENT);
		displayWidth = width;
		displayHeight = height;
		radius = radius * displayWidth / 768;
		photoNum = num;
		serverName = name;
		nfcAdapter = a;
		initView();
		// TODO Auto-generated constructor stub
	}

	void initView() {

		menuDialog = new Dialog(this.getContext());
		menuDialog.setTitle("Menu");

		menuDialog.setContentView(R.layout.realdialog);
		dlist = (ListView) menuDialog.findViewById(R.id.listView1);
		dlist.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		dlist.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				// TODO Auto-generated method stub
				if (index < myId) {
					if (selected.contains(target.get(index))) {
						selected.remove(target.get(index));
						Log.e("menu", "cancle select " + target.get(index).name);
					} else {
						selected.add(target.get(index));
						Log.e("menu", "select " + target.get(index).name);
					}
				} else {
					if (selected.contains(target.get(index + 1))) {
						selected.remove(target.get(index + 1));
						Log.e("menu", "cancle select "
								+ target.get(index + 1).name);
					} else {
						selected.add(target.get(index + 1));
						Log.e("menu", "select " + target.get(index + 1).name);
					}
				}

			}
		});
		Button btn = (Button) menuDialog.findViewById(R.id.button1);
		btn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				for (Target t : selected) {
					Log.e("list", "send to " + t.name);
				}
				if (cnt < 0) {
					isBigImage = false;
					flagCanSend = true;
				}
				menuDialog.dismiss();
			}
		});
		Button btn2 = (Button) menuDialog.findViewById(R.id.button2);
		btn2.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				menuDialog.dismiss();
			}
		});

		if (nfcAdapter != null) {

			flagHaveNfc = true;
			nfcDialog = new Builder(getContext());
			nfcDialog.setTitle("NFC");
			nfcDialog.setMessage("請接觸手機以傳遞照片");
			nfcDialog.setNegativeButton("取消",
					new AlertDialog.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							nfcAdapter.setNdefPushMessageCallback(
									new CreateNdefMessageCallback() {

										public NdefMessage createNdefMessage(
												NfcEvent event) {
											// TODO Auto-generated method stub
											return null;
										}
									}, (Activity) RealSurface.this.getContext());
							ad.dismiss();
						}
					});

			nfcAdapter.setNdefPushMessageCallback(
					new CreateNdefMessageCallback() {

						public NdefMessage createNdefMessage(NfcEvent event) {
							// TODO Auto-generated method stub
							return null;
						}
					}, (Activity) RealSurface.this.getContext());

			nfcAdapter.setOnNdefPushCompleteCallback(
					new OnNdefPushCompleteCallback() {

						public void onNdefPushComplete(NfcEvent event) {
							// TODO Auto-generated method stub

							// Toast.makeText(MainActivity.this, "finish",
							// Toast.LENGTH_LONG).show();
							nfcAdapter.setNdefPushMessageCallback(
									new CreateNdefMessageCallback() {

										public NdefMessage createNdefMessage(
												NfcEvent event) {
											// TODO Auto-generated method stub
											return null;
										}
									}, (Activity) RealSurface.this.getContext());
							if (ad != null) {
								ad.dismiss();
							}

						}
					}, (Activity) this.getContext());
		}
	}

	void drawView() {
		// Log.e("bigImage", isBigImage + "");

		Canvas canvas = holder.lockCanvas();

		if (canvas != null) {
			// canvas.drawColor(Color.argb(0, 0, 0, 0));
			if (isBigImage) {
				drawBigPicture(canvas);
			} else {
				canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
			}

			for (Target t : target) {
				if (Global.name.equals(t.name)) {
					Paint degreeP = new Paint();
					degreeP.setFlags(Paint.ANTI_ALIAS_FLAG);
					degreeP.setColor(0xaaf1e5c4);
					degreeP.setTextSize(32);
					canvas.drawText(t.degree + "", displayWidth / 2 - 50,
							displayHeight * 4 / 5, degreeP);
					break;
				}
			}

			if (flagLongTouch) {

				Paint p2 = new Paint();
				// p2.setColor(0xffeed0ff);
				p2.setColor(Color.rgb(74, 167, 224));
				p2.setFlags(Paint.ANTI_ALIAS_FLAG);
				p2.setShadowLayer(2, 0, 0, 0xffeed0ff);

				// 外圓
				canvas.drawCircle(px, py, radius * 1f, p2);
				// 內圓的色彩

				Paint p = new Paint();
				// p.setColor(0xff3d3e1a);
				p.setColor(Color.rgb(74, 167, 224));
				p.setFlags(Paint.ANTI_ALIAS_FLAG);
				p.setShadowLayer(5, 0, 0, Color.BLACK);
				// 除去title bar跟notification bar的高度

				// 是空的那環
				canvas.drawCircle(px, py, radius, p2);

				for (Target t : showTarget) {

					// float deg = (int) (t.degree - (myDeg - tmpDeg)) % 360;
					float deg = t.degree;
					Paint p3 = new Paint();

					p3.setColor(t.color);
					p3.setTextSize(32);
					p3.setShadowLayer(2, 0, 0, t.color);
					p3.setFlags(Paint.ANTI_ALIAS_FLAG);// 反鋸齒標籤，會比較漂亮

					float growth = (float) 1.3;

					// 被選取到的話就把透明度拉高
					if (selected.contains(t)) {
						p3.setAlpha(255);
						growth = (float) 1.5;
					} else {
						p3.setAlpha(155);
						growth = (float) 1.3;// 被選到的時候要變大一點
					}
					p3.setFakeBoldText(true);

					Paint pStroke = new Paint();// 邊框
					pStroke.setColor(Color.BLACK);
					pStroke.setTextSize(32);
					pStroke.setFlags(Paint.ANTI_ALIAS_FLAG);
					pStroke.setShadowLayer(3, 0, 0, Color.BLACK);

					RectF oval = new RectF();
					oval.left = (float) (px - radius * growth);
					oval.top = (float) (py - radius * growth);
					oval.right = (float) (px + radius * growth);
					oval.bottom = (float) (py + radius * growth);
					//
					// float arcSweepAngle = 360 / Global.userNumber;
					// Log.e("houpan", "Global.userNumber=" +
					// Global.userNumber);
					// if (arcSweepAngle < 45) {
					// arcSweepAngle = 45;
					// }

					canvas.drawArc(oval, deg + 90, minDeg, true, p3);

					Log.e("deg", deg + "");

					double ox = (radius + 40)
							* Math.cos((deg + 90 + minDeg / 2) / 180 * Math.PI);
					double oy = (radius + 40)
							* Math.sin((deg + 90 + minDeg / 2) / 180 * Math.PI);
					// 畫邊框

					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), pStroke);
					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), pStroke);
					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), pStroke);

					p3.setColor(Color.WHITE);
					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), p3);
				}

				p2.setShadowLayer(2, 0, 0, 0xffeed0ff);
				// p2.setColor(0xffeed0ff);
				p2.setColor(Color.rgb(161, 197, 224));
				canvas.drawCircle(px, py, radius - 15, p2);

				p2.setColor(0xffe5bdfd);
				p2.setShadowLayer(2, 0, 0, 0xffe5bdfd);
				p2.setTextSize(45);

				// if(Global.flagIsServer){
				// canvas.drawText(target.size()-1).name, (float) (px) - 70,
				// (float) (py)+15, p2);
				//
				// }else{
				// canvas.drawText(Global.userName[Global.mClientAgent.id],
				// (float) (px) - 70,
				// (float) (py)+15, p2);
				//
				// }
				canvas.drawText(target.get(myId).name, (float) (px) - 70,
						(float) (py) + 15, p2);

			} else {
				isLogged = false;
			}
			holder.unlockCanvasAndPost(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		Log.e("sur", "touch");
		if (!flagLongTouch) {
			px = e.getX();
			py = e.getY();
			tp.setTouch(e.getX(), e.getY());
		}
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			h.removeMessages(0x101);
			h.sendEmptyMessageDelayed(0x101, 200);
			flagTouchUp = false;
			if (!isBigImage) {
				setSelectedNumber(e.getX(), e.getY());
				Log.e("123", "set:" + selectedPhoto + "");
			}
			return true;
		case MotionEvent.ACTION_MOVE:
			Log.e("tar", "  ");
			if (flagLongTouch) {
				double deg = tp.moveTouch(e.getX(), e.getY(), radius);

				if (deg == -1) {
					selected.clear();
				} else {
					for (Target t : showTarget) {

						float td = t.degree;
						while (td < 0) {
							td += 360;
						}
						Log.e("tar", t.name + ":" + td + " " + deg);
						if (deg - td > 0 && deg - td < minDeg) {
							selected.add(t);
						}
					}
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			flagTouchUp = true;
			if (flagLongTouch) {
				flagLongTouch = false;

				boolean inrange = tp.removeTouch(e.getX(), e.getY(), radius);
				if (inrange && selected.size() > 0) {
					if (cnt < 0) {
						isBigImage = false;
						flagCanSend = true;
					}
				}
				// else {
				// Toast.makeText(this.getContext(), "not in range",
				// Toast.LENGTH_LONG).show();
				// }
			} else {
				flagClick = true;
			}
			return false;
		}
		return true;
	}

	public void setSelectedNumber(float x, float y) {
		int centerWidth = displayWidth / 2;
		int centerHeight = displayHeight / 2;
		int imgWidth = displayWidth / 4;
		int imgHeight = displayHeight / 4;
		int layoutMargin = displayWidth / 10;
		int imgMargin = displayWidth / 100;

		if (x >= layoutMargin + imgMargin
				&& x <= layoutMargin + imgMargin + imgWidth) {
			if (y >= layoutMargin + imgMargin
					&& y <= layoutMargin + imgMargin + imgWidth) {

				selectedPhoto = 1;
			} else if (y >= layoutMargin + imgMargin * 3 + imgWidth
					&& y <= layoutMargin + imgMargin * 3 + imgWidth * 2) {
				selectedPhoto = 4;
			} else if (y >= layoutMargin + imgMargin * 5 + imgWidth * 2
					&& y <= layoutMargin + imgMargin * 5 + imgWidth * 3) {
				selectedPhoto = 7;
			} else if (y >= layoutMargin + imgMargin * 7 + imgWidth * 3
					&& y <= layoutMargin + imgMargin * 7 + imgWidth * 4) {
				selectedPhoto = 10;
			} else {
				selectedPhoto = -1;
			}
		} else if (x >= layoutMargin + imgMargin * 3 + imgWidth
				&& x <= layoutMargin + imgMargin * 3 + imgWidth * 2) {
			if (y >= layoutMargin + imgMargin
					&& y <= layoutMargin + imgMargin + imgWidth) {
				selectedPhoto = 2;
			} else if (y >= layoutMargin + imgMargin * 3 + imgWidth
					&& y <= layoutMargin + imgMargin * 3 + imgWidth * 2) {
				selectedPhoto = 5;
			} else if (y >= layoutMargin + imgMargin * 5 + imgWidth * 2
					&& y <= layoutMargin + imgMargin * 5 + imgWidth * 3) {
				selectedPhoto = 8;
			} else if (y >= layoutMargin + imgMargin * 7 + imgWidth * 3
					&& y <= layoutMargin + imgMargin * 7 + imgWidth * 4) {
				selectedPhoto = 11;
			} else {
				selectedPhoto = -1;
			}
		} else if (x >= layoutMargin + imgMargin * 5 + imgWidth * 2
				&& x <= layoutMargin + imgMargin * 5 + imgWidth * 3) {
			if (y >= layoutMargin + imgMargin
					&& y <= layoutMargin + imgMargin + imgWidth) {
				selectedPhoto = 3;
			} else if (y >= layoutMargin + imgMargin * 3 + imgWidth
					&& y <= layoutMargin + imgMargin * 3 + imgWidth * 2) {
				selectedPhoto = 6;
			} else if (y >= layoutMargin + imgMargin * 5 + imgWidth * 2
					&& y <= layoutMargin + imgMargin * 5 + imgWidth * 3) {
				selectedPhoto = 9;
			} else if (y >= layoutMargin + imgMargin * 7 + imgWidth * 3
					&& y <= layoutMargin + imgMargin * 7 + imgWidth * 4) {
				selectedPhoto = 12;
			} else {
				selectedPhoto = -1;
			}
		} else {
			selectedPhoto = -1;
		}
		Log.e("selectedPhotoIndex", selectedPhoto + "");
	}

	// 建立圓弧
	void setTempTarget() {
		tmpDeg = myDeg;
		flagLongTouch = true;
		showTarget.clear();
		minDeg = 360f / (float) target.size();
		Log.e("real", "min deg=" + minDeg);
		// cnt for testing
		if (cnt >= 0) {
			cnt = (cnt + 1) % 2;
		}

		Target tmp[] = null;
		if (cnt < 0) {
			float d = target.get(myId).degree;
			tmp = new Target[target.size() - 1];
			for (int i = 0; i < target.size(); i++) {
				if (i < myId) {
					tmp[i] = target.get(i).clone(d);
				} else if (i > myId) {
					tmp[i - 1] = target.get(i).clone(d);
				}
			}
			// tmp=new Target[target.size()];
			// for (int i = 0; i < target.size(); i++) {
			// tmp[i]=target.get(i);
			// }
		} else {

			tmp = new Target[5];
			tmp[0] = new Target(Global.userName[0], 0, Global.userColor[0]);
			tmp[1] = new Target(Global.userName[1], 30, Global.userColor[1]);
			tmp[2] = new Target(Global.userName[2], 50, Global.userColor[2]);
			tmp[3] = new Target(Global.userName[3], 260, Global.userColor[3]);
			tmp[4] = new Target(Global.userName[4], 280, Global.userColor[4]);
		}

		for (int j = 1; j < tmp.length; j++) {
			for (int i = 0; i < tmp.length - j; i++) {
				if (tmp[i].degree > tmp[i + 1].degree) {
					Target t = tmp[i];
					tmp[i] = tmp[i + 1];
					tmp[i + 1] = t;
				}
			}
		}

		for (int i = 0; i < tmp.length; i++) {
			tmp[i].degree = minDeg * (i + 0.5f);
		}
		if (minDeg < 45) {
			minDeg = 45;
		}
		// minDeg = 90;
		// if (cnt <= 0) {

		//
		// for (int i = 1; i < tmp.length; i++) {
		// if (tmp[i].degree - tmp[i - 1].degree < minDeg) {
		// float d = tmp[i - 1].degree + minDeg;
		// if (d < 360 - minDeg) {
		// tmp[i].degree = tmp[i - 1].degree + minDeg;
		// }
		// }
		//
		// }
		// if (360 - tmp[tmp.length - 1].degree < minDeg) {
		// tmp[tmp.length - 1].degree = 360 - minDeg;
		// }
		// for (int i = tmp.length - 2; i >= 0; i--) {
		// if (tmp[i + 1].degree - tmp[i].degree < minDeg) {
		// float d = tmp[i].degree - minDeg;
		// if (d > 0) {
		// tmp[i].degree = tmp[i + 1].degree - minDeg;
		// }
		// }
		//
		// }
		// }
		for (int i = 0; i < tmp.length; i++) {
			showTarget.add(tmp[i]);
		}
	}

	void setTempTargetNoDeg() {
		minDeg = 360f / (float) target.size();
		flagLongTouch = true;
		showTarget.clear();
		Target[] tmp = new Target[target.size() - 1];
		for (int i = 0; i < target.size(); i++) {
			if (i < myId) {
				tmp[i] = target.get(i).clone();
			} else if (i > myId) {
				tmp[i - 1] = target.get(i).clone();
			}
		}

		for (int j = 1; j < tmp.length; j++) {
			for (int i = 0; i < tmp.length - j; i++) {
				if (tmp[i].name.compareTo(tmp[i + 1].name) > 0) {
					Target t = tmp[i];
					tmp[i] = tmp[i + 1];
					tmp[i + 1] = t;
				}
			}
		}

		for (int i = 0; i < tmp.length; i++) {
			tmp[i].degree = minDeg * (i + 0.5f);
		}
		if (minDeg < 45) {
			minDeg = 45;
		}
		for (int i = 0; i < tmp.length; i++) {
			showTarget.add(tmp[i]);
		}
	}

	void showTempDialog() {

		ArrayList<String> nameList = new ArrayList<String>();
		for (int i = 0; i < target.size(); i++) {
			if (i != myId) {
				nameList.add(target.get(i).name);
			}
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_list_item_multiple_choice, nameList);
		dlist.setAdapter(adapter);
		menuDialog.show();
	}

	void showNfcDialog() {
		// NdefMessage message = new NdefMessage(new NdefRecord[] {
		// new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
		// "text/plain".getBytes(), new byte[] {},
		// "test123".getBytes()),
		// new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
		// "text/plain".getBytes(), new byte[] {},
		// "test456".getBytes()),
		// NdefRecord.createApplicationRecord(this.getContext()
		// .getPackageName()) });
		// nfcAdapter.setNdefPushMessage(message, (Activity) getContext());

		nfcAdapter.setNdefPushMessageCallback(new CreateNdefMessageCallback() {

			public NdefMessage createNdefMessage(NfcEvent event) {
				// TODO Auto-generated method stub
				return new NdefMessage(new NdefRecord[] {
						new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain"
								.getBytes(), new byte[] {}, (myId + "")
								.getBytes()),
						new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain"
								.getBytes(), new byte[] {},
								(selectedPhoto + "").getBytes()),
						NdefRecord.createApplicationRecord(getContext()
								.getPackageName()) });
			}
		}, (Activity) RealSurface.this.getContext());
		ad = nfcDialog.show();
	}

	public void setId(int id) {

		myId = id;
	}

	public void sendPhoto(int id, int photo) {
		selectedPhoto = photo;
		selected.clear();
		selected.add(target.get(id));
		if (cnt < 0) {
			isBigImage = false;
			flagCanSend = true;
		}
	}

	public void setName(int id, String name) {
		target.get(id).name = name;
		Global.userName[id] = name;
	}

	public void logStart() {
		Global.startTime = new Time();
		Global.startTime.setToNow();
		Global.startTimeMs = System.currentTimeMillis();
		Global.storedDegree = (ArrayList<Target>) target.clone();
	}

	public void drawBigPicture(Canvas canvas) {
		// Canvas canvas = holder.lockCanvas();
		BitmapFactory.Options op = new BitmapFactory.Options();
		op.inSampleSize = 4;
		Log.e("123", "1:" + selectedPhoto + "");
		selectedPhotoBitmap = BitmapFactory.decodeFile(
				Global.demoTest.file_list.get(selectedPhoto), op);

		Matrix matrix = new Matrix();
		matrix.postScale(displayWidth / (float) selectedPhotoBitmap.getWidth(),
				displayHeight / (float) selectedPhotoBitmap.getHeight());
		canvas.drawBitmap(selectedPhotoBitmap, matrix, null);
		// holder.unlockCanvasAndPost(canvas);
	}
}
