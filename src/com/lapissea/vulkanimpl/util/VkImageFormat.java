package com.lapissea.vulkanimpl.util;

import com.lapissea.util.LogUtil;
import org.lwjgl.vulkan.VK10;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public class VkImageFormat{
	
	
	public enum Format{
		RGBA(b->"R"+b+"G"+b+"B"+b+"A"+b, VkImageAspect.COLOR),
		RG(b->"R"+b+"G"+b, VkImageAspect.COLOR),
		DEPTH(b->"D"+b, VkImageAspect.DEPTH),
		STENCIL(b->"S"+b, VkImageAspect.STENCIL);
		
		private final IntFunction<String> bits;
		public final  VkImageAspect       aspect;
		
		Format(IntFunction<String> bits, VkImageAspect aspect){
			this.bits=bits;
			this.aspect=aspect;
		}
		
	}
	
	public enum Type{
		NORM, INT, FLOAT, RGB, SCALED
	}
	
	private static final Map<String, VkImageFormat> CACHE=new HashMap<>();
	
	public static VkImageFormat get(Format format, int bitSize, boolean signed, Type type){
		return find("VK_FORMAT_"+join(format, signed, bitSize, type), format.aspect);
	}
	
	public static VkImageFormat get(Format format1, int bitSize1, boolean signed1, Type type1, Format format2, int bitSize2, boolean signed2, Type type2){
		return find("VK_FORMAT_"+join(format1, signed1, bitSize1, type1)+"_"+join(format2, signed2, bitSize2, type2), format1.aspect);
	}
	
	private static VkImageFormat find(String name, VkImageAspect aspect){
		VkImageFormat format=CACHE.get(name);
		if(format!=null) return format;
		
		try{
			format=new VkImageFormat(name, VK10.class.getDeclaredField(name).getInt(null), Arrays.stream(name.split("[^0-9]+")).filter(s->!s.isEmpty()).mapToInt(Integer::parseInt).sum(), aspect);
		}catch(Exception e){
			throw new RuntimeException("Format "+name+" does not exist", e);
		}
		LogUtil.println(name, format.bits, format.bytes);
		System.exit(0);
		return format;
	}
	
	
	private static String join(Format format, boolean signed, int bitSize, Type type){
		return format.bits.apply(bitSize)+"_"+(signed?"S":"U")+type;
	}
	
	public final  String        name;
	private final int           bits;
	public final  int           val;
	public final  VkImageAspect aspect;
	private final int           bytes;
	
	public VkImageFormat(String name, int val, int bits, VkImageAspect aspect){
		this.name=name;
		this.val=val;
		this.bits=bits;
		this.aspect=aspect;
		bytes=(int)Math.ceil(bits/8D);
	}
	
	public int bytes(){
		return bytes;
	}
	
	public int bits(){
		return bits;
	}
	
}
