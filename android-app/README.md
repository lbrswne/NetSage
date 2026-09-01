# NetSage Android

- Package: `com.netsage.app`
- Stack: Kotlin, Jetpack Compose, coroutines, Gson
- Minimum SDK: 26
- Version: 0.2.0

主版本是本地优先应用，没有 Retrofit、账号系统或自建后端依赖。网络探测使用 Android/JDK API 在设备上执行；日志、规则和诊断会话保存在本机。

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```
