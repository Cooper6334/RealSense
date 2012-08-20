package ntu.real.sense;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

public class RealsenseGallery extends Activity implements View.OnLongClickListener, View.OnTouchListener{
	
	List<String> pics = new ArrayList<String>();
	List<TableRow> TRs = new ArrayList<TableRow>();
	List<ImageButton> IBs = new ArrayList<ImageButton>();
	Bitmap bmp;
	int currentRowNum = -1;
	@Override  
	public void onCreate(Bundle savedInstanceState) {  
	    super.onCreate(savedInstanceState);  
	    //setContentView(R.layout.activity_main);  
	    ScrollView sv = new ScrollView(this);
	    TableLayout layout = new TableLayout(this);
	    this.addContentView(sv, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	    sv.addView(layout, 0, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	    
	    pics=readSDCard();
	    
	    for(int i = 0; i < pics.size(); i++){
	    		if(i / 3 > currentRowNum){
	    			currentRowNum++;
	    		    TRs.add(new TableRow(this));
	    			layout.addView(TRs.get(currentRowNum), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	    		}
	    		IBs.add(new ImageButton(this));
	    		bmp = decodeBitmap(pics.get(pics.size()-i-1));
	    		IBs.get(i).setOnLongClickListener(this);
	    		IBs.get(i).setOnTouchListener(this);
	    		IBs.get(i).setImageBitmap(bmp);
	    		TRs.get(currentRowNum).addView(IBs.get(i), 200, 200);
	    }  
	}  
	  
	private List<String> readSDCard()  
	{  
	 List<String> tFileList = new ArrayList<String>();  
	  
	 //It have to be matched with the directory in SDCard  
	 File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/");  
	 File[] files=f.listFiles();  
	  
	 for(int i=0; i<files.length; i++)  
	 {  
	  File file = files[i];  
	  /*It's assumed that all file in the path 
	    are in supported type*/ 
	  if(file.isDirectory()&&!file.toString().endsWith(".thumbnails")){  
		  File[] moreFiles = file.listFiles();
		  for(int j=0;j<moreFiles.length;j++){
			  File moreFile = moreFiles[j];
			  tFileList.add(moreFile.getPath());
		  }
	  }
	  if(!file.isDirectory())tFileList.add(file.getPath()); 
	 }  
	  
	 return tFileList;  
	}  
	
	private Bitmap decodeBitmap(String path){  
        BitmapFactory.Options op = new BitmapFactory.Options();    
        op.inJustDecodeBounds = true;    
        op.inSampleSize = 4;
        Bitmap bmp = BitmapFactory.decodeFile(path, op); 
        op.inJustDecodeBounds = false;    
        bmp = BitmapFactory.decodeFile(path, op);    
        return bmp;    
    } 
	  
	public class ImageAdapter extends BaseAdapter {  
	    int mGalleryItemBackground;  
	    private Context mContext;  
	    private List<String> FileList;  
	  
	    public ImageAdapter(Context c, List<String> fList) {  
	        mContext = c;  
	        FileList = fList;  
	    }  
	  
	    public int getCount() {  
	        return FileList.size();  
	    }  
	  
	    public Object getItem(int position) {  
	        return position;  
	    }   
	  
	    public long getItemId(int position) {  
	        return position;  
	    }  
	  
	    public View getView(int position, View convertView,  
	      ViewGroup parent) {  
	        ImageView i = new ImageView(mContext);  
	        BitmapFactory.Options opts = new BitmapFactory.Options();
	        opts.inSampleSize = 4;
	        Bitmap bm = BitmapFactory.decodeFile(  
	          FileList.get(position).toString(), opts);  
	        i.setImageBitmap(bm);  
	      
	        i.setLayoutParams(new Gallery.LayoutParams(150, 100));  
	        i.setScaleType(ImageView.ScaleType.FIT_XY);  
	        i.setBackgroundResource(mGalleryItemBackground);  
	      
	        return i;  
	    }  
	}  
	  
	public TypedArray obtainStyledAttributes(int theme) {  
	    // TODO Auto-generated method stub  
	    return null;  
	}


	public boolean onLongClick(View v) {
		Toast.makeText(RealsenseGallery.this, "Long Click", Toast.LENGTH_SHORT) .show();
		return false;
	}

	public boolean onTouch(View v, MotionEvent event) {
		 if (event.getAction() == MotionEvent.ACTION_UP)  
	        {  
	            Toast.makeText(RealsenseGallery.this, "Touch UP", Toast.LENGTH_SHORT) .show();
	            
	        }
	        if(event.getAction() == MotionEvent.ACTION_DOWN){
	        	
	        }
	        return super.onTouchEvent(event); 
	}
	
	
}
