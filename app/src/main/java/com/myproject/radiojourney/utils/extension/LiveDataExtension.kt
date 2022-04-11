package com.myproject.radiojourney.utils.extension

import androidx.lifecycle.MutableLiveData

fun MutableLiveData<Boolean>?.call() {
    this?.postValue(true)
}