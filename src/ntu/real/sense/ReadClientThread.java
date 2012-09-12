package ntu.real.sense;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ReadClientThread implements Runnable {
	int cId;
	Handler gameHandler;
	ServerAgent msa = Global.mServerAgent;

	ReadClientThread(int inId, Handler inHandler) {
		cId = inId;
		gameHandler = inHandler;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (Global.flagIsPlaying) {
			// Log.e("rct", cId + " rct running");
			String tmp = msa.readFromId(cId);
			// Log.e("rct", cId + " read " + tmp);
			if (tmp != null) {
				if (tmp.startsWith("setdeg")) {

					Message m = new Message();
					m.what = 0x101;
					m.arg1 = cId;
					m.arg2 = Integer.parseInt(tmp.split("_")[1]);
					// 把訊息送回server端
					gameHandler.sendMessage(m);
				} else if (tmp.equals("ClientReceive_completed")) {// client接收完成了
					Message m = new Message();
					m.what = Global.CLIENT_RECEIVE_COMPLETED;
					// 把訊息送回server端
					gameHandler.sendMessage(m);
				} else if (tmp.startsWith("ClientSend_start_")) {// client送檔案訊息到本地端server或跳板
					Message m = new Message();
					Log.e("houpan", "收到來自client的訊息");
					m.what = Global.CLIENT_SEND_FILE_START;
					m.obj = tmp.split("_", 3)[2];// 目標:來源id(to server)或是 id to
													// id
					// 把訊息送回server端
					gameHandler.sendMessage(m);

				} else if (tmp.startsWith("ClientReceive_completed_client_")) {// 接收的client接收結束，回傳給原傳的client
					Global.mServerAgent.writeToId(
							"ClientReceive_completed_client",
							Integer.parseInt(tmp.split("_")[3]));

				} else if (tmp.startsWith("nfcphoto")) {
					int toId = Integer.parseInt(tmp.split("_")[1]);
					int toPhoto = Integer.parseInt(tmp.split("_")[2]);

					if (toId == msa.getCount()) {
						Message m = new Message();
						m.what = 0x118;
						m.arg1 = cId;
						m.arg2 = toPhoto;
						gameHandler.sendMessage(m);
					} else {
						Message m = new Message();
						m.what = 0x117;
						m.arg1 = toId;
						m.arg2 = toPhoto;
						m.obj = new Integer(cId);
						gameHandler.sendMessage(m);
					}

				} else if (tmp.startsWith("setname")) {
					int setid = Integer.parseInt(tmp.split("_")[1]);
					String setname = tmp.split("_")[2];
					Message m = new Message();
					m.what = 0x119;
					m.arg1 = setid;
					m.obj = setname;
					gameHandler.sendMessage(m);
				}
			}
		}
	}
}
