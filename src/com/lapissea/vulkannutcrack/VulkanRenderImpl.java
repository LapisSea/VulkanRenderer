package com.lapissea.vulkannutcrack;

import com.lapissea.util.LogUtil;
import com.lapissea.util.PairM;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import com.lapissea.util.filechange.FileChageDetector;
import com.lapissea.vec.Vec2i;
import com.lapissea.vec.Vec3f;
import com.lapissea.vec.color.ColorM;
import com.lapissea.vulkannutcrack.model.VkModel;
import com.lapissea.vulkannutcrack.model.build.VkModelBuilder;
import com.lapissea.vulkannutcrack.model.build.VkModelUploader;
import com.lapissea.vulkannutcrack.simplevktypes.VkRenderPass;
import com.lapissea.vulkannutcrack.simplevktypes.VkSemaphore;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.lapissea.vulkannutcrack.BufferUtil.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderImpl{
	
	private final ByteBuffer winBufB=ByteBuffer.allocate(4*4);
	private final IntBuffer  winBuf =winBufB.asIntBuffer();
	private final File       winFile=new File("winData.bin");
	
	public static void main(String[] args){
		TextUtil.__REGISTER_CUSTOM_TO_STRING(VkExtent2D.class, e->e.getClass().getName()+"{h="+e.height()+", w="+e.height()+"}");
		LogUtil.__.INJECT_FILE_LOG("log.txt");
		LogUtil.__.INJECT_DEBUG_PRINT(true);
		LogUtil.println("STARTED at "+Date.from(Instant.now()));
		
		new VulkanRenderImpl();
	}
	
	private GlfwWindow window=new GlfwWindow();
	
	private final IntBuffer     ip=Buff.i.get(1);
	private final LongBuffer    lp=Buff.l.get(1);
	private final PointerBuffer pp=Buff.p.get(1);
	private final FloatBuffer   fp=Buff.f.get(1);
	private VkInstance    instance;
	private VkDebugReport debugReport;
	private VkGpu         gpu;
	private VkSwapchain   swapChain;
	private Shader        shader;
	private VkRenderPass  renderPass;
	
	private PointerBuffer layers          =null;
	private List<String>  deviceExtensions=new ArrayList<>(List.of(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
	
	private boolean swapchainRecreationPending;
	
	
	private long sec, fps;
	private boolean resizing;
	
	private VkModel           model;
	private VkViewport.Buffer viewport;
	
	private VkSemaphore imageAvailableSemaphore;
	private VkSemaphore renderFinishedSemaphore;
	
	public VulkanRenderImpl(){
		
		initWindow();
		
		Vk.stack(this::initVulkan);
		Vk.stack(this::pickPhysicalDevice);
		gpu.init(deviceExtensions, layers);
		Vk.stack(this::createSemaphores);
		
		VkModelBuilder b=new VkModelBuilder(new VkModelFormat(Vec3f.class, ColorM.class));
		b.put(new Vec3f(-0.5F, -0.5F, 0)).put(new ColorM(1, 0, 0, 1)).done();
		b.put(new Vec3f(+0.5F, -0.5F, 0)).put(new ColorM(0, 1, 0, 1)).done();
		b.put(new Vec3f(+0.5F, +0.5F, 0)).put(new ColorM(1, 1, 0, 1)).done();
		b.put(new Vec3f(-0.5F, +0.5F, 0)).put(new ColorM(0, 0, 1, 1)).done();
		b.indices(0, 1, 2,
		          0, 2, 3);
		
		model=VkModelUploader.upload(gpu, b);
		
		createSwapChain();
		run();
		destroy();
	}
	
	private void drawFrame(MemoryStack stack){
		long tim=System.currentTimeMillis();
		if(tim-sec>1000){
			sec=tim;
			LogUtil.println(fps);
			fps=0;
		}
		fps++;
		
		int imageIndex=Vk.acquireNextImageKHR(gpu.getDevice(), swapChain, imageAvailableSemaphore, ip);
		if(imageIndex==-1){
			recreateSwapChain();
			imageIndex=Vk.acquireNextImageKHR(gpu.getDevice(), swapChain, imageAvailableSemaphore, ip);
		}
		
		VkSubmitInfo submitInfo=VkSubmitInfo.callocStack(stack);
		submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
		submitInfo.waitSemaphoreCount(1);
		submitInfo.pWaitSemaphores(buffSingle(stack, imageAvailableSemaphore));
		submitInfo.pSignalSemaphores(buffSingle(stack, renderFinishedSemaphore));
		submitInfo.pWaitDstStageMask(buffSingle(stack, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
		submitInfo.pCommandBuffers(buffSingle(stack, swapChain.getBuffer(imageIndex).getCmd()));
		
		VkPresentInfoKHR presentInfo=VkPresentInfoKHR.callocStack(stack);
		presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
		presentInfo.pWaitSemaphores(submitInfo.pSignalSemaphores());
		presentInfo.swapchainCount(1);
		presentInfo.pSwapchains(buffSingle(stack, swapChain.getId()));
		presentInfo.pImageIndices(buffSingle(stack, imageIndex));
		
		vkQueueSubmit(gpu.getGraphicsQueue(), submitInfo, VK_NULL_HANDLE);
		
		Vk.queuePresentKHR(gpu.getSurfaceQueue(), presentInfo);
		
		Vk.queueWaitIdle(gpu.getSurfaceQueue());
	}
	
	
	private void destroySwapChain(){
		swapChain.destroy();
		swapChain=null;
		renderPass.destroy(gpu.getDevice());
		renderPass=null;
		shader.destroy(gpu.getDevice());
		shader=null;
	}
	
	private void createSwapChain(){
		if(resizing) return;
		resizing=true;
		
		Vk.stack(s->{
			swapChain=new VkSwapchain();
			swapChain.create(s, gpu, deviceExtensions, layers);
		});
		Vk.stack(this::createRenderPass);
		createGraphicsPipeline();
		Vk.stack(s->swapChain.initFrameBuffers(s, renderPass));
		Vk.stack(s->swapChain.initCommandBuffers(s, gpu.getGraphicsPool(), renderPass, shader, model, viewport));
		
		resizing=false;
	}
	
	private void createSemaphores(MemoryStack stack){
		VkSemaphoreCreateInfo semaphoreInfo=VkSemaphoreCreateInfo.callocStack(stack);
		semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
		             .pNext(NULL)
		             .flags(0);
		imageAvailableSemaphore=Vk.createSemaphore(gpu.getDevice(), semaphoreInfo, stack.mallocLong(1));
		renderFinishedSemaphore=Vk.createSemaphore(gpu.getDevice(), semaphoreInfo, stack.mallocLong(1));
	}
	
	
	private void createRenderPass(MemoryStack stack){
		VkAttachmentDescription.Buffer attachments=VkAttachmentDescription.mallocStack(1, stack);
		attachments.get(0)
		           .flags(0)
		           .format(swapChain.getFormat())
		           .samples(VK_SAMPLE_COUNT_1_BIT)
		/*
		VK_ATTACHMENT_LOAD_OP_LOAD: Preserve the existing contents of the attachment
		VK_ATTACHMENT_LOAD_OP_CLEAR: Clear the values to a constant at the start
		VK_ATTACHMENT_LOAD_OP_DONT_CARE: Existing contents are undefined; we don't care about them
		
		VK_ATTACHMENT_STORE_OP_STORE: Rendered contents will be stored in memory and can be read later
		VK_ATTACHMENT_STORE_OP_DONT_CARE: Contents of the framebuffer will be undefined after the rendering
		*/
		           .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
		           .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
		           .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
		           .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
		/*
		VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL: Images used as color attachment
		VK_IMAGE_LAYOUT_PRESENT_SRC_KHR: Images to be presented in the swap chain
		VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL: Images to be used as destination for a memory copy operation
		 */
		           .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
		           .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
		
		
		VkSubpassDescription.Buffer subpass=VkSubpassDescription.callocStack(1, stack);
		subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
		       .colorAttachmentCount(1)
		       .pColorAttachments(
				       VkAttachmentReference.mallocStack(1, stack)
				                            .attachment(0)
				                            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL))
		       .pDepthStencilAttachment(null);
		
		VkRenderPassCreateInfo renderPassInfo=VkRenderPassCreateInfo.callocStack(stack);
		renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
		              .pAttachments(attachments)
		              .pSubpasses(subpass);
		
		VkSubpassDependency.Buffer dependency=VkSubpassDependency.mallocStack(1, stack);
		dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
		dependency.dstSubpass(0);
		dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
		dependency.srcAccessMask(0);
		dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
		dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT|VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
		renderPassInfo.pDependencies(dependency);
		
		renderPass=Vk.createRenderPass(gpu.getDevice(), renderPassInfo, lp);
	}
	
	private void createGraphicsPipeline(){
		FileChageDetector.autoHandle(new File("res/shaders/test.vert"), ()->VkShaderCompiler.compileVertex("test.vert"));
		FileChageDetector.autoHandle(new File("res/shaders/test.frag"), ()->VkShaderCompiler.compileFragment("test.frag"));
		shader=new Shader("test");
		
		viewport=VkViewport.malloc(1);
		viewport.x(0);
		viewport.y(0);
		viewport.width(swapChain.getExtent().width());
		viewport.height(swapChain.getExtent().height());
		viewport.minDepth(0);
		viewport.maxDepth(1);
		shader.create(gpu, swapChain.getExtent(), renderPass, viewport, model);
	}
	
	
	private void initWindow(){
		
		Thread t=new Thread(()->{
			Vec2i pos =new Vec2i();
			Vec2i size=new Vec2i();
			
			while(true){
				UtilL.sleep(1000);
				
				boolean dirty=false;
				if(!window.getPos().equals(pos)){
					dirty=true;
					pos.set(window.getPos());
				}
				if(!window.getSize().equals(size)){
					dirty=true;
					size.set(window.getSize());
				}
				
				if(dirty){
					winBuf.put(0, pos.x());
					winBuf.put(1, pos.y());
					winBuf.put(2, size.x());
					winBuf.put(3, size.y());
					try{
						Files.write(winFile.toPath(), winBufB.array());
					}catch(IOException e){
						e.printStackTrace();
					}
				}
			}
		});
		t.setDaemon(true);
		t.start();
		window.setPos(200, 200).setSize(600, 400);
		try{
			winBufB.put(Files.readAllBytes(winFile.toPath()));
			window.setPos(winBuf.get(0), winBuf.get(1)).setSize(winBuf.get(2), winBuf.get(3));
		}catch(IOException e){
			e.printStackTrace();
		}
		
		Glfw.init();
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		window.setResizeable(true).setTitle("Lapis Vulkan").init();
		window.onResize(s->swapchainRecreationPending=true);
	}
	
	private void initVulkan(MemoryStack stack){
		initLayers(stack);
		VkApplicationInfo    applicationInfo   =Vk.initAppInfo(stack, "Lapis test");
		VkInstanceCreateInfo instanceCreateInfo=Vk.initInstanceInfo(stack, applicationInfo);
		instanceCreateInfo.ppEnabledExtensionNames(getExtensions(stack));
		instanceCreateInfo.ppEnabledLayerNames(layers);
		
		instance=Vk.createInstance(instanceCreateInfo, pp);
		
		if(Vk.DEBUG) debugReport=new VkDebugReport(instance, lp, stack, (type, prefix, code, message)->{
			if(type==VkDebugReport.Type.DEBUG) return;
			if(type==VkDebugReport.Type.INFORMATION) return;
			
			List<String>  msgLin=TextUtil.wrapLongString(message, 100);
			StringBuilder msg   =new StringBuilder().append(type).append(": [").append(prefix).append("] Code ").append(code).append(": ");
			
			if(msgLin.size()>1) msg.append("\n").append(TextUtil.wrappedString(UtilL.array(msgLin)));
			else msg.append(msgLin.get(0));
			
			if(type==VkDebugReport.Type.ERROR||type==VkDebugReport.Type.WARNING) throw new RuntimeException(msg.toString());
			else LogUtil.println(msg);
		});
	}
	
	private void pickPhysicalDevice(MemoryStack stack){
		int deviceCount=Vk.enumeratePhysicalDevices(instance, ip);
		if(deviceCount==0){
			LogUtil.printlnEr("No devices support Vulkan API!\n"+
			                  "Make sure your drivers are up to date. (Old hardware may not be supported regardless due to vulkan being a \"modern\" api and having a dedicated GPU helps a lot)\n"+
			                  "Stopping application...");
			System.exit(1);
		}
		PointerBuffer physicalDevices=stack.mallocPointer(deviceCount);
		Vk.enumeratePhysicalDevices(instance, ip, physicalDevices);
		
		window.createSurface(instance, lp);
		
		gpu=Arrays.stream(Vk.getPhysicalDevices(instance, physicalDevices))
		          .map(d->new VkGpu(window, stack, d))
		          .map(g->new PairM<>(g, calcDeviceUsability(g, stack)))
		          .filter(d->d.obj2>-1)
		          .sorted(Comparator.comparingInt(i->i.obj2))
		          .findFirst()
		          .map(p->p.obj1)
		          .orElseThrow(()->UtilL.exitWithErrorMsg("Sorry! Vulkan supporting "+TextUtil.plural("physicalDevice", deviceCount), "found but minimal requirements are not met! :(\n",
		                                                  "Stopping application..."));
		
		gpu.setStack(null);
		LogUtil.println("Using", VkPhysicalDeviceType.find(gpu.getProperties()), "called", gpu.getProperties().deviceNameString());
	}
	
	private boolean checkDeviceExtensions(MemoryStack stack, VkPhysicalDevice device){
		return Vk.getDeviceExtensionProperties(stack, device, ip)
		         .stream()
		         .map(VkExtensionProperties::extensionNameString)
		         .collect(Collectors.toList())
		         .containsAll(deviceExtensions);
	}
	
	private int calcDeviceUsability(VkGpu gpu, MemoryStack stack){
		VkPhysicalDeviceFeatures features=gpu.getFeatures();
		if(!features.geometryShader()||
		   !features.shaderClipDistance()||
		   !features.shaderTessellationAndGeometryPointSize()||
		   !features.tessellationShader()
				) return -1;
		
		if(!checkDeviceExtensions(stack, gpu.physicalDevice)) return -1;
		
		if(gpu.getQueueGraphicsId()==-1||
		   gpu.getQueueSurfaceId()==-1||
		   gpu.getFormats().capacity()==0||
		   gpu.getPresentModes().capacity()==0
				) return -1;
		
		int rating=0;
		rating+=VkPhysicalDeviceType.find(gpu.getProperties()).rating*100;
		
		return rating;
	}
	
	private PointerBuffer getExtensions(MemoryStack stack){
		PointerBuffer extensions=stack.mallocPointer(64);
		PointerBuffer ext       =glfwGetRequiredInstanceExtensions();
		for(int i=0;i<ext.capacity();i++){
			extensions.put(ext.get(i));
		}
		extensions.put(memASCII(EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME));
		extensions.flip();
		return extensions;
	}
	
	private void initLayers(MemoryStack stack){
		if(!Vk.DEBUG) return;
		
		PointerBuffer layers=null;
		lay:
		{
			VkLayerProperties.Buffer availableLayers=Vk.enumerateInstanceLayerProperties(stack, ip);
			if(availableLayers==null) break lay;
			if((layers=checkLayers(stack, availableLayers, "VK_LAYER_LUNARG_standard_validation"))!=null) break lay;
			layers=checkLayers(stack, availableLayers,
			                   "VK_LAYER_GOOGLE_threading",
			                   "VK_LAYER_LUNARG_parameter_validation",
			                   "VK_LAYER_LUNARG_object_tracker",
			                   "VK_LAYER_LUNARG_image",
			                   "VK_LAYER_LUNARG_core_validation",
			                   "VK_LAYER_LUNARG_swapchain",
			                   "VK_LAYER_GOOGLE_unique_objects"
			                  );
		}
		if(layers==null) throw new IllegalStateException("vkEnumerateInstanceLayerProperties failed to find required validation layer.");
		
		this.layers=memAllocPointer(layers.capacity());
		for(int i=0;i<layers.capacity();i++){
			this.layers.put(i, layers.get(i));
		}
	}
	
	private PointerBuffer checkLayers(MemoryStack stack, VkLayerProperties.Buffer available, String... layers){
		PointerBuffer required=stack.mallocPointer(layers.length);
		
		for(int i=0;i<layers.length;i++){
			boolean found=false;
			
			for(int j=0;j<available.capacity();j++){
				available.position(j);
				if(layers[i].equals(available.layerNameString())){
					found=true;
					break;
				}
			}
			
			if(!found){
				LogUtil.printlnEr("Cannot find layer:", layers[i]);
				return null;
			}
			
			required.put(i, stack.ASCII(layers[i]));
		}
		
		return required;
	}
	
	private void recreateSwapChain(){
		gpu.waitIdle();
		destroySwapChain();
		createSwapChain();
		gpu.waitIdle();
	}
	
	private void run(){
		while(!window.shouldClose()){
			glfwPollEvents();
			if(swapchainRecreationPending){
				swapchainRecreationPending=false;
				recreateSwapChain();
			}
			Vk.stack(this::drawFrame);
			gpu.waitIdle();
		}
		gpu.waitIdle();
	}
	
	private void destroy(){
		imageAvailableSemaphore.destroy(gpu.getDevice());
		renderFinishedSemaphore.destroy(gpu.getDevice());
		
		model.destroy(gpu.getDevice());
		
		destroySwapChain();
		
		gpu.destroy();
		window.destroySurface(instance);
		
		if(debugReport!=null) debugReport.destroy();
		Vk.destroyInstance(instance);
		
		window.destroy();
		glfwTerminate();
		
		renderPass=null;
	}
	
}
