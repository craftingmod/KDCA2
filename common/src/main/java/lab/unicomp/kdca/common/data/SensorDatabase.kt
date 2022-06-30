package lab.unicomp.kdca.common.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import lab.unicomp.kdca.common.data.PhoneSensorDao
import lab.unicomp.kdca.common.data.PhoneSensorData
import lab.unicomp.kdca.common.data.WearSensorDao
import lab.unicomp.kdca.common.data.WearSensorData

@Database(entities = [PhoneSensorData::class, WearSensorData::class], version = 7)
abstract class SensorDatabase : RoomDatabase() {
  /**
   * https://stackoverflow.com/questions/40398072/singleton-with-parameter-in-kotlin
   */
  companion object {
    @Volatile private var instance: SensorDatabase? = null

    fun getInstance(context: Context): SensorDatabase = instance ?: synchronized(this) {
      instance ?: buildDatabase(context).also { instance = it }
    }

    private fun buildDatabase(context: Context) =
      Room.databaseBuilder(context.applicationContext, SensorDatabase::class.java, "sensor_data.db")
        .fallbackToDestructiveMigration()
        .build()
  }

  /**
   * 폰 센서 데이터베이스
   */
  abstract fun phoneDao(): PhoneSensorDao
  /**
   * 웨어 센서 데이터베이스
   */
  abstract fun wearDao(): WearSensorDao
}