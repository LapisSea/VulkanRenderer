package com.lapissea.vulkannutcrack;

public class MyTake{
	//
	//	public static void main(String[] args){
	//		LogUtil.__.INJECT_DEBUG_PRINT(true);
	//		LogUtil.__.INJECT_FILE_LOG("log.txt");
	//		LogUtil.printWrapped("[STARTED]");
	//		new MyTake();
	//	}
	//
	//	private final VkDebugReportCallbackEXT dbgFunc=VkDebugReportCallbackEXT.create((flags, objectType, object, location, messageCode, pLayerPrefix, pMessage, pUserData)->{
	//		String type;
	//		if((flags&EXTDebugReport.VK_DEBUG_REPORT_INFORMATION_BIT_EXT)!=0) type="INFORMATION";
	//		else if((flags&EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT)!=0) type="WARNING";
	//		else if((flags&EXTDebugReport.VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT)!=0) type="PERFORMANCE WARNING";
	//		else if((flags&EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT)!=0) type="ERROR";
	//		else if((flags&EXTDebugReport.VK_DEBUG_REPORT_DEBUG_BIT_EXT)!=0) type="DEBUG";
	//		else type="UNKNOWN";
	//
	//		String msg=type+": ["+MemoryUtil.memASCII(pLayerPrefix)+"] Code "+messageCode+": "+VkDebugReportCallbackEXT.getString(pMessage);
	//		if(type.equals("ERROR")) LogUtil.printStackTrace(msg);
	//		else LogUtil.printlnEr(msg);
	//		/*
	//		 * false indicates that layer should not bail-out of an
	//		 * API call that had validation failures. This may mean that the
	//		 * app dies inside the driver due to invalid parameter(s).
	//		 * That's what would happen without validation layers, so we'll
	//		 * keep that behavior here.
	//		 */
	//		return VK10.VK_FALSE;
	//	});
	//
	//	private static final ByteBuffer KHR_SWAPCHAIN   =MemoryUtil.memASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
	//	private static final ByteBuffer EXT_DEBUG_REPORT=MemoryUtil.memASCII(EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
	//
	//
	//	private VkInstance       instance;
	//	private VkPhysicalDevice gpu;
	//	private VkDevice         device;
	//
	//	private final IntBuffer     ip=MemoryUtil.memAllocInt(1);
	//	private final LongBuffer    lp=MemoryUtil.memAllocLong(1);
	//	private final PointerBuffer pp=MemoryUtil.memAllocPointer(1);
	//
	//	private PointerBuffer extension_names=MemoryUtil.memAllocPointer(64);
	//
	//	private VkPhysicalDeviceProperties gpuProps   =VkPhysicalDeviceProperties.malloc();
	//	private VkPhysicalDeviceFeatures   gpuFeatures=VkPhysicalDeviceFeatures.malloc();
	//	private VkQueueFamilyProperties.Buffer queueProps;
	//
	//	private long window;
	//	private long surface;
	//	private Vec2i windowSize=new Vec2i(400, 300);
	//	private int graphicsQueueNodeIndex;
	//
	//	private Destroyable resourceInstance=new Destroyable(()->VK10.vkDestroyInstance(instance, null));
	//
	//	public MyTake(){
	//		NanoTimer timer=new NanoTimer();
	//		timer.start();
	//		initGlfw();
	//
	//		try(MemoryStack stack=stackPush()){
	//			initVk(stack);
	//		}
	//		initWindow();
	//
	//		try(MemoryStack stack=stackPush()){
	//			initVkSwapchain(stack);
	//		}
	//		timer.end();
	//		destroyVk();
	//		LogUtil.println(timer.ms());
	//	}
	//
	//	private void initWindow(){
	//		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
	//
	//		window=glfwCreateWindow(windowSize.x(), windowSize.y(), "The Vulkan Triangle Demo Program", MemoryUtil.NULL, MemoryUtil.NULL);
	//		if(window==MemoryUtil.NULL)throw new IllegalStateException("Cannot create a window in which to draw!");
	//
	//		glfwSetWindowRefreshCallback(window, window->draw());
	//
	//		glfwSetFramebufferSizeCallback(window, (window, width, height)->{
	//			windowSize.set(width, height);
	//			if(width!=0&&height!=0){
	//				resize();
	//			}
	//		});
	//
	//		glfwSetKeyCallback(window, (window, key, scancode, action, mods)->{
	//			if(key==GLFW_KEY_ESCAPE&&action==GLFW_RELEASE){
	//				glfwSetWindowShouldClose(window, true);
	//			}
	//		});
	//	}
	//
	//
	//	private void initGlfw(){
	//		GLFWErrorCallback.createPrint().set();
	//		if(!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
	//		if(!glfwVulkanSupported()) throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)");
	//	}
	//
	//	private static PointerBuffer checkLayers(MemoryStack stack, VkLayerProperties.Buffer available, String... layers){
	//		PointerBuffer required=stack.mallocPointer(layers.length);
	//		for(int i=0;i<layers.length;i++){
	//			boolean found=false;
	//
	//			for(int j=0;j<available.capacity();j++){
	//				available.position(j);
	//				if(layers[i].equals(available.layerNameString())){
	//					found=true;
	//					break;
	//				}
	//			}
	//
	//			if(!found){
	//				LogUtil.printlnEr("Cannot find layer:", layers[i]);
	//				return null;
	//			}
	//
	//			required.put(i, stack.ASCII(layers[i]));
	//		}
	//
	//		return required;
	//	}
	//
	//	@SuppressWarnings("boxing")
	//	private void initVk(MemoryStack stack){
	//
	//		PointerBuffer required_extensions=glfwGetRequiredInstanceExtensions();
	//		if(required_extensions==null){
	//			throw new IllegalStateException("glfwGetRequiredInstanceExtensions failed to find the platform surface extensions.");
	//		}
	//		for(int i=0;i<required_extensions.capacity();i++){
	//			extension_names.put(required_extensions.get(i));
	//		}
	//
	//		VkExtensionProperties.Buffer instanceExtension=Vk.enumerateInstanceExtensionProperties(stack, ip);
	//		for(int i=0;i<instanceExtension.capacity();i++){
	//			instanceExtension.position(i);
	//			if(EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME.equals(instanceExtension.extensionNameString())){
	//				if(DEBUG){
	//					extension_names.put(EXT_DEBUG_REPORT);
	//				}
	//			}
	//		}
	//		PointerBuffer requiredLayers=null;
	//		if(DEBUG){
	//			lay:
	//			{
	//				VkLayerProperties.Buffer availableLayers=Vk.enumerateInstanceLayerProperties(stack, ip);
	//				if(availableLayers==null) break lay;
	//
	//				requiredLayers=checkLayers(stack, availableLayers, "VK_LAYER_LUNARG_standard_validation");
	//				if(requiredLayers!=null) break lay;
	//
	//				// use alternative set of validation layers
	//				requiredLayers=checkLayers(stack, availableLayers, "VK_LAYER_GOOGLE_threading", "VK_LAYER_LUNARG_parameter_validation", "VK_LAYER_LUNARG_object_tracker", "VK_LAYER_LUNARG_image", "VK_LAYER_LUNARG_core_validation", "VK_LAYER_LUNARG_swapchain", "VK_LAYER_GOOGLE_unique_objects");
	//			}
	//
	//			if(requiredLayers==null) throw new IllegalStateException("vkEnumerateInstanceLayerProperties failed to find required validation layer.");
	//		}
	//
	//		VkApplicationInfo    appInfo     =Vk.initAppInfo(stack, "lapis");
	//		VkInstanceCreateInfo instanceInfo=Vk.initInstanceInfo(stack, appInfo);
	//		instanceInfo.ppEnabledLayerNames(requiredLayers);
	//		extension_names.flip();
	//		instanceInfo.ppEnabledExtensionNames(extension_names);
	//		extension_names.clear();
	//
	//		VkDebugReportCallbackCreateInfoEXT dbgCreateInfo;
	//
	//		if(DEBUG){
	//			dbgCreateInfo=VkDebugReportCallbackCreateInfoEXT.mallocStack(stack);
	//			dbgCreateInfo.sType(EXTDebugReport.VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
	//			             .pNext(MemoryUtil.NULL)
	//			             .flags(EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT|EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT)
	//			             .pfnCallback(dbgFunc)
	//			             .pUserData(MemoryUtil.NULL);
	//
	//			instanceInfo.pNext(dbgCreateInfo.address());
	//		}
	//		instance=Vk.createInstance(instanceInfo, pp);
	//
	//		int gpuCount=Vk.enumeratePhysicalDevices(instance, ip);
	//		LogUtil.println("Vulkan has", gpuCount, "compatible device"+(gpuCount!=1?"s":"")+"!");
	//		if(gpuCount==0) throw new IllegalStateException("Vulkan reported zero accessible devices!");
	//
	//		PointerBuffer physical_devices=stack.mallocPointer(gpuCount);
	//		Vk.enumeratePhysicalDevices(instance, ip, physical_devices);
	//		gpu=new VkPhysicalDevice(physical_devices.get(0), instance);
	//
	//		useSwapchainExt(stack);
	//		if(DEBUG){
	////			long debugCallback=Vk.createDebugReportCallbackEXT(instance, dbgCreateInfo, lp);
	////			resourceInstance.addChild(()->EXTDebugReport.vkDestroyDebugReportCallbackEXT(instance, debugCallback, null));
	//		}
	//
	//		VK10.vkGetPhysicalDeviceProperties(gpu, gpuProps);
	////		queueProps=Vk.getPhysicalDeviceQueueFamilyProperties(gpu, ip);
	//		VK10.vkGetPhysicalDeviceFeatures(gpu, gpuFeatures);
	//
	//	}
	//
	//	private void initVkSwapchain(MemoryStack stack){
	//
	//		surface=Glfw.createWindowSurface(instance, window, lp);
	//
	//		IntBuffer supportsPresent=stack.mallocInt(queueProps.capacity());
	//		for(int i=0;i<supportsPresent.capacity();i++){
	//			supportsPresent.position(i);
	//			KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(gpu, i, surface, supportsPresent);
	//		}
	//
	//		int graphicsQueueNodeIndex=-1, presentQueueNodeIndex=-1;
	//
	//		for(int i=0;i<supportsPresent.capacity();i++){
	//			VkQueueFamilyProperties prop=queueProps.get(i);
	//
	//			if((prop.queueFlags()&VK_QUEUE_GRAPHICS_BIT)==0){
	//				if(graphicsQueueNodeIndex==-1) graphicsQueueNodeIndex=i;
	//				if(supportsPresent.get(i)==VK_TRUE){
	//					graphicsQueueNodeIndex=i;
	//					presentQueueNodeIndex=i;
	//					break;
	//				}
	//			}
	//		}
	//		if(graphicsQueueNodeIndex==-1) throw new IllegalStateException("Could not find a graphics queue");
	//
	//
	//		if(presentQueueNodeIndex==-1){
	//			// If didn't find a queue that supports both graphics and present, then
	//			// find a separate present queue.
	//			for(int i=0;i<supportsPresent.capacity();++i){
	//				if(supportsPresent.get(i)==VK_TRUE){
	//					presentQueueNodeIndex=i;
	//					break;
	//				}
	//			}
	//			if(presentQueueNodeIndex==-1) throw new IllegalStateException("Could not find a present queue");
	//		}
	//		//TODO: Add support for separate queues, including presentation, synchronization, and appropriate tracking for QueueSubmit.
	//		if(graphicsQueueNodeIndex!=presentQueueNodeIndex) throw new IllegalStateException("Could not find a common graphics and a present queue");
	//		this.graphicsQueueNodeIndex=graphicsQueueNodeIndex;
	//
	//		try(MemoryStack stack0=stackPush()){
	//			initDevice(stack0);
	//		}
	//	}
	//
	//	private void initDevice(MemoryStack stack){
	//		VkDeviceQueueCreateInfo.Buffer queue=VkDeviceQueueCreateInfo.mallocStack(1, stack);
	//		queue.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
	//		     .pNext(MemoryUtil.NULL)
	//		     .flags(0)
	//		     .queueFamilyIndex(graphicsQueueNodeIndex)
	//		     .pQueuePriorities(stack.floats(0.0f));
	//
	//		VkPhysicalDeviceFeatures features=VkPhysicalDeviceFeatures.callocStack(stack);
	//		if(gpuFeatures.shaderClipDistance()) features.shaderClipDistance(true);
	//
	//		extension_names.flip();
	//		VkDeviceCreateInfo device=VkDeviceCreateInfo.mallocStack(stack);
	//		device.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
	//		      .pNext(MemoryUtil.NULL)
	//		      .flags(0)
	//		      .pQueueCreateInfos(queue)
	//		      .ppEnabledLayerNames(null)
	//		      .ppEnabledExtensionNames(extension_names)
	//		      .pEnabledFeatures(features);
	//
	//		this.physicalDevice=new VkDevice(pp.get(0), gpu, physicalDevice);
	//	}
	//
	//	private void useSwapchainExt(MemoryStack stack){
	//		boolean swapchainExtFound=false;
	//		int     propCount        =Vk.enumerateDeviceExtensionProperties(gpu, ip);
	//
	//		if(propCount>0){
	//			VkExtensionProperties.Buffer device_extensions=VkExtensionProperties.mallocStack(propCount, stack);
	//			int                          count            =Vk.enumerateDeviceExtensionProperties(gpu, null, ip, device_extensions);
	//
	//			for(int i=0;i<count;i++){
	//				device_extensions.position(i);
	//				if(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(device_extensions.extensionNameString())){
	//					swapchainExtFound=true;
	//					extension_names.put(KHR_SWAPCHAIN);
	//				}
	//			}
	//		}
	//		if(!swapchainExtFound)
	//			throw new IllegalStateException("Vulkan failed to find the "+KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME+" extension.");
	//	}
	//
	//	private void destroyVk(){
	//		resourceInstance.destroy();
	//	}
	//
	//
	//	private void resize(){
	//
	//	}
	//
	//	private void draw(){
	//
	//	}
}
