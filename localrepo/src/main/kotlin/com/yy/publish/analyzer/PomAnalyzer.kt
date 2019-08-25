package com.yy.publish.analyzer

import com.yy.publish.UnionSdkInfo
import com.yy.publish.analyzer.SdkPathUtil.getExternalSdkPath
import com.yy.publish.analyzer.SdkPathUtil.getSdkPath
import com.yy.publish.analyzer.SdkPathUtil.getSdkPomPath
import groovy.lang.Closure
import groovy.util.XmlSlurper
import groovy.util.slurpersupport.NodeChildren
import org.gradle.api.Project
import java.io.File

/**
 * Created by wangfeihang on 2019-08-19.
 * Pom文件解析器
 */
class PomAnalyzer(
    private val project: Project,
    private val isLocal: Closure<Boolean>
) {


    //解析pom文件
    fun analyze(pomFilePath: String, exclusions: List<ExclusionNode>?): List<UnionSdkInfo>? {
        println("UnionPublishPlugin, start pom analyze: $pomFilePath")
        val result = mutableListOf<UnionSdkInfo>()
        val pomFile = File(pomFilePath)
        if (!pomFile.exists()) {
            println("not exists")
            return null
        }
        val pomDependencies = XmlSlurper().parse(pomFile).getProperty("dependencies") as NodeChildren
        val nodes = PomNodeAnalyzer().analyze(pomDependencies)
        nodes.forEach { dependencyNode ->
            val isNodeExclude = exclusions?.any { exclusionNode ->
                return@any if (exclusionNode.artifactId == null) {
                    exclusionNode.groupId == dependencyNode.groupId
                } else if (exclusionNode.groupId == null) {
                    exclusionNode.artifactId == dependencyNode.artifactId
                } else {
                    exclusionNode.artifactId == dependencyNode.artifactId && exclusionNode.groupId == dependencyNode.groupId
                }
            } == true
            if (!isNodeExclude) {
                result.add(dependencyNode.toUnionSdkInfo())
            }
        }
        return result
    }

    //pom解析出来的node转换为UnionSdkInfo
    private fun DependencyNode.toUnionSdkInfo(): UnionSdkInfo {
        val parentPath = getExternalSdkPath(project, this.groupId, this.artifactId, this.version)
        val local = isLocal.call(object : DefaultFakeDependency() {

            override fun getGroup(): String? {
                return this@toUnionSdkInfo.groupId
            }

            override fun getName(): String {
                return this@toUnionSdkInfo.artifactId
            }

            override fun getVersion(): String? {
                return this@toUnionSdkInfo.version
            }

            override fun toString(): String {
                return "$group:$artifactId:$version"
            }

        })
        val sdkVersion = if (local && !this.version.endsWith("-SNAPSHOT")) this.version + "-SNAPSHOT" else this.version
        return UnionSdkInfo(
            groupId = this.groupId,
            artifactId = this.artifactId,
            sdkVersion = sdkVersion,
            isLocal = local,
            isProject = false,
            sdkPath = getSdkPath(project, parentPath),
            dependencies = analyze(getSdkPomPath(project, parentPath) ?: "", this.exclusions),
            exclusions = this.exclusions
        )
    }
}