package com.yy.publish.analyzer

import org.gradle.api.artifacts.Dependency

/**
 * Created by wangfeihang on 2019-08-21.
 */
abstract class DefaultFakeDependency : Dependency {
    override fun contentEquals(p0: Dependency): Boolean {
        return false
    }

    override fun copy(): Dependency {
        return this
    }

    override fun getGroup(): String? {
        return null
    }

    override fun getName(): String {
        return ""
    }

    override fun getVersion(): String? {
        return null
    }

    override fun toString(): String {
        return "$group:$name:$version"
    }
}