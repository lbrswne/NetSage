package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class PrivacyDocType(val title: String) {
    USER_AGREEMENT("用户协议"),
    PRIVACY_POLICY("隐私政策")
}

private const val USER_AGREEMENT_TEXT = """
欢迎您使用 NetSage（以下简称“本应用”）。

一、服务说明
1. 本应用是一款面向网络日志分析与故障辅助诊断的本地工具，不依赖 NetSage 自建服务器。
2. 您可输入、导入日志，或对自己确认的目标地址执行 DNS、TCP、TLS 与 HTTP 检测，以获得诊断结论与修复建议。
3. 主动检测产生的连接只发往您填写或确认的目标地址，不会先上传至 NetSage。

二、用户使用规范
1. 您应保证输入至本应用的日志、文本及相关内容来源合法。
2. 您不得利用本应用从事违法违规活动，不得上传、输入或传播侵犯他人合法权益的内容。
3. 您应自行判断诊断结果的适用性，本应用输出仅作为辅助参考，不构成任何专业承诺或结果担保。

三、免责说明
1. 由于网络环境、设备状态、样本差异等因素，诊断结果可能存在偏差。
2. 因您自行依据本应用建议实施操作而产生的风险，应由您结合实际情况审慎判断。
3. 对于因不可抗力、系统维护、第三方服务异常等导致的功能中断或结果误差，本应用将在法律允许范围内免责。

四、知识产权
1. 本应用相关界面、文案、程序及标识等内容的知识产权归开发者所有或依法享有。
2. 未经许可，任何人不得对本应用进行反向工程、复制、传播或用于其他商业用途。

五、协议更新
1. 本协议内容可能根据产品迭代、法律法规变化进行调整。
2. 更新后的协议将在应用内展示；您继续使用本应用即视为接受更新后的协议内容。
"""

private const val PRIVACY_POLICY_TEXT = """
NetSage 隐私政策

欢迎您使用 NetSage。我们重视您的个人信息与隐私保护。本隐私政策用于说明本应用如何处理相关信息。

一、产品形态说明
1. NetSage 当前为单机应用版本。
2. 本应用不要求您注册账号，不强制要求您提供手机号、身份证号等身份信息。
3. 本应用不集成遥测、广告 SDK、云端数据库或远程账号服务。

二、我们可能处理的信息
1. 您主动输入的日志文本、网络诊断文本或故障描述信息。
2. 主动检测所需的当前连接类型、IP、网关、DNS、目标地址、端口、检测状态与耗时。首版不读取 Wi-Fi SSID、BSSID 或精确位置。
3. 本地界面状态、诊断历史、收藏记录与结果缓存等运行信息。

三、信息处理目的
我们处理上述信息仅用于：
1. 完成日志解析与网络故障诊断；
2. 生成诊断结果与修复建议；
3. 提升应用基础可用性与稳定性。

四、信息存储与保护
1. 您输入的数据、探测结果、诊断历史与收藏记录默认仅保存在本地设备。
2. 我们会采取合理措施保护相关信息，防止未经授权的访问、披露、篡改或丢失。
3. 请您避免在日志中输入与诊断无关的高敏感个人信息。

五、对外提供与共享
1. 本应用没有 NetSage 服务端接收或存储您的日志和诊断结果。
2. 主动检测会与您指定的目标服务建立必要连接，该目标服务可能按其自身策略记录访问。

六、您的权利
您有权查看本隐私政策，并自主决定是否继续使用本应用。
若您不同意本隐私政策，可选择停止使用并退出本应用。

七、政策更新
我们可能根据产品能力或法律法规要求更新本隐私政策。更新后版本将在应用内展示。
"""

@Composable
fun PrivacyDocumentScreen(
    type: PrivacyDocType,
    onBack: () -> Unit,
) {
    val content = when (type) {
        PrivacyDocType.USER_AGREEMENT -> USER_AGREEMENT_TEXT
        PrivacyDocType.PRIVACY_POLICY -> PRIVACY_POLICY_TEXT
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F7FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF121826), Color(0xFF213F5B), Color(0xFF2B7A8B))
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("NetSage", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(type.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("当前页面为应用内文档说明，统一说明单机版、隐私处理与使用边界。", color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Text(
                text = content.trim(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
