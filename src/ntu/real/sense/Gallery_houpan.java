package ntu.real.sense;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

	class ListAllPath{
		
		public ArrayList<String> file_list=new ArrayList<String>();
		ListAllPath(){
			file_list=new ArrayList<String>();
			file_list.add(null);
			}
		
		
		 public  void print(File mFile, int mlevel){
			for(int i = 0; i < mlevel; i++){
			    Log.e("word","�i�J�ؿ�");
			}
			    if (mFile.isDirectory()){  
			    	Log.e("word","__�ؿ��G<" + getPath(mFile) + ">");  
			    	if(getPath(mFile).endsWith(".thumbnails")){
			    		return;
			    	}
			   String[] str = mFile.list();
			   for (int i = 0; i < str.length; i++){
			  print(new File(mFile.getPath() + "/" + str[i]) , mlevel + 1);
			   }  
			    }else{
			    	Log.e("word",getPath(mFile));
			    	file_list.add(getPath(mFile));
			    }   
			}

			public  String  getPath(File mFile){
			    String fullPath = mFile.getPath();
			    String[] str = fullPath.split("//");
			    return str[str.length - 1];
			}
	}


 

//��Ч@�� �n�߳n�� http://toimy.blogspot.com/ 
public class Gallery_houpan extends Activity implements OnTouchListener, OnLongClickListener {
  RelativeLayout layout;
  private Button btn1;
  private int CurrentButtonNumber = 0; //CurrentButtonNumber�y���� �]�w����ID
  
	private Bitmap decodeBitmap(String path){
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inJustDecodeBounds = true;
        op.inSampleSize = 4;
        Bitmap bmp = BitmapFactory.decodeFile(path, op);
        op.inJustDecodeBounds = false;
        bmp = BitmapFactory.decodeFile(path, op);
        return bmp;
    } 
  
  /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       //���إߤ@�� ���O��m�Ҧ�����
       int i;
       
       layout= new RelativeLayout(this);
       /*btn1 = new Button(this);
       btn1.setId(CurrentButtonNumber);      
       CurrentButtonNumber++;
       btn1.setText("SoftCat Go Button");
       btn1.setOnClickListener(this);    //�p�G�n�o�˼g �ݥ[�J implements OnClickListener �� Activity
       btn1.setOnLongClickListener(this);
       layout.addView(btn1, 150, 50); //addView(����,�e�װ���)
       */
       setContentView(layout);   //�]�w�e����ܦۤv�����O
       

		ListAllPath demoTest = new ListAllPath();
		File rootFile = new File("/sdcard/DCIM");
		demoTest.print(rootFile, 0);
		
		
		InputStream inputStream = null;  
		RelativeLayout RL_temp=new RelativeLayout(this);
		RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		RL_temp.setLayoutParams(params);
		layout.addView(RL_temp);
		
		for (i=1;i<demoTest.file_list.size();i++){
			/*
			Log.e("�a�X�ӡG" ,Integer.toString(i));
			if(i%3==0){//�歺������
				params = new RelativeLayout.LayoutParams(
										RelativeLayout.LayoutParams.WRAP_CONTENT,
										RelativeLayout.LayoutParams.WRAP_CONTENT);
				
				params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				params.setMargins(0, 30, 0, 0);

				if(i==0){//��l��
					RL_temp.setId(10000);
					params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				}else{
					RL_temp.setId(10000+i/3);
					if(i>=3){
						params.addRule(RelativeLayout.BELOW, (10000+i/3-1));
						Log.e("how",Integer.toString(10000+i/3-1));
					}
				}
				RL_temp.setLayoutParams(params);
				layout.addView(RL_temp);
			} 
			
			try {  
	            inputStream = new FileInputStream(demoTest.file_list.get(i));  
	        } catch (FileNotFoundException e) {  
	            e.printStackTrace();  
	        }  
			
	        */
			Log.e("�Ϥ����}�G",demoTest.file_list.get(i));
			Bitmap bitmap = decodeBitmap(demoTest.file_list.get(i));
	        
			ImageButton image_temp = new ImageButton(this);   
			image_temp.setImageBitmap(bitmap);
			image_temp.setBackgroundColor(Color.BLUE);
			Log.e("oriID",Integer.toString(image_temp.getId()));
			image_temp.setId(i);  //ID����O�s�A���M�|�걼�I
			Log.e("newID",Integer.toString(image_temp.getId()));
	        image_temp.setLayoutParams(params);
	        params = new RelativeLayout.LayoutParams(150,150);
	        /*
	        if(i==1){
	        	params.addRule(RelativeLayout.ALIGN_TOP,RelativeLayout.TRUE);
	        	params.addRule(RelativeLayout.ALIGN_LEFT,RelativeLayout.TRUE);
	        }else{
	        	params.addRule(RelativeLayout.RIGHT_OF, (i-1));
	        }*/
	        
	        Log.e("�I�ڬO",Integer.toString(i));
	        //params.addRule(RelativeLayout.RIGHT_OF, (i-1));
	        /*
	        if((i+3)<demoTest.file_list.size()){
	        	params.addRule(RelativeLayout.ABOVE, (i+3));
	        }
	        if((i+1)<demoTest.file_list.size()&&(i)%3!=2){
	        	params.addRule(RelativeLayout.LEFT_OF,(i+1));
	        }*/
	        
	        
	        if(i>3){
	        	Log.e("�b�֪��U���G",Integer.toString(i-3));
	        	params.addRule(RelativeLayout.BELOW, (i-3));
	        }
	        if(i%3!=1){//�D�C��������A�nmargin
	        	Log.e("�b�֪��k��G",Integer.toString(i-1));
	        	params.addRule(RelativeLayout.RIGHT_OF, (i-1));
	        }
	        
	        
	        image_temp.setLayoutParams(params);
	        image_temp.setOnTouchListener(this);    //�p�G�n�o�˼g �ݥ[�J implements OnClickListener �� Activity
	        image_temp.setOnLongClickListener(this);
	        RL_temp.addView(image_temp); 

	        
		}
       
   }
   public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP)
		{
		Toast.makeText(this, "Touch UP:"+v.getId(), Toast.LENGTH_SHORT) .show();
	
		}
		if(event.getAction() == MotionEvent.ACTION_DOWN){
	
		}
		return false;
	 }
   
	@Override
	public boolean onLongClick(View v) {
	    if(v.getId()<10000){
	  	  Toast.makeText(this, "Long click:" +v.getId(), Toast.LENGTH_SHORT).show();
	  	  
	    }
	    return true;//�o��nreturn true���Monclick�|����
		
	}
	
	
}