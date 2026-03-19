# Round 1 修复计划（Stage 11）

## P1
1. 请求超时提示优化
- 现状：仅显示“请求失败”
- 目标：区分 timeout / network unreachable / server error
- 计划：ViewModel 增加错误映射 + UI 友好提示

## P2
1. 复制报告交互反馈
- 现状：复制后无明显反馈
- 目标：复制成功显示 Toast

## 交付目标
- 48小时内修复 P1
- 72小时内修复 P2
