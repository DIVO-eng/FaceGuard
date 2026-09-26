package com.faceguard.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromEventType(v: AuthEventType): String = v.name
    @TypeConverter
    fun toEventType(v: String): AuthEventType = AuthEventType.valueOf(v)

    @TypeConverter
    fun fromRecognition(v: RecognitionStatus): String = v.name
    @TypeConverter
    fun toRecognition(v: String): RecognitionStatus = RecognitionStatus.valueOf(v)

    @TypeConverter
    fun fromBackup(v: BackupStatus): String = v.name
    @TypeConverter
    fun toBackup(v: String): BackupStatus = BackupStatus.valueOf(v)
}
