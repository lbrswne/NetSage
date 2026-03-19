# 观测与反馈计划（MVP）

## 日志建议
- 后端记录：请求ID、耗时、错误类型
- 客户端记录：接口失败原因（timeout/connect/server）

## 最小化埋点（后续可接）
1. app_open
2. diagnose_submit
3. diagnose_success
4. diagnose_failed
5. report_copied

## 数据用途
- 仅用于改进稳定性与体验，不做个体画像
