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

package org.lizhi.tiya.log

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * 插件日志类
 */
object FastBuilderLogger {
    private var logger: Logger = Logging.getLogger(FastBuilderLogger::class.java)
    var enableLogging: Boolean = true

    fun logLifecycle(message: String) {
        if (enableLogging) {
            logger.log(LogLevel.LIFECYCLE, "FastBuilder:$message")
        }
    }
}