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

package org.lizhi.tiya.task

import com.android.build.gradle.LibraryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.plugin.IPluginContext
import org.lizhi.tiya.project.ModuleProject
import java.io.File
import javax.inject.Inject

/**
 * 模块aar构建和拷贝处理,此task是多个模块工程共用的
 */
abstract class AARBuilderTask @Inject constructor(@Internal val pluginContext: IPluginContext) : DefaultTask() {

    @InputFiles
    @SkipWhenEmpty
    abstract fun getInputAARList(): ConfigurableFileCollection

    @OutputDirectory
    var outPutDirFile = File(".")

    @Internal
    var moduleProjectCacheMap = HashMap<String, ModuleProject>()

    @TaskAction
    fun perform() {
        // 设置拷贝任务
        project.copy {
            it.from(getInputAARList())
            it.into(outPutDirFile)
            it.rename { name ->
                val moduleProject = moduleProjectCacheMap[name]
                if (moduleProject != null) {
                    FastBuilderLogger.logLifecycle("Copy aar  from $name to ${moduleProject.moduleExtension.aarName}.")
                    // 更新记录
                    pluginContext.getPropertyConfig().updateModify(moduleProject)
                    return@rename moduleProject.moduleExtension.aarName
                }
                name
            }
        }
    }

    /**
     * 设置aar输入源
     */
    fun aarInput(
        taskProvider: TaskProvider<Zip>,
        moduleProject: ModuleProject,
        packageLibraryProvider: TaskProvider<Task>
    ) {
        getInputAARList().from(taskProvider)
        // 兼容高本版
        dependsOn(packageLibraryProvider.get())
        val zip = taskProvider.get()
        val archiveFileName = zip.archiveFileName.get()
        moduleProjectCacheMap[archiveFileName] = moduleProject
    }

    /**
     * 设置aar输出路径
     */
    fun aarOutDir(storeLibsDir: File) {
        outPutDirFile = storeLibsDir
    }

    /**
     * 声明aar的构建
     */
    companion object {
        fun prepare(context: IPluginContext, moduleProject: ModuleProject) {
            // 构建子模块的Project
            val project = moduleProject.obtainProject()
            project.plugins.all { plugin ->
                // 保险起见,这里还是判断下Android module工程
                if (plugin is com.android.build.gradle.LibraryPlugin) {
                    val android: LibraryExtension = project.extensions.getByName("android") as LibraryExtension
                    android.libraryVariants.all { variant ->
                        if (moduleProject.moduleExtension.useDebug
                            && variant.buildType.name.equals("debug", true) // 仅处理debug包
                            && variant.flavorName == moduleProject.moduleExtension.flavorName
                        ) {
                            val packageLibraryProvider = variant.packageLibraryProvider
                            // 构建aar输入源和输出源关联
                            context.getAARBuilderTask()
                                .aarInput(packageLibraryProvider, moduleProject, variant.assembleProvider)
                            FastBuilderLogger.logLifecycle("${moduleProject.obtainName()}:  aar join build")
                            return@all
                        }
                    }
                }
            }
        }
    }
}