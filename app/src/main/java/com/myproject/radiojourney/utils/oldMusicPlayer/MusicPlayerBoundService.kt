package com.myproject.radiojourney.utils.oldMusicPlayer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.myproject.radiojourney.R
import com.myproject.radiojourney.entities.presentation.RadioStationPresentation
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.lang.Exception

/**
 * Создадим Bound Service
 * BOUND_SERVICE -> 1. Для начала, задекларируем Bound Service в Manifest
 * BOUND_SERVICE -> 4. Расширяем Service(), а так же наш интерфейс IAppBinder
 */
@AndroidEntryPoint
class MusicPlayerBoundService : Service(), IMusicPlayerBinder {
    companion object {
        private const val TAG = "MusicPlayerBoundService"
    }

    // PLAY URL (MP3), MEDIA PLAYER -> 1. Создаём переменные
    // MediaPlayer – класс, который позволит вам проигрывать аудио/видео файлы с возможностью сделать паузу и перемотать в нужную позицию.
    // MediaPlayer умеет работать с различными источниками, это может быть: путь к файлу (на SD или в инете), адрес потока, Uri или файл из папки res/raw.
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private val intentIsPaused =
        Intent("FAILURE_PLAYING")

    override fun onCreate() {
        super.onCreate()
        // PLAY URL (MP3), MEDIA PLAYER -> 2. Получаем AudioManager
        audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // BOUND_SERVICE -> 5. Когда кто-то подписывается на сервис, он в первую очередь залетает в этот метод, где он должен получить байндер. Вернём его:
    override fun onBind(intent: Intent?): IBinder {
        return MusicPlayerBoundServiceBinder()
    }

    // PLAY URL (MP3), MEDIA PLAYER -> 6. В методе onDestroy обязательно освобождаем ресурсы проигрывателя
    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    // Далее опишем наши методы из интерфейса:

    // PLAY URL (MP3), MEDIA PLAYER -> 3. Метод для запуска проигрывания.
    override fun playMediaPlayerAudioAndShowNotification(radioStation: RadioStationPresentation) {
        // MUSIC PLAYER ON NOTIFICATION -> 3.1. Create notification on click. Called when play is play is pressed
        // Channel создан, теперь можно приступить непосредственно к созданию уведомления
        CreateNotification.updateNotification(
            this,
            radioStation,
            R.drawable.ic_pause_orange
        )

//        // Сначала мы освобождаем ресурсы текущего проигрывателя.
//        releaseMediaPlayer()
        // upd -> Иногда возникает задваивание. Продублирую так же остановку проигрывания перед запуском новой радиостанции (вызываю stopMediaPlayerAudio() вместо просто releaseMediaPlayer() для освобождения ресурсов текущего проигрывателя)
        stopMediaPlayerAudio()

        // Затем стартуем проигрывание.
        Toast.makeText(this, "Connecting to radio station...", Toast.LENGTH_SHORT).show()

        try {
            Log.d(TAG, "PLAY URL (MP3), MEDIA PLAYER -> start playing HTTP")
            mediaPlayer = MediaPlayer().apply {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                } else {
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
                // setAudioStreamType – задает аудио-поток, который будет использован для проигрывания. Их существует несколько: STREAM_MUSIC, STREAM_NOTIFICATION и пр.
                // Предполагаю, что созданы они для того, чтобы можно было задавать разные уровни громкости, например, играм, звонкам и уведомлениям.
                // Этот метод можно и пропустить, если вам не надо явно указывать какой-то поток. Насколько я понял, по умолчанию используется STREAM_MUSIC.

                reset()

                try {
                    setDataSource(radioStation.urlResolved)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                // Далее используется метод prepare или prepareAsync (в паре с OnPreparedListener).
                // Эти методы подготавливают плеер к проигрыванию. Как понятно из названия, prepareAsync делает это асинхронно и, когда все сделает, сообщит об этом слушателю из метода setOnPreparedListener.
                // А метод prepare работает синхронно. Соотвественно, если хотим прослушать файл из инета, то используем prepareAsync, иначе наше приложение повесится, т.к. заблокируется основной поток, который обслуживает UI.
                Log.d(TAG, "PLAY URL (MP3), MEDIA PLAYER -> prepareAsync")
                setOnPreparedListener {
                    Log.d(TAG, "PLAY URL (MP3), MEDIA PLAYER -> onPrepared")
                    it.start() // Метод start запускает проигрывание
                    Toast.makeText(baseContext, "Audio started playing", Toast.LENGTH_SHORT).show()
                }
                prepareAsync() // might take long! (for buffering, etc)
                setOnErrorListener { _, what, extra ->
                    // Если не получилось запустить радиостанцию, нужно изменить кнопочку обратно на паузу. Отправим intent в наш фрагмент
                    intentIsPaused.putExtra("play_failure", true)
                    sendBroadcast(intentIsPaused)

                    Toast.makeText(baseContext, "Failed to connect.", Toast.LENGTH_SHORT).show()
                    stopMediaPlayerNotification(radioStation)
                    Log.d(TAG, "PLAY URL (MP3), MEDIA PLAYER -> setOnErrorListener $what $extra")
                    true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()

            // Если не получилось запустить радиостанцию, нужно изменить кнопочку обратно на паузу. Отправим intent в наш фрагмент
            intentIsPaused.putExtra("play_failure", true)
            sendBroadcast(intentIsPaused)

            Toast.makeText(
                baseContext,
                "Failed to connect. Try to click \"play\" or select another station",
                Toast.LENGTH_SHORT
            ).show()
            stopMediaPlayerNotification(radioStation)
        }

        if (mediaPlayer == null) return
    }

    // PLAY URL (MP3), MEDIA PLAYER -> 5. Метод для остановки проигрывания
    override fun stopMediaPlayerNotification(radioStation: RadioStationPresentation) {
        // MUSIC PLAYER ON NOTIFICATION -> 3.2. Create notification on click. Called when play is play is pressed
        // Channel создан, теперь можно приступить непосредственно к созданию уведомления
        CreateNotification.updateNotification(
            this,
            radioStation,
            R.drawable.ic_play_arrow_orange
        )
    }

    override fun stopMediaPlayerAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop() // Останавливает проигрывание
            }
        }
        releaseMediaPlayer()
    }

    // PLAY URL (MP3), MEDIA PLAYER -> 4. В методе releaseMP мы выполняем метод release.
    // Он освобождает используемые проигрывателем ресурсы, его рекомендуется вызывать когда вы закончили работу с плеером.
    // Более того, хелп рекомендует вызывать этот метод и при onPause/onStop, если нет острой необходимости держать объект.
    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // BOUND_SERVICE -> 3. Создадим вложенный класс-байндер
    inner class MusicPlayerBoundServiceBinder : Binder() {
        // BOUND_SERVICE -> 6. Здесь мы должны написать код, чтобы байндер возвратил интерфейс
        fun getMusicPlayerBoundServiceInstance(): IMusicPlayerBinder = this@MusicPlayerBoundService
    }
}

// BOUND_SERVICE -> 2. Создаём интерфейс для общения с сервисом. В нём прописываем необходимые методы в дальнейшем
interface IMusicPlayerBinder {
    fun playMediaPlayerAudioAndShowNotification(radioStation: RadioStationPresentation)
    fun stopMediaPlayerNotification(radioStation: RadioStationPresentation)
    fun stopMediaPlayerAudio()
}

// BOUND_SERVICE -> 7. Теперь перейдём в нужный фрагмент и подпишемся на сервис