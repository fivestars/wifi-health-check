package com.fivestars.wifihealthcheck.utils

fun Int.frequenctyToChannel(): Int {
    return (this - 2407) / 5
}