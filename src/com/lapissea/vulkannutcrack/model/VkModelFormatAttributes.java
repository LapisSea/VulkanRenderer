package com.lapissea.vulkannutcrack.model;

import com.lapissea.util.UtilL;
import com.lapissea.vec.Vec2f;
import com.lapissea.vec.color.IColorM;
import com.lapissea.vec.interf.IVec3fR;

import java.nio.ByteBuffer;

import static com.lapissea.vulkannutcrack.model.VkModelFormatAttribute.*;
import static org.lwjgl.vulkan.VK10.*;

class VkModelFormatAttributes{
	
	static{
		registerCommonAttrubute(new Simple<>(boolean.class, VK_FORMAT_R8_UINT){
			@Override
			public void put(ByteBuffer buffer, Boolean aBoolean){ UtilL.uncheckedThrow(new IllegalAccessException()); }
		});
		registerCommonAttrubute(new Simple<>(byte.class, VK_FORMAT_R8_SINT){
			@Override
			public void put(ByteBuffer buffer, Byte aByte){ UtilL.uncheckedThrow(new IllegalAccessException()); }
		});
		registerCommonAttrubute(new Simple<>(float.class, VK_FORMAT_R32_SFLOAT){
			@Override
			public void put(ByteBuffer buffer, Float aFloat){ UtilL.uncheckedThrow(new IllegalAccessException()); }
		});
		registerCommonAttrubute(new Simple<>(short.class, VK_FORMAT_R16_SINT){
			@Override
			public void put(ByteBuffer buffer, Short aShort){ UtilL.uncheckedThrow(new IllegalAccessException()); }
		});
		registerCommonAttrubute(new Simple<>(int.class, VK_FORMAT_R32_SINT){
			@Override
			public void put(ByteBuffer buffer, Integer integer){ UtilL.uncheckedThrow(new IllegalAccessException()); }
		});
		registerCommonAttrubute(new Simple<>(long.class, VK_FORMAT_R64_SINT){
			@Override
			public void put(ByteBuffer buffer, Long aLong){ UtilL.uncheckedThrow(new IllegalAccessException()); }
		});
		
		registerCommonAttrubute(new Simple<>(double.class, VK_FORMAT_R64_SFLOAT){
			@Override
			public void put(ByteBuffer buffer, Double aDouble){ UtilL.uncheckedThrow(new IllegalAccessException()); }
		});
		registerCommonAttrubute(new Simple<>(IColorM.class, VK_FORMAT_R32G32B32A32_SFLOAT){
			@Override
			public void put(ByteBuffer buffer, IColorM iColorM){
				put(buffer, iColorM.r());
				put(buffer, iColorM.g());
				put(buffer, iColorM.b());
				put(buffer, iColorM.a());
			}
		});
		registerCommonAttrubute(new Simple<>(IVec3fR.class, VK_FORMAT_R32G32B32_SFLOAT){
			@Override
			public void put(ByteBuffer buffer, IVec3fR iVec3fR){
				put(buffer, iVec3fR.x());
				put(buffer, iVec3fR.y());
				put(buffer, iVec3fR.z());
			}
		});
		registerCommonAttrubute(new Simple<>(Vec2f.class, VK_FORMAT_R32G32_SFLOAT){
			@Override
			public void put(ByteBuffer buffer, Vec2f vec2f){
				put(buffer, vec2f.x());
				put(buffer, vec2f.y());
			}
		});
	}
}
