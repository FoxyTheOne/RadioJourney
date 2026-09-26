package com.myproject.radiojourney.presentation.content.radioStationList.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.myproject.radiojourney.databinding.LayoutRadioStationListBaseBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Общий родитель экранов со списками станций (страна, избранное, текущий плейлист): у всех одна разметка
 * layout_radio_station_list_base.xml - заголовок со стрелкой "назад" и RecyclerView.
 *
 * Сами экраны отличаются только тем, что показывают в этой разметке, поэтому здесь остался лишь onCreateView
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