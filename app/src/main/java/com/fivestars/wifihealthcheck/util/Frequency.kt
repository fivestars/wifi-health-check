package com.fivestars.wifihealthcheck.util

fun Int.frequenctyToChannel(): Int {
    return (this - 2407) / 5
}