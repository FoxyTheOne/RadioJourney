package com.myproject.radiojourney.model.local

import com.google.android.gms.maps.model.LatLng

data class Place (
    val name: String,
    val latLng: LatLng
)