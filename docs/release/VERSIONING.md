# NetSage 版本与发布日志规范

## 版本号
- `versionName`: 语义化版本（MAJOR.MINOR.PATCH）
- `versionCode`: 单调递增整数

## 发布日志模板

```markdown
## v0.1.1 (code 2)
- 修复：诊断结果页在弱网时的错误提示
- 优化：报告复制格式
- 合规：补充隐私政策链接
```

## 发布前检查（补充）
- [ ] versionCode 比线上版本大
- [ ] changelog 已填写
- [ ] 内测反馈关键问题已处理
