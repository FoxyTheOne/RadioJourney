package com.myproject.radiojourney.model.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * In order to query the list of users and corresponding playlists, you must first model the one-to-many relationship between the two entities.
 *
 * To do this, create a new data class where each instance holds an instance of the parent entity and a list of all corresponding child entity instances.
 * Add the @Relation annotation to the instance of the child entity, with parentColumn set to the name of the primary key column of the parent entity
 * and entityColumn set to the name of the column of the child entity that references the parent entity's primary key.
 */
data class UserWithStations(
    @Embedded
    val user: UserEntity,
    @Relation(
        // parentColumn - имя столбца первичного ключа родительской сущности
        parentColumn = "id",
        // entityColumn — имя столбца дочерней сущности, которая ссылается на первичный ключ родительской сущности
        entityColumn = "userCreatorId"
    )
    val stations: List<RadioStationFavouriteLocal>
)