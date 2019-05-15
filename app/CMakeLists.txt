# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required (VERSION 3.4.1)

# -- freeteuse ------------------
add_library (freeteuse SHARED
             src/main/cpp/freeteuse.cpp
             src/main/cpp/Rcu.cpp
             src/main/cpp/Key.cpp
             src/main/cpp/Pipe.cpp
             src/main/cpp/Message.cpp
             src/main/cpp/Status.cpp
             )

# -- freebxsdk --------------------
add_library (ela       SHARED IMPORTED)
add_library (rudp      SHARED IMPORTED)
add_library (foils_hid STATIC IMPORTED)

set_target_properties (ela
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/cpp/prebuilds/lib/${ANDROID_ABI}/libela.so
        )

set_target_properties (rudp
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/cpp/prebuilds/lib/${ANDROID_ABI}/librudp.so
        )

set_target_properties (foils_hid
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/cpp/prebuilds/lib/${ANDROID_ABI}/libfoils_hid.a
        )

# --------------------------------
include_directories (src/main/cpp
                     src/main/cpp/prebuilds/include
                     )

# --------------------------------
# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
find_library (# Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log
              )

# --------------------------------
target_link_libraries (# Specifies the target library.
                       freeteuse
                       ela
                       rudp
                       foils_hid

        # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       )
