# MethodTracer

Android 编译期方法扫描、替换的插件工具

- 可将指定方法的调用扫描出来（可作隐私合规检查工具使用）
- 也可 Hook 指定方法的实现（底层使用 ASM 插桩，上层在工程代码中通过注解的形式实现。开箱即用，无需了解底层知识）。

扫描报告、方法 Hook 功能可以先预览 _files 目录下的样本。

[![](https://jitpack.io/v/YorekLiu/MethodTracer.svg)](https://jitpack.io/#YorekLiu/MethodTracer)

## 添加插件

项目根目录 build.gradle 添加 jitpack 仓库：

```build.gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

项目根目录 build.gradle 添加插件：

```gradle
buildscript {
    dependencies {
        classpath "com.github.YorekLiu.MethodTracer:MethodTracer:${version}"
    }
}
```

app 的 build.gradle 中配置插件：

```gradle
apply plugin: 'method-trace'

methodTrace {
    enable = true

    /**
     * 扫描结果文件输出路径，默认为build/outputs/report/report_{variantName}_{ts}.txt
     */
    // output = null

    /**
     * 要检测的API表，整体是一个Map<String, List<String>>，格式为类名:List<方法名>
     * 其中类名支持+
     * 若方法名为*，则为检测该类下所有方法的调用
     */
     apiList = [ 
         "java.lang.Thread"                  : ["<init>", "start"],
          "android.telephony.TelephonyManager": ["getDeviceId"],
          "android.content.pm.PackageManager" : ["getInstalledPackages"],
          "android.widget.ImageView+"         : ["setImageDrawable", "setImageBitmap"],
          "android.app.ActivityManager"       : ["getRunningAppProcesses"],
          "android.os.Process"                : ["myPid"]
     ]
}
```

在 build 完成之后，会在 build/outputs/report/report_{variantName}_{ts}.txt 中报告指定 API 的调用位置。

## 方法Hook

方法Hook功能采用了注解的方式，因此需要引入另外一个库：

```gradle
implementation "com.github.YorekLiu.MethodTracer:annotation:${version}"
```

这里有两个注解：

- MethodProxyEntry
  方法Hook功能的切入点，用来标记方法Hook实现类的入口。插件在编译时会先扫描该注解，然后解析该类里面的内容。
- MethodProxy
  给需要被Hook的方法所对应的实现方法打标，这样可以让原方法与代码替换方法之间产生关联。注解参数解释如下：

详细代码可以参考 sample MethodProxyInst.java文件。

![method_hook](https://raw.githubusercontent.com/YorekLiu/MethodTracer/master/_files/method_hook.webp)