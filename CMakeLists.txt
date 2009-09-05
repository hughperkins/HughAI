### Generic Java Skirmish AI config
#

# ConfigureJavaSkirmishAI()

	set(myDir                    "${CMAKE_CURRENT_SOURCE_DIR}")
	GetLastPathPart(dirName ${myDir})
	set(myName                   "${dirName}")
	set(myLoaderSourceDir              "${myDir}/src-loader")
	set(myAiSourceDir              "${myDir}/src-ai")
	# If main Java package is "my.ai.pkg", this has to be set to "my".
	GetFirstSubDirName(firstSrcSubDir ${myLoaderSourceDir})
	set(myJavaPkgFirstPart       "${firstSrcSubDir}")

	# Check if the user wants to compile the AI
	if    (BUILD_Java_AIINTERFACE)
		set(BUILD_THIS_SKIRMISHAI TRUE)
	else  (BUILD_Java_AIINTERFACE)
		set(BUILD_THIS_SKIRMISHAI FALSE)
		message("warning: ${myName} Skirmish AI will not be built!")
	endif (BUILD_Java_AIINTERFACE)


	# Compile and install
	if    (BUILD_THIS_SKIRMISHAI)
		# Assemble meta data
		GetVersionFromFile(myVersion "${myDir}/VERSION")
		set(myLoaderTarget        "${myName}-LOADER")
		set(myAiTarget        "${myName}-AI")
		set(myLoaderSourceTarget  "${myLoaderTarget}-SRC")
		set(myAiSourceTarget  "${myAiTarget}-SRC")
		set(myDataDir       "${SKIRMISH_DATA}/${myName}/${myVersion}")
		# CMAKE_CURRENT_BINARY_DIR: .../spring-build-dir/AI/Skirmish/${myName}
		set(myBuildDir      "${CMAKE_CURRENT_BINARY_DIR}")
		set(myJavaLoaderBuildDir  "${myBuildDir}/classes/loader")
		set(myJavaAiBuildDir  "${myBuildDir}/classes/ai")
		set(myLoaderJarFile       "SkirmishAI")
		set(myAiJarFile       "Underlying")
		set(myLoaderBinJarFile    "${myLoaderJarFile}.jar")
		set(myAiBinJarFile    "${myAiJarFile}.jar")
		set(myLoaderSrcJarFile    "${myLoaderJarFile}-src.jar")
		set(myAiSrcJarFile    "${myAiJarFile}-src.jar")
		set(myJLibDir       "${myDir}/data/jlib")

		SkirmishAIMessage(STATUS "Found Skirmish AI: ${myName} ${myVersion}")

		# Create our full Java class-path
		CreateClasspath(myJavaLibs ${myJLibDir})
		ConcatClasspaths(myClassPath "${CLASSPATH_Java_AIINTERFACE}" "${myJavaLibs}" ${myAiSourceDir} ${myLoaderSourceDir})

		# Create a list of all the AIs source files
		file(GLOB_RECURSE myLoaderSources RELATIVE "${myLoaderSourceDir}" FOLLOW_SYMLINKS "${myLoaderSourceDir}/*.java")
		file(GLOB_RECURSE myAiSources RELATIVE "${myAiSourceDir}" FOLLOW_SYMLINKS "${myAiSourceDir}/*.java")

		# Compile the loader library
		# needed by javac
		file(MAKE_DIRECTORY "${myJavaLoaderBuildDir}")
		add_custom_target(${myLoaderTarget} ALL
			DEPENDS ${Java_AIINTERFACE_TARGET}
			COMMAND "${JAVA_COMPILE}"
				"${JAVA_COMPILE_FLAG_CONDITIONAL}"
				"-cp" "${myClassPath}"
				"-d" "${myJavaLoaderBuildDir}"
				${myLoaderSources}
			COMMAND "${JAVA_ARCHIVE}"
				"cmf" "${myDir}/manifest.mf" "${myBuildDir}/${myLoaderBinJarFile}"
				"-C" "${myJavaLoaderBuildDir}" "${myJavaPkgFirstPart}"
			WORKING_DIRECTORY "${myLoaderSourceDir}"
			COMMENT "  Creating library ${myLoaderBinJarFile}" VERBATIM)

		# Compile the ai library
		# needed by javac
		file(MAKE_DIRECTORY "${myJavaAiBuildDir}")
		add_custom_target(${myAiTarget} ALL
			DEPENDS ${Java_AIINTERFACE_TARGET}
			COMMAND "${JAVA_COMPILE}"
				"${JAVA_COMPILE_FLAG_CONDITIONAL}"
				"-cp" "${myClassPath}"
				"-d" "${myJavaAiBuildDir}"
				${myAiSources}
			COMMAND "${JAVA_ARCHIVE}"
				"cmf" "${myDir}/manifest.mf" "${myBuildDir}/${myAiBinJarFile}"
				"-C" "${myJavaAiBuildDir}" "${myJavaPkgFirstPart}"
			WORKING_DIRECTORY "${myAiSourceDir}"
			COMMENT "  Creating library ${myAiBinJarFile}" VERBATIM)

		# Pack the sources
		add_custom_target(${myLoaderSourceTarget} ALL
			COMMAND "${JAVA_ARCHIVE}"
				"cf" "${myBuildDir}/${myLoaderSrcJarFile}"
				"-C" "${myLoaderSourceDir}" "${myJavaPkgFirstPart}"
			WORKING_DIRECTORY "${myLoaderSourceDir}"
			COMMENT "  Creating sources archive ${myLoaderSrcJarFile}" VERBATIM)

		# Pack the sources
		add_custom_target(${myAiSourceTarget} ALL
			COMMAND "${JAVA_ARCHIVE}"
				"cf" "${myBuildDir}/${myAiSrcJarFile}"
				"-C" "${myAiSourceDir}" "${myJavaPkgFirstPart}"
			WORKING_DIRECTORY "${myAiSourceDir}"
			COMMENT "  Creating sources archive ${myAiSrcJarFile}" VERBATIM)

		# Install the data files
		install(DIRECTORY "${myDir}/data/"                DESTINATION ${myDataDir})
		# Install the library
		install (FILES    "${myBuildDir}/${myLoaderBinJarFile}" DESTINATION ${myDataDir})
		# Install the library
		install (FILES    "${myBuildDir}/${myAiBinJarFile}" DESTINATION ${myDataDir})
		# Install the sources archive
		install (FILES    "${myBuildDir}/${myLoaderSrcJarFile}" DESTINATION ${myDataDir}/jlib)
		# Install the sources archive
		install (FILES    "${myBuildDir}/${myAiSrcJarFile}" DESTINATION ${myDataDir}/jlib)
	endif (BUILD_THIS_SKIRMISHAI)

