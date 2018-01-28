package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.devonly.ValidationLayers;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer implements VkDestroyable{
	
	public static final boolean DEVELOPMENT;
	public static final String ENGINE_NAME   ="JLapisor";
	public static final String ENGINE_VERSION="0.0.1";
	
	private VkAllocationCallbacks allocator;
	
	static{
		String key="VulkanRenderer.devMode",
			devArg0=System.getProperty(key, "false"),
			devArg=devArg0.toLowerCase();
		
		if(devArg.equals("true")) DEVELOPMENT=true;
		else{
			if(devArg.equals("false")) DEVELOPMENT=false;
			else throw UtilL.exitWithErrorMsg("Invalid property: "+key+"="+devArg0+" (valid: \"true\", \"false\", \"\")");
		}
		System.setProperty("org.lwjgl.util.NoChecks", Boolean.toString(DEVELOPMENT));
		LogUtil.println("Running VulkanRenderer in "+(DEVELOPMENT?"development":"production")+" mode");
	}
	
	
	private VkInstance instance;
	
	private VkGpu renderGpu, computeGpu;
	
	private GlfwWindowVk window;
	
	public VulkanRenderer(){
	}
	
	public void render(){
	
	}
	
	public void createContext(GlfwWindowVk window){
		this.window=window;
		try(MemoryStack stack=stackPush()){
			
			
			List<String> layerNames=new ArrayList<>(), extensionNames=new ArrayList<>();
			
			if(DEVELOPMENT){
				ValidationLayers.addLayers(layerNames);
				
				extensionNames.add(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
			}
			
			PointerBuffer requ=glfwGetRequiredInstanceExtensions();
			Stream.generate(requ::getStringUTF8).limit(requ.limit()).forEach(extensionNames::add);
			
			
			BiFunction<List<String>, List<String>, Void> check=(supported, requested)->{
				String fails=requested.stream()
				                      .filter(l->!supported.contains(l))
				                      .map(s->s==null?"<NULL_STRING>":s.isEmpty()?"<EMPTY_STRING>":s)
				                      .collect(Collectors.joining(", "));
				
				if(!fails.isEmpty()) throw new IllegalStateException("Not supported: "+fails);
				return null;
			};
			
			check.apply(Vk.enumerateInstanceLayerProperties(stack).stream().map(VkLayerProperties::layerNameString).collect(Collectors.toList()), layerNames);
			check.apply(Vk.enumerateInstanceExtensionProperties(stack).stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toList()), extensionNames);
			
			
			VkInstanceCreateInfo info=VkInstanceCreateInfo.callocStack(stack);
			info.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
			    .pApplicationInfo(Vk.initAppInfo(stack, window.title.get(), "0.0.1", ENGINE_NAME, ENGINE_VERSION))
			    .ppEnabledLayerNames(Vk.stringsToPP(layerNames, stack))
			    .ppEnabledExtensionNames(Vk.stringsToPP(extensionNames, stack));
			
			instance=Vk.createInstance(info, allocator);
		}
	}
	
	public VkInstance getInstance(){
		return instance;
	}
	
	public VkGpu getRenderGpu(){
		return renderGpu;
	}
	
	public VkGpu getComputeGpu(){
		return computeGpu;
	}
	
	public VkAllocationCallbacks getAllocator(){
		return allocator;
	}
	
	@Override
	public void destroy(){
		vkDestroyInstance(instance, allocator);
	}
	
}
