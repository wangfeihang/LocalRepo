package com.yy.localrepo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolutionStrategy

public class PluginImpl implements Plugin<Project> {
    void apply(Project project) {
        project.configurations {
            localapi
        }
        project.dependencies {
            api project.configurations.localapi
        }
        project.buildscript {
            project.configurations.all {
                println("强制更新版本")
                it.resolutionStrategy{
                    force 'net.sf.proguard:proguard-gradle:6.0.3'
                }
            }
        }
        project.afterEvaluate {


            def local = project.configurations.localapi

            local.dependencies.each { dependency ->
                local.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                    if ("${dependency.group}:${dependency.name}:${dependency.version}" == artifact.moduleVersion.toString()) {
                        def removePath="${dependency.group.toString().replace(".",File.separator)}${File.separator}${dependency.name.toString()}${File.separator}${dependency.version.toString()}"
                        def libPath = artifact.file.path.toString().substring(0,artifact.file.path.indexOf(removePath))
                        println("localrepo, add depencies:$libPath")
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