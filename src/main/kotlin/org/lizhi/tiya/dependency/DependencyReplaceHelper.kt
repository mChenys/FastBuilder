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

package org.lizhi.tiya.dependency

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.plugin.IPluginContext
import org.lizhi.tiya.task.AARBuilderTask
import java.io.File

/**
 * 依赖替换帮助类
 */
class DependencyReplaceHelper(private val pluginContext: IPluginContext) {

    private val configList = mutableSetOf<String>("api", "runtimeOnly", "implementation")

    /**
     * 从根工程开始向下替换依赖
     */
    fun replaceDependency() {
        val starTime = System.currentTimeMillis()
        replaceSelfResolvingDependency()
        val endTime = System.currentTimeMillis()
        FastBuilderLogger.logLifecycle("替换files依赖耗时: ${endTime - starTime}")

        replaceDependency(pluginContext.getApplyProject())
    }


    /**
     * 用于替换 api files('xxx.aar') 依赖为 api(name:"xxx",ext:"aar")
     * files依赖移到父亲后目录改变会触发重新编译打包导致merge错误
     *
     * todo 这个函数性能太低了 需要优化
     */
    private fun replaceSelfResolvingDependency() {
        for (childProject in pluginContext.getApplyProject().rootProject.childProjects) {
            configFor@ for (configuration in childProject.value.configurations) {
                /**
                 * 此处是为了跳过不必要的config，只有后缀是api runtimeOnly implementation才处理
                 */
                var flag = false
                for (hitName in configList) {
                    if (configuration.name.endsWith(hitName, true)) {
                        flag = true
                        break
                    }
                }

                if (!flag) {
                    continue@configFor
                }

                val mutableSet = mutableSetOf<Dependency>()
                mutableSet.addAll(configuration.dependencies) // 这里转成可变集合来操作
                for (dependency in mutableSet) {
                    if (dependency is SelfResolvingDependency && dependency is FileCollectionDependency) {
                        val defaultSelfResolvingDependency = dependency as DefaultSelfResolvingDependency
                        val fileCollection = defaultSelfResolvingDependency.files
                        if (fileCollection.files.size != 1) {
                            continue
                        } else {

                            val singleFile = fileCollection.singleFile
                            if (!singleFile.name.endsWith("aar", true)) {
                                continue
                            }
                            //存在在执行拷贝操作
                            val intoFile = File(pluginContext.getProjectExtension().thirdPartyAarsDir, singleFile.name)
                            if (!intoFile.exists()) {
                                FastBuilderLogger.logLifecycle("执行文件拷贝 $singleFile to  $intoFile")

                                childProject.value.copy { copySpec: CopySpec ->
                                    copySpec.from(singleFile)
                                    copySpec.into(pluginContext.getProjectExtension().thirdPartyAarsDir)
                                }
                            }
                            DependencyUtils.suppressionDeChange(configuration)

                            configuration.dependencies.remove(dependency)
                            configuration.dependencies.add(
                                DependencyUtils.obtainDependency(
                                    pluginContext,
                                    singleFile.name.removeSuffix(".aar"),
                                    "aar"
                                )
                            )
                        }
                    }
                }

            }
        }
    }


    /**
     * 递归替换依赖
     */
    private fun replaceDependency(currentProject: Project, parent: Project? = null) {
        // 获取所有的模块工程集合
        val moduleProjectList = pluginContext.getModuleProjectList()

        // 从集合中查找到需要替换依赖的module工程, 如果 currentProject == app 工程, 这里查询结果是null
        val moduleProject = moduleProjectList.firstOrNull { it.moduleExtension.name == currentProject.path }

        // 替换所有待处理的module工程依赖
        for (configuration in currentProject.configurations) {
            // 遍历每一种依赖项集合,例如api、implementation等等
            val mutableSet = mutableSetOf<Dependency>()
            mutableSet.addAll(configuration.dependencies) // 这里转成可变集合来操作
            for (dependency in mutableSet) {
                // 动态删除源码依赖和添加aar依赖
                handleReplaceDependency(configuration, dependency, currentProject)
            }
        }
        // 当父工程也是module工程是才有值
        val parentModuleProject = moduleProjectList.firstOrNull { it.moduleExtension.name == parent?.path }
        val parentCacheValid = parentModuleProject?.cacheValid ?: false

        // 把下层的依赖投递到上层, 由于下层的 module 变成 aar 后会丢失它所引入的依赖,因此需要将这些依赖回传给上层
        if (parent == pluginContext.getApplyProject() || (parent != null && moduleProject != null
                    && (moduleProject.cacheValid
                    // fix: 上层module是aar依赖,下层module是源码依赖的情况
                    || (parentCacheValid && !moduleProject.cacheValid) ))) {
            // 原始类型
            DependencyUtils.copyDependencyWithPrefix(currentProject, parent, "")
            // Debug 前缀类型
            DependencyUtils.copyDependencyWithPrefix(currentProject, parent, "debug")
            // release前缀类型
            DependencyUtils.copyDependencyWithPrefix(currentProject, parent, "release")
            // 变体前缀
            val flavorName = moduleProject?.moduleExtension?.flavorName
            if (flavorName != null && flavorName.isNotBlank() && flavorName.isNotEmpty()) {
                //api debugApi tiyaDebugApi
                DependencyUtils.copyDependencyWithPrefix(currentProject, parent, flavorName)
                DependencyUtils.copyDependencyWithPrefix(currentProject, parent, flavorName + "Debug")
                DependencyUtils.copyDependencyWithPrefix(currentProject, parent, flavorName + "Release")
            }
        }
    }


    /**
     * 该方法用来动态替换源码依赖为aar依赖, 如果aar依赖无效,那么会声明aar的任务构建, 此方法会在replaceDependency方法内递归调用多次
     */
    private fun handleReplaceDependency(
        configuration: Configuration,
        dependency: Dependency,
        currentProject: Project
    ) {
        // 获取项目配置
        val projectExtension = pluginContext.getProjectExtension()

        // 获取module工程集合
        val moduleProjectList = pluginContext.getModuleProjectList()

        if (dependency !is ProjectDependency) {
            // 如果依赖项不是工程依赖,那么不处理
            return
        }
        // 获取依赖的project工程
        val dependencyProject = dependency.dependencyProject

        // 防止自己引用自己
        if (dependencyProject === currentProject) {
            return
        }

        // 根据依赖工程的名字获取对应的包装类ModuleProject
        val dependencyModuleProject =
            moduleProjectList.firstOrNull { it.moduleExtension.name == dependencyProject.path }

        // 如果当前模块工程在配置项中注册过且生效的才需要处理
        if (dependencyModuleProject != null && dependencyModuleProject.moduleExtension.enable) {
            // 标记这个对象被引用了
            dependencyModuleProject.flagHasOut = true

            FastBuilderLogger.logLifecycle("Handle dependency：${currentProject.name}:${dependency.name}  ")
            if (dependencyModuleProject.cacheValid) {
                // 缓存命中
                FastBuilderLogger.logLifecycle("${currentProject.name} 依赖 ${dependencyModuleProject.obtainName()} 缓存命中 ${configuration.state}")
                DependencyUtils.suppressionDeChange(configuration)
                // 移除原始的project依赖
                configuration.dependencies.remove(dependency)
                // 添加aar依赖
                configuration.dependencies.add(dependencyModuleProject.obtainAARDependency())
            } else {
                FastBuilderLogger.logLifecycle("${currentProject.name} 依赖 ${dependencyModuleProject.obtainName()} 没有命中缓存")
                // aar缓存无效,重新声明要构建的aar
                AARBuilderTask.prepare(pluginContext, dependencyModuleProject)
            }
        }

        // 获取当前工程的包装类ModuleProject
        val currentModuleProject = moduleProjectList.firstOrNull { it.obtainProject() == currentProject }

        // 记录父子工程依赖关系, 后续可能会用上
        if (dependencyModuleProject != null && currentModuleProject != null) {
            currentModuleProject.dependencyModuleProjectList.add(dependencyModuleProject)
        }

        // 处理完当前Project的依赖后,还需要继续处理它依赖的Project的 project依赖, 这是一个递归操作, 由父向子层层递归处理
        replaceDependency(dependencyProject, currentProject)
    }
}