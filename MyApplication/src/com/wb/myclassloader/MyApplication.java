package com.wb.myclassloader;

import dalvik.system.DexClassLoader;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

@SuppressLint("NewApi")
public class MyApplication extends Application {
	public OriginalApplication orgapp = null;
	private String TAG = "MyApplication";
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		orgapp = new OriginalApplication(this);
		orgapp.prepareLoad(this);
		Log.i(TAG, "prepare loaded");
		orgapp.prepareDex(this);
		Log.i(TAG, "prepapreDex loaded,classes.zip copied");
		orgapp.prepareRef(this);
	}
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.i(TAG, "prepapreRef loaded");
		orgapp.configApplicationEnv();
	}
	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	
}
