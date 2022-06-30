package lab.unicomp.kdca.common.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 폰 센서 종합 데이터
 */
@Entity(tableName = "phone_sensor_data")
data class PhoneSensorData(
  @ColumnInfo(name = "acc_x") val accX: Float,
  @ColumnInfo(name = "acc_y") val accY: Float,
  @ColumnInfo(name = "acc_z") val accZ: Float,
  @ColumnInfo(name = "gyro_x") val gyroX: Float,
  @ColumnInfo(name = "gyro_y") val gyroY: Float,
  @ColumnInfo(name = "gyro_z") val gyroZ: Float,
  @ColumnInfo(name = "pressure") val pressure: Float,
  @ColumnInfo(name = "light") val light: Float,
  @ColumnInfo(name = "timestamp") val timestamp: Long,
) {
  @PrimaryKey(autoGenerate = true) var id: Int = 0
}
