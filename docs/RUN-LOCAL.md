# 本地运行说明（当前阶段）

## 后端（FastAPI）
在 `NetSage/backend` 目录执行：

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

接口：
- `GET http://127.0.0.1:8000/health`
- `POST http://127.0.0.1:8000/diagnose`

示例请求：

```json
{
  "log_text": "nslookup failed: NXDOMAIN; destination host unreachable"
}
```

## 安卓端（骨架）
当前为 Kotlin + Compose 页面骨架，下一步在 Android Studio 创建工程后，将 `android-app/src/main/java/...` 文件并入 app module。
