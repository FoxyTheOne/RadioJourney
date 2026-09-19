package com.myproject.radiojourney.presentation.content.radioStationList.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.myproject.radiojourney.databinding.LayoutRadioStationListBaseBinding
import com.myproject.radiojourney.presentation.content.base.BaseContentFragmentAbstract
import dagger.hilt.android.AndroidEntryPoint

/**
 * Базовый фрагмент для фрагментов со списком радиостанций: общая разметка, toolbar и меню выхода
 */
@AndroidEntryPoint
abstract class BaseRadioListFragmentAbstract : BaseContentFragmentAbstract() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = LayoutRadioStationListBaseBinding.inflate(inflater, container, false)
        // TOOLBAR - где будет находиться в нашем layout
        setToolbar(binding.homeToolbar)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarMenu()
    }
}