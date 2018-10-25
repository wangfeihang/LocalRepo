package com.yy.localrepo

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.util.GradleVersion

public class GenerateRepoImpl implements Plugin<Project> {


    def gradleVersionStr = GradleVersion.current().getVersion()
    def gradleApiVersion = gradleVersionStr.substring(0, gradleVersionStr.lastIndexOf(".")).toFloat()

    String groupId
    String artifactId
    String sdkVersion
    String repo
    Closure isLocal
    Closure dontCopy

    def repoUri
    
    def nodeAdder


    def localDependency = new ArrayList()
    def repoDependency = new ArrayList()
    def dependedProjects = new ArrayList()
    
    def repoResolvedDependency=new ArrayList<ResolvedDependency>()
    def localResolvedDependency=new ArrayList<ResolvedDependency>()

    def innerRepoDependency = new ArrayList()

    def project



    private initUpload(final Project project) {
        analyzeAllDependencies(project)
        project.plugins.apply('maven')
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    //设置插件的GAV参数
                    pom.groupId = groupId
                    pom.artifactId = project.name
                    pom.version = sdkVersion
                    //文件发布到下面目录
                    repository(url: repoUri)
                    pom.withXml {
                        //分解
                        analyzeInnerDependencies(project)
                        Node dependenciesNode = asNode().getAt("dependencies")[0]
                        if(dependenciesNode!=null){
                            asNode().remove(dependenciesNode)
                        }
                        dependenciesNode = asNode().appendNode('dependencies')

                        innerRepoDependency.each {node->
                            nodeAdder.addDependencyNode(node,dependenciesNode)

                        }
                        repoDependency.each { dependency ->
//                            println("wang,repoDependency:${dependency}")
                            nodeAdder.addDependencyNode(dependency, dependenciesNode)
                        }
                        dependedProjects.each { dependedProject ->
//                            println("wang,dependedProjects:${dependedProject}")
                            nodeAdder.addDependencyNode(dependedProject, dependenciesNode)
                        }

                        repoResolvedDependency.each {dependency ->
//                            println("wang,repoResolvedDependency:${dependency}")
                            nodeAdder.addDependencyNode(dependency, dependenciesNode)
                        }
                    }
                }
            }
            project.uploadArchives.doLast {
                localDependency.each {
//                    println("wang,localDependency:${it}")
                    it.excludeRules.each{ rule->
                        println("exclude group: '${rule.group}', module: '${rule.module}'")
                    }
                    CopyDependency.copyDependencies(it, project, repo, dontCopy)
                }
                localResolvedDependency.each {
//                    println("wang,localResolvedDependency:${it}")
                    CopyDependency.copyDependencies(it,project,repo, dontCopy)
                }
            }
        }
        dependedProjects.each {
            project.getTasks().findByName("uploadArchives").dependsOn("${it.getPath()}:uploadArchives")
        }
    }

   


    private void analyzeAllDependencies(Project project) {
        project.configurations.each {
            if(it.name.equals("api")){
                analyzeSingleConfigurationsDependencies(project.configurations.api.getAllDependencies())
            }else if(it.name.equals("compile")){
                analyzeSingleConfigurationsDependencies(project.configurations.compile.getAllDependencies())
            }else if(it.name.equals("implementation")){
                analyzeSingleConfigurationsDependencies(project.configurations.implementation.getAllDependencies())
            }
        }

    }

    private void analyzeInnerDependencies(Project project){
        project.configurations.each {
            if(it.name.equals("api")){
                project.configurations.api.getAllDependencies().each{ dependency->
                    innerRepoDependency.addAll(CopyDependency.getInnerDependencies(dependency,project,isLocal,repo,dontCopy))
                }
            } else if(it.name.equals("compile")){
                project.configurations.compile.getAllDependencies().each{ dependency->
                    innerRepoDependency.addAll(CopyDependency.getInnerDependencies(dependency,project,isLocal,repo,dontCopy))
                }
            }else if(it.name.equals("implementation")){
                project.configurations.implementation.getAllDependencies().each { dependency ->
                    innerRepoDependency.addAll(CopyDependency.getInnerDependencies(dependency, project, isLocal, repo,dontCopy))
                }
            }

        }
    }

    private void analyzeSingleConfigurationsDependencies(DependencySet dependencies) {
        if (dependencies != null) {
            dependencies.each { dependency ->
                if (dependency instanceof ProjectDependency) {
                    dependedProjects.add(dependency.getDependencyProject())
                } else if (dependency instanceof ExternalModuleDependency) {
                    if (isLocal.call(dependency)) {
                        localDependency.add(dependency)
                    } else {
                        repoDependency.add(dependency)
                    }
                }
            }
        }
    }

    @Override
    void apply(Project project) {
        this.project=project
        project.extensions.create('uploadConfigLocal', UploadExtension, project)
        project.afterEvaluate {
            initUploadExtensions(project)
            initUpload(project)
        }
    }

    private void initUploadExtensions(Project project) {
        UploadExtension uploadExtensions = project.uploadConfigLocal

        println("GenerateRepoImpl, ${uploadExtensions}")


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

        dontCopy=uploadExtensions.dontCopy
        if (dontCopy == null) {
            dontCopy = {
                artifact ->
                    return false
            }
        }

        repoUri = project.uri(repo)
        nodeAdder=new NodeAdder(groupId,sdkVersion)
        
    }
}