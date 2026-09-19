package com.myproject.radiojourney.utils.exoplayer

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}

/**
 * Состояние загрузки данных и действия, которые ждут её окончания (whenReady).
 * Раньше этот код был скопирован в FirebaseMusicSource, MusicLibrarySessionCallback и MainViewModel (и enum State - дважды).
 *
 * Потокобезопасно: state меняется в потоке IO, а whenReady вызывается в главном потоке.
 */
class ReadinessState {

    // Список лямбд action, которые будут переданы в whenReady(), пока state == STATE_CREATED или state == STATE_INITIALIZING
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    @Volatile
    var state: State = State.STATE_CREATED // State on default
        set(value) {
            val listeners = synchronized(onReadyListeners) {
                field = value
                if (value == State.STATE_INITIALIZED || value == State.STATE_ERROR) {
                    // Каждая лямбда срабатывает один раз: список очищается
                    onReadyListeners.toList().also { onReadyListeners.clear() }
                } else {
                    emptyList()
                }
            }
            // If there will be STATE_ERROR instead STATE_INITIALIZED, we will get "false". So we can check, if it was successful or not
            listeners.forEach { listener -> listener(value == State.STATE_INITIALIZED) }
        }

    /**
     * Выполнить action, когда данные будут готовы (сразу, если уже готовы). В action передаётся true, если загрузка успешна.
     * Проверка state и добавление в список - под тем же synchronized, что и в setter state: иначе лямбда могла добавиться
     * сразу после того, как список уже очищен, и её никто бы не вызвал.
     * @return true, если данные уже были готовы
     */
    fun whenReady(action: (Boolean) -> Unit): Boolean {
        val readyState = synchronized(onReadyListeners) {
            if (state == State.STATE_CREATED || state == State.STATE_INITIALIZING) {
                onReadyListeners += action // We are not ready, so just add action to list (we will do it later, when we will be ready)
                null
            } else {
                state
            }
        }
        if (readyState == null) return false // not ready
        action(readyState == State.STATE_INITIALIZED) // we are ready, so we can call action
        return true
    }
}