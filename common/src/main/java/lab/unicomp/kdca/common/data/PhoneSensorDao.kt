package lab.unicomp.kdca.common.data

import androidx.room.*

/**
 * 폰 센서 데이터 Dao
 */
@Dao
interface PhoneSensorDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertData(vararg data: PhoneSensorData)

  @Delete
  fun deleteData(vararg data: PhoneSensorData)

  @Query("SELECT * FROM phone_sensor_data")
  fun getAll(): List<PhoneSensorData>

  @Query("SELECT * FROM phone_sensor_data WHERE timestamp > :timestamp")
  fun loadAllDataAfter(timestamp: Long): List<PhoneSensorData>

  @Query("SELECT * FROM phone_sensor_data WHERE timestamp < :timestamp")
  fun loadAllDataBefore(timestamp: Long): List<PhoneSensorData>

  @Query("DELETE FROM phone_sensor_data WHERE timestamp < :timestamp")
  fun deleteDataBefore(timestamp: Long): Unit

  @Query("SELECT * FROM phone_sensor_data WHERE timestamp > :startTime AND timestamp <= :endTime")
  fun loadDataBetween(startTime: Long, endTime: Long): List<PhoneSensorData>

  @Query("SELECT * FROM phone_sensor_data WHERE timestamp > 1000000000000 ORDER BY timestamp ASC LIMIT 1")
  fun loadFirst(): List<PhoneSensorData>
}