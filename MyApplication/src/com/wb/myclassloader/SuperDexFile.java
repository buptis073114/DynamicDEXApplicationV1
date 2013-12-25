package com.wb.myclassloader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.http.MethodNotSupportedException;
import dalvik.system.DexFile;
import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;
import java.util.Enumeration;

public class SuperDexFile {
    private static int mCookie;
    private String mFileName=null;
    private static String TAG = "MyDexFile";

	static Method loadDexFile;
	static Method openDexFile;
    static Method defineClass ;
    //static Method openDexfile ;	
    static Method closeDexfile ;
    static Method getClassNameList ;
    static Application application = null;
    public SuperDexFile(Application application) throws IOException {
    	this.application = application;
    	Log.i(TAG,"in MyDexFile");
    	Log.i(TAG,"before initializeMethods");
    	try {
			initializeMethods();
		} catch (MethodNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Log.i(TAG,"after initializeMethods");
    	Log.i(TAG,"before rustezedd");
//    	mCookie = NativeC.rustezedd(application.getPackageName(),
//				application.getPackageResourcePath());
    	loadDex();
    	Log.i(TAG,"after rustezedd mCookie is "+mCookie);
    }
    
	public static void loadDex() {
		try {
			/**
			 * innerMem dex loader wb 2013.9.27
			 **/
			String pkname = application.getPackageName();
			String pkpath = application.getPackageCodePath();
			String assList[];
			byte[] loadContent = null;
			AssetManager assetManager = application.getAssets();
			InputStream inputStream = null;
			assList = assetManager.list("classes.dex");
			for (String str : assList) {
				Log.d("Loader.Main", "Asset File :" + str);
			}
			inputStream = assetManager.open("classes.dex");
			loadContent = new byte[inputStream.available()];
			inputStream.read(loadContent);
			Log.i(TAG,
					"<<<<<<<<<<<<<<<<<<<<<<<<byte[] loadContent prepared!!!>>>>>>>>>>>>>>>>>>>>>>>>");
			mCookie = (Integer) openDexFile.invoke(null, loadContent);
			Log.i(TAG, "cookie is " + mCookie);

//			String[] as = (String[]) getClassNameList.invoke(dexFileReceiver,
//					cookie);
//			for (int i = 0; i < as.length; i++) {
//				defineClass.invoke(dexFileReceiver, as[i].replace(".", "/"),
//						application.getClassLoader(), cookie);
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    static public SuperDexFile loadDex(Application application) throws IOException {
            return new SuperDexFile(application);
        }
    
    
    public Class loadClass(String name, ClassLoader loader) {
        String slashName = name.replace('.', '/');
        return loadClassBinaryName(slashName, loader);
    }
    
    public Class loadClassBinaryName(String name, ClassLoader loader) {
    	Class cl = null;
    	Log.i(TAG,"in loadClassBinaryName name is "+name);
    	
		try {
		cl = (Class)defineClass.invoke(null, name.replace(".", "/"), loader, mCookie);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    //	Log.i(TAG,"after loadClassBinaryName cl is "+cl.toString());
    	return cl;
        //return defineClass(name, loader, mCookie);
    }
    



    
    public Enumeration<String> entries() {
        return new DFEnum(this);
    }
    
    
    
    private class DFEnum implements Enumeration<String> {
        private int mIndex;
        private String[] mNameList;

       
        
        DFEnum(SuperDexFile df) {
        Log.i(TAG,"in getClassNameList");
            mIndex = 0;
            try {
				mNameList =(String[])getClassNameList.invoke(null,mCookie);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
           // mNameList = getClassNameList(mCookie);
        }

        public boolean hasMoreElements() {
            return (mIndex < mNameList.length);
        }

        public String nextElement() {
            return mNameList[mIndex++];
        }
    }
    
	private void initializeMethods() throws MethodNotSupportedException{
		Method[] methods;
		try{
			methods = Class.forName("dalvik.system.DexFile").getDeclaredMethods();
			for(Method method:methods){
				if(method.getName().equalsIgnoreCase("defineClass")&&method.getParameterTypes().length==3){
					defineClass = method;
					defineClass.setAccessible(true);
				}
				else if(method.getName().equalsIgnoreCase("openDexFile")&&method.getParameterTypes().length==1){
					openDexFile = method;
					openDexFile.setAccessible(true);
				}
				else if(method.getName().equalsIgnoreCase("closeDexFile")&&method.getParameterTypes().length==1){
					closeDexfile = method;
					closeDexfile.setAccessible(true);
				}
				else if(method.getName().equalsIgnoreCase("getClassNameList")&&method.getParameterTypes().length==1){
					getClassNameList = method;
					getClassNameList.setAccessible(true);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		if((defineClass == null)||(openDexFile == null)||(closeDexfile == null)||(getClassNameList == null)){
			throw new MethodNotSupportedException("Error setting up unpacking functions");
		}
		
	}
    
}
