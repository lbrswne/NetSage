# Stage 13 最终执行单（可直接照做）

## A. 构建前
1. 更新代码到最新
2. 执行：`./scripts/preflight.sh`
3. 确认 `versionName=0.1.1` / `versionCode=2`

## B. 生成发布包
1. 进入 `android-app/`
2. 执行：`./gradlew bundleRelease`
3. 产物：`app/build/outputs/bundle/release/app-release.aab`
4. 重命名为：`netsage-v0.1.1-2-release.aab`

## C. 提交 Internal testing
1. Play Console 新建版本（internal）
2. 上传 AAB
3. 填写版本更新说明（使用 `CHANGELOG-DRAFT.md`）
4. 审核备注贴入（`PLAY-REVIEW-NOTES-TEMPLATE.md`）

## D. 提交前最后核对
- [ ] 隐私政策 URL 可访问
- [ ] 数据安全表单草稿已转正式填写
- [ ] 截图素材齐全
- [ ] 关键流程可复现（输入->诊断->复制）

## E. 提交后
- 发送测试链接给首批 5~20 人
- 收集 BUG 到 `DEFECT-TRACKER.csv`
- 24h 汇总一次问题
