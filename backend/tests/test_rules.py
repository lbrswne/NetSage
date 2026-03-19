from netsage.rules import diagnose_text


def test_dns_case():
    out = diagnose_text("nslookup failed: NXDOMAIN")
    assert out[0].name == "DNS 配置异常"


def test_fallback_case():
    out = diagnose_text("random text")
    assert out[0].name == "日志特征不足"
