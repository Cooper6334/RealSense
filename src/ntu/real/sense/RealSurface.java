package ntu.real.sense;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
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
import android.nfc.NdefMessage;

public class RealSurface extends SurfaceView {
	int selectedPhoto = -1;
	int photoNum = 0;
	boolean isLogged = false;
	String serverName = " ";
	boolean flagTouchUp = false;
	boolean flagLongTouch = false;
	boolean flagCanSend = false;
	boolean flagClick = false;
	int displayWidth = 480;
	int displayHeight = 800;
	int myDeg;
	int radius = 130;
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
	TouchPoint tp = new TouchPoint(radius);

	NfcAdapter nfcAdapter;

	Handler h = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case 0x101:
				if (!flagTouchUp && selectedPhoto > 0 && selectedPhoto <= 6) {
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
						showNfcDialog();
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
//				selected.clear();
//				SparseBooleanArray list = dlist.getCheckedItemPositions();
//
//				Log.e("server", "list" + list.size());
//				Target tmp;
//				for (int i = 0; i < list.size(); i++) {
//					Log.e("list", "select" + list.keyAt(i));
//					if (list.get(i)) {
//						if (list.keyAt(i) < myId) {
//							tmp = target.get(list.keyAt(i));
//							Log.e("list", "<id " + tmp.name);
//							selected.add(tmp);
//						} else {
//
//							tmp = target.get(list.keyAt(i) + 1);
//							Log.e("list", ">=id " + tmp.name);
//							selected.add(tmp);
//						}
//					}
//				}
				for (Target t : selected) {
					Log.e("list", "send to " + t.name);
				}
				if (cnt < 0) {
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

		nfcDialog = new Builder(getContext());
		nfcDialog.setTitle("NFC");
		nfcDialog.setMessage("請接觸手機以傳遞照片");
		nfcDialog.setNegativeButton("取消", new AlertDialog.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				nfcAdapter.setNdefPushMessageCallback(
						new CreateNdefMessageCallback() {

							public NdefMessage createNdefMessage(NfcEvent event) {
								// TODO Auto-generated method stub
								return null;
							}
						}, (Activity) RealSurface.this.getContext());
				ad.dismiss();
			}
		});

		nfcAdapter.setNdefPushMessageCallback(new CreateNdefMessageCallback() {

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

	void drawView() {

		Canvas canvas = holder.lockCanvas();

		if (canvas != null) {
			// canvas.drawColor(Color.argb(0, 0, 0, 0));
			canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
			canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);

			for (Target t : target) {
				if (serverName.equals(t.name)) {
					Paint degreeP = new Paint();
					degreeP.setFlags(Paint.ANTI_ALIAS_FLAG);
					degreeP.setColor(0xaaf1e5c4);
					degreeP.setTextSize(32);
					canvas.drawText(t.degree + "", displayWidth / 2 - 50,
							displayHeight * 4 / 5, degreeP);
				}
			}

			if (flagLongTouch) {

				if (selectedPhoto != 2 && selectedPhoto != 5
						&& selectedPhoto != 8 && selectedPhoto != 11) {
					radius = radius * 8 / 10;
				}
				Paint p2 = new Paint();
				// p2.setColor(0xff6b392d);
				p2.setColor(0xff8e1b22);
				p2.setFlags(Paint.ANTI_ALIAS_FLAG);
				p2.setShadowLayer(2, 0, 0, 0xff8e1b22);

				// canvas.drawCircle(px, py, radius * 1.5f, p2);
				// 內圓的色彩

				Paint p = new Paint();
				p.setColor(0xff3d3e1a);
				p.setFlags(Paint.ANTI_ALIAS_FLAG);
				p.setShadowLayer(5, 0, 0, Color.BLACK);
				// 除去title bar跟notification bar的高度

				// 是空的那環
				canvas.drawCircle(px, py, radius, p2);

				for (Target t : showTarget) {

					float deg = (int) (t.degree) % 360;
					Paint p3 = new Paint();
					p3.setColor(t.color);
					p3.setTextSize(32);
					p3.setShadowLayer(2, 0, 0, t.color);
					p3.setFlags(Paint.ANTI_ALIAS_FLAG);// 反鋸齒標籤，會比較漂亮
					p3.setFakeBoldText(true);

					Paint pStroke = new Paint();// 邊框
					pStroke.setColor(Color.BLACK);
					pStroke.setTextSize(32);
					pStroke.setFlags(Paint.ANTI_ALIAS_FLAG);
					pStroke.setShadowLayer(3, 0, 0, Color.BLACK);

					RectF oval = new RectF();
					oval.left = px - radius;
					oval.top = py - radius;
					oval.right = px + radius;
					oval.bottom = py + radius;
					canvas.drawArc(oval, deg + 60, 60, true, p3);

					Log.e("deg", deg + "");

					double ox = (radius + 40)
							* Math.cos((deg + 90) / 180 * Math.PI);
					double oy = (radius + 40)
							* Math.sin((deg + 90) / 180 * Math.PI);
					// 畫邊框

					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), pStroke);
					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), pStroke);
					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), pStroke);

					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), p3);
				}

				p2.setShadowLayer(2, 0, 0, 0xff78171d);
				// p2.setColor(0xff522c23);
				p2.setColor(0xff78171d);
				canvas.drawCircle(px, py, radius - 15, p2);

				p2.setColor(0xff420e12);
				p2.setShadowLayer(2, 0, 0, 0xff420e12);
				p2.setTextSize(55);

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

				if (selectedPhoto != 2 && selectedPhoto != 5
						&& selectedPhoto != 8 && selectedPhoto != 11) {

					radius = radius * 10 / 8;

				}
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
			setSelectedNumber(e.getX(), e.getY());
			return true;
		case MotionEvent.ACTION_MOVE:
			Log.e("tar", "  ");
			if (flagLongTouch) {
				double deg = tp.moveTouch(e.getX(), e.getY());

				if (deg == -1) {
					selected.clear();
				} else {
					for (Target t : showTarget) {

						float td = t.degree;
						while (td < 0) {
							td += 360;
						}
						Log.e("tar", t.name + ":" + td + " " + deg);
						if (td - deg < 30 && td - deg > -30) {
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

				boolean inrange = tp.removeTouch(e.getX(), e.getY());
				if (inrange && selected.size() > 0) {
					if (cnt < 0) {
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
		flagLongTouch = true;
		showTarget.clear();
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
		float minDeg = 180 / tmp.length;
		minDeg = 45;
		if (cnt <= 0) {
			for (int j = 1; j < tmp.length; j++) {
				for (int i = 0; i < tmp.length - j; i++) {
					if (tmp[i].degree > tmp[i + 1].degree) {
						Target t = tmp[i];
						tmp[i] = tmp[i + 1];
						tmp[i + 1] = t;
					}
				}
			}

			for (int i = 1; i < tmp.length; i++) {
				if (tmp[i].degree - tmp[i - 1].degree < minDeg) {
					float d = tmp[i - 1].degree + minDeg;
					if (d < 360 - minDeg) {
						tmp[i].degree = tmp[i - 1].degree + minDeg;
					}
				}

			}
			if (360 - tmp[tmp.length - 1].degree < minDeg) {
				tmp[tmp.length - 1].degree = 360 - minDeg;
			}
			for (int i = tmp.length - 2; i >= 0; i--) {
				if (tmp[i + 1].degree - tmp[i].degree < minDeg) {
					float d = tmp[i].degree - minDeg;
					if (d > 0) {
						tmp[i].degree = tmp[i + 1].degree - minDeg;
					}
				}

			}
		}
		for (int i = 0; i < tmp.length; i++) {
			showTarget.add(tmp[i]);
		}
	}

	void setTempTargetNoDeg() {
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
		for (int i = 0; i < tmp.length; i++) {
			tmp[i].degree = (180 - (tmp.length - 1) * 30) + i * 60;
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
}
