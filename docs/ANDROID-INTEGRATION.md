# Android 真接口接入说明（第四阶段）

已新增：
- Retrofit 工厂：`ApiFactory.kt`
- Repository：`DiagnoseRepository.kt`
- ViewModel：`DiagnoseViewModel.kt`
- Factory：`ViewModelFactory.kt`
- `NetSageApp.kt` 已改为调用真实后端

## 需要在 Android Studio 项目补的依赖（app/build.gradle）

```gradle
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4"
implementation "androidx.lifecycle:lifecycle-runtime-compose:2.8.4"
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
implementation "com.squareup.retrofit2:retrofit:2.11.0"
implementation "com.squareup.retrofit2:converter-gson:2.11.0"
implementation "com.squareup.okhttp3:logging-interceptor:4.12.0"
```

## 网络权限
`AndroidManifest.xml` 需包含：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 调试地址
- 模拟器访问宿主机：`http://10.0.2.2:8000`
