
# FastBuilder
[![versionImg](https://badgen.net/maven/v/metadata-url/https://plugins.gradle.org/m2/io/github/tiyateam/fastbuilder/FastBuilder/maven-metadata.xml?label=FastBuilder)](https://plugins.gradle.org/plugin/io.github.tiyateam.fastbuilder)

一个可以提升`Android`编译效率的Gradle插件


# 编译时间对比
集成插件前:

![snOLyFO3cj](https://user-images.githubusercontent.com/19259572/148940591-4c5c9ef7-88da-43c4-a4bd-d7264c47ff52.png)

集成插件后,且存在aar:

![KMpHUGiW6T](https://user-images.githubusercontent.com/19259572/148940640-4e27476b-a099-43db-89d4-d825ee8d3341.png)


# 插件原理
在gradle全局配置完成后设置监听，当执行项目的assemble任务后，会触发模块工程aar的生成和拷贝操作，并记录模块工程的最后修改时间，当下次执行assemble任务时会判断模块工程是否修改过（对比最后修改时间）来决定是否采用aar包进行依赖，如果模块工程没有改动，那么会修改工程的源码依赖，替换为aar包的依赖，并且将该工程下的所有依赖向上传递给对其有依赖的模块工程，这个过程是一个递归的操作，假设App工程依赖user模块，user模块依赖common模块，common模块依赖base模块，如果user和base变成了aar包方式继承，其中common工程有代码变动，那么需要将user模块gradle文件内配置的所有依赖向上传递给App工程，同时还需要将base模块的依赖向上传递给common模块。

递归调用的过程是：

App--》user--》common--》base

依赖回传的过程是：

base--》common--》user--》App

# 类图
![FastBuilder类图](https://user-images.githubusercontent.com/19259572/148940455-241585ad-8fb9-4e2b-8217-37efd8198e6f.png)



# 使用指南
在您的app的build.gradle添加如下配置
```groovy
//启用插件
plugins {
  id "io.github.tiyateam.fastbuilder" version "${LastedVersion}"
}
//插件配置
moduleArchive {
    // 可选参数.是否打印log 默认为false
    logEnable = true
    // 可选参数.是否启用插件 默认为false
    pluginEnable = true
    // 可选参数.优化应用插件kapt智能跳过 默认为false
    kaptOptimization=false
    // 可选参数.存储插件临时配置目录,不设置默认会在根工程的build/.fast_builder_module_aar下
    moduleAarsDir = project.rootProject.file("libs")
    // 可选参数.存储第三方aar的目录,不设置默认会在根工程的build/.fast_builder_thirdParty_aar下
    thirdPartyAarsDir =  project.rootProject.file("libs")
    // 可选参数.如果配置了那么只会由该任务触发执行,不配置的话,默认会检测是否包含apply的工程名字
    detectLauncherRegex = ":app:assembleDebug"
    // 下面配置哪些模块可以被编译成aar缓存
    subModuleConfig {
        // image-picker是一个module工程，具体视你项目而定,配置后会在编译时替换为aar依赖,并且会在您修改这个模块后会自动进行构建
        register(":image-picker") {
            // 可选参数.是否使用debug版本
            useDebug = true
            // 可选参数.是否启用这个模块配置 
            enable = true
            // 可选参数. 缓存的aar命中,不选的话默认命名格式为: _${module name}.aar
            aarName = "image-picker-debug.aar"
            // 可选参数.构建变体 如没有可不写
            flavorName = "tiya"
        }
        // 另一个module模块，其最简约配置
        register(":floatwindow") {
      
        }
    }
}
```
旧版本启用
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "io.github.tiyateam.fastbuilder:FastBuilder:${LastedVersion}"
  }
}
//在某个模块开启
apply plugin: "io.github.tiyateam.fastbuilder"
```

# 谁在使用

| [TIYA](https://play.google.com/store/apps/details?id=com.huanliao.tiya&hl=en_US&gl=US)        | 
| --------   | 
|[<img src="https://play-lh.googleusercontent.com/RwuBOgoBX1OmmR5W14AyBDp9pNgnh1eJD2UmJzhVSZOpZYG1xI_y1aihbE4aP3dURwc=s360-rw" alt="TIYA" width="150"/> ](https://play.google.com/store/apps/details?id=com.huanliao.tiya&hl=en_US&gl=US)       |


# License
```
Copyright 2021 Tiya.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

