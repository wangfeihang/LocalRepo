package com.yy.localrepo

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedDependency

import javax.print.DocFlavor.STRING

/**
 * Created by wangfeihang on 2018/10/19.
 */
class CopyDependency {

    static copyDependencies(Dependency dependency, Project project, String libsPath, Closure dontCopy) {
        println("CopyDependency,need copy${dependency}")
        def dependencyPath = project.gradle.getGradleUserHomeDir().path + "/caches/modules-2/files-2.1/"
        dependencyPath += dependency.group + "/" + dependency.name + "/" + dependency.version + "/"
        println("the dependency path is:${dependencyPath}")
        project.fileTree(dependencyPath).getFiles().each { file ->
            copyFile(file, project, libsPath, dontCopy)
        }
    }

    static copyDependencies(ResolvedDependency dependency, Project project, String libsPath, Closure dontCopy) {
        println("CopyDependency,need copy:${dependency}")
        dependency.getModuleArtifacts().each { resolvedArtifact ->
            copyFile(resolvedArtifact.file, project, libsPath, dontCopy)
        }
    }

    private static void copyFile(File file, Project project, String libsPath, Closure dontCopy) {
        if (file.name.contains("sources") || dontCopy.call(file.name)) {
            println("dontCopy worked")
            return
        }
        if (file.name.endsWith(".aar")) {
            println("copying----${file.name} ")
            project.copy {
                from file.path
                into project.file(libsPath)
            }
        } else if (file.name.endsWith(".jar")) {
            println("copying----${file.name} ")
            project.copy {
                from file.path
                into project.file(libsPath)
            }
        } else if (file.name.endsWith(".so")) {
            println("copying----${file.name} ")
            project.copy {
                from file.path
                into project.file("${libsPath}/armeabi-v7a")
                exclude("*x86.so")
                exclude("*armeabi.so")
            }
        }
    }

    static ArrayList<NodeAdder.PomNode> getInnerDependencies(Dependency dependency, Project project, Closure isLocal, String repo, Closure dontCopy) {
        def repoNodeResults = new ArrayList()
        def dependencyPath = project.gradle.getGradleUserHomeDir().path + "/caches/modules-2/files-2.1/"
        dependencyPath += dependency.group + "/" + dependency.name + "/" + dependency.version + "/"

        def libsPath = "$repo${File.separator}${Constants.LIB}"
        def unlocalLibsPath = "$repo${File.separator}${Constants.UN_LOCAL_LIB}"
        project.fileTree(dependencyPath).getFiles().each { file ->
            if (file.name.endsWith(".pom")) {
                processPomFile(file.path, project, repoNodeResults, isLocal,
                        libsPath, unlocalLibsPath, dontCopy)
            }
        }
        return repoNodeResults
    }

    private static processPomFile(
            String pomPath, Project project,
            ArrayList<NodeAdder.PomNode> repoNodes,
            Closure isLocal,
            String libsPath,
            String unLocalLibsPath,
            Closure dontCopy
    ) {
        def pom = new XmlSlurper().parse(new File(pomPath))
        pom.dependencies.children().each {
            def subJarLocation = project.gradle.getGradleUserHomeDir().path + "/caches/modules-2/files-2.1/"
            if (!it.scope.text().equals("test") && !it.scope.text().equals("provided")) {
                String version = it.version.text()
                if (version.startsWith("\${") && version.endsWith("}")) {
                    pom.properties.children().each {
                        if (version.contains(it.name())) {
                            version = it.text()
                        }
                    }
                }

                subJarLocation += it.groupId.text() + "/" + it.artifactId.text() + "/" + version + "/"
                def islocal = isLocal.call("${it.groupId.text()}:${it.artifactId.text()}:${version}")
                if (!islocal && version != null && !version.isEmpty() && !it.artifactId.text().contains("animal-sniffer-annotations") && !it.artifactId.text().contains("kotlin") && !it.artifactId.text().contains("jetbrains") && !it.groupId.text().contains("kotlin") && !it.groupId.text().contains("jetbrains")) {
                    repoNodes.add(new NodeAdder.PomNode(it.groupId.text(), it.artifactId.text(), version))
                } else if (!isLocal) {
                    project.fileTree(subJarLocation).getFiles().each { file ->
                        if (file.name.endsWith(".pom")) {
                            processPomFile(file.path, project, repoNodes, { false },
                                    libsPath, unLocalLibsPath, dontCopy)
                        } else {
                            copyFile(file, project, unLocalLibsPath, dontCopy)
                        }
                    }
                } else if (islocal) {
                    project.fileTree(subJarLocation).getFiles().each { file ->
                        if (file.name.endsWith(".pom")) {
                            processPomFile(file.path, project, repoNodes, isLocal,
                                    libsPath, unLocalLibsPath, dontCopy)
                        } else {
                            copyFile(file, project, libsPath, dontCopy)
                        }
                    }
                }
            }
        }
    }


}
