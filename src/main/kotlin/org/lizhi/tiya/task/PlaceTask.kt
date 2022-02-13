package org.lizhi.tiya.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class PlaceTask :DefaultTask() {
    @TaskAction
    fun perform() {
        println("xxxxxxxxx")
    }
}