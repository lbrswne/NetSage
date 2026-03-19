# Hotfix 流程（P0/P1）

1. 复现并确认影响范围
2. 建 hotfix 分支：`hotfix/<issue-id>`
3. 最小修复并自测
4. 合并并提升 versionCode
5. 重新打包并提交 internal testing
6. 通知测试者复测
