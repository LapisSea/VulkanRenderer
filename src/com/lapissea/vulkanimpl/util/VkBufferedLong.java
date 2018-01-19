package com.lapissea.vulkanimpl.util;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class VkBufferedLong implements VkDestroyable{
	
	public final LongBuffer buff;
	
	public VkBufferedLong(LongBuffer buff){
		this.buff=buff;
	}
	
	@Override
	public void destroy(){
		memFree(buff);
	}
}
