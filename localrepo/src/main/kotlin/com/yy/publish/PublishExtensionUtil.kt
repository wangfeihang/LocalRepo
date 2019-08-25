package com.yy.publish

import com.yy.publish.analyzer.SdkPathUtil
import groovy.util.Node
import groovy.xml.QName
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import java.util.*

/**
 * Created by wangfeihang on 2019-08-24.
 */
object PublishExtensionUtil {
    fun createPublishingExtension(
        project: Project,
        publishExtension: UnionPublishRepoExtension,
        currentUnionSdkInfo: UnionSdkInfo
    ) {
        project.extensions.configure("publishing", Action<PublishingExtension> {
            this.repositories(Action {
                this.maven {
                    credentials {
                        username = publishExtension.repoUserName
                        password = publishExtension.repoPassword
                    }
                    if (publishExtension.sdkVersion.contains("SNAPSHOT")) {
                        setUrl("${publishExtension.snapshotRepo}")
                    } else {
                        setUrl("${publishExtension.releaseRepo}")
                    }
                }
            })

            fun MavenPublication.applyFrom(unionSdkInfo: UnionSdkInfo, sdkPath: String, setExclusions: Boolean) {

                if (publishExtension.dontCopy.call(unionSdkInfo.baseStr())) {
                    println("UnionPublishPlugin warinng, dontCopy matched:${unionSdkInfo.baseStr()}")
                    return
                }
                this.groupId = unionSdkInfo.groupId
                this.artifactId = unionSdkInfo.artifactId
                this.version = unionSdkInfo.sdkVersion
                this.artifact(sdkPath).apply {
                    if (project.hasProperty("android")) {
                        this.builtBy(project.tasks.findByName("assembleRelease"))
                    } else {
                        this.builtBy(project.tasks.findByName("build"))
                    }
                }
                if (unionSdkInfo.dependencies?.isEmpty() == false) {
                    this.pom.withXml {
                        var dependenciesNode: Node? = null
                        this.asNode().getAt(QName("dependencies"))?.let {
                            if (!it.isEmpty()) {
                                dependenciesNode = this.asNode().getAt(QName("dependencies"))[0] as Node
                            }
                        }
                        if (dependenciesNode != null) {
                            this.asNode().remove(dependenciesNode)
                        }
                        dependenciesNode = asNode().appendNode("dependencies")
                        unionSdkInfo.dependencies?.forEach {
                            if (!publishExtension.dontCopy.call(it.baseStr())) {
                                val dependencyNode = dependenciesNode?.appendNode("dependency")
                                dependencyNode?.appendNode("groupId", it.groupId)
                                dependencyNode?.appendNode("artifactId", it.artifactId)
                                dependencyNode?.appendNode("version", it.sdkVersion)

                                if (setExclusions) {
                                    val exclusionsNode = dependencyNode?.appendNode("exclusions")
                                    it.exclusions?.forEach {
                                        val exclusionNode = exclusionsNode?.appendNode("exclusion")
                                        if (it.groupId != null) {
                                            exclusionNode?.appendNode("groupId", it.groupId)
                                        }
                                        if (it.artifactId != null) {
                                            exclusionNode?.appendNode("artifactId", it.artifactId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.publications {
                //先发布当前project的sdk
                this.create(currentUnionSdkInfo.artifactId, MavenPublication::class.java) {
                    applyFrom(currentUnionSdkInfo, currentUnionSdkInfo.sdkPath.first(), true)
                }

                //发布当前project所依赖的本公司sdk
                fun publishDependencies(unionSdkInfo: UnionSdkInfo) {
                    unionSdkInfo.dependencies?.forEach {
                        if (it.isLocal && !it.isProject) {
                            println("set ${it.baseStr()} mavenPublication, sdkpath:${it.sdkPath}")
                            if (it.sdkPath.isEmpty()) {
                                it.sdkPath = SdkPathUtil.getSdkPath(
                                    project,
                                    SdkPathUtil.getExternalSdkPath(
                                        project,
                                        it.groupId,
                                        it.artifactId,
                                        it.sdkVersion
                                    )
                                )
                            }
                            println("after fill, sdkpath:${it.sdkPath}")
                            Collections.sort(it.sdkPath, kotlin.Comparator { t1, t2 ->
                                fun getFilePathValue(path: String): Int {
                                    return when {
                                        path.endsWith(".so") -> 2
                                        path.endsWith(".aar") -> 1
                                        else -> 0
                                    }
                                }

                                val value1 = getFilePathValue(t1)
                                val value2 = getFilePathValue(t2)
                                return@Comparator value2 - value1
                            })
                            for (i in it.sdkPath.indices) {
                                val pubName = "${it.groupId}${it.artifactId}$i"
                                if (!this.any { pub -> pub.name == pubName }) {
                                    this.create(
                                        "${it.groupId}${it.artifactId}$i",
                                        MavenPublication::class.java
                                    ) {
                                        applyFrom(it, it.sdkPath[i], false)
                                    }
                                }
                            }
                            publishDependencies(it)
                        }
                    }
                }

                if (!publishExtension.onlyPublishProject) {
                    publishDependencies(currentUnionSdkInfo)
                }
            }
        })
    }
}