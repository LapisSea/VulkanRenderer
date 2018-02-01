package com.lapissea.vulkanimpl.util;

import com.lapissea.util.LogUtil;
import com.lapissea.util.UtilL;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

import static com.lapissea.vulkanimpl.VulkanRenderer.Settings.*;

public class VkShaderCompiler{
	
	public static void compileVertex(String file){
		compile(file, ".vert");
	}
	
	public static void compileFragment(String file){
		compile(file, ".frag");
	}
	
	private static void compile(String file, String type){
		LogUtil.println("Compiling", file);
		File glslSrcFile=new File("res\\assets\\shaders", file);
		if(!glslSrcFile.exists()) throw UtilL.uncheckedThrow(new FileNotFoundException());
		File   spvSrcDest=new File(glslSrcFile+".spv");
		File   target    =glslSrcFile;
		byte[] b         =new byte[2048];
		
		if(!file.endsWith(type)){
			target=new File("res\\assets\\shaders\\compiler\\"+file+type);
			target.getParentFile().mkdirs();
			try(BufferedInputStream in=new BufferedInputStream(new FileInputStream(glslSrcFile), b.length);OutputStream out=new FileOutputStream(target)){
				int len=in.read(b);
				out.write(b, 0, len);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try{
			String n=System.getProperty("line.separator");

//			spvSrcDest.createNewFile();
			File     sdk     =new File("C:\\VulkanSDK");
			String[] versions=sdk.list();
			Arrays.sort(versions);
			String command=new File(sdk, versions[versions.length-1]+"\\Bin32\\glslangValidator.exe").getAbsolutePath()+
			               " -V -t \""+target.getAbsolutePath()+"\" -o \""+spvSrcDest.getAbsolutePath()+"\""+n;
			
			StringBuilder out=new StringBuilder();
			
			Process p=Runtime.getRuntime().exec("cmd.exe");
			
			try(InputStream is=p.getInputStream();OutputStream os=p.getOutputStream()){
				//ignore start print
				while(is.available()==0) UtilL.sleep(1);
				while(is.available()>0) is.read(b);
				os.write(command.getBytes());
				os.flush();
				os.write(("exit"+n).getBytes());
				os.flush();
				while(p.isAlive()){
					int i;
					try{
						i=is.read(b);
					}catch(IOException e){
						break;
					}
					if(i>0) out.append(new String(b, 0, i));
				}
			}
			int    startCut=command.length()+target.getAbsolutePath().length()+n.length();
			int    endCut  =new File("").getAbsolutePath().length()+">exit".length()+n.length()*2;
			String output  =out.toString().substring(startCut, out.length()-endCut);
			if(!output.isEmpty()){
				if(DEVELOPMENT){
					File f     =new File(glslSrcFile+".log");
					File parent=f.getParentFile();
					f=new File(parent, "DEVELOPMENT\\"+f.getName());
					f.getParentFile().mkdirs();
					
					Files.write(f.toPath(), output.getBytes());
				}
				LogUtil.printEr("Shader "+file+" failed to compile!\n"+output);
				throw new IllegalStateException();
			}
			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}