package lab.unicomp.kdca.common.data

import androidx.room.*

/**
 * 워치 센서 데이터 Dao
 */
@Dao
interface WearSensorDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertData(vararg data: WearSensorData)

  @Delete
  fun deleteData(vararg data: WearSensorData)

  @Query("SELECT * FROM wear_sensor_data")
  fun getAll(): List<WearSensorData>

  @Query("SELECT * FROM wear_sensor_data WHERE timestamp > :timestamp")
  fun loadAllDataAfter(timestamp: Long): List<WearSensorData>

  @Query("SELECT * FROM wear_sensor_data WHERE timestamp < :timestamp")
  fun loadAllDataBefore(timestamp: Long): List<WearSensorData>

  @Query("DELETE FROM wear_sensor_data WHERE timestamp < :timestamp")
  fun deleteDataBefore(timestamp: Long): Unit

  @Query("SELECT * FROM wear_sensor_data WHERE timestamp > :startTime AND timestamp <= :endTime")
  fun loadDataBetween(startTime: Long, endTime: Long): List<WearSensorData>

  @Query("SELECT * FROM wear_sensor_data ORDER BY timestamp ASC LIMIT 1")
  fun loadFirst(): List<WearSensorData>
}