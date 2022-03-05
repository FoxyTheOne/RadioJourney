package com.myproject.radiojourney.presentation.authentication.base

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.myproject.radiojourney.R
import com.myproject.radiojourney.data.sharedPreference.IAppSharedPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseAuthFragmentAbstract : Fragment() {
    companion object {
        private const val TAG = "BaseAuthFragment"
    }

    // Воспользуемся global navigation actions, чотбы открывать сразу контент, если есть токен
    @Inject
    lateinit var preference: IAppSharedPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenForCheck =  preference.getToken()
        Log.d(TAG, "tokenForCheck = $tokenForCheck")

        if (tokenForCheck.isNotBlank()) {
            findNavController().navigate(R.id.action_global_content_nav_graph)
        }
    }
}