package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.vulkanimpl.util.VkDestroyable;

import static org.lwjgl.vulkan.VK10.*;

public class VkModel implements VkDestroyable{
	
	public enum IndexType{
		SHORT(VK_INDEX_TYPE_UINT16, 2),
		INT(VK_INDEX_TYPE_UINT32, 4);
		
		public final int val;
		public final int bytes;
		
		IndexType(int val, int bytes){
			this.val=val;
			this.bytes=bytes;
			//node based scripting material
			//nbsm
		}
	}
	
	
	@Override
	public void destroy(){
	
	}
	
}
