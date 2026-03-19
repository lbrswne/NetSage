# Release 构建与签名指南（Stage 8）

## 1. 生成签名密钥（仅一次）
```bash
keytool -genkeypair -v \
  -keystore netsage-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias netsage
```

请妥善保存：
- keystore 文件
- alias
- store password
- key password

## 2. 在 `android-app/keystore.properties` 写入（不要提交 git）
```properties
storeFile=../netsage-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=netsage
keyPassword=YOUR_KEY_PASSWORD
```

## 3. 在 app/build.gradle.kts 中接入 signingConfig（下一步可自动化）

## 4. 构建 release
```bash
cd android-app
./gradlew assembleRelease
```

产物路径（默认）：
- `app/build/outputs/apk/release/app-release.apk`

建议发布 AAB：
```bash
./gradlew bundleRelease
```
- `app/build/outputs/bundle/release/app-release.aab`
