from dataclasses import dataclass
from typing import List

@dataclass
class Cause:
    name: str
    confidence: float
    fix: str
    evidence: list[str]


def _has_any(text: str, keys: list[str]) -> bool:
    t = text.lower()
    return any(k.lower() in t for k in keys)


def diagnose_text(log_text: str) -> List[Cause]:
    causes: List[Cause] = []
    t = log_text or ""

    if _has_any(t, ["dns", "nxdomain", "server can't find", "temporary failure in name resolution"]):
        causes.append(Cause(
            name="DNS 配置异常",
            confidence=0.78,
            fix="检查 DNS 服务器地址与连通性，尝试 223.5.5.5 / 8.8.8.8 对照测试",
            evidence=["检测到 DNS 解析失败相关关键字"],
        ))

    if _has_any(t, ["dhcp", "no dhcp offers", "discover", "timeout waiting for dhcp"]):
        causes.append(Cause(
            name="DHCP 获取失败",
            confidence=0.64,
            fix="释放/续租 IP，检查 DHCP 服务与 VLAN 配置",
            evidence=["检测到 DHCP 超时/无响应关键字"],
        ))

    if _has_any(t, ["default gateway", "destination host unreachable", "no route to host", "gateway"]):
        causes.append(Cause(
            name="默认网关不可达",
            confidence=0.58,
            fix="检查网关地址、三层路由与链路状态，必要时 traceroute",
            evidence=["检测到网关不可达或路由异常关键字"],
        ))

    if _has_any(t, ["acl", "administratively prohibited", "access denied", "icmp admin prohibited"]):
        causes.append(Cause(
            name="ACL/安全策略拦截",
            confidence=0.52,
            fix="核对 ACL 命中规则、源/目的地址与端口，检查安全设备策略",
            evidence=["检测到访问被策略阻断关键字"],
        ))

    if not causes:
        causes = [Cause(
            name="日志特征不足",
            confidence=0.31,
            fix="补充 ping/nslookup/traceroute 与网卡状态信息后重试",
            evidence=["未命中核心规则"],
        )]

    causes.sort(key=lambda c: c.confidence, reverse=True)
    return causes[:3]
