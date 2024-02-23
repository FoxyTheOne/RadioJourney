package com.myproject.radiojourney.utils.service

import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.LatLng
import com.myproject.radiojourney.R
import com.myproject.radiojourney.data.dataSource.local.radio.ILocalRadioDataSource
import com.myproject.radiojourney.data.dataSource.network.INetworkRadioDataSource
import com.myproject.radiojourney.entities.local.CountryLocal
import com.myproject.radiojourney.entities.remote.CountryCodeRemote
import com.myproject.radiojourney.other.Constants.FILTER_FOR_BROADCAST
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_COUNT
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_END
import com.myproject.radiojourney.other.Constants.KEY_BROADCAST_LIST_SIZE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import javax.inject.Inject


/**
 * Создадим Foreground Service
 * FOREGROUND_SERVICE -> 1. Для начала, декларируем Foreground Service в Manifest. А так же добавим туда разрешение для Foreground Service
 */
@AndroidEntryPoint
class ProgressForegroundService @Inject constructor() : Service() {
    companion object {
        private const val TAG = "ProgressForeground"
        private const val CHANNEL_CASHING_ID = "CHANNEL_CASHING_ID" // 5
    }

    @Inject
    lateinit var networkRadioDataSource: INetworkRadioDataSource

    @Inject
    lateinit var localRadioDataSource: ILocalRadioDataSource

    private var notificationBuilder: NotificationCompat.Builder? = null
    private lateinit var dialogInternetTrouble: Dialog
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // https://developers.google.com/public-data/docs/canonical/countries_csv
    private val countyCodeAndLatLng: HashMap<String, LatLng> = hashMapOf(
        "AD" to LatLng(42.546245, 1.601554),
        "AE" to LatLng(23.424076, 53.847818),
        "AF" to LatLng(33.93911, 67.709953),
        "AG" to LatLng(17.060816, -61.796428),
        "AI" to LatLng(18.220554, -63.068615),
        "AL" to LatLng(41.153332, 20.168331),
        "AM" to LatLng(40.069099, 45.038189),
        "AN" to LatLng(12.226079, -69.060087),
        "AO" to LatLng(-11.202692, 17.873887),
        "AQ" to LatLng(-75.250973, -0.071389),
        "AR" to LatLng(-38.416097, -63.616672),
        "AS" to LatLng(-14.270972, -170.132217),
        "AT" to LatLng(47.516231, 14.550072),
        "AU" to LatLng(-25.274398, 133.775136),
        "AW" to LatLng(12.52111, -69.968338),
        "AX" to LatLng(
            60.187691,
            380.321455
        ), // wikipedia.org, .latlong.net - Åland, автономия в составе Финляндии
        "AZ" to LatLng(40.143105, 47.576927),
        "BA" to LatLng(43.915886, 17.679076),
        "BB" to LatLng(13.193887, -59.543198),
        "BD" to LatLng(23.684994, 90.356331),
        "BE" to LatLng(50.503887, 4.469936),
        "BF" to LatLng(12.238333, -1.561593),
        "BG" to LatLng(42.733883, 25.48583),
        "BH" to LatLng(25.930414, 50.637772),
        "BI" to LatLng(-3.373056, 29.918886),
        "BJ" to LatLng(9.30769, 2.315834),
        "BM" to LatLng(32.321384, -64.75737),
        "BN" to LatLng(4.535277, 114.727669),
        "BO" to LatLng(-16.290154, -63.588653),
        "BR" to LatLng(-14.235004, -51.92528),
        "BS" to LatLng(25.03428, -77.39628),
        "BT" to LatLng(27.514162, 90.433601),
        "BV" to LatLng(-54.423199, 3.413194),
        "BW" to LatLng(-22.328474, 24.684866),
        "BY" to LatLng(53.709807, 27.953389),
        "BZ" to LatLng(17.189877, -88.49765),
        "BQ" to LatLng(
            12.178361,
            -68.238533
        ), // countryName: Caribbean Netherlands, https://www.latlong.net/
        "CA" to LatLng(56.130366, -106.346771),
        "CC" to LatLng(-12.164165, 96.870956),
        "CD" to LatLng(-4.038333, 21.758664),
        "CF" to LatLng(6.611111, 20.939444),
        "CG" to LatLng(-0.228021, 15.827659),
        "CH" to LatLng(46.818188, 8.227512),
        "CI" to LatLng(7.539989, -5.54708),
        "CK" to LatLng(-21.236736, -159.777671),
        "CL" to LatLng(-35.675147, -71.542969),
        "CM" to LatLng(7.369722, 12.354722),
        "CN" to LatLng(35.86166, 104.195397),
        "CO" to LatLng(4.570868, -74.297333),
        "CR" to LatLng(9.748917, -83.753428),
        "CU" to LatLng(21.521757, -77.781167),
        "CV" to LatLng(16.002082, -24.013197),
        "CX" to LatLng(-10.447525, 105.690449),
        "CY" to LatLng(35.126413, 33.429859),
        "CZ" to LatLng(49.817492, 15.472962),
        "CW" to LatLng(12.121610, -68.949417), // countryName: Curaçao, https://www.latlong.net/
        "DE" to LatLng(51.165691, 10.451526),
        "DJ" to LatLng(11.825138, 42.590275),
        "DK" to LatLng(56.26392, 9.501785),
        "DM" to LatLng(15.414999, -61.370976),
        "DO" to LatLng(18.735693, -70.162651),
        "DZ" to LatLng(28.033886, 1.659626),
        "EC" to LatLng(-1.831239, -78.183406),
        "EE" to LatLng(58.595272, 25.013607),
        "EG" to LatLng(26.820553, 30.802498),
        "EH" to LatLng(24.215527, -12.885834),
        "ER" to LatLng(15.179384, 39.782334),
        "ES" to LatLng(40.463667, -3.74922),
        "ET" to LatLng(9.145, 40.489673),
        "FI" to LatLng(61.92411, 25.748151),
        "FJ" to LatLng(-16.578193, 179.414413),
        "FK" to LatLng(-51.796253, -59.523613),
        "FM" to LatLng(7.425554, 150.550812),
        "FO" to LatLng(61.892635, -6.911806),
        "FR" to LatLng(46.227638, 2.213749),
        "GA" to LatLng(-0.803689, 11.609444),
        "GB" to LatLng(55.378051, -3.435973),
        "GD" to LatLng(12.262776, -61.604171),
        "GE" to LatLng(42.315407, 43.356892),
        "GF" to LatLng(3.933889, -53.125782),
        "GG" to LatLng(49.465691, -2.585278),
        "GH" to LatLng(7.946527, -1.023194),
        "GI" to LatLng(36.137741, -5.345374),
        "GL" to LatLng(71.706936, -42.604303),
        "GM" to LatLng(13.443182, -15.310139),
        "GN" to LatLng(9.945587, -9.696645),
        "GP" to LatLng(16.995971, -62.067641),
        "GQ" to LatLng(1.650801, 10.267895),
        "GR" to LatLng(39.074208, 21.824312),
        "GS" to LatLng(-54.429579, -36.587909),
        "GT" to LatLng(15.783471, -90.230759),
        "GU" to LatLng(13.444304, 144.793731),
        "GW" to LatLng(11.803749, -15.180413),
        "GY" to LatLng(4.860416, -58.93018),
        "GZ" to LatLng(31.354676, 34.308825),
        "HK" to LatLng(22.396428, 114.109497),
        "HM" to LatLng(-53.08181, 73.504158),
        "HN" to LatLng(15.199999, -86.241905),
        "HR" to LatLng(45.1, 15.2),
        "HT" to LatLng(18.971187, -72.285215),
        "HU" to LatLng(47.162494, 19.503304),
        "ID" to LatLng(-0.789275, 113.921327),
        "IE" to LatLng(53.41291, -8.24389),
        "IL" to LatLng(31.046051, 34.851612),
        "IM" to LatLng(54.236107, -4.548056),
        "IN" to LatLng(20.593684, 78.96288),
        "IO" to LatLng(-6.343194, 71.876519),
        "IQ" to LatLng(33.223191, 43.679291),
        "IR" to LatLng(32.427908, 53.688046),
        "IS" to LatLng(64.963051, -19.020835),
        "IT" to LatLng(41.87194, 12.56738),
        "JE" to LatLng(49.214439, -2.13125),
        "JM" to LatLng(18.109581, -77.297508),
        "JO" to LatLng(30.585164, 36.238414),
        "JP" to LatLng(36.204824, 138.252924),
        "KE" to LatLng(-0.023559, 37.906193),
        "KG" to LatLng(41.20438, 74.766098),
        "KH" to LatLng(12.565679, 104.990963),
        "KI" to LatLng(-3.370417, -168.734039),
        "KM" to LatLng(-11.875001, 43.872219),
        "KN" to LatLng(17.357822, -62.782998),
        "KP" to LatLng(40.339852, 127.510093),
        "KR" to LatLng(35.907757, 127.766922),
        "KW" to LatLng(29.31166, 47.481766),
        "KY" to LatLng(19.513469, -80.566956),
        "KZ" to LatLng(48.019573, 66.923684),
        "LA" to LatLng(19.85627, 102.495496),
        "LB" to LatLng(33.854721, 35.862285),
        "LC" to LatLng(13.909444, -60.978893),
        "LI" to LatLng(47.166, 9.555373),
        "LK" to LatLng(7.873054, 80.771797),
        "LR" to LatLng(6.428055, -9.429499),
        "LS" to LatLng(-29.609988, 28.233608),
        "LT" to LatLng(55.169438, 23.881275),
        "LU" to LatLng(49.815273, 6.129583),
        "LV" to LatLng(56.879635, 24.603189),
        "LY" to LatLng(26.3351, 17.228331),
        "MA" to LatLng(31.791702, -7.09262),
        "MC" to LatLng(43.750298, 7.412841),
        "MD" to LatLng(47.411631, 28.369885),
        "ME" to LatLng(42.708678, 19.37439),
        "MG" to LatLng(-18.766947, 46.869107),
        "MH" to LatLng(7.131474, 171.184478),
        "MK" to LatLng(41.608635, 21.745275),
        "ML" to LatLng(17.570692, -3.996166),
        "MM" to LatLng(21.913965, 95.956223),
        "MN" to LatLng(46.862496, 103.846656),
        "MO" to LatLng(22.198745, 113.543873),
        "MP" to LatLng(17.33083, 145.38469),
        "MQ" to LatLng(14.641528, -61.024174),
        "MR" to LatLng(21.00789, -10.940835),
        "MS" to LatLng(16.742498, -62.187366),
        "MT" to LatLng(35.937496, 14.375416),
        "MU" to LatLng(-20.348404, 57.552152),
        "MV" to LatLng(3.202778, 73.22068),
        "MW" to LatLng(-13.254308, 34.301525),
        "MX" to LatLng(23.634501, -102.552784),
        "MY" to LatLng(4.210484, 101.975766),
        "MZ" to LatLng(-18.665695, 35.529562),
        "NA" to LatLng(-22.95764, 18.49041),
        "NC" to LatLng(-20.904305, 165.618042),
        "NE" to LatLng(17.607789, 8.081666),
        "NF" to LatLng(-29.040835, 167.954712),
        "NG" to LatLng(9.081999, 8.675277),
        "NI" to LatLng(12.865416, -85.207229),
        "NL" to LatLng(52.132633, 5.291266),
        "NO" to LatLng(60.472024, 8.468946),
        "NP" to LatLng(28.394857, 84.124008),
        "NR" to LatLng(-0.522778, 166.931503),
        "NU" to LatLng(-19.054445, -169.867233),
        "NZ" to LatLng(-40.900557, 174.885971),
        "OM" to LatLng(21.512583, 55.923255),
        "PA" to LatLng(8.537981, -80.782127),
        "PE" to LatLng(-9.189967, -75.015152),
        "PF" to LatLng(-17.679742, -149.406843),
        "PG" to LatLng(-6.314993, 143.95555),
        "PH" to LatLng(12.879721, 121.774017),
        "PK" to LatLng(30.375321, 69.345116),
        "PL" to LatLng(51.919438, 19.145136),
        "PM" to LatLng(46.941936, -56.27111),
        "PN" to LatLng(-24.703615, -127.439308),
        "PR" to LatLng(18.220833, -66.590149),
        "PS" to LatLng(31.952162, 35.233154),
        "PT" to LatLng(39.399872, -8.224454),
        "PW" to LatLng(7.51498, 134.58252),
        "PY" to LatLng(-23.442503, -58.443832),
        "QA" to LatLng(25.354826, 51.183884),
        "RE" to LatLng(-21.115141, 55.536384),
        "RO" to LatLng(45.943161, 24.96676),
        "RS" to LatLng(44.016521, 21.005859),
        "RU" to LatLng(61.52401, 105.318756),
        "RW" to LatLng(-1.940278, 29.873888),
        "SA" to LatLng(23.885942, 45.079162),
        "SB" to LatLng(-9.64571, 160.156194),
        "SC" to LatLng(-4.679574, 55.491977),
        "SD" to LatLng(12.862807, 30.217636),
        "SE" to LatLng(60.128161, 18.643501),
        "SG" to LatLng(1.352083, 103.819836),
        "SH" to LatLng(-24.143474, -10.030696),
        "SI" to LatLng(46.151241, 14.995463),
        "SJ" to LatLng(77.553604, 23.670272),
        "SK" to LatLng(48.669026, 19.699024),
        "SL" to LatLng(8.460555, -11.779889),
        "SM" to LatLng(43.94236, 12.457777),
        "SN" to LatLng(14.497401, -14.452362),
        "SO" to LatLng(5.152149, 46.199616),
        "SR" to LatLng(3.919305, -56.027783),
        "SS" to LatLng(6.876992, 31.306978), // countryName: South Sudan, https://www.latlong.net/
        "ST" to LatLng(0.18636, 6.613081),
        "SV" to LatLng(13.794185, -88.89653),
        "SY" to LatLng(34.802075, 38.996815),
        "SZ" to LatLng(-26.522503, 31.465866),
        "TC" to LatLng(21.694025, -71.797928),
        "TD" to LatLng(15.454166, 18.732207),
        "TF" to LatLng(-49.280366, 69.348557),
        "TG" to LatLng(8.619543, 0.824782),
        "TH" to LatLng(15.870032, 100.992541),
        "TJ" to LatLng(38.861034, 71.276093),
        "TK" to LatLng(-8.967363, -171.855881),
        "TL" to LatLng(-8.874217, 125.727539),
        "TM" to LatLng(38.969719, 59.556278),
        "TN" to LatLng(33.886917, 9.537499),
        "TO" to LatLng(-21.178986, -175.198242),
        "TR" to LatLng(38.963745, 35.243322),
        "TT" to LatLng(10.691803, -61.222503),
        "TV" to LatLng(-7.109535, 177.64933),
        "TW" to LatLng(23.69781, 120.960515),
        "TZ" to LatLng(-6.369028, 34.888822),
        "UA" to LatLng(48.379433, 31.16558),
        "UG" to LatLng(1.373333, 32.290275),
        "UM" to LatLng(13.919440, -176.692596),
        "US" to LatLng(37.09024, -95.712891),
        "UY" to LatLng(-32.522779, -55.765835),
        "UZ" to LatLng(41.377491, 64.585262),
        "VA" to LatLng(41.902916, 12.453389),
        "VC" to LatLng(12.984305, -61.287228),
        "VE" to LatLng(6.42375, -66.58973),
        "VG" to LatLng(18.420695, -64.639968),
        "VI" to LatLng(18.335765, -64.896335),
        "VN" to LatLng(14.058324, 108.277199),
        "VU" to LatLng(-15.376706, 166.959158),
        "WF" to LatLng(-13.768752, -177.156097),
        "WS" to LatLng(-13.759029, -172.104629),
        "XK" to LatLng(42.602636, 20.902977),
        "YE" to LatLng(15.552727, 48.516388),
        "YT" to LatLng(-12.8275, 45.166244),
        "ZA" to LatLng(-30.559482, 22.937506),
        "ZM" to LatLng(-13.133897, 27.849332),
        "ZW" to LatLng(-19.015438, 29.154857),
    )

    override fun onCreate() {
        super.onCreate()

        // FOREGROUND_SERVICE -> 3. Вызываем метод для создания Channel
        // Добавляем проверку, т.к. создавать NotificationChannel можно только начиная с API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannelCashingCountries()
        }

        // FOREGROUND_SERVICE -> 4. Channel создан, теперь можно приступить непосредственно к созданию уведомления
        // 4.1. Создаём notification c помощью билдера, который первым будет показываться нашему пользователю
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_CASHING_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.foregroundNotification_name))
            .setContentText(getString(R.string.foregroundNotification_name))
            // Для отображения прогресса добавляем .setProgress()
            .setProgress(100, 0, false)
            // Так же добавим приоритет
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true) // Без этого при обновлении уведомления каждый раз будет издаваться звук
            .setOngoing(true) // Показывать уведомление всегда, пользователь не может убрать

        // 4.2. Для того, чтобы Service из обычного перешел в Foreground, нам нужно вызвать метод startForeground() внутри этого сервиса
        // Между вызовом StartForegroundService и startForeground должно быть не более 5 секунд
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val dataSyncNotification = notificationBuilder?.build()
            dataSyncNotification?.let {
                startForeground(5, it, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            }
        } else {
            startForeground(5, notificationBuilder?.build())
        }

        // 4.3. И далее вызываем метод, который будет обновлять наш notification
        updateProgress(this)

        // Настройки диалогового окна
        dialogInternetTrouble = Dialog(this)
        // Передайте ссылку на разметку
        dialogInternetTrouble.setContentView(R.layout.layout_internet_trouble_dialog_cashing)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    // FOREGROUND_SERVICE -> 2. Создадим Channel CashingCountries
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannelCashingCountries() {
        // 2.4. Находим NotificationManager
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 2.1. Находим наши строки
        val channelNameCashingCounties = getString(R.string.foregroundNotification_name)
        val channelDescriptionCashingCounties =
            getString(R.string.foregroundNotification_description)
        // 2.2. Настраиваем приоритет
        val importanceCashingCounties = NotificationManager.IMPORTANCE_DEFAULT

        // 2.3. Создаём Channel и регистрируем его
        val channelCashingCounties = NotificationChannel(
            CHANNEL_CASHING_ID,
            channelNameCashingCounties,
            importanceCashingCounties
        ).apply {
            description = channelDescriptionCashingCounties
        }

        // 2.5. И вызываем у него (NotificationManager) метод createNotificationChannel(), куда передаём наш channel
        notificationManager.createNotificationChannel(channelCashingCounties)
    }

    // FOREGROUND_SERVICE -> 5. Обновляем наш notification
    private fun updateProgress(context: Context) {
        // 5.1. Находим NotificationManager
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 5.2 Описываем обновление нашего notification
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Получаем список кодов стран из networkRadioDataSource
                val countryCodeRemoteList = networkRadioDataSource.getCountryCodeList()

                // Для отображения прогресса
                val listSize = countryCodeRemoteList.size

                val intent =
                    Intent(FILTER_FOR_BROADCAST) // FILTER is a string to identify this intent
                intent.putExtra(KEY_BROADCAST_LIST_SIZE, listSize)
                sendBroadcast(intent)

                if (listSize > 0) {

                    // Преобразуем коды (remote) в читабельные страны (local) с локацией
                    val countryLocalList = mutableListOf<CountryLocal>()

                    Log.d(TAG, "Starting geocoder...")
//                    val geocoder = Geocoder(context)
//                    var addresses: MutableList<Address>
                    var latitude: Double
                    var longitude: Double

                    var countryCodeRemoteCount = 0
                    var percentCount = 10

                    // Для начала, сделаем все  полученные коды станций большими буквами
                    val countryCodeRemoteListUpperCase = mutableListOf<CountryCodeRemote>()
                    countryCodeRemoteList.forEach {
                        countryCodeRemoteListUpperCase.add(
                            CountryCodeRemote(
                                it.name.uppercase(),
                                it.stationcount
                            )
                        )
                    }

                    // Далее суммируем (группируем) количество станций по коду стран
                    val mergedCountryCodeRemoteList: Collection<CountryCodeRemote> =
                        countryCodeRemoteListUpperCase.groupBy(
                            CountryCodeRemote::name,
                            CountryCodeRemote::stationcount
                        )
                            .mapValues { (id, stationCount) ->
                                CountryCodeRemote(id, stationCount.sum())
                            }
                            .values

//                    mergedCountryCodeRemoteList.forEach { result ->
//                        Log.d(TAG, "результат2 преобразования в mergedCountryCodeRemoteList: $result")
//                    }

                    // Перебираем преобразованный не повторяющийся список кодов стран
                    mergedCountryCodeRemoteList.forEach { countryCodeRemote ->
                        countryCodeRemoteCount += 1

                        // Узнаем название страны
                        val loc = Locale("", countryCodeRemote.name)
                        val countryName = loc.displayName

                        // Узнаем местоположение
                        // В этом месте часто исключение, как будто проблема с интернетом
//                        Log.d(TAG, "Starting geocoder")

                        // Deprecated. Using lambda Geocoder.getAddress instead, it is written below this method
//                        addresses =
//                            geocoder.getFromLocationName(countryName, 1) as MutableList<Address>
//                        if (addresses.size > 0) {
//                            latitude = addresses[0].latitude
//                            longitude = addresses[0].longitude
//                        }

//                        // Using our private fun, written below this method
//                        geocoder.getAddress(countryName) { address: android.location.Address? ->
//                            if (address != null) {
//                                latitude = address.latitude
//                                longitude = address.longitude
//                            }
//                        }
//
//                        Log.d(TAG, "результат addresses: $latitude, $longitude")

                        // Ищем локацию в нашей коллекции
                        val address =
                            countyCodeAndLatLng[countryCodeRemote.name] ?: LatLng(0.0, 0.0)
                        latitude = address.latitude
                        longitude = address.longitude

                        if (latitude == 0.0) {
                            Log.d(
                                TAG,
                                "!! Адрес не найден, countryCodeRemote.name: ${countryCodeRemote.name}, countryName: $countryName,  результат address: $latitude, $longitude"
                            )
                        } else {
//                            Log.d(TAG, "результат address: $latitude, $longitude")
                        }

                        // remote -> local
                        val countryLocal = CountryLocal.fromRemoteToLocal(
                            countryCodeRemote,
                            countryName = countryName,
                            countryLocation = LatLng(latitude, longitude)
                        )
                        countryLocalList.add(countryLocal)

//                        Log.d(TAG, "End of geocoding")

                        // Если notificationBuilder != null
                        notificationBuilder?.let { builder ->
                            builder
                                .setContentText("Progress: $countryCodeRemoteCount files")
                                .setProgress(listSize, countryCodeRemoteCount, false)
                                .setSound(null)
                            // 5.3. Передаём notificationManager наш билдер notification
                            notificationManager.notify(5, builder.build())
                        }

                        // 1.Broadcast для горизонтальной полосы прогресса в фрагменте (2,3 - в фрагменте)
                        val countingForBroadcast = percentCount * listSize / 100
                        if (countryCodeRemoteCount == countingForBroadcast) {
                            percentCount += 10

                            intent.putExtra(KEY_BROADCAST_COUNT, countryCodeRemoteCount)
                            sendBroadcast(intent)
                        }
                    }
                    Log.d(TAG, "...End of geocoding")

                    Log.d(
                        TAG,
                        "Успешное преобразование стран. Получен результат [0]: ${countryLocalList[0]}"
                    )

                    // Теперь сохраним наши страны в Room
                    localRadioDataSource.saveCountryList(countryLocalList)
                    Log.d(
                        TAG,
                        "Список сохраняется в локальную базу данных: size = ${countryLocalList.size}, countryLocalList = $countryLocalList"
                    )

                } else {
                    Log.d(
                        TAG,
                        "countryCodeRemoteList size = 0. The server is down. Please, try again later"
                    )
                    // TODO notification "The server is down. Please, try again later" -> Мы делаем его не здесь, а во фрагментах: в загрузочном фрагменте по результатам пришедшей информации и в HomeFragment
                }

                // 5.4. Когда прогресс заканчивается, закрываем Foreground, удаляем уведомления, stop service
                intent.putExtra(KEY_BROADCAST_END, 100)
                sendBroadcast(intent)

                // stopForeground(true) - deprecated
                // STOP_FOREGROUND_DETACH if set, the notification previously supplied to startForeground(int, Notification) will be detached from the service's lifecycle.
                // The notification will remain shown even after the service is stopped and destroyed.
                // STOP_FOREGROUND_REMOVE if supplied, the notification previously supplied to startForeground(int, Notification) will be cancelled and removed from display.
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(true)
                }

                notificationManager.cancel(5)
                stopSelf()

            } catch (e: IOException) {
                Log.d(
                    TAG,
                    "Exception: ${e.message}. Please, try turn on and then turn off airplane mode (on the emulator). And then restart the program, if needed."
                )
                e.printStackTrace()

                // TODO notification "Cashing failed"
                Toast.makeText(context, "Cashing failed", Toast.LENGTH_LONG)
                    .show()
//                serviceScope.launch(Dispatchers.Main) {
//                    dialogInternetTrouble.show()
//                }

                // stopForeground(true) - deprecated
                // STOP_FOREGROUND_DETACH if set, the notification previously supplied to startForeground(int, Notification) will be detached from the service's lifecycle.
                // The notification will remain shown even after the service is stopped and destroyed.
                // STOP_FOREGROUND_REMOVE if supplied, the notification previously supplied to startForeground(int, Notification) will be cancelled and removed from display.
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(true)
                }

                notificationManager.cancel(5)
                stopSelf()
            }
        }
    }

//    // Lambda for getting address from countryName
//    @Suppress("DEPRECATION")
//    private fun Geocoder.getAddress(
//        countryName: String,
//        address: (android.location.Address?) -> Unit
//    ) {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            getFromLocationName(countryName, 1) { address(it.firstOrNull()) }
//            return
//        }
//
//        // Для старых версий Android:
//        try {
//            address(getFromLocationName(countryName, 1)?.firstOrNull())
//        } catch (e: Exception) {
//            //will catch if there is an internet problem
//            address(null)
//        }
//
//    }

    // FOREGROUND_SERVICE -> 6. Запустим наш Foreground Service из NotificationFragment

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        serviceJob.cancel()
    }
}