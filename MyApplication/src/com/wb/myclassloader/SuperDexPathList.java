package com.wb.myclassloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipFile;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

@SuppressLint("NewApi")
public class SuperDexPathList {

	static int cookie;
	static Class clazz;
	static Class<?> dexDexFile;
	static Class<?> dexPathList;
	static Class<?> dexClassLoader;
	static Class<?> superElement;
	static Class<?> dexBaseClassLoader;
	static Class<?> dexFileReceiver;
	static Method defineClass;
	static Method loadDexFile;
	static Method openDexFile;
	static Method makeDexElements;
	static Method closeDexFile;
	static Method getClassNameList;
	static Method splitDexPath;
	static Method splitLibraryPath;
	static Application application;
	static String appName;
	static String TAG = "SuperDexPathList";
	private static final String DEX_SUFFIX = ".dex";
	private static final String JAR_SUFFIX = ".jar";
	private static final String ZIP_SUFFIX = ".zip";
	private static final String APK_SUFFIX = ".apk";
	private final ClassLoader definingContext;
	private Element[] dexElements = null;
	private File[] nativeLibraryDirectories = null;

	@SuppressWarnings("unchecked")
	public SuperDexPathList(Application app, ClassLoader definingContext,
			String dexPath, String libraryPath, File optimizedDirectory){
		if (definingContext == null) {
			throw new NullPointerException("definingContext == null");
		}

		if (dexPath == null) {
			throw new NullPointerException("dexPath == null");
		}

		application = app;
		initalizeMethods();
		this.definingContext = definingContext;
		ArrayList<File> dexpath = null;
		
		Log.i(TAG, "in SuperDexPathList");
		
		try {
			dexpath = (ArrayList<File>) splitDexPath.invoke(null, dexPath);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.dexElements = makeDexElements(dexpath,optimizedDirectory);

		try {
			this.nativeLibraryDirectories = (File[]) splitLibraryPath.invoke(
					null, libraryPath);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void initalizeMethods() {
		Method[] methods_dexPathList;
		try {
			methods_dexPathList = Class.forName("dalvik.system.DexPathList")
					.getDeclaredMethods();
			//Log.i(TAG, "dexFileReceiver is " + dexFileReceiver.toString());
			for (Method method : methods_dexPathList) {
				if (method.getName().equalsIgnoreCase("splitLibraryPath")
						&& (method.getParameterTypes().length == 1)) {
					splitLibraryPath = method;
					splitLibraryPath.setAccessible(true);
				}
				if (method.getName().equalsIgnoreCase("splitDexPath")
						&& (method.getParameterTypes().length == 1)) {
					splitDexPath = method;
					splitDexPath.setAccessible(true);
				}
				if (method.getName().equalsIgnoreCase("makeDexElements")
						&& (method.getParameterTypes().length == 2)) {
					makeDexElements = method;
					makeDexElements.setAccessible(true);
				}
				if (method.getName().equalsIgnoreCase("loadDexFile")
						&& (method.getParameterTypes().length == 2)) {
					loadDexFile = method;
					loadDexFile.setAccessible(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Element[] makeDexElements(ArrayList<File> files ,File optimizedDirectory )  {
		ArrayList<Element> elements = new ArrayList<Element>();

		/*
		 * Open all files and load the (direct or contained) dex files up front.
		 */
		for (File file : files) {
			File zip = null;
			SuperDexFile dex = null;
			String name = file.getName();

			if (name.endsWith(DEX_SUFFIX)) {
				try {
					dex = SuperloadDexFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (name.endsWith(APK_SUFFIX) || name.endsWith(JAR_SUFFIX)
					|| name.endsWith(ZIP_SUFFIX)) {
				zip = file;

				try {
//					 dex = (DexFile) loadDexFile.invoke(null,new Class[]{File.class,File.class},new Object[]{file,optimizedDirectory} );
					dex = SuperloadDexFile();
				} catch (Exception ignored) {
				}
			} else if (file.isDirectory()) {

				elements.add(new Element(file, true, null, null));
			} else {

			}
				Element e =  new Element(file, false, null, dex);
				elements.add(e);			
		}
		Object currentActivityThread = invokeStaticMethod(
				"android.app.ActivityThread", "currentActivityThread",
				new Class[] {}, new Object[] {});
		String packageName = application.getPackageName();
		HashMap mPackages = (HashMap) getFieldOjbect(
				"android.app.ActivityThread", currentActivityThread,
				"mPackages");
		WeakReference wr = (WeakReference) mPackages.get(packageName);

		ClassLoader dexLoader = (ClassLoader) getFieldOjbect(
				"android.app.LoadedApk", wr.get(), "mClassLoader");
		Object dexpathlist = getFieldOjbect("dalvik.system.BaseDexClassLoader",
				dexLoader, "pathList");
		Object dexElements = getFieldOjbect("dalvik.system.DexPathList",
				dexpathlist, "dexElements");
		return elements.toArray(new Element[elements.size()]);
	}

	
	

private static SuperDexFile SuperloadDexFile()
            throws IOException {
           // String optimizedPath = optimizedPathFor(file, optimizedDirectory);
            return SuperDexFile.loadDex(application);
        
    }
	
	
	static class Element {
		private final File file;
		private final boolean isDirectory;
		private final File zip;
		private final SuperDexFile dexFile;

		private ZipFile zipFile;
		private boolean initialized;

		public Element(File file, boolean isDirectory, File zip, SuperDexFile dexFile) {
			this.file = file;
			this.isDirectory = isDirectory;
			this.zip = zip;
			this.dexFile = dexFile;
		}

		@Override
		public String toString() {
			if (isDirectory) {
				return "directory \"" + file + "\"";
			} else if (zip != null) {
				return "zip file \"" + zip + "\"";
			} else {
				return "dex file \"" + dexFile + "\"";
			}
		}

		public synchronized void maybeInit() {
			if (initialized) {
				return;
			}

			initialized = true;

			if (isDirectory || zip == null) {
				return;
			}

			try {
				zipFile = new ZipFile(zip);
			} catch (IOException ioe) {
				/*
				 * Note: ZipException (a subclass of IOException) might get
				 * thrown by the ZipFile constructor (e.g. if the file isn't
				 * actually a zip/jar file).
				 */

				zipFile = null;
			}
		}

		public URL findResource(String name) {
			maybeInit();

			// We support directories so we can run tests and/or legacy code
			// that uses Class.getResource.
			if (isDirectory) {
				File resourceFile = new File(file, name);
				if (resourceFile.exists()) {
					try {
						return resourceFile.toURI().toURL();
					} catch (MalformedURLException ex) {
						throw new RuntimeException(ex);
					}
				}
			}

			if (zipFile == null || zipFile.getEntry(name) == null) {
				/*
				 * Either this element has no zip/jar file (first clause), or
				 * the zip/jar file doesn't have an entry for the given name
				 * (second clause).
				 */
				return null;
			}

			try {
				/*
				 * File.toURL() is compliant with RFC 1738 in always creating
				 * absolute path names. If we construct the URL by concatenating
				 * strings, we might end up with illegal URLs for relative
				 * names.
				 */
				return new URL("jar:" + file.toURL() + "!/" + name);
			} catch (MalformedURLException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public Class findClass(String name){
//		try {
//			clazz = (Class) defineClass.invoke(null,
//					name.replace(".", "/"), application.getClassLoader(), cookie);
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		
		 for (Element element : dexElements) {
	            SuperDexFile dex = element.dexFile;
	            Class clazz = null;
	            if (dex != null) {
	            	clazz = dex.loadClassBinaryName(name, definingContext);
	                if (clazz != null) {
	                    return clazz;
	                }
	            }
	        }
		
		return clazz;
	}

	public File[] getNativeLibraryDirectories() {
		return nativeLibraryDirectories;
	}

	@Override
	public String toString() {
		return "DexPathList[" + Arrays.toString(dexElements)
				+ ",nativeLibraryDirectories="
				+ Arrays.toString(nativeLibraryDirectories) + "]";
	}

	public URL findResource(String name) {
		for (Element element : dexElements) {
			URL url = element.findResource(name);
			if (url != null) {
				return url;
			}
		}

		return null;
	}

	public Enumeration<URL> findResources(String name) {
		ArrayList<URL> result = new ArrayList<URL>();

		for (Element element : dexElements) {
			URL url = element.findResource(name);
			if (url != null) {
				result.add(url);
			}
		}

		return Collections.enumeration(result);
	}

	public String findLibrary(String libraryName) {
		String fileName = System.mapLibraryName(libraryName);
		for (File directory : nativeLibraryDirectories) {
			String path = new File(directory, fileName).getPath();
			return path;

		}
		return null;
	}

	public static Object getFieldOjbect(String class_name, Object obj,
			String filedName) {
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

	public static Object invokeStaticMethod(String class_name,
			String method_name, Class[] pareTyple, Object[] pareVaules) {

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

	public static Object getStaticFieldOjbect(String class_name,
			String filedName) {

		try {
			Class<?> obj_class = Class.forName(class_name);
			Field field = obj_class.getDeclaredField(filedName);
			field.setAccessible(true);
			return field.get(null);
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
	
	 private static DexFile loadDexFile(File file, File optimizedDirectory)
	            throws IOException {
	            return new DexFile(file);
	      
	    }
}
