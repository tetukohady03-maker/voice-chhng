cmake_minimum_required(VERSION 3.22.1)
project("voxmorph_dsp" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)
find_library(log-lib log)
find_library(android-lib android)

add_library(voxmorph_dsp SHARED
    dsp/VoiceDspProcessor.cpp
    engine/OboeAudioEngine.cpp
    jni/VoxMorphJni.cpp
)

target_include_directories(voxmorph_dsp PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/dsp
    ${CMAKE_CURRENT_SOURCE_DIR}/engine
)

target_link_libraries(voxmorph_dsp
    oboe::oboe
    ${log-lib}
    ${android-lib}
)
