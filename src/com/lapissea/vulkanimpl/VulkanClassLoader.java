package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class VulkanClassLoader extends URLClassLoader{
	
	private static URL[] lel(){
		try{
			return new URL[]{ApplicationVk.class.getProtectionDomain().getCodeSource().getLocation().toURI().toURL()};
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public VulkanClassLoader(ClassLoader parent){
		super(getUrls(parent), parent);
	}
	
	/**
	 * Loads a given class from .class file just like
	 * the default ClassLoader. This method could be
	 * changed to load the class over network from some
	 * other server or from the database.
	 *
	 * @param name Full class name
	 */
	private Class<?> getClass(String name)
		throws ClassNotFoundException{
		byte[] b;
		try{
			var cl=findLoadedClass(name);
			if(cl!=null) return cl;
			
			b=loadClassData(name);
			
			Class<?> c=defineClass(name, b, 0, b.length);
			resolveClass(c);
			return c;
		}catch(Exception e){
//			throw new ClassNotFoundException(name);
			return getParent().loadClass(name);
		}
	}
	
	@Override
	public Class<?> loadClass(String name)
		throws ClassNotFoundException{
		if(!DevelopmentInfo.DEV_ON&&name.startsWith("com.lapissea")){
			if(name.contains(".devonly."))throw new IllegalAccessError();
		}
		return getClass(name);
	}
	
	@Override
	public InputStream getResourceAsStream(String name){
		var s=super.getResourceAsStream(name);
		return s==null?getParent().getResourceAsStream(name):s;
	}
	
	private byte[] loadClassData(String name) throws IOException{
		var         f     =name.replace('.', '/')+".class";
		InputStream stream=getResourceAsStream(f);
		if(stream==null) return null;
		
		int             size  =stream.available();
		byte            buff[]=new byte[size];
		DataInputStream in    =new DataInputStream(stream);
		// Reading the binary data
		in.readFully(buff);
		in.close();
		return buff;
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException{
		LogUtil.println(name);
		return super.findClass(name);
	}
	
	@SuppressWarnings({"restriction", "unchecked"})
	public static URL[] getUrls(ClassLoader classLoader){
		if(classLoader instanceof URLClassLoader){
			return ((URLClassLoader)classLoader).getURLs();
		}
		
		// jdk9
		if(classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")){
			try{
				Field field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				field.setAccessible(true);
				sun.misc.Unsafe unsafe=(sun.misc.Unsafe)field.get(null);
				
				// jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
				Field  ucpField      =classLoader.getClass().getDeclaredField("ucp");
				long   ucpFieldOffset=unsafe.objectFieldOffset(ucpField);
				Object ucpObject     =unsafe.getObject(classLoader, ucpFieldOffset);
				
				// jdk.internal.loader.URLClassPath.path
				Field          pathField      =ucpField.getType().getDeclaredField("path");
				long           pathFieldOffset=unsafe.objectFieldOffset(pathField);
				ArrayList<URL> path           =new ArrayList<>((ArrayList<URL>)unsafe.getObject(ucpObject, pathFieldOffset));
//				path.add(new File(".").toURI().toURL());
				return path.toArray(new URL[path.size()]);
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
}
