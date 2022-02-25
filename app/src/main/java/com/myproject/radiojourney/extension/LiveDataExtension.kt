package com.myproject.radiojourney.extension

import androidx.lifecycle.MutableLiveData

fun MutableLiveData<Boolean>?.call() {
    this?.postValue(true)
}