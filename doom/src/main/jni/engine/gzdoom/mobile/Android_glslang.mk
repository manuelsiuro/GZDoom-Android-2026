
LOCAL_PATH := $(call my-dir)/../libraries/ZVulkan

include $(CLEAR_VARS)

LOCAL_MODULE := glslang_uz

LOCAL_CFLAGS :=  -fexceptions -DUNIX -D_UNIX

LOCAL_C_INCLUDES :=   $(LOCAL_PATH)/../ $(LOCAL_PATH)/include/ $(LOCAL_PATH)/include/zvulkan

LOCAL_SRC_FILES =  	\
src/vulkanbuilders.cpp \
src/vulkandevice.cpp \
src/vulkaninstance.cpp \
src/vulkansurface.cpp \
src/vulkanswapchain.cpp \
src/vk_mem_alloc/vk_mem_alloc.cpp \
src/vk_mem_alloc/vk_mem_alloc.natvis \
src/volk/volk.c \
src/glslang/glslang/MachineIndependent/propagateNoContraction.cpp \
src/glslang/glslang/MachineIndependent/PoolAlloc.cpp \
src/glslang/glslang/MachineIndependent/Intermediate.cpp \
src/glslang/glslang/MachineIndependent/attribute.cpp \
src/glslang/glslang/MachineIndependent/Scan.cpp \
src/glslang/glslang/MachineIndependent/SymbolTable.cpp \
src/glslang/glslang/MachineIndependent/RemoveTree.cpp \
src/glslang/glslang/MachineIndependent/reflection.cpp \
src/glslang/glslang/MachineIndependent/iomapper.cpp \
src/glslang/glslang/MachineIndependent/intermOut.cpp \
src/glslang/glslang/MachineIndependent/Versions.cpp \
src/glslang/glslang/MachineIndependent/linkValidate.cpp \
src/glslang/glslang/MachineIndependent/InfoSink.cpp \
src/glslang/glslang/MachineIndependent/Constant.cpp \
src/glslang/glslang/MachineIndependent/IntermTraverse.cpp \
src/glslang/glslang/MachineIndependent/glslang_tab.cpp \
src/glslang/glslang/MachineIndependent/ShaderLang.cpp \
src/glslang/glslang/MachineIndependent/preprocessor/Pp.cpp \
src/glslang/glslang/MachineIndependent/preprocessor/PpAtom.cpp \
src/glslang/glslang/MachineIndependent/preprocessor/PpContext.cpp \
src/glslang/glslang/MachineIndependent/preprocessor/PpTokens.cpp \
src/glslang/glslang/MachineIndependent/preprocessor/PpScanner.cpp \
src/glslang/glslang/MachineIndependent/parseConst.cpp \
src/glslang/glslang/MachineIndependent/Initialize.cpp \
src/glslang/glslang/MachineIndependent/limits.cpp \
src/glslang/glslang/MachineIndependent/ParseContextBase.cpp \
src/glslang/glslang/MachineIndependent/ParseHelper.cpp \
src/glslang/glslang/MachineIndependent/SpirvIntrinsics.cpp \
src/glslang/glslang/GenericCodeGen/Link.cpp \
src/glslang/glslang/GenericCodeGen/CodeGen.cpp \
src/glslang/spirv/GlslangToSpv.cpp \
src/glslang/spirv/doc.cpp \
src/glslang/spirv/disassemble.cpp \
src/glslang/spirv/SpvPostProcess.cpp \
src/glslang/spirv/InReadableOrder.cpp \
src/glslang/spirv/SPVRemapper.cpp \
src/glslang/spirv/SpvBuilder.cpp \
src/glslang/spirv/SpvTools.cpp \
src/glslang/spirv/Logger.cpp \
src/glslang/OGLCompilersDLL/InitializeDll.cpp \
src/glslang/glslang/OSDependent/Unix/ossource.cpp


include $(BUILD_STATIC_LIBRARY)








