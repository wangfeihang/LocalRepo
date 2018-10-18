package com.yy.localrepo

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.util.GradleVersion

public class GenerateRepoImpl implements Plugin<Project> {

    def localArtifacts = new ArrayList<ResolvedArtifact>()
    def repoArtifacts = new ArrayList()

    def gradleVersionStr = GradleVersion.current().getVersion()
    def gradleApiVersion = gradleVersionStr.substring(0, gradleVersionStr.lastIndexOf(".")).toFloat()

    String groupId
    String artifactId
    String sdkVersion
    String repo
    Closure isLocal


    def pomDependency = new ArrayList()
    def repoUri

    private initUpload(final Project project) {
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
                        //分解
                        getAllLocalDependency(project)

                        Node dependenciesNode = asNode().getAt("dependencies")[0]
                        if (dependenciesNode != null) {
                            dependenciesNode.replaceNode {}
                        } else {
                            dependenciesNode = asNode().appendNode('dependencies')
                        }

                        def configurationNames = ['api', 'implementation']

                        configurationNames.each { configurationName ->
                            def allDependencies = project.configurations[configurationName].allDependencies
                            allDependencies.each {
                                hasAddedToLocal(it)
                                if (it.group != null && it.name != null && !pomDependency.contains(it)) {
                                    pomDependency.add(it)
                                    addDependencyNode(it, dependenciesNode)
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
                                addDependencyNode(repoArtifact, dependenciesNode)
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


    private static void addDependencyNode(ResolvedArtifact repoArtifact, Node dependenciesNode) {
        def dependencyNode = dependenciesNode.appendNode('dependency')
        dependencyNode.appendNode('groupId', repoArtifact.moduleVersion.id.group)
        dependencyNode.appendNode('artifactId', repoArtifact.moduleVersion.id.name)
        dependencyNode.appendNode('version', repoArtifact.moduleVersion.id.version)
    }

    private static void addDependencyNode(Dependency dependency, Node dependenciesNode) {
        def dependencyNode = dependenciesNode.appendNode('dependency')
        dependencyNode.appendNode('groupId', dependency.group)
        dependencyNode.appendNode('artifactId', dependency.name)
        dependencyNode.appendNode('version', dependency.version)
        //If there are any exclusions in dependency
        if (dependency.excludeRules.size() > 0) {
            def exclusionsNode = dependencyNode.appendNode('exclusions')
            dependency.excludeRules.each { rule ->
                def exclusionNode = exclusionsNode.appendNode('exclusion')
                exclusionNode.appendNode('groupId', rule.group)
                exclusionNode.appendNode('artifactId', rule.module)
            }
        }
    }

    private boolean hasAddedToLocal(Dependency dependency) {
        localArtifacts.each { local ->
            if ("${dependency.group}:${dependency.name}:${dependency.version}" == local.moduleVersion.toString()) {
                return true
            }
        }
        return false
    }


    private void getAllLocalDependency(final ProjectInternal project) {
        def allDependencies = new ArrayList(project.configurations.embedded.resolvedConfiguration.resolvedArtifacts)
        if (allDependencies != null && !allDependencies.isEmpty()) {
            allDependencies.reverseEach {
                artifact ->
                    if (isLocal.call(artifact) && !localArtifacts.contains(artifact)) {
                        localArtifacts.add(artifact)
                    }
                    if (!isLocal.call(artifact) && !repoArtifacts.contains(artifact)) {
                        repoArtifacts.add(artifact)
                    }
            }
        }
    }

    private void copyDependencyFile(Project project) {
        def libsPath = "$repo${File.separator}${groupId.replace(".", File.separator)}${File.separator}${artifactId}${File.separator}${sdkVersion}${File.separator}libs"
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
        project.configurations {
            embedded
        }
        project.dependencies {
            compile project.configurations.embedded
        }
        project.extensions.create('uploadConfigLocal', UploadExtension, project)
        project.afterEvaluate {
            def embeddedDependencies = project.configurations.embedded.dependencies
            def hasEmbeddedDependencies = embeddedDependencies != null && !embeddedDependencies.isEmpty()
            if (hasEmbeddedDependencies) {
                initUploadExtensions(project)
                initUpload(project)
            }
        }
    }

    private void initUploadExtensions(Project project) {
        UploadExtension uploadExtensions = project.uploadConfigLocal

        println("wang ${uploadExtensions}")


        if (uploadExtensions == null) {
            throw new GradleException('uploadConfig is null,please config first!')
        }

        groupId = uploadExtensions.groupId
        assert groupId != null, 'uploadConfig\'s groupId  must not be null, please config first!'

        artifactId = uploadExtensions.artifactId
        assert artifactId != null, 'uploadConfig\'s artifactId  must not be null, please config first!'


        sdkVersion = uploadExtensions.sdkVersion
        assert sdkVersion != null, 'uploadConfig\'s sdkVersion  must not be null, please config first!'


        repo = uploadExtensions.repo
        assert repo != null, 'uploadConfig\'s repo  must not be null, please config first!'

        isLocal = uploadExtensions.isLocal
        if (isLocal == null) {
            isLocal = {
                artifact ->
                    return true
            }
        }
        repoUri = project.uri(repo)
    }
}