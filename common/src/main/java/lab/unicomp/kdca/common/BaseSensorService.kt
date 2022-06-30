package lab.unicomp.kdca.common

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.round

abstract class BaseSensorService : Service(), SensorEventListener {
  companion object {
    private const val TAG = "BaseSensorService"
  }
  // 수집할 센서 목록
  protected abstract val sensorTypes: List<Int>
  // 센서 Hz
  protected abstract val sensorHz: List<Float>
  // Worker 등록 함수
  protected open fun registerPushWorker() {}
  // 센서 값 변경 메소드
  protected abstract fun onSensorValueChanged(type:Int, values: Triple<Float, Float, Float>)

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    // BODY_SENSORS 권한 체크
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
      != PackageManager.PERMISSION_GRANTED) {
      // 권한이 없으면 알림 생성
      // 알림 매니저
      val notiManager = NotificationManagerCompat.from(this)
      // 채널 생성
      val channelId = "kdca_request_sensor_permission"
      val channel =
        NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_HIGH)
          .setName(getString(R.string.perm_channel_title))
          .build()
      notiManager.createNotificationChannel(channel)
      // 권한 해달라는 알림
      val builder = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_check)
        .setContentTitle(getString(R.string.collect_noti_title))
        .setContentText(getString(R.string.collect_noti_description))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ERROR)
        // 누르면 PermActivity 실행
        .setContentIntent(
          PendingIntent.getActivity(
            this,
            0,
            Intent(this, PermActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
          )
        )
      notiManager.notify(3013, builder.build())
      // 로그 찍고 종료
      Log.e(TAG, "Permission denied")
      stopSelf()
      return
    }
    // 권한 있으니 센서 등록
    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // for i in range(0, len(sensorTypes))랑 같은 것
    for (i in sensorTypes.indices) {
      // 위의 센서들을 등록시키기
      val sensor = sensorManager.getDefaultSensor(sensorTypes[i])
      // 원하는 Hz로 가져오기
      sensorManager.registerListener(this, sensor, (1000000 / sensorHz[i]).toInt())
    }

    // WorkManager 등록
    registerPushWorker()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // 포어그라운드 채널 생성
    val channelId = "kdca_sensor_service"
    val channel = NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW)
      .setName(getString(R.string.collect_channel_title))
      .setDescription(getString(R.string.collect_channel_description))
      .build()
    NotificationManagerCompat.from(this).createNotificationChannel(channel)
    // 포어그라운드 알림 만들기
    val builder = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.drawable.ic_run)
      .setContentTitle(getString(R.string.collect_noti_title))
      .setContentText(getString(R.string.collect_noti_description))
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)

    // 실행
    startForeground(3012, builder.build())

    return super.onStartCommand(intent, flags, startId)
  }

  /**
   * 센서 변경 이벤트
   */
  override fun onSensorChanged(sensorEvent: SensorEvent) {
    when (val type = sensorEvent.sensor.type) {
      // 가속도, 자이로 센서일 때
      Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE -> {
        // X,Y,Z 값 받아오기
        val X = roundValue(sensorEvent.values[0], 1000)
        val Y = roundValue(sensorEvent.values[1], 1000)
        val Z = roundValue(sensorEvent.values[2], 1000)
        // 콜백 호출
        onSensorValueChanged(type, Triple(X, Y, Z))
      }
      // 압력, 심박 센서일 때
      Sensor.TYPE_PRESSURE, Sensor.TYPE_HEART_RATE, Sensor.TYPE_LIGHT -> {
        // 압력값 받아오기
        val value = roundValue(sensorEvent.values[0], 100)
        // 콜백 호출
        onSensorValueChanged(type, Triple(value, 0F, 0F))
      }
      // 나머지
      else -> {
        Log.e(TAG, "Unknown Sensor! Id: $type")
      }
    }
  }

  /**
   * 생략
   */
  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
  }

  /**
   * 서비스가 꺼질 때
   */
  override fun onDestroy() {
    // 센서 끄기
    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    sensorManager.unregisterListener(this)
    super.onDestroy()
  }

  /**
   * 자릿수 버림
   */
  private fun roundValue(value:Float, division:Int):Float {
    return round(value * division) / division
  }

}