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
import org.gradle.api.Task
import org.gradle.api.artifacts.*
import org.lizhi.tiya.config.PropertyFileConfig
import org.lizhi.tiya.dependency.DependencyReplaceHelper
import org.lizhi.tiya.dependency.DependencyUtils
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

    private var taskProjectList: MutableList<Task> = mutableListOf()


    override fun apply(project: Project) {
        this.project = project

        project.gradle.taskGraph.whenReady {

            for (allTask in it.allTasks) {
                println("无环图构造完成${allTask.project} ${allTask.name}")

            }
            for (allproject in project.rootProject.allprojects) {
            allproject.configurations.all { con->
                con.dependencies.all {d->
                    if (d is ProjectDependency) {
                        if (d.name.contains("base")) {
                            println("${allproject.name} ${con.name} ${d.name} ${d is ProjectDependency}")
                        }
                    }

                }
            }
        }

        }

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
            // 设置task输出目录
            aarOutDir(projectExtension.moduleAarsDir)
        }
        // 初始化依赖替换帮助类
        this.dependencyReplaceHelper = DependencyReplaceHelper(this)
        val androidExtension = project.extensions.getByName("android") as BaseAppModuleExtension

        project.rootProject.findProject(":image-picker")?.afterEvaluate {
            println()
        }


        project.afterEvaluate {
            val starTime = System.currentTimeMillis();


            //赋值日志是否启用
            FastBuilderLogger.enableLogging = projectExtension.logEnable

            // 初始化module工程
            moduleProjectList = propertyFileConfig.prepareByConfig()


            if (!projectExtension.pluginEnable) {
                return@afterEvaluate
            }

            // 设置module工程的flat仓库,为后续添加aar做准备
            // https://issuetracker.google.com/issues/165821826
            for (childProject in project.rootProject.childProjects) {
                childProject.value.repositories.flatDir { flatDirectoryArtifactRepository ->
                    flatDirectoryArtifactRepository.dir(projectExtension.moduleAarsDir)
                    flatDirectoryArtifactRepository.dir(projectExtension.thirdPartyAarsDir)
                }
            }

            androidExtension.applicationVariants.all { variant ->
                variant.assembleProvider.get().finalizedBy(aarBuilderTask)
            }
            for (moduleProject in moduleProjectList) {
                //緩存有效
                if (!moduleProject.cacheValid) {
                    AARBuilderTask.prepare(this, moduleProject)
                }
            }




            val cacheList = moduleProjectList.filter { it.cacheValid }
//            for (moduleProject in cacheList) {
//                val maybeCreate = project.configurations.maybeCreate("forFastPlugin")
//                maybeCreate.dependencies.add(project.dependencies.create(moduleProject.obtainProject()))
//                moduleProject.obtainProject().configurations.all {con->
//                    con.withDependencies {
//                        println()
//                    }
//                }
//            }
            val nameList = cacheList.map { it.obtainName().replace(":", "") }
            for (currentProject in project.rootProject.allprojects) {
                currentProject.repositories.flatDir { flatDirectoryArtifactRepository ->
                    flatDirectoryArtifactRepository.dir(projectExtension.moduleAarsDir)
                    flatDirectoryArtifactRepository.dir(projectExtension.thirdPartyAarsDir)
                }


                currentProject.configurations.all { con ->
                    DependencyUtils.suppressionDeChange(con)
                    if (DependencyUtils.configIsMatchEnd(con)) {
                        con.withDependencies { set ->
                            val deCopy = mutableListOf<Dependency>()
                            deCopy.addAll(set)
                            for (dependency in deCopy) {
                                println("截取依赖 ${currentProject.name}:${con.name}:${dependency.name}")

                                if (dependency !is ProjectDependency) {
                                    continue
                                }

                                val dependencyProject = dependency.dependencyProject
                                // 根据依赖工程的名字获取对应的包装类ModuleProject
                                val dependencyModuleProject =
                                    moduleProjectList.firstOrNull { it.moduleExtension.name == dependencyProject.path }

                                // 如果当前模块工程在配置项中注册过且生效的才需要处理
                                if (dependencyModuleProject != null && dependencyModuleProject.moduleExtension.enable) {
                                    dependencyReplaceHelper.replaceDependency(currentProject,dependencyModuleProject.obtainProject())
                                }


                            }


                        }
                    }


                }

            }



            val endTime = System.currentTimeMillis();
            FastBuilderLogger.logLifecycle("插件花費的配置時間${endTime - starTime}")

        }

        project.gradle.projectsEvaluated {
            for (allproject in project.rootProject.allprojects) {
                allproject.configurations.all { con->
                    con.dependencies.all {d->
                        if (d.name.contains("base")) {
                            println("${allproject.name} ${con.name} ${d.name} ${d is ProjectDependency}")
                        }
                    }
                }
            }
        }


    }

    override fun getContext(): IPluginContext = this

    override fun getProjectExtension(): ProjectExtension = projectExtension

    override fun getDependencyReplaceHelper(): DependencyReplaceHelper = dependencyReplaceHelper

    override fun getAARBuilderTask(): AARBuilderTask = aarBuilderTask

    override fun getApplyProject(): Project = project

    override fun getPropertyConfig(): PropertyFileConfig = propertyFileConfig

    override fun getModuleProjectList(): List<ModuleProject> = moduleProjectList
    override fun getApkTaskList(): List<Task> {
        return taskProjectList
    }
}