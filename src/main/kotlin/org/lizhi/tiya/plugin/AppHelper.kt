package org.lizhi.tiya.plugin

import org.gradle.api.Project

object AppHelper {


    fun obtainLastModified(project: Project): Long {
        val file = project.fileTree(".").matching { patterns ->
            patterns.exclude("build", ".gradle", ".cxx")
        }.toList().maxBy {
            it.lastModified()
        }
        return file?.lastModified() ?: 0
    }
}