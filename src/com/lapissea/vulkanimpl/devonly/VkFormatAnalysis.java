package com.lapissea.vulkanimpl.devonly;

import com.lapissea.util.LogUtil;
import com.lapissea.vec.Vec2i;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.format.VkFormatInfo;
import com.lapissea.vulkanimpl.util.format.VkFormatInfo.Component;
import com.lapissea.vulkanimpl.util.format.VkFormatInfo.ComponentType;
import com.lapissea.vulkanimpl.util.format.VkFormatInfo.StorageType;
import org.lwjgl.vulkan.VK10;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VkFormatAnalysis{
	
	static{ DevelopmentInfo.checkOnLoad(); }
	
	private static class ComponentBuild{
		private ComponentType type;
		private int           bitSize;
		private StorageType   storageType;
		private boolean       signed;
		private int           packId;
		
		private ComponentBuild(ComponentType type, int bitSize){
			this.type=type;
			this.bitSize=bitSize;
			packId=-1;
		}
		
		Component finish(){
			return new Component(type, bitSize, storageType, signed, packId);
		}
	}
	
	
	public static void generateCode(){
		List<String> lines=new ArrayList<>();
		compile().parallelStream().forEach(v->{
			BiFunction<Integer, String, String> wrapp=(i, s)->{
				if(i==0) return "Collections.emptyList()";
				else if(i==1) return "Collections.singletonList("+s+")";
				else return "Arrays.asList("+s+")";
			};
			String line="INFO.put("+v.handle+", new "+VkFormatInfo.class.getSimpleName()+"("+v.handle+", \""+v.name+"\", "+
			            wrapp.apply(v.components.size(), v.components.stream().map(c->"new Component(ComponentType."+c.type+", "+c.bitSize+", StorageType."+c.storageType+", "+c.signed+(c.packId==-1?"":", "+c.packId)+")").collect(Collectors.joining(", ")))+", "+
			            wrapp.apply(v.packSizes.size(), v.packSizes.stream().map(Object::toString).collect(Collectors.joining(", ")))+", "+
			            (v.compression==null?"null":"\""+v.compression+"\"")+", "+
			            v.isBlock+", "+
			            (v.isBlock?"new Vec2iFinal("+v.blockSize.x()+", "+v.blockSize.y()+")":"null")+"));";
			synchronized(lines){
				lines.add(line.replace("Collections.emptyList(), Collections.emptyList()", "EC, EP"));
			}
		});
		
		LogUtil.__.destroy();
		LogUtil.println("List<Component> EC=Collections.emptyList();\n"+
		                "List<Integer>   EP=Collections.emptyList();");
		lines.stream().sorted(Comparator.comparingInt(String::length)).forEach(LogUtil::println);
		
		System.exit(0);
	}
	
	public static List<VkFormatInfo> compile(){
		Pattern compElementPattern, blockSizePattern;
		{
			StringBuilder sb=new StringBuilder("[");
			for(ComponentType componentType : ComponentType.values()){
				sb.append(componentType.mark);
			}
			compElementPattern=Pattern.compile(sb+"][0-9]+");
			blockSizePattern=Pattern.compile("[0-9]+x[0-9]+");
		}
		
		
		List<VkFormatInfo> allInfo=new ArrayList<>();
		for(Field field : VK10.class.getDeclaredFields()){
			String name=field.getName();
			if(!name.startsWith("VK_FORMAT_")||name.startsWith("VK_FORMAT_FEATURE_")) continue;
			String[] words=name.substring("VK_FORMAT_".length()).split("_");
			
			ArrayList<Component> components      =new ArrayList<>();
			ArrayList<Integer>   packSizes       =new ArrayList<>();
			List<ComponentBuild> compBuild       =new ArrayList<>(4);
			boolean              lastWordCompList=false;
			
			Runnable flush=()->{
				compBuild.stream().map(ComponentBuild::finish).forEach(components::add);
				compBuild.clear();
			};
			VkFormatInfo info       =null;
			String       compression=null;
			boolean      isBlock    =false;
			Vec2i        blockSize  =new Vec2i(4, 4);
			
			Predicate<String> detectStorageType=typeName->{
				try{
					StorageType type=Arrays.stream(StorageType.values()).filter(t->t.name().equals(typeName)).findAny().orElseThrow(()->new RuntimeException("Unknown StorageType: "+typeName));
					
					for(ComponentBuild component : compBuild){
						component.storageType=type;
					}
					return true;
				}catch(Exception e){
					return false;
				}
			};
			
			for(int wordId=0;wordId<words.length;wordId++){
				String word=words[wordId];
				
				if(word.equals("UNDEFINED")) break;
				
				//is word component list?
				if(compElementPattern.matcher(word).replaceAll("").isEmpty()){
					if(!lastWordCompList) flush.run();
					
					
					Matcher parts=compElementPattern.matcher(word);
					while(parts.find()){
						String part=parts.group();
						
						ComponentType type   =Arrays.stream(ComponentType.values()).filter(t->t.mark==part.charAt(0)).findAny().orElse(null);
						int           bitSize=Integer.parseInt(part.substring(1));
						compBuild.add(new ComponentBuild(type, bitSize));
					}
					
					lastWordCompList=true;
					continue;
				}
				lastWordCompList=false;
				
				//is StorageType ?
				boolean signed=word.charAt(0)=='S';
				if(signed||word.charAt(0)=='U'){
					String typeName=word.substring(1);
					if(detectStorageType.test(typeName)){
						for(ComponentBuild component : compBuild){
							component.signed=signed;
						}
						continue;
					}
				}
				
				//is pack?
				if(word.matches("PACK[0-9]+")){
					for(ComponentBuild component : compBuild){
						component.packId=packSizes.size();
					}
					packSizes.add(Integer.parseInt(word.substring(4)));
					continue;
				}
				
				// is block texture
				if(word.startsWith("BC")||
				   word.startsWith("ETC")||
				   word.startsWith("EAC")||
				   word.startsWith("ASTC")){
					try{
						compression=word;
						if(wordId+1==words.length) continue;
						String nextWord=words[wordId+1];
						
						if(blockSizePattern.matcher(nextWord).find()){
							String[] xy=nextWord.split("x");
							blockSize.set(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
							wordId++;
							
						}else if(detectStorageType.test(nextWord)) wordId++;
						
						continue;
					}catch(Exception e){}
				}
				if(word.equals("BLOCK")){
					isBlock=true;
					continue;
				}
				
				LogUtil.println("Unknown word: "+word+" in "+name);
				System.exit(0);
			}
			flush.run();
			components.trimToSize();
			packSizes.trimToSize();
			try{
				allInfo.add(new VkFormatInfo(field.getInt(null), name, components, packSizes, compression, isBlock, blockSize.immutable()));
			}catch(IllegalAccessException e){}
		}
		return allInfo;
	}
}
