package com.ingokodba.dragnav.modeli

data class MessageEvent(
    val text:String,
    val pos:Int,
    val launchIntent: String,
    val color: String = "",
    val type: MessageEventType,
    val app: AppInfo
)

enum class MessageEventType{ DRAG_N_DROP, LAUNCH_APP, FAVORITE, LONG_HOLD }