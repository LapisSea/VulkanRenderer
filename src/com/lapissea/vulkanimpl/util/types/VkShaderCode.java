package com.lapissea.vulkanimpl.util.types;

import com.lapissea.datamanager.DataSignature;
import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.NativeUtils;
import com.lapissea.vulkanimpl.assets.ICacheableResource;
import com.lapissea.vulkanimpl.exceptions.VkException;
import com.lapissea.vulkanimpl.exceptions.VkShaderCompilationException;
import com.lapissea.vulkanimpl.shaders.VkShader;
import graphics.scenery.spirvcrossj.IntVec;
import graphics.scenery.spirvcrossj.TProgram;
import graphics.scenery.spirvcrossj.TShader;
import graphics.scenery.spirvcrossj.libspirvcrossj;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;

import static graphics.scenery.spirvcrossj.EShMessages.*;
import static org.lwjgl.system.MemoryUtil.*;

public class VkShaderCode extends ICacheableResource<ByteBuffer>{
	
	private static boolean LIBSPIRVCROSSJ_INITED=false;
	
	private static synchronized void initLibspirvcrossj(){
		if(LIBSPIRVCROSSJ_INITED) return;
		LIBSPIRVCROSSJ_INITED=true;
		NativeUtils.loadLibrary(new File("natives/spirvcrossj"));
		if(!libspirvcrossj.initializeProcess()){
			throw new RuntimeException("glslang failed to initialize.");
		}
	}
	
	private static final IDataManager SPIRV_CACHE=IDataManager.APP_RUN_DIR.subData(".cache/vk/spirv");
	
	public final VkShader.Type type;
	
	private long lastSpirv;
	
	private boolean canEditCreate;
	
	public VkShaderCode(DataSignature glslSource){
		this(glslSource, VkShader.Type.fromName(glslSource.getPath()));
	}
	private VkShaderCode(DataSignature glslSource, VkShader.Type type){
		super(glslSource.derive(type::append));
		this.type=type;
	}
	
	private ByteBuffer compileGlsl(DataSignature source){
		if(!source.exists()) throw new VkException("Missing shader source and no spirv code was supplied! Expected glsl source: "+source);
		initLibspirvcrossj();
		
		var messages=EShMsgDefault|EShMsgVulkanRules|EShMsgSpvRules;
		
		TShader  shader  =new TShader(type.libspirvcrossj);
		TProgram program =null;
		IntVec   compiled=null;
		try{
			
			String glslCode=source.readAll();
			
			shader.setStrings(new String[]{glslCode}, 1);
			shader.setAutoMapBindings(true);
			
			
			if(!shader.parse(libspirvcrossj.getDefaultTBuiltInResource(), 400, false, messages))
				throw new VkShaderCompilationException("Shader "+source.toStringSimple()+" failed to compile!\nLog:\n"+shader.getInfoLog());
			
			program=new TProgram();
			program.addShader(shader);
			
			
			if(!program.link(messages)||!program.mapIO())
				throw new VkShaderCompilationException("Shader failed to link! Source: "+source+" Log:\n"+program.getInfoLog());
			
			compiled=new IntVec();
			libspirvcrossj.glslangToSpv(program.getIntermediate(type.libspirvcrossj), compiled);
			
			
			var spirv=memAlloc((int)(compiled.size()*Integer.SIZE/Byte.SIZE));
			var ib   =spirv.asIntBuffer();
			
			for(int i=0;i<compiled.size();i++){
				ib.put((int)compiled.get(i));
			}
			
			return spirv;
			
		}finally{
			if(compiled!=null) compiled.delete();
			if(program!=null) program.delete();
			shader.delete();
		}
	}
	
	
	@Override
	protected ByteBuffer loadSource(DataSignature source){
		return compileGlsl(source);
	}
	
	@Override
	protected DataSignature getCacheSource(DataSignature source){
		var spirvSource=source.derive(path->path+".spv");
		
		canEditCreate=spirvSource.canEditCreate();
		//can't create cached and there is none so no point in trying to use it
		if(!canEditCreate&&!spirvSource.exists()){
			spirvSource=spirvSource.migrate(SPIRV_CACHE);
			canEditCreate=true;
		}
		
		return spirvSource;
	}
	
	@Override
	protected ByteBuffer loadCache(DataSignature cacheSource){
		return cacheSource.readAllBytes(MemoryUtil::memAlloc);
	}
	
	@Override
	protected void makeCache(DataSignature cacheSource, ByteBuffer data){
		if(!canEditCreate) throw new RuntimeException("");
		cacheSource.makeFile(data);
	}
	
	@Override
	protected boolean observe(){
		return getSource().newerThan(getCacheSource(getSource()));
	}
}
