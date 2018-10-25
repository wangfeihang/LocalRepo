package com.yy.localrepo

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedDependency

/**
 * Created by wangfeihang on 2018/10/19.
 */
public class NodeAdder {

    String groupId
    String sdkVersion

    public NodeAdder(String groupId,String sdkVersion){
        this.groupId=groupId
        this.sdkVersion=sdkVersion
    }

    def pomDependency = new ArrayList()

    static class PomNode {
        String groupId
        String artifactId
        String sdkVersion

        PomNode(String groupId,
                String artifactId,
                String sdkVersion) {
            this.groupId=groupId
            this.artifactId=artifactId
            this.sdkVersion=sdkVersion
        }

        public String toString(){
            return "${groupId}:${artifactId}:${sdkVersion}"
        }
    }


    public  void addDependencyNode(ResolvedDependency resolvedDependency, Node dependenciesNode) {
        def pomNode=new PomNode(resolvedDependency.moduleGroup,resolvedDependency.name,resolvedDependency.moduleVersion)
        addDependencyNode(pomNode,dependenciesNode)

    }

    public  void addDependencyNode(Project project, Node dependenciesNode) {
        def pomNode=new PomNode(groupId,project.name,sdkVersion)
        addDependencyNode(pomNode,dependenciesNode)

    }

    public void addDependencyNode(Node node,Node dependenciesNode){
        dependenciesNode.append(node)
    }

    public  Node addDependencyNode(PomNode pomNode,Node dependenciesNode){
        def has=hasNode(pomNode,dependenciesNode)
        if(!has){
            pomDependency.add(pomNode)
            def dependencyNode = dependenciesNode.appendNode('dependency')
            dependencyNode.appendNode('groupId', pomNode.groupId)
            dependencyNode.appendNode('artifactId', pomNode.artifactId)
            dependencyNode.appendNode('version', pomNode.sdkVersion)
            return dependencyNode
        }
    }

    public  addDependencyNode(Dependency dependency, Node dependenciesNode) {
        def pomNode=new PomNode(dependency.group,dependency.name,dependency.version)
        if(!hasNode(pomNode,dependenciesNode)){
            def node=addDependencyNode(pomNode,dependenciesNode)
            if (dependency.excludeRules.size() > 0) {
                def exclusionsNode = node.appendNode('exclusions')
                dependency.excludeRules.each { rule ->
                    def exclusionNode = exclusionsNode.appendNode('exclusion')
                    if(rule.group!=null){
                        exclusionNode.appendNode('groupId', rule.group)
                    }
                    if(rule.module!=null){
                        exclusionNode.appendNode('artifactId', rule.module)
                    }
                }
            }
        }
    }

    private boolean hasNode(PomNode pomNode,Node dependenciesNode){
        def result=false
        dependenciesNode.children().each {
            if(it.getAt("groupId").get(0).value().equals(pomNode.groupId)&&
                    it.getAt("artifactId").get(0).value().equals(pomNode.artifactId)&&
                    it.getAt("version").get(0).value().equals(pomNode.sdkVersion)){
                result=true
            }
        }
        return result
    }
}
