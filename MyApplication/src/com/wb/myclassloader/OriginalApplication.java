package com.wb.myclassloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import dalvik.system.DexClassLoader;

public class OriginalApplication {
	Class<?> dexClassLoader;
	Class<?> dexBaseClassLoader;
	Class<?> dexPathList;
	public Object currentActivityThread = null;
	public Object dexpathlist;
	public File dexInternalStoragePath;
	public File optimizedDexOutputPath;
	private static final String SECONDARY_DEX_NAME = "classes.dex";
	private static final int BUF_SIZE = 8 * 1024;
	private Application application = null;
	private String TAG = "OriginalApplication";
	private static final String appkey = "APPLICATION_CLASS_NAME";
	private String libPath;
	String appClassName = null;
	public OriginalApplication() {
		
	}
	public OriginalApplication(Application app) {
		this.application = app;
	}
	public void prepareLoad(Application application) {
		libPath = "/data/data/" + application.getPackageName() + "/lib";
		optimizedDexOutputPath = application.getDir("outdex",
				Context.MODE_PRIVATE);
		dexInternalStoragePath = new File("/data/data/" + application.getPackageName() , SECONDARY_DEX_NAME);
	}

	/**
	 * original prepareDex copy dex to the phone storage
	 **/
	public boolean prepareDex(Application application) {
		BufferedInputStream bis = null;
		OutputStream dexWriter = null;
		try {
			bis = new BufferedInputStream(application.getAssets().open(
					SECONDARY_DEX_NAME));
			dexWriter = new BufferedOutputStream(new FileOutputStream(
					dexInternalStoragePath));
			byte[] buf = new byte[BUF_SIZE];
			int len;
			while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
				dexWriter.write(buf, 0, len);
			}
			dexWriter.close();
			bis.close();
			return true;
		} catch (IOException e) {
			if (dexWriter != null) {
				try {
					dexWriter.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			return false;
		}
	}



	@SuppressWarnings("unused")
	@SuppressLint("NewApi")
	public void prepareRef(Application application) {
		currentActivityThread = invokeStaticMethod(
				"android.app.ActivityThread", "currentActivityThread",
				new Class[] {}, new Object[] {});
		String packageName = application.getPackageName();
		HashMap mPackages = (HashMap) getFieldOjbect(
				"android.app.ActivityThread", currentActivityThread,
				"mPackages");
		WeakReference wr = (WeakReference) mPackages.get(packageName);
		Log.i(TAG, "---------------->INNER LOADREF<----------------");
		Log.i(TAG, "prepareRef DexClassLoader dexInternalStoragePath is "
				+ dexInternalStoragePath);
//		Log.i(TAG, "prepareRef DexClassLoader optimizedDexOutputPath is "
//				+ optimizedDexOutputPath);
		
		
//		DexClassLoader dLoader = new DexClassLoader(
//				dexInternalStoragePath.getAbsolutePath(),
//				optimizedDexOutputPath.getAbsolutePath(), libPath,
//				(ClassLoader) getFieldOjbect("android.app.LoadedApk", wr.get(),
//						"mClassLoader"));
		
		
		 SuperClassLoader dLoader = new SuperClassLoader(application,
		 dexInternalStoragePath.getAbsolutePath(),
		 optimizedDexOutputPath, libPath, (ClassLoader) getFieldOjbect(
		 "android.app.LoadedApk", wr.get(), "mClassLoader"));
		setFieldOjbect("android.app.LoadedApk", "mClassLoader", wr.get(),
				dLoader);
		
		
		Log.i(TAG, "<<<<<<<<<<<<<<<INNER REFLECTION TEST Begin>>>>>>>>>>>>>>>");
		Log.i(TAG, "++++++++++++++++++++++++++++++++++");
		Log.i(TAG, "<<<<<<<<<<<<<<<INNER REFLECTION TEST End>>>>>>>>>>>>>>>");

	}

	@SuppressLint("NewApi")
	public void configApplicationEnv() {
		Log.i(TAG, "enter into configApplicationEnv()");
		try {
			ApplicationInfo ai = application.getPackageManager()
					.getApplicationInfo(application.getPackageName(),
							PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			if (bundle != null
					&& bundle.containsKey("APPLICATION_CLASS_NAME")) {
				appClassName = bundle.getString("APPLICATION_CLASS_NAME");
			} else {
				return;
			}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  
		Log.i(TAG, "appClassName is "+appClassName);
		Object currentActivityThread = invokeStaticMethod(
				"android.app.ActivityThread", "currentActivityThread",
				new Class[] {}, new Object[] {});
		Object mBoundApplication = getFieldOjbect(
				"android.app.ActivityThread", currentActivityThread,
				"mBoundApplication");
		Object loadedApkInfo = getFieldOjbect(
				"android.app.ActivityThread$AppBindData",
				mBoundApplication, "info");
		setFieldOjbect("android.app.LoadedApk", "mApplication",
				loadedApkInfo, null);
		Object oldApplication = getFieldOjbect(
				"android.app.ActivityThread", currentActivityThread,
				"mInitialApplication");
		ArrayList<Application> mAllApplications = (ArrayList<Application>)getFieldOjbect("android.app.ActivityThread",currentActivityThread, "mAllApplications");
		mAllApplications.remove(oldApplication);
		ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo)getFieldOjbect("android.app.LoadedApk", loadedApkInfo,
						"mApplicationInfo");
		ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) getFieldOjbect("android.app.ActivityThread$AppBindData",
						mBoundApplication, "appInfo");
		appinfo_In_LoadedApk.className = appClassName;
		appinfo_In_AppBindData.className = appClassName;
		Application app1 = (Application) invokeMethod(
				"android.app.LoadedApk", "makeApplication", loadedApkInfo,
				new Class[] { boolean.class, Instrumentation.class },
				new Object[] { false, null });
		setFieldOjbect("android.app.ActivityThread",
				"mInitialApplication", currentActivityThread, app1);
		HashMap mProviderMap = (HashMap)getFieldOjbect(
				"android.app.ActivityThread", currentActivityThread,
				"mProviderMap");
		Iterator it = mProviderMap.values().iterator();
			while (it.hasNext()) {
				Object providerClientRecord = it.next();
				Object localProvider = getFieldOjbect(
						"android.app.ActivityThread$ProviderClientRecord",
						providerClientRecord, "mLocalProvider");
				setFieldOjbect("android.content.ContentProvider",
						"mContext", localProvider, app1);
			}
		app1.onCreate();
		Log.i(TAG, "app1.onCreate() finished");
	}

	public Object invokeStaticMethod(String class_name, String method_name,
			Class[] pareTyple, Object[] pareVaules) {

		try {
			Class<?> obj_class = Class.forName(class_name);
			Method method = obj_class.getMethod(method_name, pareTyple);
			return method.invoke(null, pareVaules);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;

	}

	public Object invokeMethod(String class_name, String method_name,
			Object obj, Class[] pareTyple, Object[] pareVaules) {

		try {
			Class<?> obj_class = Class.forName(class_name);
			Method method = obj_class.getMethod(method_name, pareTyple);
			return method.invoke(obj, pareVaules);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;

	}

	public Object getFieldOjbect(String class_name, Object obj, String filedName) {
		try {
			Class<?> obj_class = Class.forName(class_name);
			Field field = obj_class.getDeclaredField(filedName);
			field.setAccessible(true);
			return field.get(obj);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;

	}

	public void setFieldOjbect(String classname, String filedName, Object obj,
			Object filedVaule) {
		try {
			Class<?> obj_class = Class.forName(classname);
			Field field = obj_class.getDeclaredField(filedName);
			field.setAccessible(true);
			field.set(obj, filedVaule);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
