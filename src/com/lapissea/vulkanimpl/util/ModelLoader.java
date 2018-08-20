package com.lapissea.vulkanimpl.util;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.renderer.model.VkMaterial;
import com.lapissea.vulkanimpl.renderer.model.VkMesh;
import com.lapissea.vulkanimpl.renderer.model.VkModelBuilder;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

import static com.lapissea.datamanager.IDataManager.Mode.*;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class ModelLoader{
	
	public static List<VkMesh> load(VkGpuCtx gpuCtx, IDataManager source, String path){
//		File cacheFile=new File(".cache/rawModel", path+".zip");
//		return FileChageDetector.getInfo(new File(path)).getChangeTime() >= FileChageDetector.getInfo(cacheFile).getChangeTime()?parseModel(gpuCtx.getGpu(), path, cacheFile, source):readCached(gpuCtx.getGpu(), cacheFile);
		return parseModel(gpuCtx.getGpu(), path, source);
	}
	
	private static synchronized AIScene loadScene(String path, IDataManager source, int flags){
		try(MemoryStack stack=MemoryStack.stackPush()){
			
			var ioSystem=AIFileIO.callocStack();
			
			TLongObjectHashMap<FileChannel> open=new TLongObjectHashMap<>(2);
			
			ioSystem
				.OpenProc((pFileIO, fileNameP, openModeP)->{
					
					String fileName=stack.pointers(fileNameP).getStringUTF8();
					String openMode=stack.pointers(openModeP).getStringUTF8();
					
					FileChannel channel=source.getRandomAccess(fileName, READ_ONLY);
					if(channel==null) return NULL;
					
					long   fileSize=source.getSize(fileName);
					long[] pos     ={0};
					
					AIFileReadProcI read=(filePtr, bufferPtr, size, count)->{
						
						long remaining=fileSize-pos[0];
						
						try{
							channel.read(memByteBuffer(bufferPtr, (int)count));
						}catch(IOException e){
							e.printStackTrace();
							System.exit(0);//TODO properly handle
						}
						
						return remaining;
					};
					
					AIFileSeekI seek=(pFile, offset, origin)->{
						pos[0]=origin+offset;
						return pos[0]==fileSize?0:1;
					};
					long pfil=AIFile.callocStack(stack)
					                .FileSizeProc(pFile->fileSize)
					                .ReadProc(read)
					                .SeekProc(seek)
					                .address();
					
					open.put(pfil, channel);
					return pfil;
				})
				.CloseProc((pFileIO, pFile)->{
					try{
						open.remove(pFile).close();
					}catch(IOException ignored){}
				});
			
			return Assimp.aiImportFileEx(path, flags, ioSystem);
		}catch(Throwable th){
			return null;
		}
	}
	
	private static List<VkMesh> parseModel(VkGpu gpu, String path, IDataManager source){
		LogUtil.println("parseModel", path);
		
		int flags=
			aiProcess_FlipUVs|
			aiProcess_Triangulate|
			aiProcess_FlipWindingOrder|
			aiProcess_CalcTangentSpace|
			
			aiProcess_ImproveCacheLocality|
			aiProcess_RemoveRedundantMaterials;
		
		AIScene scene=loadScene(path, source, flags);
		Objects.requireNonNull(scene);
		
		String modelRoot=new File(path).getParent();
		
		List<VkMaterial> mats=new ArrayList<>(scene.mNumMaterials());
		
		for(int i1=0;i1<scene.mNumMaterials();i1++){
			int i=i1;
			IntFunction<String> getTexture=type->{
				String ps;
				
				try(MemoryStack stack=stackPush()){
					
					var m=AIMaterial.create(scene.mMaterials().get(i));
					var p=AIString.callocStack();
					
					synchronized(mats){
						Assimp.aiGetMaterialTexture(m, type, 0, p, (IntBuffer)null, null, null, null, null, null);
					}
					
					ps=p.dataString();
				}

//				if(!ps.isEmpty()) async(()->textures.getByName(modelRoot+"/"+ps));
				return ps.isEmpty()?ps:modelRoot+"/"+ps;
			};
			
			mats.add(new VkMaterial(source.createSignature(getTexture.apply(aiTextureType_DIFFUSE)), source.createSignature(getTexture.apply(aiTextureType_HEIGHT))));
		}
		
		PointerBuffer meshes=scene.mMeshes();
		Objects.requireNonNull(meshes);
		
		int          count =0;
		List<VkMesh> result=new ArrayList<>();
		
		for(int i2=0, j2=scene.mNumMeshes();i2<j2;i2++){
			
			AIMesh mesh=AIMesh.create(meshes.get(i2));
			
			AIFace.Buffer fa=mesh.mFaces();
			
			AIVector3D.Buffer vt=mesh.mVertices();
			AIVector3D.Buffer nr=mesh.mNormals();
			AIVector3D.Buffer tg=mesh.mTangents();
			AIVector3D.Buffer uv=mesh.mTextureCoords(0);
			
			VkModelBuilder modelBuilder=new VkModelBuilder(VK_FORMAT_R32G32B32_SFLOAT, VK_FORMAT_R32G32B32_SFLOAT, VK_FORMAT_R32G32B32_SFLOAT, VK_FORMAT_R32G32_SFLOAT);
			
			if(nr==null){
				modelBuilder.format.disable(1);
				modelBuilder.format.disable(2);
			}
			if(uv==null){
				modelBuilder.format.disable(3);
			}
			
			for(int i1=0;i1<vt.limit();i1++){
				modelBuilder.put3F(vt.position(i1).x(), vt.y(), vt.z());
				
				if(nr!=null) modelBuilder.put3F(nr.position(i1).x(), nr.y(), nr.z())
				                         .put3F(tg.position(i1).x(), tg.y(), tg.z());
				
				if(uv!=null) modelBuilder.put2F(uv.position(i1).x(), uv.y());
				
				modelBuilder.next();
			}
			
			int     idCheck   =0;
			boolean continuous=true;
			
			for(int i=0;i<fa.limit();i++){
				fa.position(i);
				var ib=fa.mIndices();
				
				for(int i1=0;i1<fa.mNumIndices();i1++){
					var id=ib.get(i1);
					
					if(continuous){
						if(idCheck++!=id){
							continuous=false;
						}
					}
					modelBuilder.addIndices(id);
				}
			}
			if(continuous) modelBuilder.clearIndex();
			
			int indexCount=modelBuilder.indexCount();
			int totalSize =modelBuilder.totalSize();
			
			if(totalSize==0) continue;
			
			byte[] rawModelData=modelBuilder.writeAll(ByteBuffer.allocate(totalSize).order(ByteOrder.nativeOrder())).array();
			modelBuilder.clearAll();
			
			VkMesh model=gpu.upload(unsafeMemory->unsafeMemory.put(rawModelData), modelBuilder.format, totalSize, indexCount);
			
			model.material=mats.get(mesh.mMaterialIndex());
			result.add(model);
		}
		
		Assimp.aiFreeScene(scene);
		
		return result;
		
	}
	
}
