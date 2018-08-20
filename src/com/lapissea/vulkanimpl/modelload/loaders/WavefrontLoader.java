package com.lapissea.vulkanimpl.modelload.loaders;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.vulkanimpl.modelload.IModelLoader;
import com.lapissea.vulkanimpl.modelload.IVertexConsumer;
import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

public class WavefrontLoader implements IModelLoader{
	
	@Override
	public void load(IDataManager source, String target, IVertexConsumer consumer, Consumer<int[]> indexFaceConsumer){
		if(!target.endsWith(".obj")) target+=".obj";
		
		TFloatArrayList v =new TFloatArrayList();
		TFloatArrayList vn=new TFloatArrayList();
		TFloatArrayList vt=new TFloatArrayList();
		
		TFloatArrayList positions=new TFloatArrayList();
		TFloatArrayList uvs      =new TFloatArrayList();
		TFloatArrayList normal   =new TFloatArrayList();
		
		TIntArrayList ids    =new TIntArrayList();
		TIntArrayList dumpIds=new TIntArrayList();
		
		try(BufferedReader reader=source.getReader(target)){
			if(reader==null) throw new IllegalArgumentException("Failed to load model: "+target);
			
			int read;
			while((read=reader.read())!=-1){
				char ch=(char)read;
				if(ch=='\n') continue;
				if(ch=='\r') continue;
				
				if(ch=='#'){
					if(reader.readLine()==null) break;
					continue;
				}
				
				if(ch=='m'){//mtlib?
					
					if((read=reader.read())==-1) break;
					ch=(char)read;
					
					if(ch=='t'){//yup
						reader.skip(5);
						
						String mtlibName=reader.readLine();
						
						String modelFolder=new File(target).getParent();
						
						if(modelFolder==null) loadMtlib(source, mtlibName);
						else loadMtlib(source, modelFolder+"/"+mtlibName);
						
						continue;
						
					}
					
					throw new RuntimeException(ch+"["+read+"]");
				}
				if(ch=='u'){//usemtl?
					
					if((read=reader.read())==-1) break;
					ch=(char)read;
					
					if(ch=='s'){//yup
						reader.skip(5);
						
						String materialName=reader.readLine();
						
						//TODO
						continue;
						
					}
					
					throw new RuntimeException(ch+"["+read+"]");
				}
				if(ch=='s'){// shading
					reader.skip(1);
					
					reader.readLine();
					continue;//TODO
				}
				
				if(ch=='o'){// object
					reader.readLine();
					continue;//TODO
				}
				
				if(ch=='v'){ // vertex
					
					if((read=reader.read())==-1) break;
					ch=(char)read;
					
					if(ch==' '){// position
						
						readFloat(reader, v);
						readFloat(reader, v);
						char end=readFloat(reader, v);
						if(end!='\n') reader.readLine();
						continue;
					}
					if(ch=='t'){// uvs
						reader.skip(1);
						vt.ensureCapacity(v.size()/3*2);
						
						readFloat(reader, vt);
						char end=readFloat(reader, vt);
						if(end!='\n') reader.readLine();
						continue;
					}
					if(ch=='n'){// normals
						vn.ensureCapacity(v.size());
						reader.skip(1);
						
						readFloat(reader, vn);
						readFloat(reader, vn);
						char end=readFloat(reader, vn);
						if(end!='\n') reader.readLine();
						continue;
					}
					throw new RuntimeException(ch+"["+(int)ch+"]");
				}
				
				
				if(ch=='f'){
					reader.skip(1);
					
					int     faceSize =0;
					char    iEnd     =0;
					boolean hasUv    =false;
					boolean hasNormal=false;
					
					while(iEnd!='\r'&&iEnd!='\n'){
						
						faceSize++;
						
						iEnd=readInt(reader, ids);
						{
							int pos=ids.get(ids.size()-1)*3;
							positions.add(v.get(pos));
							positions.add(v.get(pos+1));
							positions.add(v.get(pos+2));
						}
						
						if(iEnd=='/'){
							iEnd=readInt(reader, dumpIds);
							if(!dumpIds.isEmpty()){
								int pos=dumpIds.get(0)*2;
								uvs.add(vt.get(pos));
								uvs.add(vt.get(pos+1));
								dumpIds.clear();
								hasUv=true;
							}
						}
						if(iEnd=='/'){
							iEnd=readInt(reader, dumpIds);
							if(!dumpIds.isEmpty()){
								int pos=dumpIds.get(0)*3;
								normal.add(vn.get(pos));
								normal.add(vn.get(pos+1));
								normal.add(vn.get(pos+2));
								dumpIds.clear();
								hasNormal=true;
							}
						}
					}
					
					if(faceSize==4){
						
						int pos1=4;
						int pos2=2;
						int siz =ids.size();
						
						int p3, p2, p3org=positions.size()/3;
						
						
						ids.add(ids.get(siz-pos1));
						p2=(p3org-pos1)*2;
						p3=(p3org-pos1)*3;
						
						positions.add(positions.get(p3));
						positions.add(positions.get(p3+1));
						positions.add(positions.get(p3+2));
						if(hasNormal){
							normal.add(normal.get(p3));
							normal.add(normal.get(p3+1));
							normal.add(normal.get(p3+2));
						}
						if(hasUv){
							uvs.add(uvs.get(p2));
							uvs.add(uvs.get(p2+1));
						}
						
						
						ids.add(ids.get(siz-pos2));
						p2=(p3org-pos2)*2;
						p3=(p3org-pos2)*3;
						
						positions.add(positions.get(p3));
						positions.add(positions.get(p3+1));
						positions.add(positions.get(p3+2));
						if(hasNormal){
							normal.add(normal.get(p3));
							normal.add(normal.get(p3+1));
							normal.add(normal.get(p3+2));
						}
						if(hasUv){
							uvs.add(uvs.get(p2));
							uvs.add(uvs.get(p2+1));
						}
					}
					
					continue;
				}
				
				if(ch=='l'){// object
					reader.readLine();
					continue;//TODO
				}
				if(ch=='g'){// group
					reader.readLine();
					continue;//TODO
				}
				
				throw new RuntimeException(ch+"["+(int)ch+"], "+reader.readLine());
				
			}
			
		}catch(IOException e){
			throw new IllegalArgumentException("Failed to load model: "+target, e);
		}
		
		boolean hasNormals=!normal.isEmpty();
		boolean hasUvs    =!uvs.isEmpty();
		for(int i=0, j=positions.size()/3;i<j;i++){
			
			int i3=i*3;
			int i2=i*2;
			consumer.consume(positions.get(i3),
			                 positions.get(i3+2),
			                 -positions.get(i3+1),
			                 hasNormals?normal.get(i3):0,
			                 hasNormals?normal.get(i3+1):0,
			                 hasNormals?normal.get(i3+2):0,
			                 hasUvs?uvs.get(i2):0,
			                 hasUvs?uvs.get(i2+1):0
			                );
		}
//		int[] face=new int[3];
//		for(int i=0;i<ids.size();i+=3){
//			face[0]=ids.get(i);
//			face[1]=ids.get(i+1);
//			face[2]=ids.get(i+2);
//			indexFaceConsumer.accept(face);
//		}
	}
	
	private char readInt(BufferedReader reader, TIntList dest) throws IOException{
		int  read=reader.read();
		char dig =(char)read;
		if(!Character.isDigit(dig)) return dig;
		
		int result=Character.digit(dig, 10);
		
		while((read=reader.read())!=-1){
			dig=(char)read;
			
			if(!Character.isDigit(dig)){
				dest.add(result-1);
				return dig;
			}
			
			result*=10;
			result+=Character.digit(dig, 10);
		}
		return 0;
	}
	
	private char readFloat(Reader reader, TFloatList dest) throws IOException{
		float decimalMul=1;
		int   mul       =1;
		
		float   pos=0;
		int     read;
		boolean dot=false;
		while((read=reader.read())!=-1){
			char dig=(char)read;
			
			
			if(dig=='-'){
				mul=-1;
				continue;
			}
			if(dig==' '||dig=='\r'||dig=='\n'){
				dest.add(pos*mul);
				return dig;
			}
			
			if(dig=='.'){
				dot=true;
				continue;
			}
			
			int dg=Character.digit(dig, 10);
			if(!dot){
				pos*=10;
				pos+=dg;
			}else{
				decimalMul/=10;
				pos+=dg*decimalMul;
			}
		}
		return 0;
	}
	
	private void loadMtlib(IDataManager source, String mtlibName){
	
	}
}
