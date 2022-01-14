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

package org.lizhi.tiya.config

import org.gradle.api.Project
import org.gradle.internal.impldep.aQute.bnd.annotation.component.Modified
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptWithKotlincTask
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.plugin.AppHelper
import org.lizhi.tiya.plugin.IPluginContext
import org.lizhi.tiya.project.ModuleProject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * 配置文件
 */
class PropertyFileConfig(private val pluginContext: IPluginContext) {

    private val props = Properties()

    var isInit = false

    private val fileName = "FastBuilder.properties"

    private fun getPropertyInfo(): Properties {
        if (!isInit) {
            val moduleAarsDir = pluginContext.getProjectExtension().moduleAarsDir
            if (!moduleAarsDir.exists()) {
                moduleAarsDir.mkdirs()
            }
            val configFile = File(moduleAarsDir, fileName)
            if (!configFile.exists()) {
                configFile.createNewFile()
            } else {
                props.load(FileReader(configFile))
            }
            if (configFile.exists()) {
                props.load(FileReader(configFile))
            }
        }
        isInit = true
        return props
    }

    /**
     * 是否存在配置文件
     */
    fun existConfigFile(): Boolean {
        val configFile = File(pluginContext.getProjectExtension().moduleAarsDir, fileName)
        return configFile.exists() && configFile.length() > 0
    }


    /**
     * 缓存是否有效
     */
    fun isCacheValid(project: ModuleProject): Boolean {
        val aarFile = project.obtainCacheAARFile()
        if (!aarFile.exists()) {
            FastBuilderLogger.logLifecycle("${project.moduleExtension.name} don't find cache aar.")
            return false
        }
        val propertyInfo = getPropertyInfo()
        val currentModify = project.obtainLastModified()
        val lastModify = propertyInfo.getProperty(project.moduleExtension.aarName)
        if (currentModify.toString() == lastModify) {
            FastBuilderLogger.logLifecycle("${project.moduleExtension.name}  found cache aar.")
            return true
        }
        FastBuilderLogger.logLifecycle("${project.moduleExtension.name} aar cache invalid.")
        return false
    }

    /**
     * 保存缓存
     */
    fun updateModify(project: ModuleProject) {
        val propertyInfo = getPropertyInfo()
        val curAARProLastModified = project.obtainLastModified()
        propertyInfo.setProperty(project.moduleExtension.aarName, curAARProLastModified.toString())
    }

    /**
     * 保存配置
     */
    fun saveConfig() {
        val propertyInfo = getPropertyInfo()
        val moduleAarsDir = pluginContext.getProjectExtension().moduleAarsDir
        val configFile = File(moduleAarsDir, fileName)
        propertyInfo.store(FileWriter(configFile), "用于存储缓存aar映射关系")
    }

    /**
     * 通过配置初始化
     */
    fun prepareByConfig(): List<ModuleProject> {
        // 读取工程配置的子模块配置,并转成对应的模块工程对象
        val moduleProjectList = pluginContext.getProjectExtension().moduleExtension.toList().map {
            it.convertModuleProject(pluginContext)
        }
        //启用了kapt优化
        val countDownLatch = if (pluginContext.getProjectExtension().kaptOptimization) {
            CountDownLatch(moduleProjectList.size + 1)
        } else {
            CountDownLatch(moduleProjectList.size)
        }
        // 开启多线程处理
        for (moduleProject in moduleProjectList) {
            thread {
                moduleProject.cacheValid = isCacheValid(moduleProject)
                countDownLatch.countDown()
            }
        }
        if (pluginContext.getProjectExtension().kaptOptimization) {
            thread {
                //禁用kapt
                if (pluginContext.getPropertyConfig().appIsCacheValid()) {

                    val applyProject = pluginContext.getApplyProject()
                    val kaptGenerateStubsTask = applyProject.tasks.withType(KaptGenerateStubsTask::class.java)
                    val KaptWithKotlincTask = applyProject.tasks.withType(KaptWithKotlincTask::class.java)
                    for (kaptGenerateStubsTask in kaptGenerateStubsTask) {
                        kaptGenerateStubsTask.onlyIf {
                            false
                        }
                    }
                    for (kaptWithKotlincTask in KaptWithKotlincTask) {
                        kaptWithKotlincTask.onlyIf {
                            false
                        }
                    }
                    FastBuilderLogger.logLifecycle("App 模块Kapt 缓存有效 ${kaptGenerateStubsTask.size} ${KaptWithKotlincTask.size}")
                } else {
                    FastBuilderLogger.logLifecycle("App 模块Kapt 缓存无效")
                }
                countDownLatch.countDown()
            }
        }

        countDownLatch.await()

        return moduleProjectList
    }

    fun saveAppLastModified() {
        val propertyInfo = getPropertyInfo()
        propertyInfo[pluginContext.getApplyProject().name.replace(":", "")] = "$appLastModified"
    }

    fun obtainAppLastModifiedFromConfig(): Long {
        val propertyInfo = getPropertyInfo()
        val name = pluginContext.getApplyProject().name.replace(":", "")
        return propertyInfo.getProperty(name, "0").toLong()
    }

    var appLastModified = 0L

    /**
     * app的缓存是否有效
     */
    fun appIsCacheValid(): Boolean {
        if (appLastModified == 0L) {
            appLastModified = AppHelper.obtainLastModified(pluginContext.getApplyProject())
        }

        return obtainAppLastModifiedFromConfig() == appLastModified

    }
}