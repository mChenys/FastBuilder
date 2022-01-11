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

package org.lizhi.tiya.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.io.File

/**
 * 插件配置项
 */
open class ProjectExtension(private val project: Project) {

    /**
     * 是否启用插件
     */
    var pluginEnable: Boolean = false

    /**
     * 是否打印日志
     */
    var logEnable: Boolean = false

    /***
     * 如果当前的task任务名称满足就启动依赖替换,例如task名字是:app:assembleDebug
     * 如果不配置的话,默认检测到包含apply的Project名字就表示满足运行条件
     */
    var detectLauncherRegex: String = ""


    /**
     * 可选,存储工程模块aar的目录
     */
    var moduleAarsDir: File = File(project.rootProject.buildDir, ".fast_builder_module_aar")

    /**
     * 可选,存储第三方aar的目录,解决files('xxx.aar') 依赖的传递问题
     */
    var thirdPartyAarsDir: File = File(project.rootProject.buildDir, ".fast_builder_thirdParty_aar")


    /**
     * 子module的配置项
     */
    var moduleExtension: NamedDomainObjectContainer<ModuleExtension> = project.container(ModuleExtension::class.java)

    /**
     *  模块注册入口
     */
    fun subModuleConfig(action: Action<NamedDomainObjectContainer<ModuleExtension>>) {
        action.execute(moduleExtension)
    }

}