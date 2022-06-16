package com.myproject.radiojourney.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myproject.radiojourney.other.Constants.UPDATE_PLAYER_POSITION_INTERVAL
import com.myproject.radiojourney.utils.exoplayer.MusicService
import com.myproject.radiojourney.utils.exoplayer.MusicServiceConnection
import com.myproject.radiojourney.utils.extension.currentPlaybackPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class RadioStationPlayingViewModel @Inject constructor(
    musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val playbackState = musicServiceConnection.playbackStateLiveData

    private val _curSongDuration = MutableLiveData<Long>()
    val curSongDuration: LiveData<Long> = _curSongDuration

    private val _curPlayerPosition = MutableLiveData<Long>()
    val curPlayerPosition: LiveData<Long> = _curPlayerPosition

    init {
        updateCurrentPlayerPosition()
    }

    // coroutine for continuously updating _curPlayerPosition and _curSongDuration
    private fun updateCurrentPlayerPosition() {
        viewModelScope.launch {
            // the coroutine will be cleared when view model is cleared, so we can use such circle
            while(true) {
                // there is no function for getting value from exoplayer of player current position. So, we must calculate it on our oun (we'll write an extension)
                val pos = playbackState.value?.currentPlaybackPosition
                if(curPlayerPosition.value != pos) {
                    _curPlayerPosition.postValue(pos ?: continue)
                    _curSongDuration.postValue(MusicService.curSongDuration)
                }
                delay(UPDATE_PLAYER_POSITION_INTERVAL)
            }
        }
    }

}