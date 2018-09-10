package com.yy.localrepo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.Upload
import org.gradle.util.GradleVersion

public class GenerateRepoImpl implements Plugin<Project> {

    def localArtifacts = new ArrayList<ResolvedArtifact>()

    def repoArtifacts = new ArrayList()

    def gradleVersionStr = GradleVersion.current().getVersion()
    def gradleApiVersion = gradleVersionStr.substring(0, gradleVersionStr.lastIndexOf(".")).toFloat()
    def root_dir


    def groupId = 'com.yy.rice'
    def artifactId = 'mibosdklala'
    def sdkVersion = '1.0.4'
    def repo = 'E:\\GitHub\\midwares\\MyApplication\\repolala'


    def pomDependency = new ArrayList()
    def repoUri

    private boolean isLocal(ResolvedArtifact artifact) {
        def artifactStr = artifact.toString()
        return artifactStr.contains("yy") || artifactStr.contains("duowan") || artifactStr.contains("athena")
    }

    private initUpload(final Project project) {
        println("wang initUpload")
        project.plugins.apply('maven')
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    //设置插件的GAV参数
                    pom.groupId = groupId
                    pom.artifactId = artifactId
                    pom.version = sdkVersion
                    //文件发布到下面目录
                    repository(url: repoUri)
                    pom.withXml {
                        getAllLocalDependency(project)


                        Node dependenciesNode = asNode().getAt("dependencies")[0]
                        if (dependenciesNode != null) {
                            dependenciesNode.replaceNode {}
                        } else {
                            dependenciesNode = asNode().appendNode('dependencies')
                        }

//                        def extensionsNode=asNode().appendNode('extensions')
//                        def extensionNode = extensionsNode.appendNode('extension')
//                        extensionNode.appendNode('artifactId', "wagon-webdav-jackrabbit")
//                        extensionNode.appendNode('groupId', "org.apache.maven.wagon")
//                        extensionNode.appendNode('version', "2.2")

                        def configurationNames = ['api', 'implementation']

                        configurationNames.each { configurationName ->
                            def allDependencies = project.configurations[configurationName].allDependencies
                            allDependencies.each {

                                def islocal = false
                                localArtifacts.each { local ->
                                    if ("${it.group}:${it.name}:${it.version}" == local.moduleVersion.toString()) {
                                        islocal = true
                                    }
                                }

                                if (it.group != null && it.name != null && !islocal && !pomDependency.contains(it)) {

                                    pomDependency.add(it)

                                    def dependencyNode = dependenciesNode.appendNode('dependency')
                                    dependencyNode.appendNode('groupId', it.group)
                                    dependencyNode.appendNode('artifactId', it.name)
                                    dependencyNode.appendNode('version', it.version)
                                    //If there are any exclusions in dependency
                                    if (it.excludeRules.size() > 0) {
                                        def exclusionsNode = dependencyNode.appendNode('exclusions')
                                        it.excludeRules.each { rule ->
                                            def exclusionNode = exclusionsNode.appendNode('exclusion')
                                            exclusionNode.appendNode('groupId', rule.group)
                                            exclusionNode.appendNode('artifactId', rule.module)
                                        }
                                    }
                                }
                            }
                        }

                        repoArtifacts.each { repoArtifact ->
                            boolean isAdded = false
                            pomDependency.each {
                                if ("${it.group}:${it.name}:${it.version}" == repoArtifact.moduleVersion.toString()) {
                                    isAdded = true
                                }
                            }
                            if (!isAdded) {
                                def dependencyNode = dependenciesNode.appendNode('dependency')
                                dependencyNode.appendNode('groupId', repoArtifact.moduleVersion.id.group)
                                dependencyNode.appendNode('artifactId', repoArtifact.moduleVersion.id.name)
                                dependencyNode.appendNode('version', repoArtifact.moduleVersion.id.version)
                            }

                        }
                        asNode().appendNode(dependenciesNode)
                    }
                }
            }
            project.uploadArchives.doLast {
                copyDependencyFile(project)
            }
        }
    }


    private void getAllLocalDependency(final ProjectInternal project) {
        def allDependencies = new ArrayList(project.configurations.embedded.resolvedConfiguration.resolvedArtifacts)
        if (allDependencies != null && !allDependencies.isEmpty()) {
            allDependencies.reverseEach {
                artifact ->
                    if (isLocal(artifact) && !localArtifacts.contains(artifact)) {
                        localArtifacts.add(artifact)
                    }
                    if (!isLocal(artifact) && !repoArtifacts.contains(artifact)) {
                        repoArtifacts.add(artifact)
                    }
            }
        }
    }

    private void copyDependencyFile(Project project) {
        def libsPath = "$repo/${groupId.replace(".", "/")}/${artifactId}/${sdkVersion}/libs"

//        delete "${libsPath}"
        localArtifacts.forEach {
            artifact ->
                def artifactPath = artifact.file
                if (artifact.type == 'aar') {
                    project.copy {
                        from artifactPath
                        into project.file(libsPath)
                    }
                } else if (artifact.type == 'jar') {
                    project.copy {
                        from artifactPath
                        into project.file(libsPath)
                    }
                } else if (artifact.type == 'so') {
                    project.copy {
                        from artifactPath
                        into project.file("${libsPath}/armeabi-v7a")
                        exclude("*x86.so")
                        exclude("*armeabi.so")
                    }
                } else {
                    throw new Exception("Unhandled Artifact of type ${artifact.type}")
                }
        }
    }

    @Override
    void apply(Project project) {

        root_dir = project.rootDir.absolutePath.replace(File.separator, '/')

        project.configurations {
            embedded
        }

        project.dependencies {
            compile project.configurations.embedded
        }
        repoUri = project.uri(repo)

        initUpload(project)
    }
}