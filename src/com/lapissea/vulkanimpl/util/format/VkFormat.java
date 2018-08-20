package com.lapissea.vulkanimpl.util.format;

import com.lapissea.util.UtilL;
import com.lapissea.vec.Vec2iFinal;
import com.lapissea.vec.interf.IVec2iR;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class VkFormat{
	
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
		
		public int byteSite(){
			return bitSize/Byte.SIZE;
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
	
	
	public final int             handle;
	public final String          name;
	public final List<Component> components;
	public final List<Integer>   packSizes;
	public final String          compression;
	public final boolean         isBlock;
	public final IVec2iR         blockSize;
	public final int             totalByteSize;
	
	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		if(!(o instanceof VkFormat)) return false;
		return handle==((VkFormat)o).handle;
	}
	
	@Override
	public int hashCode(){
		return handle;
	}
	
	private VKFormatWriter writer;
	
	public VkFormat(int handle, String name, List<Component> components, List<Integer> packSizes, String compression, boolean isBlock, IVec2iR blockSize){
		this.handle=handle;
		this.name=name;
		this.components=Collections.unmodifiableList(components);
		this.packSizes=Collections.unmodifiableList(packSizes);
		this.compression=compression;
		this.isBlock=isBlock;
		this.blockSize=blockSize;
		
		totalByteSize=(packSizes.isEmpty()?components.stream().mapToInt(c->c.bitSize):packSizes.stream().mapToInt(c->c)).sum()/Byte.SIZE;
	}
	
	public VKFormatWriter getWriter(){
		if(writer==null&&(writer=VKFormatWriter.get(this))==null) UtilL.uncheckedThrow(new UnsupportedEncodingException("Unable to detect writer for: "+name));
		return writer;
	}
	
	public boolean hasComponentType(ComponentType type){
		for(Component component : components){
			if(component.type==type) return true;
		}
		return false;
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
	
	private static class Map extends TIntObjectHashMap<VkFormat>{
		Object[] valuesDirect(){
			return _values;
		}
	}
	
	private static final Map INFO=new Map();
	
	public static VkFormat get(int format){
		return INFO.get(format);
	}
	
	public static Stream<VkFormat> stream(){
		return Stream.of(INFO.valuesDirect()).limit(INFO.size()).map(o->(VkFormat)o);
	}
	
	static{
//		if(DEV_ON) VkFormatAnalysis.generateCode();
		
		
		List<Component> EC=Collections.emptyList();
		List<Integer>   EP=Collections.emptyList();
		INFO.put(0, new VkFormat(0, "VK_FORMAT_UNDEFINED", EC, EP, null, false, null));
		INFO.put(146, new VkFormat(146, "VK_FORMAT_BC7_SRGB_BLOCK", EC, EP, "BC7", true, new Vec2iFinal(4, 4)));
		INFO.put(136, new VkFormat(136, "VK_FORMAT_BC2_SRGB_BLOCK", EC, EP, "BC2", true, new Vec2iFinal(4, 4)));
		INFO.put(138, new VkFormat(138, "VK_FORMAT_BC3_SRGB_BLOCK", EC, EP, "BC3", true, new Vec2iFinal(4, 4)));
		INFO.put(145, new VkFormat(145, "VK_FORMAT_BC7_UNORM_BLOCK", EC, EP, "BC7", true, new Vec2iFinal(4, 4)));
		INFO.put(135, new VkFormat(135, "VK_FORMAT_BC2_UNORM_BLOCK", EC, EP, "BC2", true, new Vec2iFinal(4, 4)));
		INFO.put(137, new VkFormat(137, "VK_FORMAT_BC3_UNORM_BLOCK", EC, EP, "BC3", true, new Vec2iFinal(4, 4)));
		INFO.put(139, new VkFormat(139, "VK_FORMAT_BC4_UNORM_BLOCK", EC, EP, "BC4", true, new Vec2iFinal(4, 4)));
		INFO.put(140, new VkFormat(140, "VK_FORMAT_BC4_SNORM_BLOCK", EC, EP, "BC4", true, new Vec2iFinal(4, 4)));
		INFO.put(141, new VkFormat(141, "VK_FORMAT_BC5_UNORM_BLOCK", EC, EP, "BC5", true, new Vec2iFinal(4, 4)));
		INFO.put(142, new VkFormat(142, "VK_FORMAT_BC5_SNORM_BLOCK", EC, EP, "BC5", true, new Vec2iFinal(4, 4)));
		INFO.put(143, new VkFormat(143, "VK_FORMAT_BC6H_UFLOAT_BLOCK", EC, EP, "BC6H", true, new Vec2iFinal(4, 4)));
		INFO.put(144, new VkFormat(144, "VK_FORMAT_BC6H_SFLOAT_BLOCK", EC, EP, "BC6H", true, new Vec2iFinal(4, 4)));
		INFO.put(132, new VkFormat(132, "VK_FORMAT_BC1_RGB_SRGB_BLOCK", EC, EP, "BC1", true, new Vec2iFinal(4, 4)));
		INFO.put(134, new VkFormat(134, "VK_FORMAT_BC1_RGBA_SRGB_BLOCK", EC, EP, "BC1", true, new Vec2iFinal(4, 4)));
		INFO.put(131, new VkFormat(131, "VK_FORMAT_BC1_RGB_UNORM_BLOCK", EC, EP, "BC1", true, new Vec2iFinal(4, 4)));
		INFO.put(168, new VkFormat(168, "VK_FORMAT_ASTC_8x5_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 5)));
		INFO.put(170, new VkFormat(170, "VK_FORMAT_ASTC_8x6_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 6)));
		INFO.put(158, new VkFormat(158, "VK_FORMAT_ASTC_4x4_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(4, 4)));
		INFO.put(160, new VkFormat(160, "VK_FORMAT_ASTC_5x4_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(5, 4)));
		INFO.put(172, new VkFormat(172, "VK_FORMAT_ASTC_8x8_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 8)));
		INFO.put(162, new VkFormat(162, "VK_FORMAT_ASTC_5x5_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(5, 5)));
		INFO.put(164, new VkFormat(164, "VK_FORMAT_ASTC_6x5_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(6, 5)));
		INFO.put(166, new VkFormat(166, "VK_FORMAT_ASTC_6x6_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(6, 6)));
		INFO.put(133, new VkFormat(133, "VK_FORMAT_BC1_RGBA_UNORM_BLOCK", EC, EP, "BC1", true, new Vec2iFinal(4, 4)));
		INFO.put(167, new VkFormat(167, "VK_FORMAT_ASTC_8x5_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 5)));
		INFO.put(169, new VkFormat(169, "VK_FORMAT_ASTC_8x6_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 6)));
		INFO.put(157, new VkFormat(157, "VK_FORMAT_ASTC_4x4_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(4, 4)));
		INFO.put(159, new VkFormat(159, "VK_FORMAT_ASTC_5x4_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(5, 4)));
		INFO.put(171, new VkFormat(171, "VK_FORMAT_ASTC_8x8_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(8, 8)));
		INFO.put(161, new VkFormat(161, "VK_FORMAT_ASTC_5x5_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(5, 5)));
		INFO.put(163, new VkFormat(163, "VK_FORMAT_ASTC_6x5_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(6, 5)));
		INFO.put(165, new VkFormat(165, "VK_FORMAT_ASTC_6x6_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(6, 6)));
		INFO.put(174, new VkFormat(174, "VK_FORMAT_ASTC_10x5_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 5)));
		INFO.put(176, new VkFormat(176, "VK_FORMAT_ASTC_10x6_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 6)));
		INFO.put(178, new VkFormat(178, "VK_FORMAT_ASTC_10x8_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 8)));
		INFO.put(173, new VkFormat(173, "VK_FORMAT_ASTC_10x5_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 5)));
		INFO.put(175, new VkFormat(175, "VK_FORMAT_ASTC_10x6_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 6)));
		INFO.put(177, new VkFormat(177, "VK_FORMAT_ASTC_10x8_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 8)));
		INFO.put(180, new VkFormat(180, "VK_FORMAT_ASTC_10x10_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 10)));
		INFO.put(182, new VkFormat(182, "VK_FORMAT_ASTC_12x10_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(12, 10)));
		INFO.put(184, new VkFormat(184, "VK_FORMAT_ASTC_12x12_SRGB_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(12, 12)));
		INFO.put(179, new VkFormat(179, "VK_FORMAT_ASTC_10x10_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(10, 10)));
		INFO.put(181, new VkFormat(181, "VK_FORMAT_ASTC_12x10_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(12, 10)));
		INFO.put(183, new VkFormat(183, "VK_FORMAT_ASTC_12x12_UNORM_BLOCK", EC, EP, "ASTC", true, new Vec2iFinal(12, 12)));
		INFO.put(14, new VkFormat(14, "VK_FORMAT_R8_SINT", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(15, new VkFormat(15, "VK_FORMAT_R8_SRGB", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(13, new VkFormat(13, "VK_FORMAT_R8_UINT", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(9, new VkFormat(9, "VK_FORMAT_R8_UNORM", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(10, new VkFormat(10, "VK_FORMAT_R8_SNORM", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(75, new VkFormat(75, "VK_FORMAT_R16_SINT", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(99, new VkFormat(99, "VK_FORMAT_R32_SINT", Collections.singletonList(new Component(ComponentType.RED, 32, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(74, new VkFormat(74, "VK_FORMAT_R16_UINT", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(98, new VkFormat(98, "VK_FORMAT_R32_UINT", Collections.singletonList(new Component(ComponentType.RED, 32, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(111, new VkFormat(111, "VK_FORMAT_R64_SINT", Collections.singletonList(new Component(ComponentType.RED, 64, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(71, new VkFormat(71, "VK_FORMAT_R16_SNORM", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(110, new VkFormat(110, "VK_FORMAT_R64_UINT", Collections.singletonList(new Component(ComponentType.RED, 64, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(70, new VkFormat(70, "VK_FORMAT_R16_UNORM", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(12, new VkFormat(12, "VK_FORMAT_R8_SSCALED", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(76, new VkFormat(76, "VK_FORMAT_R16_SFLOAT", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(11, new VkFormat(11, "VK_FORMAT_R8_USCALED", Collections.singletonList(new Component(ComponentType.RED, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(127, new VkFormat(127, "VK_FORMAT_S8_UINT", Collections.singletonList(new Component(ComponentType.STENCIL, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(112, new VkFormat(112, "VK_FORMAT_R64_SFLOAT", Collections.singletonList(new Component(ComponentType.RED, 64, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(73, new VkFormat(73, "VK_FORMAT_R16_SSCALED", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(100, new VkFormat(100, "VK_FORMAT_R32_SFLOAT", Collections.singletonList(new Component(ComponentType.RED, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(124, new VkFormat(124, "VK_FORMAT_D16_UNORM", Collections.singletonList(new Component(ComponentType.DEPTH, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(72, new VkFormat(72, "VK_FORMAT_R16_USCALED", Collections.singletonList(new Component(ComponentType.RED, 16, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(126, new VkFormat(126, "VK_FORMAT_D32_SFLOAT", Collections.singletonList(new Component(ComponentType.DEPTH, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(154, new VkFormat(154, "VK_FORMAT_EAC_R11_SNORM_BLOCK", Collections.singletonList(new Component(ComponentType.RED, 11, StorageType.NORM, true)), Collections.emptyList(), "EAC", true, new Vec2iFinal(4, 4)));
		INFO.put(153, new VkFormat(153, "VK_FORMAT_EAC_R11_UNORM_BLOCK", Collections.singletonList(new Component(ComponentType.RED, 11, StorageType.NORM, false)), Collections.emptyList(), "EAC", true, new Vec2iFinal(4, 4)));
		INFO.put(21, new VkFormat(21, "VK_FORMAT_R8G8_SINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(22, new VkFormat(22, "VK_FORMAT_R8G8_SRGB", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(20, new VkFormat(20, "VK_FORMAT_R8G8_UINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(17, new VkFormat(17, "VK_FORMAT_R8G8_SNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(82, new VkFormat(82, "VK_FORMAT_R16G16_SINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, true), new Component(ComponentType.GREEN, 16, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(16, new VkFormat(16, "VK_FORMAT_R8G8_UNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(81, new VkFormat(81, "VK_FORMAT_R16G16_UINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, false), new Component(ComponentType.GREEN, 16, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(114, new VkFormat(114, "VK_FORMAT_R64G64_SINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, true), new Component(ComponentType.GREEN, 64, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(102, new VkFormat(102, "VK_FORMAT_R32G32_SINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, true), new Component(ComponentType.GREEN, 32, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(78, new VkFormat(78, "VK_FORMAT_R16G16_SNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, true), new Component(ComponentType.GREEN, 16, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(113, new VkFormat(113, "VK_FORMAT_R64G64_UINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, false), new Component(ComponentType.GREEN, 64, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(101, new VkFormat(101, "VK_FORMAT_R32G32_UINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, false), new Component(ComponentType.GREEN, 32, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(19, new VkFormat(19, "VK_FORMAT_R8G8_SSCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(77, new VkFormat(77, "VK_FORMAT_R16G16_UNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, false), new Component(ComponentType.GREEN, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(83, new VkFormat(83, "VK_FORMAT_R16G16_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 16, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(18, new VkFormat(18, "VK_FORMAT_R8G8_USCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(115, new VkFormat(115, "VK_FORMAT_R64G64_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 64, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(103, new VkFormat(103, "VK_FORMAT_R32G32_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(80, new VkFormat(80, "VK_FORMAT_R16G16_SSCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, true), new Component(ComponentType.GREEN, 16, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(79, new VkFormat(79, "VK_FORMAT_R16G16_USCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, false), new Component(ComponentType.GREEN, 16, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(128, new VkFormat(128, "VK_FORMAT_D16_UNORM_S8_UINT", Arrays.asList(new Component(ComponentType.DEPTH, 16, StorageType.NORM, false), new Component(ComponentType.STENCIL, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(129, new VkFormat(129, "VK_FORMAT_D24_UNORM_S8_UINT", Arrays.asList(new Component(ComponentType.DEPTH, 24, StorageType.NORM, false), new Component(ComponentType.STENCIL, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(130, new VkFormat(130, "VK_FORMAT_D32_SFLOAT_S8_UINT", Arrays.asList(new Component(ComponentType.DEPTH, 32, StorageType.FLOAT, true), new Component(ComponentType.STENCIL, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(1, new VkFormat(1, "VK_FORMAT_R4G4_UNORM_PACK8", Arrays.asList(new Component(ComponentType.RED, 4, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 4, StorageType.NORM, false, 0)), Collections.singletonList(8), null, false, null));
		INFO.put(125, new VkFormat(125, "VK_FORMAT_X8_D24_UNORM_PACK32", Arrays.asList(new Component(ComponentType.UNUSED, 8, StorageType.NORM, false, 0), new Component(ComponentType.DEPTH, 24, StorageType.NORM, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(156, new VkFormat(156, "VK_FORMAT_EAC_R11G11_SNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 11, StorageType.NORM, true), new Component(ComponentType.GREEN, 11, StorageType.NORM, true)), Collections.emptyList(), "EAC", true, new Vec2iFinal(4, 4)));
		INFO.put(155, new VkFormat(155, "VK_FORMAT_EAC_R11G11_UNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 11, StorageType.NORM, false), new Component(ComponentType.GREEN, 11, StorageType.NORM, false)), Collections.emptyList(), "EAC", true, new Vec2iFinal(4, 4)));
		INFO.put(28, new VkFormat(28, "VK_FORMAT_R8G8B8_SINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.BLUE, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(29, new VkFormat(29, "VK_FORMAT_R8G8B8_SRGB", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(35, new VkFormat(35, "VK_FORMAT_B8G8R8_SINT", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.RED, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(36, new VkFormat(36, "VK_FORMAT_B8G8R8_SRGB", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.RED, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(27, new VkFormat(27, "VK_FORMAT_R8G8B8_UINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.BLUE, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(34, new VkFormat(34, "VK_FORMAT_B8G8R8_UINT", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.RED, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(31, new VkFormat(31, "VK_FORMAT_B8G8R8_SNORM", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.RED, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(24, new VkFormat(24, "VK_FORMAT_R8G8B8_SNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.BLUE, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(89, new VkFormat(89, "VK_FORMAT_R16G16B16_SINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, true), new Component(ComponentType.GREEN, 16, StorageType.INT, true), new Component(ComponentType.BLUE, 16, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(30, new VkFormat(30, "VK_FORMAT_B8G8R8_UNORM", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.RED, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(23, new VkFormat(23, "VK_FORMAT_R8G8B8_UNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(117, new VkFormat(117, "VK_FORMAT_R64G64B64_SINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, true), new Component(ComponentType.GREEN, 64, StorageType.INT, true), new Component(ComponentType.BLUE, 64, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(105, new VkFormat(105, "VK_FORMAT_R32G32B32_SINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, true), new Component(ComponentType.GREEN, 32, StorageType.INT, true), new Component(ComponentType.BLUE, 32, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(88, new VkFormat(88, "VK_FORMAT_R16G16B16_UINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, false), new Component(ComponentType.GREEN, 16, StorageType.INT, false), new Component(ComponentType.BLUE, 16, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(85, new VkFormat(85, "VK_FORMAT_R16G16B16_SNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, true), new Component(ComponentType.GREEN, 16, StorageType.NORM, true), new Component(ComponentType.BLUE, 16, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(116, new VkFormat(116, "VK_FORMAT_R64G64B64_UINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, false), new Component(ComponentType.GREEN, 64, StorageType.INT, false), new Component(ComponentType.BLUE, 64, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(104, new VkFormat(104, "VK_FORMAT_R32G32B32_UINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, false), new Component(ComponentType.GREEN, 32, StorageType.INT, false), new Component(ComponentType.BLUE, 32, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(33, new VkFormat(33, "VK_FORMAT_B8G8R8_SSCALED", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.RED, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(26, new VkFormat(26, "VK_FORMAT_R8G8B8_SSCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.BLUE, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(84, new VkFormat(84, "VK_FORMAT_R16G16B16_UNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, false), new Component(ComponentType.GREEN, 16, StorageType.NORM, false), new Component(ComponentType.BLUE, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(90, new VkFormat(90, "VK_FORMAT_R16G16B16_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 16, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 16, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(32, new VkFormat(32, "VK_FORMAT_B8G8R8_USCALED", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.RED, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(25, new VkFormat(25, "VK_FORMAT_R8G8B8_USCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.BLUE, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(106, new VkFormat(106, "VK_FORMAT_R32G32B32_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 32, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(118, new VkFormat(118, "VK_FORMAT_R64G64B64_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 64, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 64, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(87, new VkFormat(87, "VK_FORMAT_R16G16B16_SSCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, true), new Component(ComponentType.GREEN, 16, StorageType.SCALED, true), new Component(ComponentType.BLUE, 16, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(86, new VkFormat(86, "VK_FORMAT_R16G16B16_USCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, false), new Component(ComponentType.GREEN, 16, StorageType.SCALED, false), new Component(ComponentType.BLUE, 16, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(5, new VkFormat(5, "VK_FORMAT_B5G6R5_UNORM_PACK16", Arrays.asList(new Component(ComponentType.BLUE, 5, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 6, StorageType.NORM, false, 0), new Component(ComponentType.RED, 5, StorageType.NORM, false, 0)), Collections.singletonList(16), null, false, null));
		INFO.put(4, new VkFormat(4, "VK_FORMAT_R5G6B5_UNORM_PACK16", Arrays.asList(new Component(ComponentType.RED, 5, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 6, StorageType.NORM, false, 0), new Component(ComponentType.BLUE, 5, StorageType.NORM, false, 0)), Collections.singletonList(16), null, false, null));
		INFO.put(148, new VkFormat(148, "VK_FORMAT_ETC2_R8G8B8_SRGB_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(147, new VkFormat(147, "VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(122, new VkFormat(122, "VK_FORMAT_B10G11R11_UFLOAT_PACK32", Arrays.asList(new Component(ComponentType.BLUE, 10, StorageType.FLOAT, false, 0), new Component(ComponentType.GREEN, 11, StorageType.FLOAT, false, 0), new Component(ComponentType.RED, 11, StorageType.FLOAT, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(42, new VkFormat(42, "VK_FORMAT_R8G8B8A8_SINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.BLUE, 8, StorageType.INT, true), new Component(ComponentType.ALPHA, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(43, new VkFormat(43, "VK_FORMAT_R8G8B8A8_SRGB", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.ALPHA, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(49, new VkFormat(49, "VK_FORMAT_B8G8R8A8_SINT", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.INT, true), new Component(ComponentType.GREEN, 8, StorageType.INT, true), new Component(ComponentType.RED, 8, StorageType.INT, true), new Component(ComponentType.ALPHA, 8, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(50, new VkFormat(50, "VK_FORMAT_B8G8R8A8_SRGB", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.ALPHA, 8, StorageType.RGB, true)), Collections.emptyList(), null, false, null));
		INFO.put(48, new VkFormat(48, "VK_FORMAT_B8G8R8A8_UINT", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.RED, 8, StorageType.INT, false), new Component(ComponentType.ALPHA, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(41, new VkFormat(41, "VK_FORMAT_R8G8B8A8_UINT", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.INT, false), new Component(ComponentType.GREEN, 8, StorageType.INT, false), new Component(ComponentType.BLUE, 8, StorageType.INT, false), new Component(ComponentType.ALPHA, 8, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(38, new VkFormat(38, "VK_FORMAT_R8G8B8A8_SNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.BLUE, 8, StorageType.NORM, true), new Component(ComponentType.ALPHA, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(45, new VkFormat(45, "VK_FORMAT_B8G8R8A8_SNORM", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.NORM, true), new Component(ComponentType.GREEN, 8, StorageType.NORM, true), new Component(ComponentType.RED, 8, StorageType.NORM, true), new Component(ComponentType.ALPHA, 8, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(96, new VkFormat(96, "VK_FORMAT_R16G16B16A16_SINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, true), new Component(ComponentType.GREEN, 16, StorageType.INT, true), new Component(ComponentType.BLUE, 16, StorageType.INT, true), new Component(ComponentType.ALPHA, 16, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(37, new VkFormat(37, "VK_FORMAT_R8G8B8A8_UNORM", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.ALPHA, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(44, new VkFormat(44, "VK_FORMAT_B8G8R8A8_UNORM", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.ALPHA, 8, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(120, new VkFormat(120, "VK_FORMAT_R64G64B64A64_SINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, true), new Component(ComponentType.GREEN, 64, StorageType.INT, true), new Component(ComponentType.BLUE, 64, StorageType.INT, true), new Component(ComponentType.ALPHA, 64, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(108, new VkFormat(108, "VK_FORMAT_R32G32B32A32_SINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, true), new Component(ComponentType.GREEN, 32, StorageType.INT, true), new Component(ComponentType.BLUE, 32, StorageType.INT, true), new Component(ComponentType.ALPHA, 32, StorageType.INT, true)), Collections.emptyList(), null, false, null));
		INFO.put(95, new VkFormat(95, "VK_FORMAT_R16G16B16A16_UINT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.INT, false), new Component(ComponentType.GREEN, 16, StorageType.INT, false), new Component(ComponentType.BLUE, 16, StorageType.INT, false), new Component(ComponentType.ALPHA, 16, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(92, new VkFormat(92, "VK_FORMAT_R16G16B16A16_SNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, true), new Component(ComponentType.GREEN, 16, StorageType.NORM, true), new Component(ComponentType.BLUE, 16, StorageType.NORM, true), new Component(ComponentType.ALPHA, 16, StorageType.NORM, true)), Collections.emptyList(), null, false, null));
		INFO.put(107, new VkFormat(107, "VK_FORMAT_R32G32B32A32_UINT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.INT, false), new Component(ComponentType.GREEN, 32, StorageType.INT, false), new Component(ComponentType.BLUE, 32, StorageType.INT, false), new Component(ComponentType.ALPHA, 32, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(119, new VkFormat(119, "VK_FORMAT_R64G64B64A64_UINT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.INT, false), new Component(ComponentType.GREEN, 64, StorageType.INT, false), new Component(ComponentType.BLUE, 64, StorageType.INT, false), new Component(ComponentType.ALPHA, 64, StorageType.INT, false)), Collections.emptyList(), null, false, null));
		INFO.put(40, new VkFormat(40, "VK_FORMAT_R8G8B8A8_SSCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.BLUE, 8, StorageType.SCALED, true), new Component(ComponentType.ALPHA, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(47, new VkFormat(47, "VK_FORMAT_B8G8R8A8_SSCALED", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.SCALED, true), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true), new Component(ComponentType.RED, 8, StorageType.SCALED, true), new Component(ComponentType.ALPHA, 8, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(91, new VkFormat(91, "VK_FORMAT_R16G16B16A16_UNORM", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.NORM, false), new Component(ComponentType.GREEN, 16, StorageType.NORM, false), new Component(ComponentType.BLUE, 16, StorageType.NORM, false), new Component(ComponentType.ALPHA, 16, StorageType.NORM, false)), Collections.emptyList(), null, false, null));
		INFO.put(97, new VkFormat(97, "VK_FORMAT_R16G16B16A16_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 16, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 16, StorageType.FLOAT, true), new Component(ComponentType.ALPHA, 16, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(46, new VkFormat(46, "VK_FORMAT_B8G8R8A8_USCALED", Arrays.asList(new Component(ComponentType.BLUE, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.RED, 8, StorageType.SCALED, false), new Component(ComponentType.ALPHA, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(39, new VkFormat(39, "VK_FORMAT_R8G8B8A8_USCALED", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.SCALED, false), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false), new Component(ComponentType.BLUE, 8, StorageType.SCALED, false), new Component(ComponentType.ALPHA, 8, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(121, new VkFormat(121, "VK_FORMAT_R64G64B64A64_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 64, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 64, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 64, StorageType.FLOAT, true), new Component(ComponentType.ALPHA, 64, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(109, new VkFormat(109, "VK_FORMAT_R32G32B32A32_SFLOAT", Arrays.asList(new Component(ComponentType.RED, 32, StorageType.FLOAT, true), new Component(ComponentType.GREEN, 32, StorageType.FLOAT, true), new Component(ComponentType.BLUE, 32, StorageType.FLOAT, true), new Component(ComponentType.ALPHA, 32, StorageType.FLOAT, true)), Collections.emptyList(), null, false, null));
		INFO.put(94, new VkFormat(94, "VK_FORMAT_R16G16B16A16_SSCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, true), new Component(ComponentType.GREEN, 16, StorageType.SCALED, true), new Component(ComponentType.BLUE, 16, StorageType.SCALED, true), new Component(ComponentType.ALPHA, 16, StorageType.SCALED, true)), Collections.emptyList(), null, false, null));
		INFO.put(57, new VkFormat(57, "VK_FORMAT_A8B8G8R8_SRGB_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.RGB, true, 0), new Component(ComponentType.BLUE, 8, StorageType.RGB, true, 0), new Component(ComponentType.GREEN, 8, StorageType.RGB, true, 0), new Component(ComponentType.RED, 8, StorageType.RGB, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(56, new VkFormat(56, "VK_FORMAT_A8B8G8R8_SINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.INT, true, 0), new Component(ComponentType.BLUE, 8, StorageType.INT, true, 0), new Component(ComponentType.GREEN, 8, StorageType.INT, true, 0), new Component(ComponentType.RED, 8, StorageType.INT, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(93, new VkFormat(93, "VK_FORMAT_R16G16B16A16_USCALED", Arrays.asList(new Component(ComponentType.RED, 16, StorageType.SCALED, false), new Component(ComponentType.GREEN, 16, StorageType.SCALED, false), new Component(ComponentType.BLUE, 16, StorageType.SCALED, false), new Component(ComponentType.ALPHA, 16, StorageType.SCALED, false)), Collections.emptyList(), null, false, null));
		INFO.put(55, new VkFormat(55, "VK_FORMAT_A8B8G8R8_UINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.INT, false, 0), new Component(ComponentType.BLUE, 8, StorageType.INT, false, 0), new Component(ComponentType.GREEN, 8, StorageType.INT, false, 0), new Component(ComponentType.RED, 8, StorageType.INT, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(150, new VkFormat(150, "VK_FORMAT_ETC2_R8G8B8A1_SRGB_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.ALPHA, 1, StorageType.RGB, true)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(152, new VkFormat(152, "VK_FORMAT_ETC2_R8G8B8A8_SRGB_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.RGB, true), new Component(ComponentType.GREEN, 8, StorageType.RGB, true), new Component(ComponentType.BLUE, 8, StorageType.RGB, true), new Component(ComponentType.ALPHA, 8, StorageType.RGB, true)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(52, new VkFormat(52, "VK_FORMAT_A8B8G8R8_SNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.NORM, true, 0), new Component(ComponentType.BLUE, 8, StorageType.NORM, true, 0), new Component(ComponentType.GREEN, 8, StorageType.NORM, true, 0), new Component(ComponentType.RED, 8, StorageType.NORM, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(69, new VkFormat(69, "VK_FORMAT_A2B10G10R10_SINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.INT, true, 0), new Component(ComponentType.BLUE, 10, StorageType.INT, true, 0), new Component(ComponentType.GREEN, 10, StorageType.INT, true, 0), new Component(ComponentType.RED, 10, StorageType.INT, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(63, new VkFormat(63, "VK_FORMAT_A2R10G10B10_SINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.INT, true, 0), new Component(ComponentType.RED, 10, StorageType.INT, true, 0), new Component(ComponentType.GREEN, 10, StorageType.INT, true, 0), new Component(ComponentType.BLUE, 10, StorageType.INT, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(6, new VkFormat(6, "VK_FORMAT_R5G5B5A1_UNORM_PACK16", Arrays.asList(new Component(ComponentType.RED, 5, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 5, StorageType.NORM, false, 0), new Component(ComponentType.BLUE, 5, StorageType.NORM, false, 0), new Component(ComponentType.ALPHA, 1, StorageType.NORM, false, 0)), Collections.singletonList(16), null, false, null));
		INFO.put(7, new VkFormat(7, "VK_FORMAT_B5G5R5A1_UNORM_PACK16", Arrays.asList(new Component(ComponentType.BLUE, 5, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 5, StorageType.NORM, false, 0), new Component(ComponentType.RED, 5, StorageType.NORM, false, 0), new Component(ComponentType.ALPHA, 1, StorageType.NORM, false, 0)), Collections.singletonList(16), null, false, null));
		INFO.put(8, new VkFormat(8, "VK_FORMAT_A1R5G5B5_UNORM_PACK16", Arrays.asList(new Component(ComponentType.ALPHA, 1, StorageType.NORM, false, 0), new Component(ComponentType.RED, 5, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 5, StorageType.NORM, false, 0), new Component(ComponentType.BLUE, 5, StorageType.NORM, false, 0)), Collections.singletonList(16), null, false, null));
		INFO.put(2, new VkFormat(2, "VK_FORMAT_R4G4B4A4_UNORM_PACK16", Arrays.asList(new Component(ComponentType.RED, 4, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 4, StorageType.NORM, false, 0), new Component(ComponentType.BLUE, 4, StorageType.NORM, false, 0), new Component(ComponentType.ALPHA, 4, StorageType.NORM, false, 0)), Collections.singletonList(16), null, false, null));
		INFO.put(3, new VkFormat(3, "VK_FORMAT_B4G4R4A4_UNORM_PACK16", Arrays.asList(new Component(ComponentType.BLUE, 4, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 4, StorageType.NORM, false, 0), new Component(ComponentType.RED, 4, StorageType.NORM, false, 0), new Component(ComponentType.ALPHA, 4, StorageType.NORM, false, 0)), Collections.singletonList(16), null, false, null));
		INFO.put(51, new VkFormat(51, "VK_FORMAT_A8B8G8R8_UNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.NORM, false, 0), new Component(ComponentType.BLUE, 8, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 8, StorageType.NORM, false, 0), new Component(ComponentType.RED, 8, StorageType.NORM, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(68, new VkFormat(68, "VK_FORMAT_A2B10G10R10_UINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.INT, false, 0), new Component(ComponentType.BLUE, 10, StorageType.INT, false, 0), new Component(ComponentType.GREEN, 10, StorageType.INT, false, 0), new Component(ComponentType.RED, 10, StorageType.INT, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(62, new VkFormat(62, "VK_FORMAT_A2R10G10B10_UINT_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.INT, false, 0), new Component(ComponentType.RED, 10, StorageType.INT, false, 0), new Component(ComponentType.GREEN, 10, StorageType.INT, false, 0), new Component(ComponentType.BLUE, 10, StorageType.INT, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(65, new VkFormat(65, "VK_FORMAT_A2B10G10R10_SNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.NORM, true, 0), new Component(ComponentType.BLUE, 10, StorageType.NORM, true, 0), new Component(ComponentType.GREEN, 10, StorageType.NORM, true, 0), new Component(ComponentType.RED, 10, StorageType.NORM, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(59, new VkFormat(59, "VK_FORMAT_A2R10G10B10_SNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.NORM, true, 0), new Component(ComponentType.RED, 10, StorageType.NORM, true, 0), new Component(ComponentType.GREEN, 10, StorageType.NORM, true, 0), new Component(ComponentType.BLUE, 10, StorageType.NORM, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(149, new VkFormat(149, "VK_FORMAT_ETC2_R8G8B8A1_UNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.ALPHA, 1, StorageType.NORM, false)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(151, new VkFormat(151, "VK_FORMAT_ETC2_R8G8B8A8_UNORM_BLOCK", Arrays.asList(new Component(ComponentType.RED, 8, StorageType.NORM, false), new Component(ComponentType.GREEN, 8, StorageType.NORM, false), new Component(ComponentType.BLUE, 8, StorageType.NORM, false), new Component(ComponentType.ALPHA, 8, StorageType.NORM, false)), Collections.emptyList(), "ETC2", true, new Vec2iFinal(4, 4)));
		INFO.put(64, new VkFormat(64, "VK_FORMAT_A2B10G10R10_UNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.NORM, false, 0), new Component(ComponentType.BLUE, 10, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 10, StorageType.NORM, false, 0), new Component(ComponentType.RED, 10, StorageType.NORM, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(54, new VkFormat(54, "VK_FORMAT_A8B8G8R8_SSCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.SCALED, true, 0), new Component(ComponentType.BLUE, 8, StorageType.SCALED, true, 0), new Component(ComponentType.GREEN, 8, StorageType.SCALED, true, 0), new Component(ComponentType.RED, 8, StorageType.SCALED, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(58, new VkFormat(58, "VK_FORMAT_A2R10G10B10_UNORM_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.NORM, false, 0), new Component(ComponentType.RED, 10, StorageType.NORM, false, 0), new Component(ComponentType.GREEN, 10, StorageType.NORM, false, 0), new Component(ComponentType.BLUE, 10, StorageType.NORM, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(53, new VkFormat(53, "VK_FORMAT_A8B8G8R8_USCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 8, StorageType.SCALED, false, 0), new Component(ComponentType.BLUE, 8, StorageType.SCALED, false, 0), new Component(ComponentType.GREEN, 8, StorageType.SCALED, false, 0), new Component(ComponentType.RED, 8, StorageType.SCALED, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(67, new VkFormat(67, "VK_FORMAT_A2B10G10R10_SSCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.SCALED, true, 0), new Component(ComponentType.BLUE, 10, StorageType.SCALED, true, 0), new Component(ComponentType.GREEN, 10, StorageType.SCALED, true, 0), new Component(ComponentType.RED, 10, StorageType.SCALED, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(61, new VkFormat(61, "VK_FORMAT_A2R10G10B10_SSCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.SCALED, true, 0), new Component(ComponentType.RED, 10, StorageType.SCALED, true, 0), new Component(ComponentType.GREEN, 10, StorageType.SCALED, true, 0), new Component(ComponentType.BLUE, 10, StorageType.SCALED, true, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(66, new VkFormat(66, "VK_FORMAT_A2B10G10R10_USCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.SCALED, false, 0), new Component(ComponentType.BLUE, 10, StorageType.SCALED, false, 0), new Component(ComponentType.GREEN, 10, StorageType.SCALED, false, 0), new Component(ComponentType.RED, 10, StorageType.SCALED, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(60, new VkFormat(60, "VK_FORMAT_A2R10G10B10_USCALED_PACK32", Arrays.asList(new Component(ComponentType.ALPHA, 2, StorageType.SCALED, false, 0), new Component(ComponentType.RED, 10, StorageType.SCALED, false, 0), new Component(ComponentType.GREEN, 10, StorageType.SCALED, false, 0), new Component(ComponentType.BLUE, 10, StorageType.SCALED, false, 0)), Collections.singletonList(32), null, false, null));
		INFO.put(123, new VkFormat(123, "VK_FORMAT_E5B9G9R9_UFLOAT_PACK32", Arrays.asList(new Component(ComponentType.SHARED_EXPONENT, 5, StorageType.FLOAT, false, 0), new Component(ComponentType.BLUE, 9, StorageType.FLOAT, false, 0), new Component(ComponentType.GREEN, 9, StorageType.FLOAT, false, 0), new Component(ComponentType.RED, 9, StorageType.FLOAT, false, 0)), Collections.singletonList(32), null, false, null));
	}
	
}
