package com.wb.myclassloader;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;

@SuppressLint("NewApi")
public class SuperClassLoader extends ClassLoader {
	 private final SuperDexPathList pathList;
	 private final String TAG = "SuperClassLoader";
	 public SuperClassLoader(Application app,String dexPath, File optimizedDirectory,
	            String libraryPath, ClassLoader parent) {
	        super(parent);
	        this.pathList = new SuperDexPathList(app,this, dexPath, libraryPath, optimizedDirectory);
	    }

	    @Override
	    protected Class<?> findClass(String name) throws ClassNotFoundException {
	        Class c = pathList.findClass(name);
	        Log.i(TAG, "in findClass name is "+name);
	        if (c == null) {
	            throw new ClassNotFoundException("Didn't find class \"" + name + "\" on path: " + pathList);
	        }
	        return c;
	    }

	    @Override
	    protected URL findResource(String name) {
	        return pathList.findResource(name);
	    }

	    @Override
	    protected Enumeration<URL> findResources(String name) {
	        return pathList.findResources(name);
	    }

	    @Override
	    public String findLibrary(String name) {
	        return pathList.findLibrary(name);
	    }

	  	@Override
	    protected synchronized Package getPackage(String name) {
	        if (name != null && !name.isEmpty()) {
	            Package pack = super.getPackage(name);

	            if (pack == null) {
	                pack = definePackage(name, "Unknown", "0.0", "Unknown",
	                        "Unknown", "0.0", "Unknown", null);
	            }

	            return pack;
	        }

	        return null;
	    }

	    /**
	     * @hide
	     */
	    public String getLdLibraryPath() {
	        StringBuilder result = new StringBuilder();
	        for (File directory : pathList.getNativeLibraryDirectories()) {
	            if (result.length() > 0) {
	                result.append(':');
	            }
	            result.append(directory);
	        }
	        return result.toString();
	    }

	    @Override public String toString() {
	        return getClass().getName() + "[" + pathList + "]";
	    }

}
