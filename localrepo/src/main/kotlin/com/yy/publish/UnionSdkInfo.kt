package com.yy.publish

import com.yy.publish.analyzer.ExclusionNode

/**
 * Created by wangfeihang on 2019-08-19.
 */
data class UnionSdkInfo(
    val groupId: String,
    val artifactId: String,
    val sdkVersion: String,
    var isLocal: Boolean,
    var isProject: Boolean,
    var sdkPath: List<String>,
    val dependencies: List<UnionSdkInfo>?,
    val exclusions: List<ExclusionNode>?
) {

    var projectPath = ""

    //打印UnionSdkInfo的完整信息，包括全部依赖树
    fun customStr(prefixStr: String): String {
        var result =
            "\n$prefixStr$groupId\n$prefixStr$artifactId\n$prefixStr$sdkVersion\n$prefixStr Local:$isLocal\n" +
                    "$prefixStr isProject:$isProject\n$prefixStr$sdkPath\n$prefixStr exclusions:$exclusions\n$prefixStr dependencies:${dependencies == null}\n"
        dependencies?.forEach {
            result += it.customStr(prefixStr + "              ")
        }
        println("name:$artifactId,dependencies size:${dependencies?.size}, prefixStr:$prefixStr, ${prefixStr + "              "};")
        return result
    }

    //打印UnionSdkInfo的基本信息
    fun baseStr(): String {
        return "$groupId:$artifactId:$sdkVersion"
    }
}