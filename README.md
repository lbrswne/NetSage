# NetSage Android (MVP)

目标：做一个可上架的安卓应用，核心是“网络故障智能诊断”。

## MVP 功能
1. 上传/粘贴网络日志
2. 输出 Top3 根因 + 修复建议
3. 导出排障报告（文本）

## 目录
- `docs/`：PRD、路线图、上架清单
- `android-app/`：Android 客户端（Kotlin + Compose）
- `backend/`：诊断 API（FastAPI）


## Author
NingWanZheng

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
