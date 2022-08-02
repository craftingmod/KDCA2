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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import lab.unicomp.kdca.common.data.SensorDatabase
import lab.unicomp.kdca.common.data.WearSensorData
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.abs

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
  private lateinit var prefStore:DataStoreModule

  override fun onCreate() {
    super.onCreate()
    prefStore = DataStoreModule(applicationContext)
  }


  override fun onDataChanged(dataEvents: DataEventBuffer) {
    super.onDataChanged(dataEvents)
    Log.d(TAG, "onDataChanged called!!")
    val currentTime = System.currentTimeMillis()
    // 이벤트마다 루프
    for (dataEvent in dataEvents) {
      val uri = dataEvent.dataItem.uri // URI
      Log.d(TAG, "URI Path: ${uri.path}")
      Log.d(TAG, "Event type: ${dataEvent.type}")
      when (uri.path) {
        SENSOR_PATH -> {
          if (dataEvent.type == DataEvent.TYPE_CHANGED) {
            val assetData = DataMapItem.fromDataItem(dataEvent.dataItem)
              .dataMap.getAsset(SENSOR_KEY)!!
            val assetInputStream:InputStream? = Tasks.await(Wearable.getDataClient(applicationContext)
              .getFdForAsset(assetData))?.inputStream
            scope.launch {
              Log.d(TAG, "InputStream: ${assetInputStream}")
              // 가장 마지막 동기화 불러오기
              val lastSyncTime = prefStore.lastSync.first()
              // 센서 값 받아오기 (어셋에서)
              if (assetInputStream != null) {
                // 데이터가 있으면
                // 바이트 전부 읽어오기
                val sensorBytes = assetInputStream.readBytes()
                val sensorData = arrayListOf<WearSensorData>()
                for (i in sensorBytes.indices step 40) {
                  // 40바이트 읽어 데이터 추가하기
                  sensorData.add(WearSensorData(
                    accX = byteArrayToFloat(sensorBytes, i + 0),
                    accY = byteArrayToFloat(sensorBytes, i + 4),
                    accZ = byteArrayToFloat(sensorBytes, i + 8),
                    gyroX = byteArrayToFloat(sensorBytes, i + 12),
                    gyroY = byteArrayToFloat(sensorBytes, i + 16),
                    gyroZ = byteArrayToFloat(sensorBytes, i + 20),
                    pressure = byteArrayToFloat(sensorBytes, i + 24),
                    heartRate = byteArrayToInt(sensorBytes, i + 28),
                    timestamp = byteArrayToLong(sensorBytes, i + 32),
                  ))
                }
                Log.d(TAG, "sensorData Length: ${sensorData.size}")
                // 모두 추가하기
                val dao = SensorDatabase.getInstance(applicationContext).wearDao()
                dao.insertData(*sensorData.toTypedArray())
                Log.d(TAG, "sensorData[0]: ${sensorData.firstOrNull()}")
                // 간격 확인
                // 1000 * 60 * 14 + 1000 * 30
                Log.d(TAG, "lastSyncTime: $lastSyncTime / $currentTime")
                if (abs(currentTime - lastSyncTime) >= 1000 * 60 * 14) {
                  Log.d(TAG, "test Worker")
                  // Work 실행
                  WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "MobiusUploadWorker",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(MobiusSyncWorker::class.java).build()
                  )
                  prefStore.setLastSync(currentTime)
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

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
  }

  private fun byteArrayToFloat(arr:ByteArray, offset:Int): Float {
    return Float.fromBits(byteArrayToInt(arr, offset))
  }

  private fun byteArrayToInt(arr:ByteArray, offset:Int): Int {
    return ByteBuffer.wrap(arr.sliceArray(offset until offset+4)).int
  }

  private fun byteArrayToLong(arr:ByteArray, offset:Int): Long {
    return ByteBuffer.wrap(arr.sliceArray(offset until offset+8)).long
  }
}