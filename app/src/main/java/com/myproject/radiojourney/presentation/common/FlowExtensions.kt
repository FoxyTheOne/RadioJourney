package com.myproject.radiojourney.presentation.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Получать значения Flow, пока экран виден (STARTED), - замена LiveData.observe() по рекомендации developer.android.com.
 * В Activity передаётся сама Activity, во фрагменте - viewLifecycleOwner
 */
fun <T> LifecycleOwner.collectWhenStarted(flow: Flow<T>, action: suspend (T) -> Unit): Job =
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }