package lab.unicomp.kdca

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lab.unicomp.kdca.common.data.SensorDatabase
import lab.unicomp.kdca.common.data.WearSensorData
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * https://github.com/android/wear-os-samples/blob/master/DataLayer/Wearable/src/main/java/com/example/android/wearable/datalayer/DataLayerListenerService.kt
 */
class WearListenerService : WearableListenerService() {

  companion object {
    private const val TAG = "WearListenerService"

    private const val START_ACTIVITY_PATH = "/start-activity"
    private const val DATA_ITEM_RECEIVED_PATH = "/data-item-received"
    const val COUNT_PATH = "/count"
    const val SENSOR_PATH = "/sensor-data"
    const val SENSOR_KEY = "data"
  }

  // message client 할당
  private val messageClient by lazy { Wearable.getMessageClient(this) }
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


  override fun onDataChanged(dataEvents: DataEventBuffer) {
    super.onDataChanged(dataEvents)
    // 가장 마지막 동기화 불러오기
    val currentTime = System.currentTimeMillis()
    val syncConf = applicationContext.getSharedPreferences("sync_conf", Context.MODE_PRIVATE)
    val lastSyncTime = syncConf.getLong("last_sync_time", 0L)
    // 이벤트마다 루프
    for (dataEvent in dataEvents) {
      val uri = dataEvent.dataItem.uri // URI
      when (uri.path) {
        SENSOR_PATH -> {
          if (dataEvent.type == DataEvent.TYPE_CHANGED) {
            val assetData = DataMapItem.fromDataItem(dataEvent.dataItem)
              .dataMap.getAsset(SENSOR_KEY)!!
            val assetInputStream:InputStream? = Tasks.await(Wearable.getDataClient(applicationContext)
              .getFdForAsset(assetData))?.inputStream
            scope.launch {
              // 센서 값 받아오기 (어셋에서)
              if (assetInputStream != null) {
                // 데이터가 있으면
                // 바이트 전부 읽어오기
                val sensorBytes = assetInputStream.readBytes()
                val sensorData = arrayListOf<WearSensorData>()
                for (i in sensorBytes.indices step 40) {
                  // 40바이트 읽어오기
                  val buffer = ByteBuffer.wrap(sensorBytes, i, 40)
                  // 데이터 추가하기
                  sensorData.add(WearSensorData(
                    accX = buffer.getFloat(0),
                    accY = buffer.getFloat(4),
                    accZ = buffer.getFloat(8),
                    gyroX = buffer.getFloat(12),
                    gyroY = buffer.getFloat(16),
                    gyroZ = buffer.getFloat(20),
                    pressure = buffer.getFloat(24),
                    heartRate = buffer.getInt(28),
                    timestamp = buffer.getLong(32),
                  ))
                }
                Log.d(TAG, "sensorData Length: ${sensorData.size}")
                // 모두 추가하기
                val dao = SensorDatabase.getInstance(applicationContext).wearDao()
                dao.insertData(*sensorData.toTypedArray())
                Log.d(TAG, "sensorData[0]: ${sensorData.firstOrNull()}")
                // 간격 확인
                if (currentTime - lastSyncTime >= 1000 * 60 * 14) {
                  syncConf.edit().putLong("last_sync_time", currentTime).apply()
                  // Work 실행
                  WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "MobiusSyncWorker-Wear", ExistingWorkPolicy.APPEND,
                    OneTimeWorkRequest.Builder(MobiusSyncWorker::class.java).build()
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  override fun onMessageReceived(messageEvent: MessageEvent) {
    super.onMessageReceived(messageEvent)

    when (messageEvent.path) {
      START_ACTIVITY_PATH -> {
        startActivity(
          Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
      }
    }
  }
}