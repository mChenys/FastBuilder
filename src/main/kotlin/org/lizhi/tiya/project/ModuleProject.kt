/*
 * Copyright (C) 2021 Tiya.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lizhi.tiya.project

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.lizhi.tiya.extension.ModuleExtension
import org.lizhi.tiya.plugin.IPluginContext
import java.io.File

/**
 * 和module对应的工程
 */
class ModuleProject(val moduleExtension: ModuleExtension, private val pluginContext: IPluginContext) {

    /**
     * 缓存是否有效
     */
    var cacheValid = false

    /**
     * 对应的project
     */
    private var project: Project? = null

    /**
     * 缓存的aar文件的地址
     */
    private var aarFile: File? = null

    /**
     * 最後修改文件夾的時間
     */
    private var lastModified: Long = 0

    /**
     * 对应替换的后的依赖声明
     */
    private var dependency: Dependency? = null


    /**
     * 这个工程依赖的其他module工程
     */
    var dependencyModuleProjectList: MutableSet<ModuleProject> = mutableSetOf()


    /**
     * 获取关联的project对象
     */
    fun obtainProject(): Project {
        if (this.project == null) {
            this.project = pluginContext.getApplyProject().project(obtainName())

        }
        return this.project!!
    }

    /**
     * 获取缓存的aar文件地址
     */
    fun obtainCacheAARFile(): File {
        if (this.aarFile == null) {
            val extension = pluginContext.getProjectExtension()
            aarFile = File(extension.moduleAarsDir, moduleExtension.aarName)
        }
        return aarFile!!
    }


    /**
     * 这个project文件下的最后修改时间
     */
    fun obtainLastModified(): Long {
        if (lastModified <= 0) {
            val moduleProject = obtainProject()
            val file = moduleProject.fileTree(".").matching { patterns ->
                patterns.exclude("build", ".gradle", ".cxx")
            }.toList().maxBy {
                it.lastModified()
            }
            lastModified = file?.lastModified() ?: 0
        }
        return lastModified
    }

    /**
     * 这个对象的名称
     */
    fun obtainName(): String = moduleExtension.name


    /**
     * 获取替换的aar依赖声明
     */
    fun obtainAARDependency(): Dependency {
        if (this.dependency == null) {
            val obtainProject = obtainProject()
            this.dependency = obtainProject.dependencies.create(
                mapOf(
                    "name" to obtainCacheAARFile().name.replace(
                        ".aar",
                        ""
                    ), "ext" to "aar"
                )
            )
        }
        return dependency as Dependency
    }

    /**
     * 当前依赖被引用
     */
    var flagHasOut: Boolean = false


}