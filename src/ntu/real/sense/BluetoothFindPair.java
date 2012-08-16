package ntu.real.sense;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class BluetoothFindPair extends Activity {
	/** Called when the activity is first created. */
	BluetoothAdapter mBluetoothAdapter;
	ListView lv;
	ArrayAdapter<String> adapter;
	BroadcastReceiver mReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.find);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		mReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context arg0, Intent arg1) {
				// TODO Auto-generated method stub
				String act = arg1.getAction();
				if (BluetoothDevice.ACTION_FOUND.equals(act)) {
					BluetoothDevice b = arg1
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					adapter.add(b.getName() + "\n" + b.getAddress());
					Log.e("bfp", "find " + b.getName() + " " + b.getAddress());
					lv.invalidate();
				}
			}

		};

		IntentFilter f = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, f);

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		lv = (ListView) findViewById(R.id.listView1);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				String s = (String) lv.getItemAtPosition(arg2);
				s = s.substring(s.indexOf("\n"));
				s = s.substring(1, s.length());
				Global.mMAC = s;
				Log.e("bfp", Global.mMAC);
				Global.flagIsServer = false;
				Intent i = new Intent(BluetoothFindPair.this,
						BluetoothServerActivity.class);
				startActivity(i);
				BluetoothFindPair.this.unregisterReceiver(mReceiver);
				BluetoothFindPair.this.finish();
			}

		});
		lv.setAdapter(adapter);

		Set<BluetoothDevice> paired = mBluetoothAdapter.getBondedDevices();
		for (BluetoothDevice b : paired) {
			adapter.add(b.getName() + "\n" + b.getAddress());
		}
		lv.invalidate();

		mBluetoothAdapter.startDiscovery();
	}
}
