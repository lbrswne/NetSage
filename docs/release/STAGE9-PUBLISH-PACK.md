# Stage 9 发布冲刺包

## 1) app/build.gradle.kts 签名配置模板（可直接粘贴）

```kotlin
import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        load(keystorePropsFile.inputStream())
    }
}

android {
    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}
```

> 注意：`keystore.properties` 不入库。

## 2) 版本策略（建议）

- `versionName`: `0.1.0` -> `0.1.1` -> `0.2.0`
- `versionCode`: 每次发布递增（1, 2, 3...）

建议规则：
- 小修复 + 文案调整：`versionName` patch +1，`versionCode` +1
- 功能新增：`versionName` minor +1，`versionCode` +1
- 重大变更：`versionName` major +1，`versionCode` +1

## 3) 构建产物命名规范

- AAB: `netsage-v{versionName}-{versionCode}-release.aab`
- APK: `netsage-v{versionName}-{versionCode}-release.apk`

示例：
- `netsage-v0.1.0-1-release.aab`

## 4) 审核备注模板（提交 Play Console）

```text
Test instructions:
1. Open app and paste sample log text on the first screen.
2. Tap "开始诊断".
3. Verify that Top3 causes and suggestions are shown.
4. Tap "复制报告" and paste into any text editor to verify output.

No account is required for testing.
No in-app purchase.
Core feature depends on network request to backend diagnose API.
```

## 5) 发布建议节奏

1. Internal testing（5-20人）
2. Closed testing（50-200人）
3. Production rollout（10% -> 50% -> 100%）
