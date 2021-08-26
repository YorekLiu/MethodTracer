# MethodTracer

Android 编译期扫描危险 API 并可将所有调用重定向到自己的实现。

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
        classpath "com.github.YorekLiu:MethodTracer:${version}"
    }
}
```

app 的 build.gradle 中配置插件：

```gradle
apply plugin: 'method-trace'

methodTrace {
    enable = true

    /**
     * 扫描结果文件输出路径，默认取build/asmoutput/result.txt
     */
    // output = null

    /**
     * 要检测的API表，整体是一个Map<String, List<String>>，格式为类名:List<方法名>
     * 其中类名支持+，同aspectJ语法
     * 若方法名为*，则为检测该类下所有方法的调用
     */
    apiList = [
            "java/lang/Thread"                  : ["<init>", "start"],
            "android/telephony/TelephonyManager": ["getDeviceId"],
            "android/content/pm/PackageManager" : ["getInstalledPackages"],
            "android/widget/ImageView+"         : ["setImageDrawable", "setImageBitmap"],
            "android/app/ActivityManager"       : ["getRunningAppProcesses"],
            "android/os/Process"                : ["myPid"]
    ]
}
```

配置完毕后 sync 一下，然后 build 一下，完毕后会在 app/build/asmoutput/result.txt 中生成危险 API 的调用位置。

## 代码替换

代码替换功能采用了注解的方式，因此需要引入另外一个库：

```gradle
implementation "com.github.YorekLiu.MethodTracer:annotation:${version}"
```

这里有两个注解：

- MethodProxyEntry
  该注解标记类，用作代码替换的实现方法的入口
- MethodProxy
  给代码替换的实现方法打标，这样可以让原方法与代码替换方法之间产生关联。注解参数解释如下：
  ```java
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  public @interface MethodProxy {
      /**
       * 要代理的class
       */
      Class<?> target();

      /**
       * 要代理的方法名
       * 若被代理的方法是静态方法，则该注解修饰的方法的签名一定要与被代理的保持一致
       * 若不是静态方法，方法参数第一个为该对象，其他参数从第二个开始摆放
       * e.g.
       * @MethodProxy(target = Process.class, method = "myPid")
       * public static int myPid() {
       *     return android.os.Process.myPid();
       * }
       *
       * @MethodProxy(target = ActivityManager.class, method = "getRunningAppProcesses")
       * public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses(ActivityManager activityManager) {
       *     return activityManager.getRunningAppProcesses();
       * }
       */
      String method();

      /**
       * 是否扩展匹配类
       */
      boolean extend() default false;
  }
  ```

详细代码可以参考 sample MethodProxyInst.java文件。