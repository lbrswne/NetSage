# Stage 16 闭测提交通关流程

## 1. 打包
1) `./scripts/preflight.sh`
2) `cd android-app && ./gradlew bundleRelease`
3) 产物重命名：`netsage-v0.1.2-3-release.aab`

## 2. 提交 closed testing
1) Play Console -> Closed testing
2) 上传 AAB
3) 填写 release notes（用 `RELEASE-NOTES-v0.1.2-DRAFT.md`）
4) 审核备注（复用模板）

## 3. 发布后 24h 观察
- 安装成功率
- 启动崩溃率
- 核心流程成功率（输入->诊断->复制）

## 4. Go / No-Go
- 若 P0>0 -> No-Go，立即 hotfix
- 若 P1>=3 -> 暂缓扩量
