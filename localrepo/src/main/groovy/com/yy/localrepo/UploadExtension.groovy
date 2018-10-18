package com.yy.localrepo

import org.gradle.api.Project

class UploadExtension {
    protected Project project


    String groupId
    String artifactId
    String sdkVersion
    String repo
    Closure isLocal

    UploadExtension(Project project) {
        this.project = project
    }

    void setProject(Project project) {
        this.project = project
    }

    void setGroupId(String groupId) {
        this.groupId = groupId
    }

    void setArtifactId(String artifactId) {
        this.artifactId = artifactId
    }

    void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion
    }

    void setRepo(String repo) {
        this.repo = repo
    }

    void setIsLocal(Closure isLocal) {
        this.isLocal = isLocal
    }

    Project getProject() {
        return project
    }

    String getGroupId() {
        return groupId
    }

    String getArtifactId() {
        return artifactId
    }

    String getSdkVersion() {
        return sdkVersion
    }

    String getRepo() {
        return repo
    }

    Closure getIsLocal() {
        return isLocal
    }


    @Override
    public String toString() {
        return "UploadExtension{" +
                "project=" + project +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", sdkVersion='" + sdkVersion + '\'' +
                ", repo='" + repo + '\'' +
                ", isLocal=" + isLocal +
                '}';
    }
}