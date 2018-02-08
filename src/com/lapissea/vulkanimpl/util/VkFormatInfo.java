package com.lapissea.vulkanimpl.util;

import com.lapissea.vec.Vec2iFinal;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.devonly.VkFormatAnalysis;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;

public class VkFormatInfo{
	
	public enum StorageType{
		INT(int.class),
		FLOAT(float.class),
		NORM(float.class),
		SCALED(int.class),
		RGB(float.class),
		RGBA(float.class);
		
		public final Class<?> primitiveType;
		
		StorageType(Class<?> primitiveType){this.primitiveType=primitiveType;}
	}
	
	public enum ComponentType{
		UNUSED('X'), RED('R'), GREEN('G'), BLUE('B'), ALPHA('A'), STENCIL('S'), DEPTH('D'), SHARED_EXPONENT('E');
		
		public final char mark;
		
		ComponentType(char mark){this.mark=mark;}
	}
	
	public static class Component{
		public final ComponentType type;
		public final int           bitSize;
		public final StorageType   storageType;
		public final boolean       signed;
		public final int           packId;
		
		public Component(ComponentType type, int bitSize, StorageType storageType, boolean signed){
			this(type, bitSize, storageType, signed, -1);
		}
		
		public Component(ComponentType type, int bitSize, StorageType storageType, boolean signed, int packId){
			this.type=type;
			this.bitSize=bitSize;
			this.storageType=storageType;
			this.signed=signed;
			this.packId=packId;
		}
		
		@Override
		public String toString(){
			return "Component{"+
			       "type="+type+
			       ", bitSize="+bitSize+
			       ", storageType="+storageType+
			       ", signed="+signed+
			       ", packId="+packId+
			       '}';
		}
	}
	
	
	public final String          name;
	public final List<Component> components;
	public final List<Integer>   packSizes;
	public final String          compression;
	public final boolean         isBlock;
	public final IVec2iR         blockSize;
	public final int             totalByteSize;
	
	public VkFormatInfo(String name, List<Component> components, List<Integer> packSizes, String compression, boolean isBlock, IVec2iR blockSize){
		this.name=name;
		this.components=Collections.unmodifiableList(components);
		this.packSizes=Collections.unmodifiableList(packSizes);
		this.compression=compression;
		this.isBlock=isBlock;
		this.blockSize=blockSize;
		
		totalByteSize=(packSizes.isEmpty()?components.stream().mapToInt(c->c.bitSize):packSizes.stream().mapToInt(c->c)).sum();
	}
	
	@Override
	public String toString(){
		return "Info{"+
		       "name='"+name+'\''+
		       ", components="+components+
		       ", packSizes="+packSizes+
		       ", compression='"+compression+'\''+
		       ", isBlock="+isBlock+
		       ", blockSize="+blockSize+
		       '}';
	}
	
	private static final TIntObjectHashMap<VkFormatInfo> INFO=new TIntObjectHashMap<>();
	
	public static VkFormatInfo get(int format){
		return INFO.get(format);
	}
	
	static{
		if(DEV_ON) VkFormatAnalysis.generateCode();
		
		List<Component> EC=Collections.emptyList();
		List<Integer>   EP=Collections.emptyList();
		INFO.put(0, new VkFormatInfo("VK_FORMAT_UNDEFINED", EC, EP, null, false, null));
		INFO.put(146, new VkFormatInfo("VK_FORMAT_BC7_SRGB_BLOCK", EC, EP, "BC7", true, new Vec2iFinal(4, 4)));
		INFO.put(138, new VkFormatInfo("VK_FORMAT_BC3_SRGB_BLOCK", EC, EP, "BC3", true, new Vec2iFinal(4, 4)));
		INFO.put(136, new VkFormatInfo("VK_FORMAT_BC2_SRGB_BLOCK", EC, EP, "BC2", true, new Vec2iFinal(4, 4)));
		INFO.put(145, new VkFormatInfo("VK_FORMAT_BC7_UNORM_BLOCK", EC, EP, "BC7", true, new Vec2iFinal(4, 4)));
		INFO.put(142, new VkFormatInfo("VK_FORMAT_BC5_SNORM_BLOCK", EC, EP, "BC5", true, new Vec2iFinal(4, 4)));
		INFO.put(141, new VkFormatInfo("VK_FORMAT_BC5_UNORM_BLOCK", EC, EP, "BC5", true, new Vec2iFinal(4, 4)));
		INFO.put(140, new VkFormatInfo("VK_FORMAT_BC4_SNORM_BLOCK", EC, EP, "BC4", true, new Vec2iFinal(4, 4)));
		INFO.put(139, new VkFormatInfo("VK_FORMAT_BC4_UNORM_BLOCK", EC, EP, "BC4", true, new Vec2iFinal(4, 4)));
		INFO.put(137, new VkFormatInfo("VK_FORMAT_BC3_UNORM_BLOCK", EC, EP, "BC3", true, new Vec2iFinal(4, 4)));
		INFO.put(135, new VkFormatInfo("VK_FORMAT_BC2_UNORM_BLOCK", EC, EP, "BC2", true, new Vec2iFinal(4, 4)));
		INFO.put(144, new VkFormatInfo("VK_FORMAT_BC6H_SFLOAT_BLOCK", EC, EP, "BC6H", true, new Vec2iFinal(4, 4)));
		INFO.put(143, new VkFormatInfo("VK_FORMAT_BC6H_UFLOAT_BLOCK", EC, EP, "BC6H", true, new Vec2iFinal(4, 4)));
		INFO.put(132, new VkFormatInfo("VK_FORMAT_BC1_RGB_SRGB_BLOCK", EC, EP, "BC1", true, new Vec2iFinal(4, 4)));
		INFO.put(134, new VkFormatInfo("VK_FORMAT_BC1_RGBA_SRGB_BLOCK", EC, EP, "BC1", true, new Vec2iFinal(4, 4)));
		INFO.put(131, new VkFormatInfo("VK_FORMAT_BC1_RGB_UNORM_BLOCK", EC, EP, "BC1", true, new Vec2iFinal(4, 4)));
		INFO.put(172, new VkFormatInfo("VK_FORMAT_ASTC_8x8_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 8)));
		INFO.put(170, new VkFormatInfo("VK_FORMAT_ASTC_8x6_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 6)));
		INFO.put(168, new VkFormatInfo("VK_FORMAT_ASTC_8x5_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 5)));
		INFO.put(166, new VkFormatInfo("VK_FORMAT_ASTC_6x6_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(6, 6)));
		INFO.put(164, new VkFormatInfo("VK_FORMAT_ASTC_6x5_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(6, 5)));
		INFO.put(162, new VkFormatInfo("VK_FORMAT_ASTC_5x5_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(5, 5)));
		INFO.put(160, new VkFormatInfo("VK_FORMAT_ASTC_5x4_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(5, 4)));
		INFO.put(158, new VkFormatInfo("VK_FORMAT_ASTC_4x4_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(4, 4)));
		INFO.put(133, new VkFormatInfo("VK_FORMAT_BC1_RGBA_UNORM_BLOCK", EC, EP, "BC1", true, new Vec2iFinal(4, 4)));
		INFO.put(171, new VkFormatInfo("VK_FORMAT_ASTC_8x8_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 8)));
		INFO.put(169, new VkFormatInfo("VK_FORMAT_ASTC_8x6_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 6)));
		INFO.put(167, new VkFormatInfo("VK_FORMAT_ASTC_8x5_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 5)));
		INFO.put(165, new VkFormatInfo("VK_FORMAT_ASTC_6x6_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(6, 6)));
		INFO.put(163, new VkFormatInfo("VK_FORMAT_ASTC_6x5_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(6, 5)));
		INFO.put(161, new VkFormatInfo("VK_FORMAT_ASTC_5x5_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(5, 5)));
		INFO.put(159, new VkFormatInfo("VK_FORMAT_ASTC_5x4_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(5, 4)));
		INFO.put(157, new VkFormatInfo("VK_FORMAT_ASTC_4x4_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(4, 4)));
		INFO.put(178, new VkFormatInfo("VK_FORMAT_ASTC_10x8_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 8)));
		INFO.put(176, new VkFormatInfo("VK_FORMAT_ASTC_10x6_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 6)));
		INFO.put(174, new VkFormatInfo("VK_FORMAT_ASTC_10x5_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 5)));
		INFO.put(177, new VkFormatInfo("VK_FORMAT_ASTC_10x8_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 8)));
		INFO.put(175, new VkFormatInfo("VK_FORMAT_ASTC_10x6_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 6)));
		INFO.put(173, new VkFormatInfo("VK_FORMAT_ASTC_10x5_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 5)));
		INFO.put(184, new VkFormatInfo("VK_FORMAT_ASTC_12x12_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(12, 12)));
		INFO.put(182, new VkFormatInfo("VK_FORMAT_ASTC_12x10_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(12, 10)));
		INFO.put(180, new VkFormatInfo("VK_FORMAT_ASTC_10x10_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 10)));
		INFO.put(183, new VkFormatInfo("VK_FORMAT_ASTC_12x12_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(12, 12)));
		INFO.put(181, new VkFormatInfo("VK_FORMAT_ASTC_12x10_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(12, 10)));
		INFO.put(179, new VkFormatInfo("VK_FORMAT_ASTC_10x10_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 10)));
		INFO.put(15, new VkFormatInfo("VK_FORMAT_R8_SRGB", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(14, new VkFormatInfo("VK_FORMAT_R8_SINT", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(13, new VkFormatInfo("VK_FORMAT_R8_UINT", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(99, new VkFormatInfo("VK_FORMAT_R32_SINT", Collections.singletonList(new Component(ComponentType.RED, 32, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(75, new VkFormatInfo("VK_FORMAT_R16_SINT", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(10, new VkFormatInfo("VK_FORMAT_R8_SNORM", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(9, new VkFormatInfo("VK_FORMAT_R8_UNORM", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(111, new VkFormatInfo("VK_FORMAT_R64_SINT", Collections.singletonList(new Component(ComponentType.RED, 64, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(98, new VkFormatInfo("VK_FORMAT_R32_UINT", Collections.singletonList(new Component(ComponentType.RED, 32, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(74, new VkFormatInfo("VK_FORMAT_R16_UINT", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(110, new VkFormatInfo("VK_FORMAT_R64_UINT", Collections.singletonList(new Component(ComponentType.RED, 64, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(71, new VkFormatInfo("VK_FORMAT_R16_SNORM", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(70, new VkFormatInfo("VK_FORMAT_R16_UNORM", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(127, new VkFormatInfo("VK_FORMAT_S8_UINT", Collections.singletonList(new Component(ComponentType.STENCIL, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(76, new VkFormatInfo("VK_FORMAT_R16_SFLOAT", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(12, new VkFormatInfo("VK_FORMAT_R8_SSCALED", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(112, new VkFormatInfo("VK_FORMAT_R64_SFLOAT", Collections.singletonList(new Component(ComponentType.RED, 64, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(100, new VkFormatInfo("VK_FORMAT_R32_SFLOAT", Collections.singletonList(new Component(ComponentType.RED, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(11, new VkFormatInfo("VK_FORMAT_R8_USCALED", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(124, new VkFormatInfo("VK_FORMAT_D16_UNORM", Collections.singletonList(new Component(ComponentType.DEPTH, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(73, new VkFormatInfo("VK_FORMAT_R16_SSCALED", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(126, new VkFormatInfo("VK_FORMAT_D32_SFLOAT", Collections.singletonList(new Component(ComponentType.DEPTH, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(72, new VkFormatInfo("VK_FORMAT_R16_USCALED", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(154, new VkFormatInfo("VK_FORMAT_EAC_R11_SNORM_BLOCK", Collections.singletonList(new Component(ComponentType.RED, 11, StorageType.NORM, true)), Collections.emptyList(), "EAC", true, new Vec2iFinal(4, 4)));
		INFO.put(153, new VkFormatInfo("VK_FORMAT_EAC_R11_UNORM_BLOCK", Collections.singletonList(new Component(ComponentType.RED, 11, StorageType.NORM, false)), Collections.emptyList(), "EAC", true, new Vec2iFinal(4, 4)));
		INFO.put(22, new VkFormatInfo("VK_FORMAT_R8G8_SRGB", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(21, new VkFormatInfo("VK_FORMAT_R8G8_SINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(20, new VkFormatInfo("VK_FORMAT_R8G8_UINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(17, new VkFormatInfo("VK_FORMAT_R8G8_SNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(82, new VkFormatInfo("VK_FORMAT_R16G16_SINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, true), new Component(ComponentType.GREEN, 16, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(114, new VkFormatInfo("VK_FORMAT_R64G64_SINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, true), new Component(ComponentType.GREEN, 64, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(102, new VkFormatInfo("VK_FORMAT_R32G32_SINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, true), new Component(ComponentType.GREEN, 32, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(16, new VkFormatInfo("VK_FORMAT_R8G8_UNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(81, new VkFormatInfo("VK_FORMAT_R16G16_UINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, false), new Component(ComponentType.GREEN, 16, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(113, new VkFormatInfo("VK_FORMAT_R64G64_UINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, false), new Component(ComponentType.GREEN, 64, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(101, new VkFormatInfo("VK_FORMAT_R32G32_UINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, false), new Component(ComponentType.GREEN, 32, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(78, new VkFormatInfo("VK_FORMAT_R16G16_SNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, true), new Component(ComponentType.GREEN, 16, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(77, new VkFormatInfo("VK_FORMAT_R16G16_UNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, false), new Component(ComponentType.GREEN, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(19, new VkFormatInfo("VK_FORMAT_R8G8_SSCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(83, new VkFormatInfo("VK_FORMAT_R16G16_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 16, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(115, new VkFormatInfo("VK_FORMAT_R64G64_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 64, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(103, new VkFormatInfo("VK_FORMAT_R32G32_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(18, new VkFormatInfo("VK_FORMAT_R8G8_USCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(80, new VkFormatInfo("VK_FORMAT_R16G16_SSCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, true), new Component(ComponentType.GREEN, 16, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(79, new VkFormatInfo("VK_FORMAT_R16G16_USCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, false), new Component(ComponentType.GREEN, 16, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(1, new VkFormatInfo("VK_FORMAT_R4G4_UNORM_PACK8", Arrays.asList(new Component(ComponentType.RED, 4, StorageType.NORM, false), new Component(ComponentType.GREEN, 4, StorageType.NORM, false)), Collections.singletonList(8), null, false, null));
		INFO.put(129, new VkFormatInfo("VK_FORMAT_D24_UNORM_S8_UINT", Arrays.asList(new Component(ComponentType.DEPTH, 24, StorageType.NORM, false), new Component(ComponentType.STENCIL, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(128, new VkFormatInfo("VK_FORMAT_D16_UNORM_S8_UINT", Arrays.asList(new Component(ComponentType.DEPTH, 16, StorageType.NORM, false), new Component(ComponentType.STENCIL, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(130, new VkFormatInfo("VK_FORMAT_D32_SFLOAT_S8_UINT", Arrays.asList(new Component(ComponentType.DEPTH, 32, StorageType.FLOAT, true), new Component(ComponentType.STENCIL, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(125, new VkFormatInfo("VK_FORMAT_X8_D24_UNORM_PACK32", Arrays.asList(new Component(ComponentType.UNUSED, 8, StorageType.NORM, false), new Component(ComponentType.DEPTH, 24, StorageType.NORM, false)), Collections.singletonList(32), null, false, null));
		INFO.put(156, new VkFormatInfo("VK_FORMAT_EAC_R11G11_SNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 11, StorageType.NORM, true), new Component(ComponentType.GREEN, 11, StorageType.NORM, true)), Collections.emptyList(), "EAC", true, new Vec2iFinal(4, 4)));
		INFO.put(155, new VkFormatInfo("VK_FORMAT_EAC_R11G11_UNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 11, StorageType.NORM, false), new Component(ComponentType.GREEN, 11, StorageType.NORM, false)), Collections.emptyList(), "EAC", true, new Vec2iFinal(4, 4)));
		INFO.put(36, new VkFormatInfo("VK_FORMAT_B8G8R8_SRGB", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.RED, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(35, new VkFormatInfo("VK_FORMAT_B8G8R8_SINT", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.RED, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(29, new VkFormatInfo("VK_FORMAT_R8G8B8_SRGB", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(28, new VkFormatInfo("VK_FORMAT_R8G8B8_SINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.BLUE, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(34, new VkFormatInfo("VK_FORMAT_B8G8R8_UINT", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.RED, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(27, new VkFormatInfo("VK_FORMAT_R8G8B8_UINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.BLUE, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(31, new VkFormatInfo("VK_FORMAT_B8G8R8_SNORM", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.RED, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(24, new VkFormatInfo("VK_FORMAT_R8G8B8_SNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.BLUE, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(89, new VkFormatInfo("VK_FORMAT_R16G16B16_SINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, true), new Component(ComponentType.GREEN, 16, StorageType.INT, true), new Component(ComponentType.BLUE, 16, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(117, new VkFormatInfo("VK_FORMAT_R64G64B64_SINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, true), new Component(ComponentType.GREEN, 64, StorageType.INT, true), new Component(ComponentType.BLUE, 64, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(105, new VkFormatInfo("VK_FORMAT_R32G32B32_SINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, true), new Component(ComponentType.GREEN, 32, StorageType.INT, true), new Component(ComponentType.BLUE, 32, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(30, new VkFormatInfo("VK_FORMAT_B8G8R8_UNORM", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.RED, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(23, new VkFormatInfo("VK_FORMAT_R8G8B8_UNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(88, new VkFormatInfo("VK_FORMAT_R16G16B16_UINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, false), new Component(ComponentType.GREEN, 16, StorageType.INT, false), new Component(ComponentType.BLUE, 16, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(116, new VkFormatInfo("VK_FORMAT_R64G64B64_UINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, false), new Component(ComponentType.GREEN, 64, StorageType.INT, false), new Component(ComponentType.BLUE, 64, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(104, new VkFormatInfo("VK_FORMAT_R32G32B32_UINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, false), new Component(ComponentType.GREEN, 32, StorageType.INT, false), new Component(ComponentType.BLUE, 32, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(85, new VkFormatInfo("VK_FORMAT_R16G16B16_SNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, true), new Component(ComponentType.GREEN, 16, StorageType.NORM, true), new Component(ComponentType.BLUE, 16, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(33, new VkFormatInfo("VK_FORMAT_B8G8R8_SSCALED", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.RED, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(26, new VkFormatInfo("VK_FORMAT_R8G8B8_SSCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.BLUE, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(84, new VkFormatInfo("VK_FORMAT_R16G16B16_UNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, false), new Component(ComponentType.GREEN, 16, StorageType.NORM, false), new Component(ComponentType.BLUE, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(90, new VkFormatInfo("VK_FORMAT_R16G16B16_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 16, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 16, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(118, new VkFormatInfo("VK_FORMAT_R64G64B64_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 64, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 64, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(106, new VkFormatInfo("VK_FORMAT_R32G32B32_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 32, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(32, new VkFormatInfo("VK_FORMAT_B8G8R8_USCALED", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.RED, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(25, new VkFormatInfo("VK_FORMAT_R8G8B8_USCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.BLUE, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(87, new VkFormatInfo("VK_FORMAT_R16G16B16_SSCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, true), new Component(ComponentType.GREEN, 16, StorageType.SCALED, true), new Component(ComponentType.BLUE, 16, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(5, new VkFormatInfo("VK_FORMAT_B5G6R5_UNORM_PACK16", Arrays.asList(new Component(ComponentType.BLUE, 5, StorageType.NORM, false), new Component(ComponentType.GREEN, 6, StorageType.NORM, false), new Component(ComponentType.RED, 5, StorageType.NORM, false)), Collections.singletonList(16), null, false, null));
		INFO.put(4, new VkFormatInfo("VK_FORMAT_R5G6B5_UNORM_PACK16", Arrays.asList(new Component(ComponentType.RED, 5, StorageType.NORM, false), new Component(ComponentType.GREEN, 6, StorageType.NORM, false), new Component(ComponentType.BLUE, 5, StorageType.NORM, false)), Collections.singletonList(16), null, false, null));
		INFO.put(86, new VkFormatInfo("VK_FORMAT_R16G16B16_USCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, false), new Component(ComponentType.GREEN, 16, StorageType.SCALED, false), new Component(ComponentType.BLUE, 16, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(148, new VkFormatInfo("VK_FORMAT_ETC2_R8G8B8_SRGB_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(122, new VkFormatInfo("VK_FORMAT_B10G11R11_UFLOAT_PACK32", Arrays.asList(new Component(ComponentType.BLUE, 10, StorageType.FLOAT, false), new Component(ComponentType.GREEN, 11, StorageType.FLOAT, false), new Component(ComponentType.RED, 11, StorageType.FLOAT, false)), Collections.singletonList(32), null, false, null));
		INFO.put(147, new VkFormatInfo("VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(50, new VkFormatInfo("VK_FORMAT_B8G8R8A8_SRGB", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.ALPHA, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(49, new VkFormatInfo("VK_FORMAT_B8G8R8A8_SINT", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.RED, 8, StorageType.INT, true), new Component(ComponentType.ALPHA, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(43, new VkFormatInfo("VK_FORMAT_R8G8B8A8_SRGB", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.ALPHA, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(42, new VkFormatInfo("VK_FORMAT_R8G8B8A8_SINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.BLUE, 8, StorageType.INT, true), new Component(ComponentType.ALPHA, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(48, new VkFormatInfo("VK_FORMAT_B8G8R8A8_UINT", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.RED, 8, StorageType.INT, false), new Component(ComponentType.ALPHA, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(41, new VkFormatInfo("VK_FORMAT_R8G8B8A8_UINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.BLUE, 8, StorageType.INT, false), new Component(ComponentType.ALPHA, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(45, new VkFormatInfo("VK_FORMAT_B8G8R8A8_SNORM", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.RED, 8, StorageType.NORM, true), new Component(ComponentType.ALPHA, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(38, new VkFormatInfo("VK_FORMAT_R8G8B8A8_SNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.BLUE, 8, StorageType.NORM, true), new Component(ComponentType.ALPHA, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(96, new VkFormatInfo("VK_FORMAT_R16G16B16A16_SINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, true), new Component(ComponentType.GREEN, 16, StorageType.INT, true), new Component(ComponentType.BLUE, 16, StorageType.INT, true), new Component(ComponentType.ALPHA, 16, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(120, new VkFormatInfo("VK_FORMAT_R64G64B64A64_SINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, true), new Component(ComponentType.GREEN, 64, StorageType.INT, true), new Component(ComponentType.BLUE, 64, StorageType.INT, true), new Component(ComponentType.ALPHA, 64, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(108, new VkFormatInfo("VK_FORMAT_R32G32B32A32_SINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, true), new Component(ComponentType.GREEN, 32, StorageType.INT, true), new Component(ComponentType.BLUE, 32, StorageType.INT, true), new Component(ComponentType.ALPHA, 32, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(44, new VkFormatInfo("VK_FORMAT_B8G8R8A8_UNORM", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.ALPHA, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(37, new VkFormatInfo("VK_FORMAT_R8G8B8A8_UNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.ALPHA, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(95, new VkFormatInfo("VK_FORMAT_R16G16B16A16_UINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, false), new Component(ComponentType.GREEN, 16, StorageType.INT, false), new Component(ComponentType.BLUE, 16, StorageType.INT, false), new Component(ComponentType.ALPHA, 16, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(119, new VkFormatInfo("VK_FORMAT_R64G64B64A64_UINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, false), new Component(ComponentType.GREEN, 64, StorageType.INT, false), new Component(ComponentType.BLUE, 64, StorageType.INT, false), new Component(ComponentType.ALPHA, 64, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(107, new VkFormatInfo("VK_FORMAT_R32G32B32A32_UINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, false), new Component(ComponentType.GREEN, 32, StorageType.INT, false), new Component(ComponentType.BLUE, 32, StorageType.INT, false), new Component(ComponentType.ALPHA, 32, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(92, new VkFormatInfo("VK_FORMAT_R16G16B16A16_SNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, true), new Component(ComponentType.GREEN, 16, StorageType.NORM, true), new Component(ComponentType.BLUE, 16, StorageType.NORM, true), new Component(ComponentType.ALPHA, 16, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(57, new VkFormatInfo("VK_FORMAT_A8B8G8R8_SRGB_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.RED, 8, StorageType.RGB, true)), Collections.singletonList(32), null, false, null));
		INFO.put(56, new VkFormatInfo("VK_FORMAT_A8B8G8R8_SINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.INT, true), new Component(ComponentType.BLUE, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.RED, 8, StorageType.INT, true)), Collections.singletonList(32), null, false, null));
		INFO.put(47, new VkFormatInfo("VK_FORMAT_B8G8R8A8_SSCALED", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.RED, 8, StorageType.SCALED, true), new Component(ComponentType.ALPHA, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(40, new VkFormatInfo("VK_FORMAT_R8G8B8A8_SSCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.BLUE, 8, StorageType.SCALED, true), new Component(ComponentType.ALPHA, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(91, new VkFormatInfo("VK_FORMAT_R16G16B16A16_UNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, false), new Component(ComponentType.GREEN, 16, StorageType.NORM, false), new Component(ComponentType.BLUE, 16, StorageType.NORM, false), new Component(ComponentType.ALPHA, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(55, new VkFormatInfo("VK_FORMAT_A8B8G8R8_UINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.INT, false), new Component(ComponentType.BLUE, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.RED, 8, StorageType.INT, false)), Collections.singletonList(32), null, false, null));
		INFO.put(97, new VkFormatInfo("VK_FORMAT_R16G16B16A16_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 16, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 16, StorageType.FLOAT, true), new Component(ComponentType.ALPHA, 16, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(52, new VkFormatInfo("VK_FORMAT_A8B8G8R8_SNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.NORM, true), new Component(ComponentType.BLUE, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.RED, 8, StorageType.NORM, true)), Collections.singletonList(32), null, false, null));
		INFO.put(121, new VkFormatInfo("VK_FORMAT_R64G64B64A64_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 64, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 64, StorageType.FLOAT, true), new Component(ComponentType.ALPHA, 64, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(109, new VkFormatInfo("VK_FORMAT_R32G32B32A32_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 32, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 32, StorageType.FLOAT, true), new Component(ComponentType.ALPHA, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(69, new VkFormatInfo("VK_FORMAT_A2B10G10R10_SINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.INT, true), new Component(ComponentType.BLUE, 10, StorageType.INT, true), new Component(ComponentType.GREEN, 10, StorageType.INT, true), new Component(ComponentType.RED, 10, StorageType.INT, true)), Collections.singletonList(32), null, false, null));
		INFO.put(63, new VkFormatInfo("VK_FORMAT_A2R10G10B10_SINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.INT, true), new Component(ComponentType.RED, 10, StorageType.INT, true), new Component(ComponentType.GREEN, 10, StorageType.INT, true), new Component(ComponentType.BLUE, 10, StorageType.INT, true)), Collections.singletonList(32), null, false, null));
		INFO.put(46, new VkFormatInfo("VK_FORMAT_B8G8R8A8_USCALED", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.RED, 8, StorageType.SCALED, false), new Component(ComponentType.ALPHA, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(39, new VkFormatInfo("VK_FORMAT_R8G8B8A8_USCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.BLUE, 8, StorageType.SCALED, false), new Component(ComponentType.ALPHA, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(8, new VkFormatInfo("VK_FORMAT_A1R5G5B5_UNORM_PACK16", Arrays.asList(new Component(ComponentType.ALPHA, 1, StorageType.NORM, false), new Component(ComponentType.RED, 5, StorageType.NORM, false), new Component(ComponentType.GREEN, 5, StorageType.NORM, false), new Component(ComponentType.BLUE, 5, StorageType.NORM, false)), Collections.singletonList(16), null, false, null));
		INFO.put(7, new VkFormatInfo("VK_FORMAT_B5G5R5A1_UNORM_PACK16", Arrays.asList(new Component(ComponentType.BLUE, 5, StorageType.NORM, false), new Component(ComponentType.GREEN, 5, StorageType.NORM, false), new Component(ComponentType.RED, 5, StorageType.NORM, false), new Component(ComponentType.ALPHA, 1, StorageType.NORM, false)), Collections.singletonList(16), null, false, null));
		INFO.put(6, new VkFormatInfo("VK_FORMAT_R5G5B5A1_UNORM_PACK16", Arrays.asList(new Component(ComponentType.RED, 5, StorageType.NORM, false), new Component(ComponentType.GREEN, 5, StorageType.NORM, false), new Component(ComponentType.BLUE, 5, StorageType.NORM, false), new Component(ComponentType.ALPHA, 1, StorageType.NORM, false)), Collections.singletonList(16), null, false, null));
		INFO.put(3, new VkFormatInfo("VK_FORMAT_B4G4R4A4_UNORM_PACK16", Arrays.asList(new Component(ComponentType.BLUE, 4, StorageType.NORM, false), new Component(ComponentType.GREEN, 4, StorageType.NORM, false), new Component(ComponentType.RED, 4, StorageType.NORM, false), new Component(ComponentType.ALPHA, 4, StorageType.NORM, false)), Collections.singletonList(16), null, false, null));
		INFO.put(2, new VkFormatInfo("VK_FORMAT_R4G4B4A4_UNORM_PACK16", Arrays.asList(new Component(ComponentType.RED, 4, StorageType.NORM, false), new Component(ComponentType.GREEN, 4, StorageType.NORM, false), new Component(ComponentType.BLUE, 4, StorageType.NORM, false), new Component(ComponentType.ALPHA, 4, StorageType.NORM, false)), Collections.singletonList(16), null, false, null));
		INFO.put(51, new VkFormatInfo("VK_FORMAT_A8B8G8R8_UNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.RED, 8, StorageType.NORM, false)), Collections.singletonList(32), null, false, null));
		INFO.put(94, new VkFormatInfo("VK_FORMAT_R16G16B16A16_SSCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, true), new Component(ComponentType.GREEN, 16, StorageType.SCALED, true), new Component(ComponentType.BLUE, 16, StorageType.SCALED, true), new Component(ComponentType.ALPHA, 16, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(68, new VkFormatInfo("VK_FORMAT_A2B10G10R10_UINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.INT, false), new Component(ComponentType.BLUE, 10, StorageType.INT, false), new Component(ComponentType.GREEN, 10, StorageType.INT, false), new Component(ComponentType.RED, 10, StorageType.INT, false)), Collections.singletonList(32), null, false, null));
		INFO.put(62, new VkFormatInfo("VK_FORMAT_A2R10G10B10_UINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.INT, false), new Component(ComponentType.RED, 10, StorageType.INT, false), new Component(ComponentType.GREEN, 10, StorageType.INT, false), new Component(ComponentType.BLUE, 10, StorageType.INT, false)), Collections.singletonList(32), null, false, null));
		INFO.put(65, new VkFormatInfo("VK_FORMAT_A2B10G10R10_SNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.NORM, true), new Component(ComponentType.BLUE, 10, StorageType.NORM, true), new Component(ComponentType.GREEN, 10, StorageType.NORM, true), new Component(ComponentType.RED, 10, StorageType.NORM, true)), Collections.singletonList(32), null, false, null));
		INFO.put(59, new VkFormatInfo("VK_FORMAT_A2R10G10B10_SNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.NORM, true), new Component(ComponentType.RED, 10, StorageType.NORM, true), new Component(ComponentType.GREEN, 10, StorageType.NORM, true), new Component(ComponentType.BLUE, 10, StorageType.NORM, true)), Collections.singletonList(32), null, false, null));
		INFO.put(93, new VkFormatInfo("VK_FORMAT_R16G16B16A16_USCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, false), new Component(ComponentType.GREEN, 16, StorageType.SCALED, false), new Component(ComponentType.BLUE, 16, StorageType.SCALED, false), new Component(ComponentType.ALPHA, 16, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(64, new VkFormatInfo("VK_FORMAT_A2B10G10R10_UNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.NORM, false), new Component(ComponentType.BLUE, 10, StorageType.NORM, false), new Component(ComponentType.GREEN, 10, StorageType.NORM, false), new Component(ComponentType.RED, 10, StorageType.NORM, false)), Collections.singletonList(32), null, false, null));
		INFO.put(58, new VkFormatInfo("VK_FORMAT_A2R10G10B10_UNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.NORM, false), new Component(ComponentType.RED, 10, StorageType.NORM, false), new Component(ComponentType.GREEN, 10, StorageType.NORM, false), new Component(ComponentType.BLUE, 10, StorageType.NORM, false)), Collections.singletonList(32), null, false, null));
		INFO.put(54, new VkFormatInfo("VK_FORMAT_A8B8G8R8_SSCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.SCALED, true), new Component(ComponentType.BLUE, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.RED, 8, StorageType.SCALED, true)), Collections.singletonList(32), null, false, null));
		INFO.put(152, new VkFormatInfo("VK_FORMAT_ETC2_R8G8B8A8_SRGB_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.ALPHA, 8, StorageType.RGB, true)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(150, new VkFormatInfo("VK_FORMAT_ETC2_R8G8B8A1_SRGB_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.ALPHA, 1, StorageType.RGB, true)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(53, new VkFormatInfo("VK_FORMAT_A8B8G8R8_USCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.SCALED, false), new Component(ComponentType.BLUE, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.RED, 8, StorageType.SCALED, false)), Collections.singletonList(32), null, false, null));
		INFO.put(67, new VkFormatInfo("VK_FORMAT_A2B10G10R10_SSCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.SCALED, true), new Component(ComponentType.BLUE, 10, StorageType.SCALED, true), new Component(ComponentType.GREEN, 10, StorageType.SCALED, true), new Component(ComponentType.RED, 10, StorageType.SCALED, true)), Collections.singletonList(32), null, false, null));
		INFO.put(61, new VkFormatInfo("VK_FORMAT_A2R10G10B10_SSCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.SCALED, true), new Component(ComponentType.RED, 10, StorageType.SCALED, true), new Component(ComponentType.GREEN, 10, StorageType.SCALED, true), new Component(ComponentType.BLUE, 10, StorageType.SCALED, true)), Collections.singletonList(32), null, false, null));
		INFO.put(151, new VkFormatInfo("VK_FORMAT_ETC2_R8G8B8A8_UNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.ALPHA, 8, StorageType.NORM, false)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(149, new VkFormatInfo("VK_FORMAT_ETC2_R8G8B8A1_UNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.ALPHA, 1, StorageType.NORM, false)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(123, new VkFormatInfo("VK_FORMAT_E5B9G9R9_UFLOAT_PACK32", Arrays.asList(new Component(ComponentType.SHARED_EXPONENT, 5, StorageType.FLOAT, false), new Component(ComponentType.BLUE, 9, StorageType.FLOAT, false), new Component(ComponentType.GREEN, 9, StorageType.FLOAT, false), new Component(ComponentType.RED, 9, StorageType.FLOAT, false)), Collections.singletonList(32), null, false, null));
		INFO.put(66, new VkFormatInfo("VK_FORMAT_A2B10G10R10_USCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.SCALED, false), new Component(ComponentType.BLUE, 10, StorageType.SCALED, false), new Component(ComponentType.GREEN, 10, StorageType.SCALED, false), new Component(ComponentType.RED, 10, StorageType.SCALED, false)), Collections.singletonList(32), null, false, null));
		INFO.put(60, new VkFormatInfo("VK_FORMAT_A2R10G10B10_USCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.SCALED, false), new Component(ComponentType.RED, 10, StorageType.SCALED, false), new Component(ComponentType.GREEN, 10, StorageType.SCALED, false), new Component(ComponentType.BLUE, 10, StorageType.SCALED, false)), Collections.singletonList(32), null, false, null));
		
	}
	
}
