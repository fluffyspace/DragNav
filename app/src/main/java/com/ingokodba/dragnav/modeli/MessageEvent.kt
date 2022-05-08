package com.ingokodba.dragnav.modeli

data class MessageEvent(
    val text:String,
    val pos:Int,
    val launchIntent: String,
    val color: String = "",
    val draganddrop: Boolean = false,
    val app: AppInfo
)
