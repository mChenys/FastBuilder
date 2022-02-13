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

package org.lizhi.tiya.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.lizhi.tiya.config.PropertyFileConfig
import org.lizhi.tiya.dependency.DependencyReplaceHelper
import org.lizhi.tiya.extension.ProjectExtension
import org.lizhi.tiya.project.ModuleProject
import org.lizhi.tiya.task.AARBuilderTask

/**
 * 插件上下文
 */
interface IPluginContext {

    /**
     * 获取插件上下文
     */
    fun getContext(): IPluginContext

    /**
     * 获取构建的配置项
     */
    fun getProjectExtension(): ProjectExtension

    /**
     * 获取依赖帮助类
     */
    fun getDependencyReplaceHelper(): DependencyReplaceHelper

    /**
     * 获取aar构建任务
     */
    fun getAARBuilderTask(): AARBuilderTask

    /**
     * 获取插件所依赖的工程
     */
    fun getApplyProject(): Project

    /**
     * 获取配置文件
     */
    fun getPropertyConfig(): PropertyFileConfig

    /**
     * 获取模块的工程集合
     */
    fun getModuleProjectList():List<ModuleProject>

    fun getApkTaskList():List<Task>
}