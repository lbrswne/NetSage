from fastapi import FastAPI
from pydantic import BaseModel, Field
from netsage.rules import diagnose_text

app = FastAPI(title="NetSage API", version="0.2.0")


class DiagnoseRequest(BaseModel):
    log_text: str = Field(..., min_length=1)


@app.get('/health')
def health():
    return {'status': 'ok', 'service': 'netsage-api'}


@app.post('/diagnose')
def diagnose(req: DiagnoseRequest):
    top = diagnose_text(req.log_text)
    return {
        'top_causes': [
            {
                'name': c.name,
                'confidence': c.confidence,
                'fix': c.fix,
                'evidence': c.evidence,
            }
            for c in top
        ]
    }
