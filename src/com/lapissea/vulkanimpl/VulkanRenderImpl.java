package com.lapissea.vulkanimpl;

import com.lapissea.datamanager.DataManager;
import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.LogUtil;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import com.lapissea.util.filechange.FileChageDetector;
import com.lapissea.vec.Vec2f;
import com.lapissea.vec.Vec2i;
import com.lapissea.vec.Vec3f;
import com.lapissea.vec.color.ColorM;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.model.VkBufferMemory;
import com.lapissea.vulkanimpl.model.VkModel;
import com.lapissea.vulkanimpl.model.build.VkModelBuilder;
import com.lapissea.vulkanimpl.simplevktypes.VkBuffer;
import com.lapissea.vulkanimpl.simplevktypes.VkImage;
import com.lapissea.vulkanimpl.simplevktypes.VkRenderPass;
import com.lapissea.vulkanimpl.simplevktypes.VkSemaphore;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import com.lapissea.vulkanimpl.util.VkImageFormat;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.lapissea.vulkanimpl.util.VkImageFormat.Format.*;
import static com.lapissea.vulkanimpl.util.VkImageFormat.Type.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderImpl{
	
	public static void main(String[] args){
		System.setProperty("joml.nounsafe", "true");
		System.runFinalizersOnExit(true);
		
		TextUtil.__REGISTER_CUSTOM_TO_STRING(VkExtent2D.class, e->e.getClass().getName()+"{h="+e.height()+", w="+e.height()+"}");
		LogUtil.__.INIT(true, false, "log");
		LogUtil.println("STARTED at "+Date.from(Instant.now()));
		
		new VulkanRenderImpl();
	}
	
	
	private final ByteBuffer winBufB=ByteBuffer.allocate(4*4+1);
	private final IntBuffer  winBuf =winBufB.asIntBuffer();
	private final File       winFile=new File("winData.bin");
	
	private GlfwWindow window=new GlfwWindow();
	
	private VkInstance    instance;
	private VkDebugReport debugReport;
	private VkGpu         gpu;
	private VkSwapchain   swapChain;
	private Shader        shader;
	private VkRenderPass  renderPass;
	
	private PointerBuffer layers;
	private List<String> deviceExtensions=new ArrayList<>(List.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME));
	
	private boolean swapchainRecreationPending=false;
	
	private FpsCounter fps=new FpsCounter(true);
	private long sec;
	
	private VkModel           model;
	private VkViewport.Buffer viewport;
	
	private VkSemaphore imageAvailableSemaphore;
	private VkSemaphore renderFinishedSemaphore;
	
	private DataManager  assets;
	private IDataManager textures;
	
	public VulkanRenderImpl(){

//		VkImageFormat.get(DEPTH, 24, false, NORM, STENCIL, 8, false, INT);
//		LogUtil.println(VkImageFormat.fromValue(VK_FORMAT_D24_UNORM_S8_UINT).val, VK_FORMAT_D24_UNORM_S8_UINT);
//		System.exit(0);
		
		assets=new DataManager();
//		assets.registerDomain(new File("res"));
		assets.registerDomain(new File("res.zip"));
		textures=assets.subData("textures");
		
		initWindow();
		Vk.stack(this::initVulkan);
		
		pickDevice();
		
		
		imageAvailableSemaphore=gpu.createSemaphore();
		renderFinishedSemaphore=gpu.createSemaphore();
		
		VkModelBuilder b=new VkModelBuilder(new VkModelFormat(Vec3f.class, ColorM.class, Vec2f.class));
		b.put(new Vec3f(-0.5F, -0.5F, +0.5F)).put(new ColorM(1, 0, 0, 1)).put(new Vec2f(1, 0)).done();
		b.put(new Vec3f(+0.5F, -0.5F, +0.5F)).put(new ColorM(0, 1, 0, 1)).put(new Vec2f(0, 0)).done();
		b.put(new Vec3f(+0.5F, +0.5F, +0.5F)).put(new ColorM(0, 0, 1, 1)).put(new Vec2f(0, 1)).done();
		b.put(new Vec3f(-0.5F, +0.5F, +0.5F)).put(new ColorM(1, 1, 1, 1)).put(new Vec2f(1, 1)).done();
		
		b.put(new Vec3f(-0.5F, -0.5F, -0.5F)).put(new ColorM(1, 0, 0, 1)).put(new Vec2f(1, 1)).done();
		b.put(new Vec3f(+0.5F, -0.5F, -0.5F)).put(new ColorM(0, 1, 0, 1)).put(new Vec2f(0, 1)).done();
		b.put(new Vec3f(+0.5F, +0.5F, -0.5F)).put(new ColorM(0, 0, 1, 1)).put(new Vec2f(0, 0)).done();
		b.put(new Vec3f(-0.5F, +0.5F, -0.5F)).put(new ColorM(1, 1, 1, 1)).put(new Vec2f(1, 0)).done();
		b.indices(0, 1, 2,
		          0, 2, 3,
		
		          6, 5, 4,
		          7, 6, 4,
		
		          4, 1, 0,
		          5, 1, 4,
		
		          2, 6, 7,
		          3, 2, 7);
		
		model=b.upload(gpu);
		
		loadTexture();
		
		createSwapChain();
		
		window.show();
		
		fps.activate();
		run();
		
		destroy();
	}
	
	
	private void loadTexture(){
		
		try(MemoryStack stack=stackPush()){
			
			BufferedImage img=ImageIO.read(textures.getInStream("SmugDude.png"));
			
			int imageSize=img.getWidth()*img.getHeight()*4;
			
			VkBufferMemory stagingMemory=gpu.createBufferMem(imageSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			
			try(VkBufferMemory.MemorySession buff=stagingMemory.requestMemory(gpu)){
				imgToBuff(buff.memory, img);
			}
			stagingMemory.flushMemory(gpu);
			
			
			model.texture=gpu.createImageTexture(img.getWidth(), img.getHeight(), VkImageFormat.get(RGBA, 8, false, NORM), VkImage.ON_GPU_TEXTURE, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			
			
			model.texture.image.transitionImageLayout(gpu, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			copyBufferToImage(stagingMemory.getBuffer(), model.texture.image);
			stagingMemory.destroy();
			model.texture.image.transitionImageLayout(gpu, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			
			model.texture.init(gpu, VkImageAspect.COLOR);
			
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void copyBufferToImage(VkBuffer buffer, VkImage image){
		try(MemoryStack stack=stackPush();SingleUseCommands commands=new SingleUseCommands(stack, gpu)){
			
			VkBufferImageCopy.Buffer region=VkBufferImageCopy.callocStack(1, stack);
			region.get(0)
			      .bufferOffset(0)
			      .bufferRowLength(0)
			      .bufferImageHeight(0);
			region.get(0).imageOffset().set(0, 0, 0);
			region.get(0).imageExtent().set(image.width, image.height, 1);
			region.get(0).imageSubresource()
			      .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
			      .mipLevel(0)
			      .baseArrayLayer(0)
			      .layerCount(1);
			
			vkCmdCopyBufferToImage(
				commands.commandBuffer,
				buffer.get(),
				image.get(),
				VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
				region);
			
		}
	}
	
	
	private static void imgToBuff(ByteBuffer buffer, BufferedImage image){

//		int[] pixels=new int[image.getWidth()*image.getHeight()];
//		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
		WritableRaster raster    =image.getRaster();
		ColorModel     colorModel=image.getColorModel();
		int[]          p         =new int[colorModel.getNumComponents()];
		for(int y=0;y<image.getHeight();y++){
			for(int x=0;x<image.getWidth();x++){
//				int pixel=pixels[y*image.getWidth()+x];
//				int pixel=image.getRGB(x,y);
//
				raster.getPixel(x, y, p);
//				buffer.put((byte)p[0]);
//				buffer.put((byte)p[1]);
//				buffer.put((byte)p[2]);
//				buffer.put((byte)p[3]);
				int r=p[0],
					g=p[1],
					b=p[2],
					a=p.length>3?p[3]:1;
				buffer.put((byte)r);
				buffer.put((byte)g);
				buffer.put((byte)b);
				buffer.put((byte)a);
			}
		}
		
	}
	
	private void drawFrame(MemoryStack stack){
		long tim=System.currentTimeMillis();
		if(tim-sec>1000){
			sec=tim;
			LogUtil.println(fps);
		}
		shader.uploadUniforms();
		
		int imageIndex=Vk.acquireNextImageKHR(gpu.getDevice(), swapChain, imageAvailableSemaphore, stack.mallocInt(1));
		if(imageIndex==-1){
			recreateSwapChain();
			imageIndex=Vk.acquireNextImageKHR(gpu.getDevice(), swapChain, imageAvailableSemaphore, stack.mallocInt(1));
		}
		
		VkSubmitInfo submitInfo=VkSubmitInfo.callocStack(stack);
		submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
		submitInfo.waitSemaphoreCount(1);
		submitInfo.pWaitSemaphores(imageAvailableSemaphore.buff());
		submitInfo.pSignalSemaphores(renderFinishedSemaphore.buff());
		submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
		submitInfo.pCommandBuffers(stack.pointers(swapChain.getBuffer(imageIndex).getCmd()));
		
		VkPresentInfoKHR presentInfo=VkPresentInfoKHR.callocStack(stack);
		presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
		presentInfo.pWaitSemaphores(submitInfo.pSignalSemaphores());
		presentInfo.swapchainCount(1);
		presentInfo.pSwapchains(stack.longs(swapChain.getId()));
		presentInfo.pImageIndices(stack.ints(imageIndex));
		
		vkQueueSubmit(gpu.getGraphicsQueue(), submitInfo, VK_NULL_HANDLE);
		
		Vk.queuePresentKHR(gpu.getSurfaceQueue(), presentInfo);
		
		Vk.queueWaitIdle(gpu.getSurfaceQueue());
		fps.newFrame();
		
	}
	
	
	private void destroySwapChain(){
		if(swapChain==null) return;
		
		swapChain.destroy();
		swapChain=null;
		renderPass.destroy();
		renderPass=null;
		shader.destroy();
		shader=null;
	}
	
	private void createSwapChain(){
		
		if(viewport!=null) viewport.free();
		
		IVec2iR siz=window.getSize();
		viewport=VkViewport.calloc(1);
		viewport.x(0);
		viewport.y(0);
		viewport.width(siz.x());
		viewport.height(siz.y());
		viewport.minDepth(0);
		viewport.maxDepth(1);
		
		swapChain=new VkSwapchain();
		swapChain.create(gpu);
		createRenderPass();
		
		FileChageDetector.autoHandle(new File("res/shaders/test.vert"), ()->VkShaderCompiler.compileVertex("test.vert"));
		FileChageDetector.autoHandle(new File("res/shaders/test.frag"), ()->VkShaderCompiler.compileFragment("test.frag"));
		shader=new Shader("test");
		shader.create(gpu, viewport, renderPass, model);
		
		Vk.stack(s->swapChain.initFrameBuffers(s, renderPass));
		Vk.stack(s->swapChain.initCommandBuffers(s, gpu.getGraphicsPool(), renderPass, shader, model, viewport));
	}
	
	private void createRenderPass(){
		try(MemoryStack stack=stackPush()){
			
			VkAttachmentDescription.Buffer attachments=VkAttachmentDescription.callocStack(2, stack);
			attachments.get(0)
			           .flags(0)
			           .format(swapChain.getFormat())
			           .samples(VK_SAMPLE_COUNT_1_BIT)
			           .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
			           .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
			           .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
			           .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
			           .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
			           .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
			attachments.get(1)
			           .flags(0)
			           .format(swapChain.getDepth().image.format.val)
			           .samples(VK_SAMPLE_COUNT_1_BIT)
			           .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
			           .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
			           .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
			           .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
			           .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
			           .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
			
			VkSubpassDescription.Buffer subpass=VkSubpassDescription.callocStack(1, stack);
			subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
			       .colorAttachmentCount(1)
			       .pColorAttachments(
				       VkAttachmentReference.mallocStack(1, stack)
				                            .attachment(0)
				                            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL));
			
			VkSubpassDependency.Buffer dependency=VkSubpassDependency.mallocStack(1, stack);
			dependency.srcSubpass(VK_SUBPASS_EXTERNAL)
			          .dstSubpass(0)
			          .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
			          .srcAccessMask(0)
			          .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
			          .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT|VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
			
			VkRenderPassCreateInfo renderPassInfo=VkRenderPassCreateInfo.callocStack(stack);
			renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
			              .pAttachments(attachments)
			              .pSubpasses(subpass)
			              .pDependencies(dependency);
			
			renderPass=Vk.createRenderPass(gpu, renderPassInfo, stack.mallocLong(1));
		}
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
		
		Glfw.init();
		
		window.setSize(600, 400).setPos(-1, -1);
		try{
			winBufB.put(Files.readAllBytes(winFile.toPath()));
			window.setSize(Math.max(winBuf.get(2), 100), Math.max(winBuf.get(3), 100)).setPos(winBuf.get(0), winBuf.get(1));
		}catch(IOException e){}
		
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		window.setResizeable(true).setTitle("Lapis Vulkan").init();
		window.onResize(s->swapchainRecreationPending=true);
		glfwSetErrorCallback((error, description)->{
			System.err.println("GLFW error ["+Integer.toHexString(error)+"]: "+GLFWErrorCallback.getDescription(description));
		});
	}
	
	private void initVulkan(MemoryStack stack){
		initLayers(stack);
		VkApplicationInfo    applicationInfo   =Vk.initAppInfo(stack, "Lapis test");
		VkInstanceCreateInfo instanceCreateInfo=Vk.initInstanceInfo(stack, applicationInfo);
		instanceCreateInfo.ppEnabledExtensionNames(getExtensions(stack));
		instanceCreateInfo.ppEnabledLayerNames(layers);
		
		instance=Vk.createInstance(instanceCreateInfo, stack.mallocPointer(1));
		window.createSurface(instance);
		
		if(Vk.DEVELOPMENT) debugReport=new VkDebugReport(instance, stack.mallocLong(1), stack, (type, prefix, code, message)->{
//			if(type==VkDebugReport.Type.DEVELOPMENT) return;
//			if(type==VkDebugReport.Type.INFORMATION) return;
			if(message.startsWith("Device Extension")) return;
			
			List<String>  msgLin=TextUtil.wrapLongString(message, 100);
			StringBuilder msg   =new StringBuilder().append(type).append(": [").append(prefix).append("] Code ").append(code).append(": ");
			
			if(msgLin.size()>1) msg.append("\n").append(TextUtil.wrappedString(UtilL.array(msgLin)));
			else msg.append(msgLin.get(0));
			
			if(type==VkDebugReport.Type.ERROR||type==VkDebugReport.Type.WARNING) throw new RuntimeException(msg.toString());
			else LogUtil.println(msg);
		});
	}
	
	private void pickDevice(){
		try(MemoryStack stack=stackPush()){
			int deviceCount=Vk.enumeratePhysicalDevices(instance, stack.mallocInt(1));
			if(deviceCount==0){
				LogUtil.printlnEr("No devices support Vulkan API!\n"+
				                  "Make sure your drivers are up to date. (Old hardware may not be supported regardless due to vulkan being a \"modern\" api and having a dedicated GPU helps a lot)\n"+
				                  "Stopping application...");
				System.exit(1);
			}
			
			VkGpu bestGpu      =null;
			int   bestUsability=-1;
			
			for(VkPhysicalDevice device : Vk.getPhysicalDevices(stack, instance)){
				
				VkGpu gpu      =new VkGpu(window, device, deviceExtensions, layers);
				int   usability=VkGpuRater.rateGpuInit(gpu, stack);
				
				if(usability<0){
					gpu.destroy();
					continue;
				}
				
				if(bestUsability<usability){
					bestUsability=usability;
					
					if(bestGpu!=null) bestGpu.destroy();
					bestGpu=gpu;
				}
			}
			
			if((gpu=bestGpu)==null){
				throw UtilL.exitWithErrorMsg("Sorry! Vulkan supporting "+TextUtil.plural("physicalDevice", deviceCount), "found but minimal requirements are not met! :(\n",
				                             "Stopping application...");
			}
		}
		
		LogUtil.println("Using", VkPhysicalDeviceType.find(gpu.getProperties()), "called", gpu.getProperties().deviceNameString());
		
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
		if(!Vk.DEVELOPMENT) return;
		
		PointerBuffer layers=null;
		lay:
		{
			VkLayerProperties.Buffer availableLayers=Vk.enumerateInstanceLayerProperties(stack, stack.mallocInt(1));
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
		window.hide();
		
		imageAvailableSemaphore.destroy();
		renderFinishedSemaphore.destroy();
		
		model.destroy();
		
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
