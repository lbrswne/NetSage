package com.netsage.app.util

object ErrorMapper {
    fun mapMessage(raw: String?): String {
        val msg = raw?.lowercase().orEmpty()
        return when {
            msg.contains("timeout") -> "请求超时，请稍后重试"
            msg.contains("failed to connect") || msg.contains("unable to resolve host") -> "网络不可达，请检查网络或后端服务"
            msg.contains("500") || msg.contains("502") || msg.contains("503") -> "服务暂时不可用，请稍后再试"
            else -> "服务异常，请稍后重试"
        }
    }
}
