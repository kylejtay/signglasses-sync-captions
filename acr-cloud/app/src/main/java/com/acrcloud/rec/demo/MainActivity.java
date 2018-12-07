package com.acrcloud.rec.demo;

import java.io.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.acrcloud.rec.sdk.ACRCloudConfig;
import com.acrcloud.rec.sdk.ACRCloudClient;
import com.acrcloud.rec.sdk.IACRCloudListener;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.text.Html;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.TimeUnit;
import java.util.Timer;
import java.util.TimerTask;
import android.graphics.Typeface;

public class MainActivity extends Activity implements IACRCloudListener {
    //NOTE: You can also implement IACRCloudResultWithAudioListener, replace "onResult(String result)" with "onResult(ACRCloudResult result)"

	private ACRCloudClient mClient;
	private ACRCloudConfig mConfig;
	
	public TextView mResult;
	
	private boolean mProcessing = false;
	private boolean initState = false;
	
	private String path = "";

	private long startTime = 0; 
	private long stopTime = 0;
	private volatile int time = 0;

	protected Context mContext;
	private Context context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// path = "/storage/external/acrcloud/model";	
		path = "/storage/extSdCard/acrcloud/model";	
		Log.d("file path", path);

		Log.d("database path", path);
		
		File file = new File(path);
		if(!file.exists()){
			file.mkdirs();
		}		
			
		mResult = (TextView) findViewById(R.id.result);			

		 this.mConfig = new ACRCloudConfig();
		 this.mConfig.acrcloudListener = this;

		 this.mConfig.context = this;
		 // this.mConfig.host = "identify-us-west-2.acrcloud.com";
		 this.mConfig.dbPath = path; // offline db path, you can change it with other path which this app can access.
		 this.mConfig.accessKey = "babf11b4ea9745db5d96a48a5a41728c";
		 this.mConfig.accessSecret = "tAzjkWfPULuOxVMpH8NkN0wgx7snIpqbOcjcocAE";
		 // this.mConfig.protocol = ACRCloudConfig.ACRCloudNetworkProtocol.PROTOCOL_HTTP; // PROTOCOL_HTTPS
		// this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_REMOTE;
		 this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_LOCAL;
		 //this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_BOTH;

		 this.mClient = new ACRCloudClient();
		 // If reqMode is REC_MODE_LOCAL or REC_MODE_BOTH,
		 // the function initWithConfig is used to load offline db, and it may cost long time.
		 this.initState = this.mClient.initWithConfig(this.mConfig);
		 if (this.initState) {
		     this.mClient.startPreRecord(3000); //start prerecord, you can call "this.mClient.stopPreRecord()" to stop prerecord.
		 }
		 Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/roboto.ttf");
		 mResult.setTypeface(tf);
		 mResult.setText("Syncing captions with video...");

		 start();
	}

	public void getCaptions(final int offset, final String id) {		
		if (time == 0) {
			Timer timer = new Timer();	
			timer.scheduleAtFixedRate(new TimerTask(){
			    @Override
			    public void run(){
			    	showCaptions(time, id);
	       		time += 300;
			    }		    
			},0,300);
		}
		if (time < (offset - 500) || time > (offset + 500)){
			time = offset;	
		}		
	}

	public void showCaptions(final int offset, final String id){
		String json = null;
		try {
			mContext = this;
			Log.d("Video ID: ", id);
			InputStream is = this.getAssets().open(id+".json");
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			json = new String(buffer, "UTF-8");
		} catch (IOException ex) {
		   ex.printStackTrace();
		}

		try {
			JSONArray captions = new JSONArray(json);
			int found = 0;
			for(int i=0; i<captions.length(); i++){
				final JSONObject caption = captions.getJSONObject(i);
				int start = Integer.parseInt(caption.getString("start"));
				int end = Integer.parseInt(caption.getString("end"));
				final String text = caption.getString("text");
				if(start < offset && end > offset){
					found++;
					final String offset_display = String.format("%02d:%02d",
			     TimeUnit.MILLISECONDS.toMinutes(offset),
			     TimeUnit.MILLISECONDS.toSeconds(offset) -
			     TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(offset)));
					Log.d("Caption: ", caption.getString("text"));
					runOnUiThread(new Runnable() {
				    @Override
				    public void run() {
				    	mResult.setText(Html.fromHtml(text));				    	
				    }
					});
				}     	
		 }

		 if(found == 0){
		 	runOnUiThread(new Runnable() {
		     @Override
		     public void run() {
		     	mResult.setText("");
		     }
		 	});
		 }	
		} catch (JSONException e) {
		   //some exception handler code.
		} 	
	}

	public void setText(final String string){
		
	}

	
	public void start() {
        if (!this.initState) {
            Toast.makeText(this, "init error", Toast.LENGTH_SHORT).show();
            return;
        }
		
		if (!mProcessing) {
			mProcessing = true;
			if (this.mClient == null || !this.mClient.startRecognize()) {
				mProcessing = false;
				mResult.setText("start error!");
			}
            startTime = System.currentTimeMillis();
		}
	}

	protected void stop() {
		if (mProcessing && this.mClient != null) {
			this.mClient.stopRecordToRecognize();
		}
		mProcessing = false;

		stopTime = System.currentTimeMillis();
	}
	
	protected void cancel() {
		if (mProcessing && this.mClient != null) {
			mProcessing = false;
			this.mClient.cancel();
		} 		
	}

	public void onVolumeChanged(double volume) {
		long time = (System.currentTimeMillis() - startTime) / 1000;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    // Old api
	@Override
	public void onResult(String result) {	
		if (this.mClient != null) {
			// this.mClient.cancel();
			mProcessing = false;
		} 
		
		String tres = "\n";
		
		try {
		    JSONObject j = new JSONObject(result);
		    JSONObject j1 = j.getJSONObject("status");
		    int j2 = j1.getInt("code");
		    if(j2 == 0){
		    	JSONObject metadata = j.getJSONObject("metadata");
		    	//
		    	if (metadata.has("custom_files")) {
		    		JSONArray musics = metadata.getJSONArray("custom_files");
		    		for(int i=0; i<musics.length(); i++) {
		    			JSONObject tt = (JSONObject) musics.get(i); 
		    			String offset = tt.getString("play_offset_ms");
		    			String id = tt.getString("id");
		    			int offset_int = Integer.parseInt(offset);
		    			getCaptions(offset_int, id);
		    		}
		    	}
		    }else{
		    	if(j2 == 2005){
		    		start();
		    	}
		    	tres = result;
		    }
		} catch (JSONException e) {
			tres = result;
		    e.printStackTrace();
		}
	}
	
	@Override  
    protected void onDestroy() {  
        super.onDestroy();  
        Log.e("MainActivity", "release");
        if (this.mClient != null) {
        	this.mClient.release();
        	this.initState = false;
        	this.mClient = null;
        }
    } 
}
