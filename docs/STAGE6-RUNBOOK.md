# Stage 6 Runbook（联调一页通）

## 0) 前置
- Windows + Android Studio (Hedgehog 或更高)
- Python 3.10+
- 安卓模拟器（Pixel 6 / API 34）

## 1) 启动后端
```bash
cd /mnt/c/new\ project-test/NetSage/backend
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：
- `http://127.0.0.1:8000/health`

## 2) 打开安卓工程
- Android Studio -> Open
- 选择目录：`C:\new project-test\NetSage\android-app`
- 等待 Gradle Sync

## 3) 关键网络点
- 模拟器访问宿主机：`http://10.0.2.2:8000`
- 已在代码中配置（`ApiFactory.kt`）

## 4) 首次联调步骤
1. 启动后端
2. 启动模拟器
3. Run app
4. 在输入页点击“填充样例”
5. 点击“开始诊断”
6. 看到 Top3 结果卡片即联调成功

## 5) 常见问题
- `CLEARTEXT not permitted`：改为 https 或在 debug 网络策略放开 http（后续可加）
- 连不上后端：确认后端监听 `0.0.0.0:8000` 且模拟器用 `10.0.2.2`
- Gradle 报 JDK：Android Studio 设置 JDK 17
