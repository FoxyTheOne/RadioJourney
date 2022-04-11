# TMS_GRADUATE_WORK
It is a repository for preparing graduate work. I will continue improving the project in private repository.

This source code is free for studying purposes but you are not allowed to copy and use it in other applications (projects).

Created by Alina Piatrova.

------------------

My graduate work is an application for listening to Internet radio stations. I am using API.radio-browser.info which allows you to access to collected internet radio stations from all over the world (https://www.radio-browser.info/). This API is available for free. The author allows to use it in free and commercial software without restrictions.

On the main page you will find a google map with markers, by clicking on which you can see the number of available Internet radio stations in this country. A list of Internet radio stations in the selected country (recycler view) can be opened by clicking on this message and then you can select the radio you are interested in.

By clicking on the radio, the user returns to the main screen and can listen to it if this radio is currently working.

There are Sign up and Sign in screens with minimal checks of data entry. They are implemented just for practice. Before entering the application, you are asked for your location permission.

- The project uses the MVVM architectural pattern and the Clean Architecture concept;
- Hilt is used here, as well as Navigation component and View Binding;
- To store small key-value pairs (token for instance), I use Shared preferences;
- The Room database is used to store marker locations on the map, as well as to store favorite radio stations. You need to wait for the end of caching at the first start. Further the data is taken from the subscription to the local database. User registration is done for practice, so the logged data is also stored in the Room;
- I use Foreground service to display caching progress in notification;
- I make all requests to the server, or to the local database from the ViewModel, through Coroutines;
- For the request to the server, Retrofit2 is used.

------------------

Данный проект представляет собой приложение для прослушивания интернет радиостанций. Я использую API.radio-browser.info, который предоставляет доступ к собранным интернет-радиостанциям со всего мира (https://www.radio-browser.info/). Этот API доступен бесплатно. Автор разрешает его использовать в бесплатном и платном программном обеспечении без ограничений.

На главной странице вы найдете google карту с маркерами, нажимая на которые можно увидеть количество доступных в этой стране интернет радиостанций. Нажав на это сообщение, открывается список интернет радиостанций в выбранной стране (recycler view), где можно выбрать интересующее радио.

По клику на радио, пользователь возвращается на главный экран и может его прослушать, если это радио в данный момент работает.

Перед входом в приложение для практики реализованы экраны Sign up и Sign in с минимальными проверками правильности ввода данных. Перед входом в приложение запрашивается разрешение на доступ к местоположению.

- В проекте используется архитектурный паттерн MVVM и подход Clean Architecture;
- Используется DI – Hilt, а также Navigation component и View Binding;
- Для хранения небольших пар ключ-значение (логин и пароль, токен и тп.) я использую Shared preferences;
- Для сохранения локаций маркеров на карте, а также для хранения избранных радиостанций используется реляционная база данных Room. При первом запуске нужно дождаться окончания кеширования, в дальнейшем данные берутся из подписки на локальную базу данных. Регистрация пользователя сделана для примера, поэтому регистрируемые данные так же сохраняются в Room;
- Для отображения прогресса кеширования в уведомлении используется Foreground service;
- Все запросы на сервер, либо в локальную БД из ViewModel я делаю через Coroutines;
- Для запроса на сервер используется Retrofit2.

------------------

При использовании функции родного android геокодера для получения местоположения по адресу - geocoder.getFromLocationName(), довольно часто приходит исключение «grpc failed». 

Процитирую один из комментариев по этому поводу с сайта stackoverflow.com:

It looks like this is ongoing issue that was reported in the Google issue tracker both for real devices and emulators. You can refer to the following bugs:

https://issuetracker.google.com/issues/64418751

https://issuetracker.google.com/issues/64247769

Unfortunately, Google haven't solved these issues yet.

As a workaround you can consider using the Geocoding API web service. Please note that there is a Java client library for web services that you can find on Github:

https://github.com/googlemaps/google-maps-services-java

Using Java client library for web services you can implement reverse geocoding lookup that shouldn't give you the error that you experience with native Android geocoder.

The Javadoc for client library is located at

https://googlemaps.github.io/google-maps-services-java/v0.2.5/javadoc/

I hope this helps!

В своём проекте я не использовала Geocoding API на данный момент, т.к. он платный. Если при первом запуске программы во время кэширования возникнет ошибка, следует закрыть программу и все его уведомления (если они есть), затем включить и выключить авиа режим, после чего запустить приложение снова.

Это имеет значение только при первом запуске программы. При дальнейшем использовании программа будет работать даже если во время кеширования возникнет сбой.

------------------

Copyright 2022, Piatrova Alina. All rights reserved.

