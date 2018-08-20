package com.lapissea.vulkanimpl.assets;

import com.lapissea.datamanager.DataSignature;
import com.lapissea.glfw.BuffUtil;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import com.lapissea.vulkanimpl.util.types.VkBuffer;
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory;
import com.lapissea.vulkanimpl.util.types.VkImage;
import com.lapissea.vulkanimpl.util.types.VkTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class TextureNode extends ResourcePool.Node<VkTexture>{
	
	public static VkTexture bufferedImageToGpu(BufferedImage image, VkGpu gpu){
		int width =image.getWidth();
		int height=image.getHeight();
		
		ByteBuffer buff=BuffUtil.imageToBuffer(image, memAlloc(width*height*4));
		
		VkBuffer       stagingBuffer=gpu.createBuffer(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, buff.capacity());
		VkDeviceMemory stagingMemory=stagingBuffer.createMemory(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
		
		try(VkDeviceMemory.MemorySession ses=stagingMemory.memorySession(stagingBuffer.getSize())){
			ses.memory.put(buff);
			memFree(buff);
		}
		
		VkImage textureImage=gpu.create2DImage(width, height, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_TRANSFER_DST_BIT|VK_IMAGE_USAGE_SAMPLED_BIT, VK_SAMPLE_COUNT_1_BIT);
		
		VkDeviceMemory textureImageMemory=textureImage.createMemory(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		
		synchronized(gpu.getInstance().recreateLock){
			
			gpu.getTransferQueue().waitIdle();
			textureImage.transitionLayout(gpu.getTransferQueue(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VkImage.TOP_TO_TRANSFER_WRITE);
			textureImage.copyFromBuffer(gpu.getTransferQueue(), stagingBuffer);
			
			gpu.getGraphicsQueue().waitIdle();
			textureImage.transitionLayout(gpu.getGraphicsQueue(), VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VkImage.TRANSFER_WRITE_TO_FRAGMENT_READ);
		}
		
		stagingMemory.destroy();
		stagingBuffer.destroy();
		
		return new VkTexture(textureImage, textureImageMemory).createView(VkImageAspect.COLOR).createSampler();
	}
	
	private final VkGpu gpu;
	
	private VkTexture texture;
	
	public TextureNode(DataSignature signature, VkGpu gpu){
		super(signature);
		this.gpu=gpu;
	}
	
	@Override
	protected VkTexture get(){
		if(texture==null) load();
		return texture;
	}
	
	private void load(){
		if(texture!=null) return;
		
		BufferedImage image;
		try{
			image=ImageIO.read(getInStream());
		}catch(Exception e){
			return;
		}
		
		
		texture=bufferedImageToGpu(image, gpu);
	}
	
	@Override
	protected boolean hasData(){
		return path.endsWith(".jpg")||path.endsWith(".png");
	}
	
	@Override
	public void destroy(){
		if(texture!=null) texture.destroy();
	}
}
