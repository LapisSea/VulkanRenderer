package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.util.NotNull;
import com.lapissea.vec.color.IColorM;
import com.lapissea.vec.interf.IVec3fR;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.renderer.model.VkMesh.IndexType;
import com.lapissea.vulkanimpl.util.format.VKFormatWriter;
import com.lapissea.vulkanimpl.util.format.VkFormat;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;

public class VkModelBuilder{
	
	public final VkMeshFormat format;
	
	private final TIntArrayList  indices=new TIntArrayList();
	private final TByteArrayList data   =new TByteArrayList();
	private final ByteBuffer     vertex;
	private       int            typePos;
	
	public VkModelBuilder(int... parts){
		this(new VkMeshFormat(parts));
	}
	
	public VkModelBuilder(VkFormat... parts){
		this(new VkMeshFormat(parts));
	}
	
	public VkModelBuilder(VkMeshFormat format){
		this.format=format;
		vertex=ByteBuffer.allocate(format.getSize()).order(ByteOrder.nativeOrder());
	}
	
	
	public VkModelBuilder put1I(int x){
		this.<VKFormatWriter.I>get().write(x);
		return this;
	}
	
	
	public VkModelBuilder put1F(float x){
		this.<VKFormatWriter.F>get().write(x);
		return this;
	}
	
	public VkModelBuilder put2F(float x, float y){
		this.<VKFormatWriter.FF>get().write(x, y);
		return this;
	}
	
	public VkModelBuilder put3F(float x, float y, float z){
		this.<VKFormatWriter.FFF>get().write(x, y, z);
		return this;
	}
	
	public VkModelBuilder put3F(@NotNull IVec3fR vec){
		return put3F(vec.x(), vec.y(), vec.z());
	}
	
	public VkModelBuilder put4F(float x, float y, float z, float w){
		this.<VKFormatWriter.FFFF>get().write(x, y, z, w);
		return this;
	}
	
	public VkModelBuilder put4F(@NotNull IColorM color){
		return put4F(color.r(), color.g(), color.b(), color.a());
	}
	
	
	@SuppressWarnings("unchecked")
	private <T extends VKFormatWriter> T get(){
		T t=(T)format.getType(typePos).getWriter();
		
		t.setDest(vertex);
		
		typePos++;
		while(format.getTypeCount()>typePos&&format.isDisabled(typePos))typePos++;
		
		return t;
	}
	
	public void next(){
		if(vertex.capacity()!=vertex.position()) throw new IllegalStateException("Vertex not finished: "+vertex.position()+" out of "+vertex.capacity()+" bytes");
		data.addAll(vertex.array());
		typePos=0;
		vertex.position(0);
	}
	
	public void addIndices(int... indices){
		this.indices.addAll(indices);
	}
	
	public int indexCount(){
		return indices.size();
	}
	
	public int dataSize(){
		return data.size();
	}
	
	
	public VkMesh bake(VkGpu gpu){
		if(dataSize()==0) return VkMesh.EMPTY_MESH;
		if(vertex.position()!=0) throw new IllegalStateException("Vertex not finished ");
		
		return gpu.upload(this::writeAll, format, (int)totalSize(), (int)indexCount());
	}
	
	public int indexSize(){
		return indexCount()*indexType().bytes;
	}
	
	public int totalSize(){
		return dataSize()+indexSize();
	}
	
	public IndexType indexType(){
		return IndexType.get((int)indexCount());
	}
	
	public ByteBuffer writeAll(ByteBuffer mem){
		writeVertex(mem);
		writeIndex(mem);
		return mem;
	}
	
	public void writeVertex(ByteBuffer mem){
		if(data.isEmpty()) return;
		mem.put(data.toArray());
	}
	
	public void writeIndex(ByteBuffer mem){
		if(indexCount()==0) return;
		
		IndexType indexType=indexType();
		
		for(int i=0, j=indices.size();i<j;i++){
			writeIndex(mem, indexType, indices.get(i));
		}
	}
	
	private void writeIndex(ByteBuffer mem, IndexType indexType, int id){
		
		if(DEV_ON){
			if(id<0) throw new IndexOutOfBoundsException("Index must be positive!");
		}
		indexType.writer.write(mem, id);
	}
	
	public void clearAll(){
		clearData();
		clearIndex();
	}
	
	public void clearData(){
		data.clear();
		typePos=0;
		vertex.position(0);
	}
	
	public void clearIndex(){
		indices.clear();
	}
}
