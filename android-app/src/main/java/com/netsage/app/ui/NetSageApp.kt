package com.netsage.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.netsage.app.model.CauseItem
import com.netsage.app.ui.screen.InputScreen
import com.netsage.app.ui.screen.ResultScreen

@Composable
fun NetSageApp() {
    val state = remember { AppState() }

    when (state.page) {
        AppPage.INPUT -> InputScreen(onDiagnose = { _ ->
            // TODO: replace mock with ViewModel + Retrofit call
            state.showResult(
                listOf(
                    CauseItem("DNS 配置异常", 0.78, "检查 DNS 服务器地址与连通性"),
                    CauseItem("DHCP 获取失败", 0.64, "释放/续租 IP，检查 DHCP 服务状态"),
                    CauseItem("默认网关不可达", 0.58, "检查网关与路由")
                )
            )
        })

        AppPage.RESULT -> ResultScreen(
            causes = state.causes,
            onBack = { state.backToInput() }
        )
    }
}
