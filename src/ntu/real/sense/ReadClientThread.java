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
			//Log.e("rct", cId + " rct running");
			String tmp = msa.readFromId(cId);
			//Log.e("rct", cId + " read " + tmp);
			if (tmp != null) {
				if (tmp.equals("setdeg")) {
	
						Message m = new Message();
						m.what = 0x101;
						m.arg1 = cId;
						m.arg2 = Integer.parseInt(msa.readFromId(cId));
						//把訊息送回server端
						gameHandler.sendMessage(m);
				}else if(tmp.equals("ClientReceive_completed")){//client接收完成了
						Message m = new Message();
						m.what = Global.CLIENT_RECEIVE_COMPLETED;
						//把訊息送回server端
						gameHandler.sendMessage(m);
				}else if(tmp.startsWith("ClientSend_start_")){//client送檔案訊息到本地端server或跳板
					Message m = new Message();
					Log.e("houpan","收到來自client的訊息");
					m.what = Global.CLIENT_SEND_FILE_START;
					m.obj=tmp.split("_",3)[2];//目標:server或是 id to id
					//把訊息送回server端
					gameHandler.sendMessage(m);
						
				}
			}
		}
	}
}
