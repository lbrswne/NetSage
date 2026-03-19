from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="NetSage API", version="0.1.0")

class DiagnoseRequest(BaseModel):
    log_text: str

@app.get('/health')
def health():
    return {'status': 'ok'}

@app.post('/diagnose')
def diagnose(req: DiagnoseRequest):
    # TODO: replace with real rule engine
    return {
        'top_causes': [
            {'name': 'DNS 配置异常', 'confidence': 0.72, 'fix': '检查 DNS 服务器地址与连通性'},
            {'name': 'DHCP 获取失败', 'confidence': 0.54, 'fix': '释放/续租 IP，检查 DHCP 服务状态'},
            {'name': '默认网关不可达', 'confidence': 0.38, 'fix': '检查网关地址、路由与链路状态'},
        ]
    }
