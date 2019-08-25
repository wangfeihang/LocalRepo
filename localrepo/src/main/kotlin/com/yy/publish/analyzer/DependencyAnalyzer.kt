package com.yy.publish.analyzer

import com.yy.publish.UnionSdkInfo
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency

/**
 * Created by wangfeihang on 2019-08-19.
 */
class DependencyAnalyzer(
    private val project: Project,
    private val isLocal: Closure<Boolean>,
    private val groupId: String,
    private val sdkVersion: String
) {
    private val analyzedConfigurations = mutableListOf("api", "compile", "implementation")


    //分析project的info
    fun analyze(): UnionSdkInfo {
        val dependencies = mutableListOf<UnionSdkInfo>()
        println("UnionPublishPlugin,DependencyAnalyzer analyze project name:${project.name}, project path:${project.path}")
        project.configurations.forEach { it ->
            if (analyzedConfigurations.contains(it.name)) {
                it.allDependencies.forEach { dependency ->
                    if (dependency is ProjectDependency) {
                        dependencies.add(
                            getProjectUnionSdkInfo(dependency)
                        )
                    } else if (dependency is ExternalModuleDependency) {
                        dependencies.add(getExternalModuleUnionSdkInfo(dependency))
                    }
                }
            }
        }

        return UnionSdkInfo(
            groupId = groupId,
            artifactId = project.name,
            sdkVersion = sdkVersion,
            isLocal = true,
            isProject = true,
            sdkPath = SdkPathUtil.getProjectSdkPath(project),
            dependencies = dependencies.distinct(),
            exclusions = null
        ).apply {
            this.projectPath = project.path
        }
    }

    private fun getProjectUnionSdkInfo(dependency: ProjectDependency): UnionSdkInfo {
        println("UnionPublishPlugin,getProjectUnionSdkInfo:${dependency.group}:${dependency.name}:${dependency.version}")
        return UnionSdkInfo(
            groupId = groupId,
            artifactId = dependency.name,
            sdkVersion = sdkVersion,
            isLocal = true,
            isProject = true,
            sdkPath = SdkPathUtil.getProjectSdkPath(dependency.dependencyProject),
            dependencies = null,
            exclusions = dependency.getExclusions()
        ).apply {
            this.projectPath = dependency.dependencyProject.path
        }
    }

    private fun getExternalModuleUnionSdkInfo(dependency: ExternalModuleDependency): UnionSdkInfo {
        val sdkParentPath = SdkPathUtil.getExternalSdkPath(
            project = project,
            groupId = dependency.group ?: "",
            artifactId = dependency.name,
            version = dependency.version ?: ""
        )

        val exclusions = dependency.getExclusions()
        val sdkVersion =
            if (isLocal.call(dependency) && dependency.version?.endsWith("-SNAPSHOT") == false) {
                dependency.version + "-SNAPSHOT"
            } else {
                dependency.version
            }
        return UnionSdkInfo(
            groupId = dependency.group ?: "",
            artifactId = dependency.name,
            sdkVersion = sdkVersion ?: "",
            isLocal = isLocal.call(dependency),
            isProject = false,
            sdkPath = SdkPathUtil.getSdkPath(
                project = project,
                parentPath = sdkParentPath
            ),
            dependencies = PomAnalyzer(project, isLocal).analyze(
                SdkPathUtil.getSdkPomPath(project, sdkParentPath) ?: "",
                exclusions
            ),
            exclusions = exclusions
        )
    }

    private fun ModuleDependency.getExclusions(): List<ExclusionNode>? {
        val result = mutableListOf<ExclusionNode>()
        this.excludeRules.forEach {
            result.add(ExclusionNode(it.group, it.module))
        }
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }


}