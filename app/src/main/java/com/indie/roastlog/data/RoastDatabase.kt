package com.indie.roastlog.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.indie.roastlog.ui.model.IntervalData
import com.indie.roastlog.ui.model.RoastingEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "roast_sessions")
data class RoastSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val beanType: String,
    val waterContent: String,
    val density: String,
    val weightIn: String,
    val weightOut: String,
    val roastType: String,
    val intervalSeconds: Int = 60,
    val targetDuration: Int = 0,
    // Results
    val endTimeTemp: String,
    val roastTime: String,
    val devTime: String,
    // Events (Stored as JSON Strings)
    val turnPoint: RoastingEvent?,
    val yellowing: RoastingEvent?,
    val firstCrack: RoastingEvent?,
    val endRoast: RoastingEvent?,
    val burnerPlan: List<RoastingEvent>,
    val temperatureData: List<IntervalData>
)

class Converters {
    @TypeConverter
    fun fromRoastingEvent(value: RoastingEvent?): String? = value?.let { Json.encodeToString(it) }
    
    @TypeConverter
    fun toRoastingEvent(value: String?): RoastingEvent? = value?.let { Json.decodeFromString(it) }

    @TypeConverter
    fun fromEventList(value: List<RoastingEvent>): String = Json.encodeToString(value)

    @TypeConverter
    fun toEventList(value: String): List<RoastingEvent> = Json.decodeFromString(value)

    @TypeConverter
    fun fromIntervalList(value: List<IntervalData>): String = Json.encodeToString(value)

    @TypeConverter
    fun toIntervalList(value: String): List<IntervalData> = Json.decodeFromString(value)
}

@Dao
interface RoastDao {
    @Insert
    suspend fun insertSession(session: RoastSessionEntity)

    @Delete
    suspend fun deleteSession(session: RoastSessionEntity)

    @Query("SELECT * FROM roast_sessions ORDER BY date DESC")
    fun getAllSessions(): kotlinx.coroutines.flow.Flow<List<RoastSessionEntity>>

    @Query("SELECT * FROM roast_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): RoastSessionEntity?
}

@Database(entities = [RoastSessionEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RoastDatabase : RoomDatabase() {
    abstract fun roastDao(): RoastDao

    companion object {
        @Volatile private var instance: RoastDatabase? = null
        fun getDatabase(context: Context): RoastDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, RoastDatabase::class.java, "roast_db")
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
