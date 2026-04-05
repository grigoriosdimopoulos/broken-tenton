package com.linkedinautomation.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.linkedinautomation.UserPreferencesProto
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserPreferencesProto> {
    override val defaultValue: UserPreferencesProto = UserPreferencesProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserPreferencesProto =
        UserPreferencesProto.parseFrom(input)

    override suspend fun writeTo(t: UserPreferencesProto, output: OutputStream) =
        t.writeTo(output)
}

val Context.userPreferencesDataStore: DataStore<UserPreferencesProto> by dataStore(
    fileName = "user_preferences.pb",
    serializer = UserPreferencesSerializer
)
