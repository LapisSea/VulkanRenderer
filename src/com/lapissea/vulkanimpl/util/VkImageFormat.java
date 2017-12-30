package com.lapissea.vulkanimpl.util;

import com.lapissea.vulkanimpl.Vk;
import org.lwjgl.vulkan.VK10;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

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
		return find("VK_FORMAT_"+join(format, signed, bitSize, type));
	}
	
	public static VkImageFormat get(Format format1, int bitSize1, boolean signed1, Type type1, Format format2, int bitSize2, boolean signed2, Type type2){
		return find("VK_FORMAT_"+join(format1, signed1, bitSize1, type1)+"_"+join(format2, signed2, bitSize2, type2));
	}
	
	private static VkImageFormat find(String name){
		VkImageFormat format=CACHE.get(name);
		if(format!=null) return format;
		return formatFromName(name);
	}
	
	private static VkImageFormat formatFromName(String name){
		try{
			List<VkImageAspect> aspects=Arrays.stream(VkImageAspect.values())
			                                  .filter(a->a.detectionPattern.matcher(name).find())
			                                  .collect(Collectors.toList());
			if(Vk.DEVELOPMENT&&aspects.isEmpty()) throw new RuntimeException("Unable to detect aspect at "+name+"!");
			
			VkImageFormat format=new VkImageFormat(name,
			                                       VK10.class.getDeclaredField(name).getInt(null),
			                                       Arrays.stream(name.split("[^0-9]+")).filter(s->!s.isEmpty())
			                                             .mapToInt(Integer::parseInt)
			                                             .sum(),
			                                       aspects);
			CACHE.put(name, format);
			return format;
		}catch(Exception e){
			throw new RuntimeException("Format "+name+" does not exist", e);
		}
	}
	
	
	private static String join(Format format, boolean signed, int bitSize, Type type){
		return format.bits.apply(bitSize)+"_"+(signed?"S":"U")+type;
	}
	
	public static VkImageFormat fromValue(int value){
		VkImageFormat format=CACHE.entrySet().stream().filter(e->e.getValue().val==value).findAny().map(Map.Entry::getValue).orElse(null);
		if(format!=null) return format;
		
		return Arrays.stream(VK10.class.getDeclaredFields())
		             .filter(var->var.getName().startsWith("VK_FORMAT_")&&!var.getName().startsWith("VK_FORMAT_FEATURE_"))
		             .filter(var->{
			             try{
				             return var.getInt(null)==value;
			             }catch(IllegalAccessException e){
				             return false;
			             }
		             }).findAny().map(var->formatFromName(var.getName())).orElse(null);
	}
	
	public final  String              name;
	private final int                 bits;
	private final int                 bytes;
	public final  int                 val;
	private final List<VkImageAspect> aspects;
	public final  VkImageAspect       aspect;
	
	public VkImageFormat(String name, int val, int bits, List<VkImageAspect> aspects){
		this.name=name;
		this.val=val;
		this.bits=bits;
		this.aspects=aspects;
		aspect=aspects.get(0);
		bytes=(int)Math.ceil(bits/8D);
	}
	
	public int bytes(){
		return bytes;
	}
	
	public int bits(){
		return bits;
	}
	
	public boolean hasAspect(VkImageAspect aspect){
		return aspects.contains(aspect);
	}
	
}
