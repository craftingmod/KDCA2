package lab.unicomp.kdca.common.data

/**
 * 변경 가능한 센서 데이터
 */
data class MutableWearSensorData(
  var accX: Float,
  var accY: Float,
  var accZ: Float,
  var gyroX: Float,
  var gyroY: Float,
  var gyroZ: Float,
  var pressure: Float,
  var heartRate: Int,
  var timestamp: Long,
) {
  /**
   * 변경 불가능한 데이터로 변경
   */
  fun toImmutable():WearSensorData {
    return WearSensorData(
      accX,
      accY,
      accZ,
      gyroX,
      gyroY,
      gyroZ,
      pressure,
      heartRate,
      timestamp
    )
  }
}