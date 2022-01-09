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

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.lizhi.tiya.config.PropertyFileConfig
import org.lizhi.tiya.dependency.DependencyReplaceHelper
import org.lizhi.tiya.extension.ProjectExtension
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.project.ModuleProject
import org.lizhi.tiya.task.AARBuilderTask

/**
 * 插件入口
 */
class FastBuilderPlugin : Plugin<Project>, IPluginContext {

    // apply的工程
    private lateinit var project: Project

    // aar构建task
    private lateinit var aarBuilderTask: AARBuilderTask

    // 工程的配置项
    private lateinit var projectExtension: ProjectExtension

    // 依赖处理帮助类
    private lateinit var dependencyReplaceHelper: DependencyReplaceHelper

    // 配置文件
    private lateinit var propertyFileConfig: PropertyFileConfig

    // module工程集合
    private var moduleProjectList: List<ModuleProject> = emptyList()


    override fun apply(project: Project) {
        this.project = project

        // 注册Project的配置项
        this.projectExtension = project.extensions.create<ProjectExtension>(
            "moduleArchive",
            ProjectExtension::class.java,
            project
        )
        // 初始化配置文件
        this.propertyFileConfig = PropertyFileConfig(this)
        // 注册构建的task
        this.aarBuilderTask = project.tasks.register("AARBuilderTask", AARBuilderTask::class.java, this).get().apply {
            doLast {
                propertyFileConfig.saveConfig()
            }
        }
        // 初始化依赖替换帮助类
        this.dependencyReplaceHelper = DependencyReplaceHelper(this)

        // 全局配置完成后执行
        project.gradle.projectsEvaluated {
            if (!projectExtension.pluginEnable) {
                return@projectsEvaluated
            }
            val androidExtension = project.extensions.getByName("android") as BaseAppModuleExtension
            androidExtension.applicationVariants.all { variant ->
                // 在assemble任务之后执行aar的构建任务
                variant.assembleProvider.get().finalizedBy(this.aarBuilderTask)
            }

            var starTime = System.currentTimeMillis();
            //赋值日志是否启用
            FastBuilderLogger.enableLogging = projectExtension.logEnable

            // 设置module工程的flat仓库,为后续添加aar做准备
            // https://issuetracker.google.com/issues/165821826
            for (childProject in project.rootProject.childProjects) {
                childProject.value.repositories.flatDir { flatDirectoryArtifactRepository ->
                    flatDirectoryArtifactRepository.dir(projectExtension.storeLibsDir)
                    flatDirectoryArtifactRepository.dir(projectExtension.storeSelfLibsDir)
                }
            }
            // 获取有效的启动任务,若没有配置,则采主工程命名的task
            val launcherTaskName = project.gradle.startParameter.taskNames.firstOrNull { taskName ->
                if (projectExtension.detectLauncherRegex.isNullOrBlank()) {
                    taskName.contains(project.name)
                } else {
                    taskName.contains(projectExtension.detectLauncherRegex)
                }
            }
            // 避免无效的任务执行
            if (launcherTaskName.isNullOrBlank()) {
                FastBuilderLogger.logLifecycle("检测任务不相关不启用替换逻辑")
                return@projectsEvaluated
            }
            // 初始化module工程
            moduleProjectList = propertyFileConfig.prepareByConfig()

            // 设置task输出目录
            aarBuilderTask.aarOutDir(projectExtension.storeLibsDir)

            // 开始替换依赖
            dependencyReplaceHelper.replaceDependency()

            val endTime = System.currentTimeMillis();
            FastBuilderLogger.logLifecycle("插件花費的配置時間${endTime - starTime}")
        }
    }

    override fun getContext(): IPluginContext = this

    override fun getProjectExtension(): ProjectExtension = projectExtension

    override fun getDependencyReplaceHelper(): DependencyReplaceHelper = dependencyReplaceHelper

    override fun getAARBuilderTask(): AARBuilderTask = aarBuilderTask

    override fun getApplyProject(): Project = project

    override fun getPropertyConfig(): PropertyFileConfig = propertyFileConfig

    override fun getModuleProjectList(): List<ModuleProject> = moduleProjectList
}