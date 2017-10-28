package com.lapissea.vulkannutcrack.model;

import com.lapissea.util.UtilL;
import org.lwjgl.BufferUtils;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import static com.lapissea.util.ObjectSize.*;

public abstract class VkModelFormatAttribute<T>{
	
	public static void put(ByteBuffer buffer, boolean aBoolean){
		byte b;
		if(aBoolean) b=1;
		else b=0;
		buffer.put(buffer.position(), b);
		buffer.position(buffer.position()+1);
	}
	
	
	public static void put(ByteBuffer buffer, byte aByte){
		buffer.put(buffer.position(), aByte);
		buffer.position(buffer.position()+1);
	}
	
	
	private static final ByteBuffer  bbFloat=BufferUtils.createByteBuffer(Float.SIZE/Byte.SIZE).order(ByteOrder.nativeOrder());
	private static final FloatBuffer fbFloat=bbFloat.asFloatBuffer();
	
	public static void put(ByteBuffer buffer, float aFloat){
		synchronized(fbFloat){
			fbFloat.put(0, aFloat);
			for(int i=0;i<bbFloat.capacity();i++){
				buffer.put(buffer.position()+i, bbFloat.get(i));
			}
			buffer.position(buffer.position()+bbFloat.capacity());
		}
	}
	
	
	private static final ByteBuffer  bbShort=BufferUtils.createByteBuffer(Short.SIZE/Byte.SIZE).order(ByteOrder.nativeOrder());
	private static final ShortBuffer sbShort=bbShort.asShortBuffer();
	
	public static void put(ByteBuffer buffer, short aShort){
		synchronized(sbShort){
			sbShort.put(0, aShort);
			for(int i=0;i<bbShort.capacity();i++) buffer.put(buffer.position()+i, bbShort.get(i));
			buffer.position(buffer.position()+bbShort.capacity());
		}
	}
	
	
	private static final ByteBuffer bbInt=BufferUtils.createByteBuffer(Integer.SIZE/Byte.SIZE).order(ByteOrder.nativeOrder());
	private static final IntBuffer  ibInt=bbInt.asIntBuffer();
	
	public static void put(ByteBuffer buffer, int integer){
		synchronized(ibInt){
			ibInt.put(0, integer);
			for(int i=0;i<bbInt.capacity();i++) buffer.put(buffer.position()+i, bbInt.get(i));
			buffer.position(buffer.position()+bbInt.capacity());
		}
	}
	
	
	private static final ByteBuffer bbLong=BufferUtils.createByteBuffer(Long.SIZE/Byte.SIZE).order(ByteOrder.nativeOrder());
	private static final LongBuffer lbLong=bbLong.asLongBuffer();
	
	public static void put(ByteBuffer buffer, long aLong){
		synchronized(lbLong){
			lbLong.put(0, aLong);
			for(int i=0;i<bbLong.capacity();i++) buffer.put(buffer.position()+i, bbLong.get(i));
			buffer.position(buffer.position()+bbLong.capacity());
		}
	}
	
	
	private static final ByteBuffer   bbDouble=BufferUtils.createByteBuffer(Double.SIZE/Byte.SIZE).order(ByteOrder.nativeOrder());
	private static final DoubleBuffer dbDouble=bbDouble.asDoubleBuffer();
	
	public static void put(ByteBuffer buffer, double aDouble){
		synchronized(dbDouble){
			dbDouble.put(0, aDouble);
			for(int i=0;i<bbDouble.capacity();i++) buffer.put(buffer.position()+i, bbDouble.get(i));
			buffer.position(buffer.position()+bbDouble.capacity());
		}
	}
	
	/////////////////////////////////////////////////////////////////
	
	public static abstract class Simple<T> extends VkModelFormatAttribute<T>{
		
		private final int size, format;
		
		protected Simple(Class<T> type, int format){
			this(type, sizeof(type), format);
		}
		
		protected Simple(Class<T> type, int size, int format){
			super(type);
			this.size=size;
			this.format=format;
		}
		
		@Override
		public int getSizeBits(){
			return size;
		}
		
		@Override
		public int getFormat(){
			return format;
		}
	}
	
	private static final List<VkModelFormatAttribute> COMMON_TYPE=new ArrayList<>(1);
	
	public static void registerCommonAttrubute(VkModelFormatAttribute attribute){
		COMMON_TYPE.add(attribute);
	}
	
	
	public static VkModelFormatAttribute find(Class aClass, int pos){
		return COMMON_TYPE.stream().filter(attr->attr.checkClass(aClass)).findAny()
		                  .orElseThrow(()->new IllegalArgumentException("Unrecognised class "+aClass.getName()+" for argument "+pos));
	}
	
	static{
		new VkModelFormatAttributes();
	}
	
	///////////////////////////////////////////////////////////////////////
	
	
	protected final Class<T> type;
	
	protected VkModelFormatAttribute(Class<T> type){this.type=type;}
	
	public abstract int getSizeBits();
	
	public abstract int getFormat();
	
	public boolean checkClass(Class aClass){
		return UtilL.instanceOf(aClass, type);
	}
	
	public abstract void put(ByteBuffer buffer, T t);
	
	@Override
	public String toString(){
		return "ModelFormat{class="+type.getName()+"}";
	}
}
