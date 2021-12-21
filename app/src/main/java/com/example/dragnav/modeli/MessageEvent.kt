package com.example.dragnav.modeli

data class MessageEvent(
    val text:String,
    val pos:Int,
    val launchIntent: String,
    val color: String = ""
)
