package ntu.real.sense;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import android.util.Log;

class ListAllPath {

	public Vector<String> file_list = new Vector<String>();

	ListAllPath() {
		file_list = new Vector<String>();
		file_list.add(null);
	}

	public void print(File mFile, int mlevel) {
		for (int i = 0; i < mlevel; i++) {
			Log.e("word", "進入目錄");
		}
		if (mFile.isDirectory()) {
			Log.e("word", "__目錄：<" + getPath(mFile) + ">");
			if (getPath(mFile).endsWith(".thumbnails")) {
				return;
			}
			String[] str = mFile.list();
			
			for (int i = str.length-1; i >=0;i--) {
				print(new File(mFile.getPath() + "/" + str[i]), mlevel + 1);
			}
		} else {
			Log.e("word", getPath(mFile));
			file_list.add(getPath(mFile));
		}
	}

	public String getPath(File mFile) {
		String fullPath = mFile.getPath();
		String[] str = fullPath.split("//");
		return str[str.length - 1];
	}
}