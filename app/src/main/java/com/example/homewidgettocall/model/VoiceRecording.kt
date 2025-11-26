package com.example.homewidgettocall.model

import java.io.File

data class VoiceRecording(
    val file: File,
    val name: String,
    val date: Long
)
