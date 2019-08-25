package com.yy.publish

import groovy.lang.Closure
import org.gradle.kotlin.dsl.closureOf

/**
 * Created by wangfeihang on 2019-08-19.
 */
open class UnionPublishRepoExtension {
    var local: Closure<Boolean> = closureOf<Boolean> { false } as Closure<Boolean>
    var dontCopy: Closure<Boolean> = closureOf<Boolean> { false } as Closure<Boolean>
    var onlyPublishProject: Boolean = false
    var groupId: String = ""
    var sdkVersion: String = ""
    var snapshotRepo: String = ""
    var releaseRepo: String = ""
    var repoUserName: String = ""
    var repoPassword: String = ""
}