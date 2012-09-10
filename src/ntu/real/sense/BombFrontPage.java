package ntu.real.sense;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

public class BombFrontPage extends Activity {
	RadioButton[] rb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.frontpage);

		rb = new RadioButton[4];
		rb[0] = (RadioButton) findViewById(R.id.radio0);
		rb[1] = (RadioButton) findViewById(R.id.radio1);
		rb[2] = (RadioButton) findViewById(R.id.radio2);
		rb[3] = (RadioButton) findViewById(R.id.radio3);
		Button b1 = (Button) findViewById(R.id.Host);
		b1.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				for (int i = 0; i < rb.length; i++) {
					if (rb[i].isChecked()) {
						Global.selectWay = i;
						break;
					}
				}

				Intent i1 = new Intent();
				i1.setClass(BombFrontPage.this, BluetoothServerActivity.class);
				startActivity(i1);
				finish();
			}

		});

		Button b2 = (Button) findViewById(R.id.join);
		b2.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				for (int i = 0; i < rb.length; i++) {
					if (rb[i].isChecked()) {
						Global.selectWay = i;
						break;
					}
				}
				Intent i2 = new Intent();
				i2.setClass(BombFrontPage.this, BluetoothFindPair.class);
				startActivity(i2);
				finish();
			}

		});
	}

}
