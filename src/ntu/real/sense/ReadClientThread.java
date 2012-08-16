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
			Log.e("rct", cId + " rct running");
			String tmp = msa.readFromId(cId);
			Log.e("rct", cId + " read " + tmp);
			if (tmp != null) {
				if (tmp.equals("setdeg")) {

					Message m = new Message();
					m.what = 0x101;
					m.arg1 = cId;
					m.arg2 = Integer.parseInt(msa.readFromId(cId));
					gameHandler.sendMessage(m);
				}
			}

		}
	}
}
