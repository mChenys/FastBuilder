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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.internal.ImmutableActionSet
import org.joor.Reflect
import org.lizhi.tiya.log.FastBuilderLogger
import org.lizhi.tiya.plugin.IPluginContext

class DependencyUtils {


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


}