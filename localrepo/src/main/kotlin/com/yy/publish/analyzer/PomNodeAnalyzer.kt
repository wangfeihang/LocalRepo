package com.yy.publish.analyzer

import groovy.util.slurpersupport.Node
import groovy.util.slurpersupport.NodeChildren

/**
 * Created by wangfeihang on 2019-08-20.
 */
class PomNodeAnalyzer {

    fun analyze(dependenciesNode: NodeChildren): List<DependencyNode> {
        val result = mutableListOf<DependencyNode>()
        dependenciesNode.childNodes().forEach {
            var groupId = ""
            var artifactId = ""
            var version = ""
            var exclusions: List<ExclusionNode>? = null
            if (it is Node) {
                it.childNodes().forEach {
                    if (it is Node) {
                        when (it.name()) {
                            "groupId" -> groupId = it.text()
                            "artifactId" -> artifactId = it.text()
                            "version" -> version = it.text()
                            "exclusions" -> exclusions = analyzeExclusionNode(it)
                        }
                    }
                }
            }
            println("$groupId $artifactId $version")
            result.add(DependencyNode(groupId, artifactId, version, exclusions))
        }
        return result
    }

    private fun analyzeExclusionNode(node: Node): List<ExclusionNode>? {
        val result = mutableListOf<ExclusionNode>()
        node.childNodes().forEach {
            var groupId = ""
            var artifactId = ""
            if (it is Node) {
                it.childNodes().forEach {
                    if (it is Node) {
                        when (it.name()) {
                            "groupId" -> groupId = it.text()
                            "artifactId" -> artifactId = it.text()
                        }
                    }
                }
            }
            result.add(ExclusionNode(groupId, artifactId))
        }
        return result
    }
}

data class DependencyNode(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val exclusions: List<ExclusionNode>?
)

data class ExclusionNode(
    val groupId: String?,
    val artifactId: String?
)

