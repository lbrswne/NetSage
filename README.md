# NetSage Android v0.2.0

NetSage 是一个本地优先的 Android 网络诊断工作台。主版本不要求登录，不依赖自建服务器，也不会把日志、诊断结果或使用数据上传给 NetSage。

## 当前功能

- 日志诊断：粘贴、从本机导入或从其他应用分享文本，使用版本化本地规则生成 Top 3 诊断假设。
- 快速体检：读取当前连接、IP、网关、DNS、IPv4/IPv6 等网络快照，并依次执行 DNS、TCP、TLS 与 HTTP 检测。
- DNS 解析详情：结果页按 IPv4/IPv6 展示本次返回的地址，以及已有的解析器、RCODE 可用性、错误代码与耗时；不额外查询公共 DNS，解析成功不代表服务可达。
- TCP 地址族检查：从 DNS 结果中分别取首个 IPv4/IPv6 地址进行建连；用户可手动开启每个可用地址族 3 次的连接稳定性采样，默认各 1 次。显示成功次数与尝试次数，复测沿用原次数；这不是丢包率或持续可用性测量。
- 可选对照检测：用户可再指定一个不同目标，应用以相同协议和端口顺序检测两者，并展示本次 HTTP 结果的对照摘要。两次检测之间若网络环境变化，会提示不能直接比较；摘要不能单独证明故障根因。
- 组合诊断：把主动探测证据与用户日志放入同一条诊断会话。
- 可解释结果：显示匹配证据、冲突证据、规则优先级和下一步行动；规则分数不是统计概率。
- 修复后复测：比较检测状态、耗时、网络环境和诊断原因变化。
- 复测 TCP 证据：并列显示前后各地址族的成功/尝试次数；网络环境变化时提示不能把差异直接归因于修复。
- 本地会话：最多保存 20 条诊断会话；每条原始日志最多保存 100,000 个字符。导入、分享或手动输入超限时会明确提示，不会静默截断。
- 会话历史支持按目标地址或摘要即时搜索，并按体检/组合、日志、复测筛选；只筛选本机已有记录。
- 会话历史卡片可直接通过 Android 系统分享面板分享该会话的 Markdown 报告，无需打开详情页。
- 两处报告分享入口都会先提示：完整报告可能包含原始日志、网络快照和探测证据；取消不会打开分享面板。该提示不会自动脱敏报告。
- 删除单条会话前需确认目标，清空全部前需确认删除数量；删除后无法恢复。
- 报告导出：通过 Android 系统界面导出 Markdown/JSON，或使用系统分享面板发送。

主动检测只访问用户确认的目标地址；它不经过 NetSage 服务器。主版本不包含遥测、广告 SDK、云数据库、Ping、Traceroute、抓包或后台持续监控。
对照检测关闭时只访问主目标；开启后还会访问用户填写的对照目标。HTTP 检测使用系统网络连接，可能与前面的 DNS/TCP/TLS 步骤选用不同 IP，报告保留各步骤目标与可取得的连接证据。

HTTPS 是默认协议。只有用户明确选择 HTTP 时，App 才会发起明文 HTTP 探测；它不会把 HTTPS 目标自动降级为 HTTP。

## 目录

- `android-app/`：Kotlin + Jetpack Compose 客户端和本地诊断引擎。
- `docs/`：产品、发布和测试文档。
- `backend/`：早期 FastAPI 架构实验；`main` Android 应用不调用它。


## Author
NingWanZheng/宁琬正（东北大学）

## Project Highlights

### 1) End-to-end delivery from product idea to store submission
I independently drove NetSage from concept to deliverable: requirement scoping, Android development, release packaging, signing workflow, compliance materials, and AppGallery submission preparation.
This project demonstrates full-cycle execution instead of isolated coding tasks.

### 2) Practical engineering decision-making under constraints
During release preparation, I identified key launch blockers (network dependency, compliance/filing constraints, and release readiness).
To ensure timely delivery, I refactored the app from a server-dependent flow to an offline-capable single-device version, preserving core user value while reducing launch risk.

### 3) Real-world problem solving and ownership
I handled multiple production-like issues including signing pipeline setup, build/release verification, repository hygiene (sensitive file exclusion), and open-source publication workflow.
The project reflects strong ownership, structured troubleshooting, and the ability to turn ambiguity into executable steps.

## Branch Strategy / 分支说明

This repository maintains two runnable variants of NetSage:

- **offline-version**
Earlier offline single-device release line.

- **online-version**
Network-enabled version (Android + backend API workflow).
Preserves the original online diagnosis architecture for future cloud deployment.

- **main**
Default local-first v0.2 showcase branch. Active network probes run on the device and do not require a NetSage backend.

---

本仓库维护 NetSage 的两个可运行版本：

- **offline-version（早期单机版）**
早期不依赖后端的交付分支。

- **online-version（联网版）**
保留 Android + 后端 API 的联网诊断架构，便于后续云端部署演进。

- **main**
默认本地优先 v0.2 展示分支。主动探测在手机上运行，不需要 NetSage 后端。

## Build and test

```powershell
cd android-app
.\gradlew.bat testDebugUnitTest assembleDebug
```

连接 Android 模拟器或真机后，可运行真实回环网络集成测试：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

集成测试使用设备本机的 `127.0.0.1`、MockWebServer 和自签名测试证书，覆盖 HTTP 成功、同主机重定向、连接拒绝、响应超时、TLS 证书链异常和用户取消。测试不访问外网，也不会启动 NetSage 后端。
