package com.myproject.radiojourney.presentation.content.radioStationList.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.myproject.radiojourney.databinding.LayoutRadioStationListBaseBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Базовый фрагмент для фрагментов со списком радиостанций: общая разметка
 */
@AndroidEntryPoint
abstract class BaseRadioListFragmentAbstract : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = LayoutRadioStationListBaseBinding.inflate(inflater, container, false)
        return binding.root
    }
}