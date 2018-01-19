package com.lapissea.vulkanimpl.devonly;

import com.lapissea.util.LogUtil;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UnsafeConsumer;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VulkanRenderer;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Generator_Vk{
	
	static{ if(!VulkanRenderer.DEVELOPMENT) throw new RuntimeException(); }
	
	interface Gen{
		void print(UnsafeConsumer<String, IOException> out) throws IOException;
	}
	
	public static void run(){
		
		File   srcFile =new File("src/"+Vk.class.getName().replace('.', '/')+".java");
		File   destFile=new File(srcFile.getPath()+"0");
		String newLine =System.getProperty("line.separator");
		
		try{
			destFile.createNewFile();
			try(Reader vkSrc=new BufferedReader(new FileReader(srcFile));Writer newVkSrc=new BufferedWriter(new FileWriter(destFile))){
				UnsafeConsumer<String, IOException> out=line->{
					newVkSrc.write(line);
					newVkSrc.write(newLine);
				};
				
				UnsafeConsumer<String, IOException> tabbedOut=line->newVkSrc.write((int)'\t');
				tabbedOut=tabbedOut.andThen(out);
				
				boolean inData     =false;
				boolean inDataVars =true;
				boolean inGenerated=false;
				
				Map<String, String> vars=new HashMap<>();
				List<Gen>           data=new ArrayList<>();
				
				StringBuilder lineB  =new StringBuilder();
				int           lineNum=0;
				int           i;
				while((i=vkSrc.read())!=-1){
					switch(i){
					case '\r':
						continue;
					case '\n':{
						String line=lineB.toString();
						lineB.setLength(0);
						
						if(inData&&line.equals("\\*GEN*/")) inData=false;
						
						
						if(inData){
							if(line.equals("START")) inDataVars=false;
							else if(inDataVars){
								
								String[] split=line.split("=");
								if(split.length!=2) UtilL.exitWithErrorMsg("Bad var("+lineNum+"): "+line);
								vars.put(split[0].trim(), split[1].trim());
								
							}else{
								data.add(parse(line, vars));
							}
							
						}
						if(!inData&&line.equals("/*GEN*\\")) inData=true;
						
						if(inGenerated&&line.endsWith("/*/END_GEN/*/")) inGenerated=false;
						if(!inGenerated) out.accept(line);
						
						if(!inGenerated&&line.endsWith("/*/START_GEN/*/")){
							for(Gen e : data){
								e.print(tabbedOut);
							}
							inGenerated=true;
						}
						lineNum++;
						continue;
					}
					default:{
						lineB.append((char)i);
						continue;
					}
					}
				}
			}
//			try(FileChannel src=new FileInputStream(destFile).getChannel();FileChannel dest=new FileOutputStream(srcFile).getChannel()){
//				dest.transferFrom(src, 0, src.size());
//			}
			System.exit(0);
			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	private static Gen parse(String line, Map<String, String> vars){
		String[] parts=line.split(" +");
		for(int i=0;i<parts.length;i++){
			parts[i]=parts[i].trim();
		}
		Class<?> target;
		try{
			target=Class.forName(vars.getOrDefault(parts[0], parts[0]));
		}catch(ClassNotFoundException e){
			throw UtilL.exitWithErrorMsg("Class \""+vars.getOrDefault(parts[0], parts[0])+"\" not found at: "+line);
		}
		String methodName=parts[1];
		
		Method method=Arrays.stream(target.getMethods())
		                    .filter(f->f.getName().equals(methodName))//get matching methods
		                    .sorted(Comparator.comparingInt(b->{//prefer buffer vs array
			                    int count=0;
			                    for(Class<?> aClass : b.getParameterTypes()){
				                    if(aClass.isArray()) count++;
			                    }
			                    return count;
		                    }))
		                    .findFirst()
		                    .orElseThrow(()->UtilL.exitWithErrorMsg("Method \""+methodName+"\" not found at: "+line));//rip
		
		String returnType=method.getReturnType().getSimpleName();
		String vkName    =TextUtil.firstToLoverCase(method.getName().substring(2));
		
		Map<String, String> args=new HashMap<>();
		
		LogUtil.println(UtilL.class.getMethods()[0].getParameters()[0].getName());
		
		for(Parameter parameter : method.getParameters()){
			Class<?> type=parameter.getType();
			String   parm;
			
			LogUtil.println(parameter.getName());
			if(parameter.isNamePresent()) parm=parameter.getName();
			else if(type.isArray()) parm=TextUtil.plural(type.getComponentType().getSimpleName());
			else if(type.isPrimitive()) parm=type.getSimpleName()+"0";
			else parm=TextUtil.firstToLoverCase(type.getSimpleName());
			
			args.put(type.getSimpleName(), parm);
		}
		
		switch(parts[2]){//use type
		case "basic":{
			Collector<CharSequence, ?, String> comma=Collectors.joining(", ");
			String
				l1=args.entrySet().stream().map(e->e.getKey()+" "+e.getValue()).collect(comma),
				l2=args.entrySet().stream().map(Map.Entry::getValue).collect(comma);
			
			if(returnType.equals("int")){//code
				return basicCodeCheck(methodName, vkName, l1, l2);
			}else if(returnType.equals("void")){
				return pass(methodName, vkName, l1, l2);
			}
		}
		}
		
		throw UtilL.exitWithErrorMsg("Class \""+vars.getOrDefault(parts[0], parts[0])+"\" not found at: "+line);
	}
	
	private static Gen basicCodeCheck(String methodName, String vkName, String args, String argList){
		return out->{
			out.accept("public static <<TODO>> "+vkName+"("+args+"){");
			out.accept("\tint code="+methodName+"("+argList+")");
			out.accept("\tif(DEVELOPMENT) check(code);");
			out.accept("\treturn dest.get(0)");
			out.accept("}");
			out.accept("");
		};
	}
	
	private static Gen pass(String methodName, String vkName, String args, String argList){
		return out->{
			out.accept("public static void "+vkName+"("+args+"){");
			out.accept("\t"+methodName+"("+argList+")");
			out.accept("}");
			out.accept("");
		};
	}
	
}
