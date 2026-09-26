# Указатель по коду: какая задача где решена

Слева задача, справа файл, где она решена. У каждого класса есть комментарий (KDoc) с пояснением,
зачем он нужен и на что стоит обратить внимание. Этот файл - и мой собственный конспект,
и подсказка тому, кто пришёл посмотреть, как сделана та или иная вещь.

Копировать код в свои проекты без моего разрешения нельзя, см. "Условия использования"
в [README](README.md).
Разобраться, как что устроено, и сделать у себя по-своему - пожалуйста.

Пути указаны от `app/src/main/java/com/myproject/radiojourney/`.

## Настройка проекта

| Задача                                                                     | Где смотреть                                                        |
|----------------------------------------------------------------------------|---------------------------------------------------------------------|
| Hilt: точка входа приложения, WorkManager с `@Inject` в Worker             | `App.kt`, `AndroidManifest.xml` (удаление `InitializationProvider`) |
| Hilt-модули: что в SingletonComponent, что в ViewModelComponent            | `di/Module.kt`                                                      |
| Hilt для сервиса (свой компонент и scope)                                  | `di/ServiceModule.kt`                                               |
| Scope приложения для работы, которая не должна отмениться вместе с экраном | `di/ApplicationScope.kt`                                            |
| Gradle: KSP вместо kapt, secrets-plugin для ключа карт, имя APK с датой    | `app/build.gradle`                                                  |

## Данные

| Задача                                                                                          | Где смотреть                                                                                                                  |
|-------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| DataStore вместо SharedPreferences, с переносом старых значений                                 | `data/preference/AppPreferenceStorage.kt`                                                                                     |
| Room: база, DAO, entity, конвертер типов                                                        | `data/localDatabaseRoom/`                                                                                                     |
| Room: подписка на таблицу через Flow                                                            | `data/localDatabaseRoom/ICountryDAO.kt`                                                                                       |
| Room: несколько запросов как одна операция (`@Transaction`)                                     | `ICountryDAO.replaceCountryList`, `IRadioStationDAO.setStationFavourite`                                                      |
| Retrofit + OkHttp: один клиент на приложение, таймауты, свой User-Agent                         | `di/Module.kt`, `data/dataSource/network/service/`                                                                            |
| Запрос к нескольким серверам по очереди, пока один не ответит                                   | `data/dataSource/network/NetworkRadioDataSource.kt` (`requestFromAnyServer`)                                                  |
| Получение списка серверов через DNS                                                             | там же, `updateDNSList`                                                                                                       |
| Ответ сети с состоянием (успех / ошибка / загрузка)                                             | `other/Resource.kt`                                                                                                           |
| Разделение моделей: remote, local, domain, presentation и мапперы между ними                    | `data/mapper/DataMappers.kt`, `domain/model/`, `presentation/model/`                                                          |
| Чтение данных из файла в assets (CSV)                                                           | `data/dataSource/local/country/CountryCoordinatesDataSource.kt`                                                               |
| Координаты страны по её коду, с запасным вариантом через Geocoder                               | там же                                                                                                                        |
| Room: миграция базы при добавлении таблицы                                                      | `data/localDatabaseRoom/DatabaseMigrations.kt`, `di/Module.kt` (`addMigrations`)                                              |
| Данные из сети с запасным вариантом из базы, если сервер недоступен                             | `data/repository/MainRadioStationRepository.kt` (`getRadioStationList`), `data/localDatabaseRoom/entity/SavedStationLocal.kt` |
| Понять по ошибке, что именно случилось с запросом (нет сети / сервер молчит / ответ обрывается) | `data/dataSource/network/NetworkRadioDataSource.kt` (`failureReason`), `other/ServerError.kt`                                 |
| Запрос по новому соединению, без переиспользования открытых                                     | `data/dataSource/network/service/RadioServiceWrapper.kt`                                                                      |
| Страна телефона без разрешения на местоположение (сотовая сеть, SIM, регион)                    | `data/dataSource/local/country/DeviceCountryDataSource.kt`                                                                    |

## Фоновая работа

| Задача                                                                            | Где смотреть                                                                |
|-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| WorkManager: единожды запускаемая задача, ожидание интернета, прогресс для экрана | `data/worker/CountryCacheWorker.kt`, `data/worker/CountryCacheScheduler.kt` |
| Прогресс WorkManager как Flow                                                     | `CountryCacheScheduler.progress`                                            |

## Плеер (media3)

| Задача                                                   | Где смотреть                                                  |
|----------------------------------------------------------|---------------------------------------------------------------|
| Фоновый плеер с уведомлением (MediaLibraryService)       | `utils/exoplayer/MusicService.kt`                             |
| Связь экрана с сервисом плеера (MediaBrowser)            | `utils/exoplayer/MusicServiceConnection.kt`                   |
| Свои команды экран - сервис и ответ сервиса экрану       | `utils/exoplayer/callback/MusicLibrarySessionCallback.kt`     |
| Станция ↔ MediaItem (метаданные для уведомления, extras) | `utils/exoplayer/RadioStationMediaItems.kt`                   |
| Воспроизведение потоков HLS (.m3u8)                      | `di/ServiceModule.kt`, `RadioStationMediaItems.kt` (MIME-тип) |
| Ожидание готовности данных из другого потока             | `utils/exoplayer/ReadinessState.kt`                           |

## Экраны

| Задача                                                                                                                              | Где смотреть                                                                                               |
|-------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| Подписка на Flow по жизненному циклу экрана (замена `LiveData.observe`)                                                             | `presentation/common/FlowExtensions.kt`                                                                    |
| Состояние экрана одним `StateFlow` (загрузка / данные / ошибка)                                                                     | `presentation/content/radioStationList/radioList/RadioListViewModel.kt`                                    |
| Одноразовые события для экрана (ошибка, переход) через Channel                                                                      | `presentation/MainViewModel.kt`, `presentation/firstScreen/FirstScreenLoadingViewModel.kt`                 |
| Аргументы навигации внутри ViewModel (`SavedStateHandle`)                                                                           | `RadioListViewModel.kt`                                                                                    |
| Стартовый экран зависит от данных (условная навигация)                                                                              | `presentation/MainActivity.kt` + `MainViewModel.startDestinationId`                                        |
| Ответ диалога, переживающий пересоздание экрана (Fragment Result API)                                                               | `presentation/content/radioStationList/myStations/AddMyStationDialogFragment.kt` + `MyStationsFragment.kt` |
| Запрос разрешений: объяснение до системного окна и что делать при отказе                                                            | `presentation/common/PermissionRationale.kt`                                                               |
| Список, который пользователь наполняет сам: форма в диалоге, проверка ввода с ошибкой под полем, правка и удаление с подтверждением | `presentation/content/radioStationList/myStations/`, `domain/myStationsUseCase/`                           |
| Показать что-то один раз за запуск приложения (не за экран)                                                                         | `presentation/common/PermissionSessionState.kt`                                                            |
| Диалог с заголовком и текстом (один класс на все экраны)                                                                            | `presentation/common/InfoDialog.kt`                                                                        |
| Проверка интернета                                                                                                                  | `presentation/common/NetworkExtensions.kt`                                                                 |
| RecyclerView: общий адаптер с DiffUtil и клик через лямбду                                                                          | `presentation/content/radioStationList/adapter/`                                                           |
| ViewPager2 как "листалка" элементов                                                                                                 | `SwipeRadioStationAdapter.kt` + `MainActivity`                                                             |
| Отступы и свой цвет под строкой состояния и панелью навигации (edge-to-edge, Android 15+)                                           | `MainActivity.applySystemBarInsets`, `res/values/themes.xml`                                               |
| Безопасные переходы между экранами (двойное нажатие не роняет приложение)                                                           | `presentation/common/NavigationExtensions.kt`                                                              |
| Вопрос "выйти из приложения?" по системной кнопке "Назад"                                                                           | `presentation/content/homeRadio/HomeRadioFragment.kt` (`confirmExit`)                                      |
| Дата в формате языка телефона                                                                                                       | `presentation/common/StationListNotice.kt`                                                                 |
| Google Maps: маркеры, своё окошко над маркером, текущее местоположение                                                              | `presentation/content/homeRadio/HomeRadioFragment.kt`, `MarkerInfoWindowAdapter.kt`                        |
