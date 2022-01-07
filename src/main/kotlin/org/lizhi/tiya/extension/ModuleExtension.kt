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

import org.lizhi.tiya.plugin.IPluginContext
import org.lizhi.tiya.project.ModuleProject

/**
 * 模块工程配置项,注意name必须是:开头
 */
class ModuleExtension(val name: String) {
    /**
     * 使用debug包
     */
    var useDebug: Boolean = true

    /**
     * 是否启用
     */
    var enable: Boolean = true

    /**
     * 配置aar
     */
    var aarName: String = "_${name.replace(":", "")}.aar"

    /**
     * 风味组合名 如AAXX
     */
    var flavorName: String = ""


    /**
     * 转成ModuleProject
     */
    fun convertModuleProject(pluginContext: IPluginContext): ModuleProject {
        return ModuleProject(this, pluginContext)
    }
}