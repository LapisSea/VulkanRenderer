package com.lapissea.vulkanimpl.util;

import java.nio.LongBuffer;

public interface VkBufferedHandle{
	
	default long getHandle(){
		return getBuff().get(0);
	}
	
	LongBuffer getBuff();
	
}
