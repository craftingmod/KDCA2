package lab.unicomp.kdca

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lab.unicomp.kdca.common.BaseSensorService
import lab.unicomp.kdca.common.data.MutableWearSensorData
import lab.unicomp.kdca.common.data.SensorDatabase
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.round

class SensorService : BaseSensorService() {

  // static 멤버들
  companion object {
    private const val TAG = "SensorService"
    private const val testSocket = false
    fun startService(context:Context) {
      val intent = Intent(context, SensorService::class.java)
      ContextCompat.startForegroundService(context, intent)
    }
  }

  override val sensorTypes: List<Int> = listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_PRESSURE, Sensor.TYPE_HEART_RATE)
  override val sensorHz: List<Float> = listOf(10F, 10F, 10F, 10F)

  // 센서 데이터들 캐시
  private var sensorCache: MutableWearSensorData = MutableWearSensorData(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0, 0L)
  // 센서 데이터들 측정했는지 체크 (acc, gyro, pressure, heartRate)
  private var sensorObserved = arrayOf(false, false, false, false)
  // 가장 마지막으로 센서 값 넣었을 때
  private var lastSynced: Long = 0L

  override fun registerPushWorker() {
    // WorkManager 등록 (15분마다 시계와 동기화)
    val syncWorkRequest = PeriodicWorkRequest.Builder(SyncWorker::class.java, 15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync_worker", ExistingPeriodicWorkPolicy.REPLACE, syncWorkRequest)
  }

  /**
   * 센서 값이 바뀌었을 때
   */
  override fun onSensorValueChanged(type: Int, values: Triple<Float, Float, Float>) {
    when (type) {
      // 가속도 센서
      Sensor.TYPE_ACCELEROMETER -> {
        sensorCache.accX = values.first
        sensorCache.accY = values.second
        sensorCache.accZ = values.third
        sensorObserved[0] = true
        emitValue()
      }
      // 자이로 센서
      Sensor.TYPE_GYROSCOPE -> {
        sensorCache.gyroX = values.first
        sensorCache.gyroY = values.second
        sensorCache.gyroZ = values.third
        sensorObserved[1] = true
      }
      // 압력 센서
      Sensor.TYPE_PRESSURE -> {
        sensorCache.pressure = values.first
        sensorObserved[2] = true
      }
      // 심박수 센서
      Sensor.TYPE_HEART_RATE -> {
        sensorCache.heartRate = values.first.toInt()
        sensorObserved[3] = true
      }
      // 아닐경우 return
      else -> return
    }
  }

  /**
   * Value 저장
   */
  private fun emitValue() {
    val currentTime = System.currentTimeMillis()
    // 센서 값을 다 받아왔는 지 검사
    if (sensorObserved.contains(false)) {
      // 하나라도 false 가 있으면 return
      return
    }
    // 시간 정보 넣기
    sensorCache.timestamp = currentTime
    // 데이터베이스에 넣기
    CoroutineScope(Dispatchers.IO).launch {
      val dao = SensorDatabase.getInstance(applicationContext).wearDao()
      dao.insertData(sensorCache.toImmutable())
      Log.d(TAG, "[Sensor] Sensor data pushed: $sensorCache")
    }

    if (testSocket && (currentTime - lastSynced) >= 15000) {
      // 15초 sync 테스트
      val testSyncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
      WorkManager.getInstance(this).enqueue(testSyncWorkRequest)
      lastSynced = currentTime
    }

  }

}