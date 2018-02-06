package com.lapissea.vulkanimpl.util.types;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.shaders.VkShader;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static java.nio.ByteOrder.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkPipelineCache implements VkDestroyable, VkGpuCtx{
	
	public static VkPipelineCache create(VkShader shader){
		ByteBuffer data=null;
		try(MemoryStack stack=stackPush()){
			
			VkPipelineCacheCreateInfo cacheInfo=VkPipelineCacheCreateInfo.callocStack(stack);
			cacheInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);
			cacheInfo.pInitialData(data=getData(shader));
//			System.exit(0);
			LongBuffer dest=stack.mallocLong(1);
			int        code=vkCreatePipelineCache(shader.getGpu().getDevice(), cacheInfo, null, dest);
			if(DevelopmentInfo.DEV_ON) Vk.check(code);
			return new VkPipelineCache(dest.get(0), shader);
		}finally{
			if(data!=null) memFree(data);
		}
		
	}
	
	private static ByteBuffer getData(VkShader shader){
		ByteBuffer data=null;
		try{
			
			File cache=getFile(shader);
			if(!cache.isFile()) return null;
			
			ByteBuffer header=stackMalloc(4*4+VK_UUID_SIZE).order(LITTLE_ENDIAN);
			data=memAlloc((int)cache.length()-header.capacity()).order(LITTLE_ENDIAN);
			try(BufferedInputStream fileData=new BufferedInputStream(new FileInputStream(cache))){
				Vk.readBytes(header, fileData);
				Vk.readBytes(data, fileData);
			}
			data.flip();
			header.flip();
			
			int headerLength=header.getInt(0),
				cacheHeaderVersion=header.getInt(4),
				vendorID=header.getInt(8),
				deviceID=header.getInt(12);
			
			String error=null;
			
			if(headerLength!=data.limit()) error="Bad header length ("+headerLength+")";
			else if(cacheHeaderVersion!=VK_PIPELINE_CACHE_HEADER_VERSION_ONE) error="Unsupported cache header version ("+cacheHeaderVersion+")";
			else{
				VkPhysicalDeviceProperties props=shader.getGpu().getPhysicalProperties();
				if(vendorID!=props.vendorID()) error="Vendor ID mismatch ("+vendorID+"/"+props.vendorID()+")";
				else if(deviceID!=props.deviceID()) error="Device ID mismatch ("+deviceID+"/"+props.deviceID()+")";
				else{
					for(int i=0;i<VK_UUID_SIZE;i++){
						if(header.get(i+16)!=props.pipelineCacheUUID(i)) error="UUID mismatch";
					}
				}
			}
			if(error!=null){
				LogUtil.printlnEr("Cache at \""+cache+"\" has: "+error);
				memFree(data);
//				System.exit(0);
				return null;
			}
//			System.exit(0);
		}catch(Exception e){ }
		
		if(data!=null) memFree(data);
		return null;
	}
	
	private static File getFile(VkShader shader){
		return new File(".cache/vk/pipeline/"+shader.name+".bin");
	}
	
	public final  long     handle;
	private final VkShader shader;
	
	public VkPipelineCache(long handle, VkShader shader){
		this.handle=handle;
		this.shader=shader;
	}
	
	public void save(){
		try(MemoryStack stack=stackPush()){
			
			PointerBuffer size=stack.callocPointer(1);
			Vk.getPipelineCacheData(getGpu(), handle, size, null);
			ByteBuffer data=memAlloc((int)size.get(0));
			try{
				Vk.getPipelineCacheData(getGpu(), handle, size, data);
				
				File cache=getFile(shader);
				cache.getParentFile().mkdirs();
				cache.createNewFile();
				
				VkPhysicalDeviceProperties props =shader.getGpu().getPhysicalProperties();
				ByteBuffer                 header=stack.malloc(4*4+VK_UUID_SIZE).order(LITTLE_ENDIAN);
				header.putInt(data.limit())
				      .putInt(VK_PIPELINE_CACHE_HEADER_VERSION_ONE)
				      .putInt(props.vendorID())
				      .putInt(props.deviceID());
				for(int i=0;i<VK_UUID_SIZE;i++){
					header.put(props.pipelineCacheUUID(i));
				}
				header.flip();
				
				try(BufferedOutputStream out=new BufferedOutputStream(new FileOutputStream(cache))){
					
					Vk.writeBytes(header, out);
					Vk.writeBytes(data, out);
					
					out.flush();
				}
			}catch(IOException e){ }finally{
				memFree(data);
			}
		}
	}
	
	@Override
	public void destroy(){
		vkDestroyPipelineCache(getGpu().getDevice(), handle, null);
	}
	
	@Override
	public VkGpu getGpu(){
		return shader.getGpu();
	}
}
