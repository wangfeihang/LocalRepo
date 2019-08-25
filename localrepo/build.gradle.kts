

plugins {
    groovy
    `kotlin-dsl`
    kotlin("jvm") version "1.2.20"
    "com.github.dcendents.android-maven"
}

dependencies {
    impl(
            gradleApi(),
            localGroovy(),
            "com.android.tools.build:gradle:3.1.2",
            kotlin("stdlib"),
            "org.apache.httpcomponents:httpclient:4.5.2"
    )
}

fun DependencyHandlerScope.impl(vararg dependency: Any) {
    dependency.forEach {
        "implementation"(it)
    }
}

group = "com.github.wangfeihang"
version = "1.0.1"