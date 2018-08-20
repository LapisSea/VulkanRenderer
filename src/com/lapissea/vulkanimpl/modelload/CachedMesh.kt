package com.lapissea.vulkanimpl.modelload

import com.lapissea.vulkanimpl.renderer.model.VkMeshFormat

class CachedMesh(
	var modelData: ByteArray=ByteArray(0),
	var indexCount: Int=0,
	var vertexFormat: IntArray=IntArray(0)
) {
	
	fun getFormat()=VkMeshFormat(vertexFormat)
	
}
