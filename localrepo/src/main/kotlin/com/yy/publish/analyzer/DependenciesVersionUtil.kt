package com.yy.publish.analyzer

/**
 * Created by wangfeihang on 2019-08-26.
 */
object DependenciesVersionUtil {
    fun getVersion(originVersion: String): String {
        return if (!originVersion.endsWith("-SNAPSHOT")) {
            "$originVersion-unionlocal-SNAPSHOT"
        } else {
            originVersion.subSequence(0, originVersion.indexOf("-SNAPSHOT")).toString() + "-unionlocal-SNAPSHOT"
        }
    }
}