package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.model.VkBufferMemory;
import com.lapissea.vulkanimpl.simplevktypes.ExtendableLong;
import com.lapissea.vulkanimpl.simplevktypes.VkBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.function.Consumer;

public class BufferUtil{
	
	
	public static <T extends Struct, SELF extends StructBuffer<T, SELF>> Consumer<T> builder(SELF buffer){
		return t->{
			buffer.put(buffer.position(), t);
			buffer.position(buffer.position());
		};
	}
	
	public static LongBuffer buffSingle(MemoryStack stack, ExtendableLong value){
		return buffSingle(stack, value.get());
	}
	
	public static LongBuffer buffSingle(MemoryStack stack, long value){
		return stack.mallocLong(1).put(0, value);
	}
	
	public static IntBuffer buffSingle(MemoryStack stack, int value){
		return stack.mallocInt(1).put(0, value);
	}
	
	public static PointerBuffer buffSingle(MemoryStack stack, Pointer value){
		return stack.mallocPointer(1).put(0, value);
	}
	
	public static IntBuffer of(MemoryStack stack, int... data){
		return of(stack.callocInt(data.length), data);
	}
	
	public static IntBuffer of(IntBuffer buffer, int... data){
		for(int i=0;i<data.length;i++){
			buffer.put(buffer.position()+i, data[i]);
		}
		buffer.position(buffer.position()+data.length);
		
		return buffer;
	}
	
	public static ByteBuffer of(MemoryStack stack, byte... data){
		return of(stack.malloc(data.length), data);
	}
	
	public static ByteBuffer of(ByteBuffer buffer, byte... data){
		for(int i=0;i<data.length;i++){
			buffer.put(buffer.position()+i, data[i]);
		}
		buffer.position(buffer.position()+data.length);
		
		return buffer;
	}
	
}
