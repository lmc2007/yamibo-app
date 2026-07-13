package me.thenano.yamibo.yamibo_app.util

import kotlin.math.roundToInt

fun formatStorageSize(size: Long): String {
    return when {
        size >= 1024L * 1024L * 1024L -> "${(size / (1024f * 1024f * 1024f) * 100).roundToInt() / 100f} GB"
        size >= 1024L * 1024L -> "${(size / (1024f * 1024f) * 100).roundToInt() / 100f} MB"
        else -> "${(size / 1024f * 100).roundToInt() / 100f} kB"
    }
}