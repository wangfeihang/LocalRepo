package com.yy.publish

import com.yy.publish.analyzer.DependencyAnalyzer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get

/**
 * Created by wangfeihang on 2019-08-19.
 */
class UnionPublishPlugin : Plugin<Project> {

    private var project: Project? = null
    private var publishExtension: UnionPublishRepoExtension? = null
    private var currentUnionSdkInfo: UnionSdkInfo? = null


    private fun defineTask(unionSdkInfo: UnionSdkInfo, project: Project) {
        val publishTaskName = "publish"

        project.tasks.findByName(publishTaskName)?.let { publishTask ->
            unionSdkInfo.dependencies?.forEach {
                if (it.isProject) {
                    publishTask.dependsOn("${it.projectPath}:$publishTaskName")
                }
            }
        }
    }

    override fun apply(target: Project) {
        project = target
        project?.extensions?.create("publishPublicRepo", UnionPublishRepoExtension::class.java)
        project?.plugins?.apply("maven")
        project?.plugins?.apply("maven-publish")
        project?.plugins?.apply("signing")
        project?.afterEvaluate {

            publishExtension = this.extensions["publishPublicRepo"] as UnionPublishRepoExtension


            println("UnionPublishPlugin publishExtension:$publishExtension")
            publishExtension?.let {
                currentUnionSdkInfo = DependencyAnalyzer(
                    project = this,
                    isLocal = it.local,
                    groupId = it.groupId,
                    sdkVersion = it.sdkVersion
                ).analyze()
            }
            println(
                "UnionPublishPlugin,${project.name} analyze finish, " +
                        "currentUnionSdkInfo:${currentUnionSdkInfo?.customStr("")}"
            )


            publishExtension?.let { extension ->
                currentUnionSdkInfo?.let { sdkinfo ->
                    PublishExtensionUtil.createPublishingExtension(project, extension, sdkinfo)
                }
            }

            currentUnionSdkInfo?.let {
                defineTask(it, project)
            }
        }
    }


}