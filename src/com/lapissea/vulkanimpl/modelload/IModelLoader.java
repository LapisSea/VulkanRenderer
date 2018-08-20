package com.lapissea.vulkanimpl.modelload;

import com.lapissea.datamanager.IDataManager;

import java.util.function.Consumer;

public interface IModelLoader{
	
	void load(IDataManager source, String target, IVertexConsumer consumer, Consumer<int[]> indexFaceConsumer);
	
}
