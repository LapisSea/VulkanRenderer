package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.util.DevelopmentMemorySafety;
import com.lapissea.vulkanimpl.util.VkDestroyable;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class VkDescriptorSet extends ExtendableLong implements VkDestroyable{
	
	private LongBuffer pointer;
	
	public VkDescriptorSet(LongBuffer val){
		super(val.get(0));
		pointer=val;
	}
	
	public LongBuffer pointer(){
		return pointer;
	}
	
	@Override
	public void destroy(){
		memFree(pointer);
		pointer=null;
	}
	
	private final StackTraceElement[] _ctorStack=Vk.DEVELOPMENT?DevelopmentMemorySafety.make():null;
	
	@Override
	protected void finalize() throws Throwable{
		if(Vk.DEVELOPMENT) DevelopmentMemorySafety.check(this, pointer);
	}
}
