package com.yy.localrepo

import org.gradle.api.Plugin
import org.gradle.api.Project

public class PluginImpl implements Plugin<Project> {
    void apply(Project project) {
        project.configurations {
            localapi
        }
        project.dependencies {
            api project.configurations.localapi
        }
        project.afterEvaluate {


            def local = project.configurations.localapi

            local.dependencies.each { dependency ->
                println("wang dependency:$dependency")
                local.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                    println("wang dependency:${dependency.group}:${dependency.name}:${dependency.version},  artifact:${artifact.moduleVersion.toString()}")
                    if ("${dependency.group}:${dependency.name}:${dependency.version}" == artifact.moduleVersion.toString()) {
                        def libPath = "${artifact.file.parentFile.path}\\libs"
                        println("wang truetrue,$libPath")
                        project.getRepositories().flatDir {
                            dirs(libPath)
                        }
                        project.dependencies.add('api', project.fileTree(dir: libPath, include: ['*.jar', '*.aar', '*.so']))
                    }
                }
            }

//            def libPath="E:/GitHub/midwares/MyApplication/repo/com/yy/rice/mibosdk/1.0.3/libs"
//            project.getRepositories().flatDir {
//                dirs(libPath)
//            }
//            project.dependencies.add('api', project.fileTree(dir: libPath, include: ['*.jar', '*.aar', '*.so']))
        }
    }
}