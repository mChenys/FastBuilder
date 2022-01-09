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

import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.internal.ImmutableActionSet
import org.joor.Reflect
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.plugin.IPluginContext

/**
 * 依赖处理工具类
 */
object DependencyUtils {

    /**
     * 根据名称获取一个依赖
     */
    fun obtainDependency(context: IPluginContext, file: String, subFix: String = "aar"): Dependency {
        return context.getApplyProject().dependencies.create(mapOf("name" to file, "ext" to subFix))
    }

    var changeConfiguration = mutableSetOf<Configuration>()


    /**
     * 为修复 Cannot change dependencies of dependency configuration xxxx after it has been included in dependency resolution.
     * @author fmy
     */
    fun suppressionDeChange(configuration: Configuration): Boolean {
        return try {
            if (changeConfiguration.contains(configuration)) {
                return true
            }
            changeConfiguration.add(configuration)
            val domainObjectSet = Reflect.on(configuration.dependencies)
                .get<DomainObjectSet<Dependency>>("backingSet") as? DefaultDomainObjectSet<Dependency> ?: false

            Reflect.on(domainObjectSet).set("beforeContainerChange", ImmutableActionSet.empty<Void>())
            true
        } catch (e: Exception) {
            FastBuilderLogger.logLifecycle("suppressionDeChange 反射错误")
            e.printStackTrace()
            false
        }
    }

    /**
     * 将currentProject的依赖copy到parentProject
     */
    fun copyDependencyWithPrefix(
        currentProject: Project,
        parentProject: Project,
        prefix: String,
        list: Set<String> = mutableSetOf("api", "runtimeOnly", "implementation")
    ) {
        for (configName in list) {
            val newConfigName = if (prefix.isBlank()) {
                configName
            } else {
                prefix + configName.capitalize()
            }
            // ModuleArchiveLogger.logLifecycle("赋值依赖: ${newConfigName}")
            copyDependency(currentProject, parentProject, newConfigName)
        }
    }


    fun copyDependency(currentProject: Project, parentProject: Project, configName: String) {
        val srcConfig = currentProject.configurations.getByName(configName)
        val dstConfig = parentProject.configurations.getByName(configName)
        val parentContains = parentProject.configurations.names.contains(configName)
        if (parentContains) {
            // 必须要保证依赖配置的名字在父工程存在,比如子工程用了一个叫xxxApi的configName,父工程也需要存在
            copyDependency(srcConfig, dstConfig)
        }
    }

    fun copyDependency(src: Configuration, dst: Configuration) {
        for (dependency in src.dependencies) {
            if (dependency is ModuleDependency) {
                val srcExclude = configMatchExclude(src, dependency)
                val destExclude = configMatchExclude(dst, dependency)
                // 如果当前依赖存在忽略配置中,那么不需要拷贝
                if (srcExclude || destExclude) {
                    continue
                } else {
                    /**
                     * 如果子工程或者父工程配置了依赖忽略配置,那么需要给每一项依赖增加一条依赖忽略关系,例如这种配置:
                     * configurations {
                     *  all*.exclude group:'org.jetbrains.kotlin',module:'kotlin-stdlib-jre7'
                     *  all*.exclude group:'com.yibasan.lizhifm.sdk.network',module:'http'
                     *  all*.exclude group:'com.lizhi.component.lib',module:'itnet-http-lib'
                     * }
                     */
                    src.excludeRules.forEach {
                        dependency.exclude(mapOf("group" to it.group, "module" to it.module))
                    }
                    dst.excludeRules.forEach {
                        dependency.exclude(mapOf("group" to it.group, "module" to it.module))
                    }
                    // dependency.excludeRules.addAll(src.excludeRules)
                    // dependency.excludeRules.addAll(dest.excludeRules)
                }
            }
            suppressionDeChange(dst)
            // 将子工程的依赖添加到父工程中
            dst.dependencies.add(dependency)
        }
    }

    /**
     * 判断当前依赖是否已经加入到忽略规则里面
     */
    fun configMatchExclude(configuration: Configuration, dependency: Dependency): Boolean {
        for (excludeRule in configuration.excludeRules) {
            return if (excludeRule.module.isNullOrBlank()) {
                dependency.group == excludeRule.group
            } else {
                dependency.group == excludeRule.group && dependency.name == excludeRule.module
            }
        }
        return false
    }

}