package com.myproject.radiojourney.utils.extension

import androidx.core.util.PatternsCompat

fun String?.isEmailValid(): Boolean {
    return if (this != null && this.isNotBlank()) {
        isNotBlank() && PatternsCompat.EMAIL_ADDRESS.matcher(this).matches()
    } else {
        false
    }
}

fun String?.isPasswordValid(): Boolean {
    return if (this != null && this.isNotBlank()) {
        isNotBlank() && this.length > 5
    } else {
        false
    }
}

fun String?.removeLastNchars(str: String?, n: Int): String? {
    return if (str == null || str.length < n) {
        str
    } else str.substring(0, str.length - n)
}