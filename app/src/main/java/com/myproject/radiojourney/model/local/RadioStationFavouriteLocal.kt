package com.myproject.radiojourney.model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myproject.radiojourney.model.presentation.RadioStationFavouritePresentation
import com.myproject.radiojourney.model.presentation.RadioStationPresentation

/**
 * В нашем случае в одной программе может быть несколько пользователей, но одна общая база Room.
 * Следовательно, User может добавлять себе в избранное разные station (relation one-to-many). Однако,
 * одну и ту же station могут себе добавить разные User, а это уже relation many-to-many.
 *
 * Поэтому в данном случае (т.к. пока что это тестовый проект, в будущем экраны регистрации в этом приложении не пригодятся),
 * чтобы облегчить себе работу и ограничиться relation one-to-many, будем создавать копии station при добавлении в избранное.
 * Тогда мы сможем делать копии одинаковых станций, но с разным UserId (как если бы User создавал плейлисты)
 */
@Entity
data class RadioStationFavouriteLocal(
    @PrimaryKey
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "userCreatorId") val userCreatorId: Int,
    @ColumnInfo(name = "stationName") val stationName: String,
    @ColumnInfo(name = "clickCount") val clickCount: Int,
    @ColumnInfo(name = "countryCode") val countryCode: String,
    @ColumnInfo(name = "isStationInFavourite") var isStationInFavourite: Boolean
) {

    companion object {
        fun fromPresentationToFavouriteLocal(
            presentation: RadioStationPresentation,
            userCreatorId: Int,
            isStationInFavourite: Boolean = presentation.isStationInFavourite
        ): RadioStationFavouriteLocal =
            RadioStationFavouriteLocal(
                url = presentation.url,
                userCreatorId = userCreatorId,
                stationName = presentation.stationName,
                clickCount = presentation.clickCount,
                countryCode = presentation.countryCode,
                isStationInFavourite = isStationInFavourite
            )

        fun fromFavouritePresentationToFavouriteLocal(
            favouritePresentation: RadioStationFavouritePresentation,
            isStationInFavourite: Boolean = favouritePresentation.isStationInFavourite
        ): RadioStationFavouriteLocal =
            RadioStationFavouriteLocal(
                url = favouritePresentation.url,
                userCreatorId = favouritePresentation.userCreatorId,
                stationName = favouritePresentation.stationName,
                clickCount = favouritePresentation.clickCount,
                countryCode = favouritePresentation.countryCode,
                isStationInFavourite = isStationInFavourite
            )
    }

}