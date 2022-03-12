package com.myproject.radiojourney.presentation.content.homeRadio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.myproject.radiojourney.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MarkerInfoWindowAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : GoogleMap.InfoWindowAdapter {

    private lateinit var inflater: LayoutInflater

    override fun getInfoContents(marker: Marker): View? {
//        // 1. Get tag
//        val place = marker.tag as? Place ?: return null
//
//        // 2. Inflate view and set title, address, and rating
//        val view = LayoutInflater.from(context).inflate(
//            R.layout.marker_info_contents, null
//        )
//
//        view.findViewById<TextView>(
//            R.id.text_view_title
//        ).text = place.name
//        view.findViewById<TextView>(
//            R.id.text_view_address
//        ).text = place.address.toString()
//        view.findViewById<TextView>(
//            R.id.text_view_rating
//        ).text = "Rating: %.2f".format(place.rating)
//
//        return view

        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
//        // Return null to indicate that the
//        // default window (white bubble) should be used
//        return null

        inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val v: View = inflater.inflate(R.layout.marker_info_contents, null);
        val title = v.findViewById(R.id.text_view_title) as TextView
        val subtitle = v.findViewById(R.id.text_view_list) as TextView
        title.text = marker.title
        subtitle.text = marker.snippet
        return v
    }
}