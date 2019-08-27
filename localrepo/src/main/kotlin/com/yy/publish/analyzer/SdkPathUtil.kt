package com.yy.publish.analyzer

import org.gradle.api.Project
import java.io.File

/**
 * Created by wangfeihang on 2019-08-21.
 * 方便获取sdk的aar、jar、pom文件路径的工具类
 */
object SdkPathUtil {


    fun getExternalSdkPath(
        project: Project,
        groupId: String,
        artifactId: String,
        version: String
    ): String {
        val gradlePath = project.gradle.gradleUserHomeDir.path + "/caches/modules-2/files-2.1/"
        return "$gradlePath$groupId${File.separator}$artifactId${File.separator}$version${File.separator}"
    }

    fun getProjectSdkPath(project: Project): List<String> {
        return listOf(
            if (project.hasProperty("android")) {
                "${project.buildDir}${File.separator}outputs${File.separator}aar${File.separator}${project.name}-release.aar"
            } else {
                "${project.buildDir}${File.separator}libs${File.separator}${project.name}-${project.version}.jar"
            }
        )
    }

    fun getSdkPath(project: Project, parentPath: String): List<String> {
        val sdkFileExtensionNames = listOf(".so", ".aar", ".jar")

        fun File.nameEndWith(extensionName: List<String>): Boolean {
            return extensionName.firstOrNull {
                this.name.endsWith(it)
            } != null
        }

        val result = mutableListOf<String>()

        project.fileTree(parentPath).forEach {
            if (!it.name.contains("sources") && !it.name.contains("javadoc") && it.nameEndWith(sdkFileExtensionNames)) {
                result.add(it.absolutePath)
            }
        }
        return result
    }

    fun getSdkPomPath(project: Project, parentPath: String): String? {
        val fileCreateTime: Long = 0
        var file: File? = null
        project.fileTree(parentPath).forEach {
            if (it.name.endsWith(".pom") && it.lastModified() > fileCreateTime) {
                file = it
            }
        }
        return file?.absolutePath
    }
}