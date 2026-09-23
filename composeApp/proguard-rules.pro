# Hilt
-keep class androidx.hilt.** { *; }
-keep class com.apagon.rhythm.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-keep interface * extends androidx.room.Dao

# Keep Data Models (Prevents issues with Room mapping)
-keep class com.apagon.rhythm.data.model.** { *; }

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep specific activities that might be called by Intent (Full Screen Intents)
-keep class com.apagon.rhythm.ui.alarms.TimerAlertActivity { *; }
-keep class com.apagon.rhythm.ui.alarms.AlarmAlertActivity { *; }
