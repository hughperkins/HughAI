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
	if    (BUILD_Java_AIINTERFACE AND NOT myName MATCHES "${AI_EXCLUDE_REGEX}")
		set(BUILD_THIS_SKIRMISHAI TRUE)
	else  (BUILD_Java_AIINTERFACE AND NOT myName MATCHES "${AI_EXCLUDE_REGEX}")
		set(BUILD_THIS_SKIRMISHAI FALSE)
		message("warning: ${myName} Skirmish AI will not be built!")
	endif (BUILD_Java_AIINTERFACE AND NOT myName MATCHES "${AI_EXCLUDE_REGEX}")


	# Compile and install
	if    (BUILD_THIS_SKIRMISHAI)
		# Assemble meta data
		GetVersionFromFile(myVersion "${myDir}/VERSION")
		set(myTarget             "${myName}")
		set(myLoaderTarget       "${myTarget}-Loader")
		set(myAiTarget           "${myTarget}-AI")
		set(myLoaderSourceTarget "${myLoaderTarget}-Source")
		set(myAiSourceTarget     "${myAiTarget}-Source")
		set(myDataDir       "${SKIRMISH_AI_DATA}/${myName}/${myVersion}")
		# CMAKE_CURRENT_BINARY_DIR: .../spring-build-dir/AI/Skirmish/${myName}
		set(myBuildDir      "${CMAKE_CURRENT_BINARY_DIR}")
		set(myJavaLoaderBuildDir  "${myBuildDir}/classes/loader")
		set(myJavaAiBuildDir  "${myBuildDir}/classes/ai")
		set(myLoaderJarFile       "SkirmishAI")
		set(myAiJarFile       "UnderlyingAI")
		set(myLoaderBinJarFile    "${myLoaderJarFile}.jar")
		set(myAiBinJarFile    "${myAiJarFile}.jar")
		set(myLoaderSrcJarFile    "${myLoaderJarFile}-src.jar")
		set(myAiSrcJarFile    "${myAiJarFile}-src.jar")
		set(myJLibDir       "${myDir}/data/jlib")

		SkirmishAIMessage(STATUS "Found Skirmish AI: ${myName} ${myVersion}")

		# Create our full Java class-path
		CreateClasspath(myJavaLibs ${myJLibDir})
		ConcatClasspaths(myClassPath "${CLASSPATH_Java_AIINTERFACE}" "${myJavaLibs}" ${myAiSourceDir} ${myLoaderSourceDir} ${JavaOO_AIWRAPPER_JAR_CLASSPATH})

		# Create a list of all the AIs source files
		file(GLOB_RECURSE myLoaderSources RELATIVE "${myLoaderSourceDir}" FOLLOW_SYMLINKS "${myLoaderSourceDir}/*.java")
		file(GLOB_RECURSE myAiSources RELATIVE "${myAiSourceDir}" FOLLOW_SYMLINKS "${myAiSourceDir}/*.java")

		# Compile the loader library
		# needed by javac
		file(MAKE_DIRECTORY "${myJavaLoaderBuildDir}")
		add_custom_target(${myLoaderTarget} ALL
			#OUTPUT "${myBuildDir}/${myLoaderBinJarFile}"
			COMMAND "${JAVA_COMPILE}"
				"${JAVA_COMPILE_FLAG_CONDITIONAL}"
				"-cp" "${myClassPath}"
				"-d" "${myJavaLoaderBuildDir}"
				${myLoaderSources}
			COMMAND "${JAVA_ARCHIVE}"
				"cmf" "${myDir}/manifest.mf" "${myBuildDir}/${myLoaderBinJarFile}"
				"-C" "${myJavaLoaderBuildDir}" "${myJavaPkgFirstPart}"
			WORKING_DIRECTORY "${myLoaderSourceDir}"
			COMMENT "  ${myTarget}: Creating library ${myLoaderBinJarFile}" VERBATIM)
		add_dependencies(${myLoaderTarget} ${Java_AIINTERFACE_TARGET} ${JavaOO_AIWRAPPER_TARGET})

		# Compile the ai library
		# needed by javac
		file(MAKE_DIRECTORY "${myJavaAiBuildDir}")
		add_custom_target(${myAiTarget} ALL
			#OUTPUT "${myBuildDir}/${myAiBinJarFile}"
			COMMAND "${JAVA_COMPILE}"
				"${JAVA_COMPILE_FLAG_CONDITIONAL}"
				"-cp" "${myClassPath}"
				"-d" "${myJavaAiBuildDir}"
				${myAiSources}
			COMMAND "${JAVA_ARCHIVE}"
				"cmf" "${myDir}/manifest.mf" "${myBuildDir}/${myAiBinJarFile}"
				"-C" "${myJavaAiBuildDir}" "${myJavaPkgFirstPart}"
			WORKING_DIRECTORY "${myAiSourceDir}"
			COMMENT "  ${myTarget}: Creating library ${myAiBinJarFile}" VERBATIM)
		add_dependencies(${myAiTarget} ${Java_AIINTERFACE_TARGET} ${JavaOO_AIWRAPPER_TARGET})

		# Pack the sources
		add_custom_target(${myLoaderSourceTarget} ALL
			#OUTPUT "${myBuildDir}/${myLoaderSrcJarFile}"
			COMMAND "${JAVA_ARCHIVE}"
				"cf" "${myBuildDir}/${myLoaderSrcJarFile}"
				"-C" "${myLoaderSourceDir}" "${myJavaPkgFirstPart}"
			WORKING_DIRECTORY "${myLoaderSourceDir}"
			COMMENT "  ${myTarget}: Creating sources archive ${myLoaderSrcJarFile}" VERBATIM)

		# Pack the sources
		add_custom_target(${myAiSourceTarget} ALL
			#OUTPUT "${myBuildDir}/${myAiSrcJarFile}"
			COMMAND "${JAVA_ARCHIVE}"
				"cf" "${myBuildDir}/${myAiSrcJarFile}"
				"-C" "${myAiSourceDir}" "${myJavaPkgFirstPart}"
			WORKING_DIRECTORY "${myAiSourceDir}"
			COMMENT "  ${myTarget}: Creating sources archive ${myAiSrcJarFile}" VERBATIM)

		add_custom_target(${myTarget} ALL)
		add_dependencies(${myTarget} ${myLoaderTarget} ${myAiTarget} ${myLoaderSourceTarget} ${myAiSourceTarget})

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
		# Install the Java OO Wrapper libraries
		install(FILES "${JavaOO_AIWRAPPER_JAR_BIN}" DESTINATION ${myDataDir}/jlib)
		set(wrapperJLibDir "${CMAKE_SOURCE_DIR}/AI/Wrappers/JavaOO/jlib")
		if    (EXISTS ${wrapperJLibDir})
			install(DIRECTORY "${wrapperJLibDir}/" DESTINATION ${myDataDir}/jlib)
		endif (EXISTS ${wrapperJLibDir})
	endif (BUILD_THIS_SKIRMISHAI)

