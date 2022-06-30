package lab.unicomp.kdca.common.data

/**
 * 변경 가능한 센서 데이터
 */
data class MutablePhoneSensorData(
  var accX: Float,
  var accY: Float,
  var accZ: Float,
  var gyroX: Float,
  var gyroY: Float,
  var gyroZ: Float,
  var pressure: Float,
  var light: Float,
  var timestamp: Long,
) {
  /**
   * 변경 불가능한 데이터로 변경
   */
  fun toImmutable():PhoneSensorData {
    return PhoneSensorData(
      accX,
      accY,
      accZ,
      gyroX,
      gyroY,
      gyroZ,
      pressure,
      light,
      timestamp,
    )
  }
}