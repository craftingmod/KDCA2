package lab.unicomp.kdca

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import lab.unicomp.kdca.common.data.SensorDatabase
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SyncWorker(appContext: Context, workerParam: WorkerParameters) : CoroutineWorker(appContext, workerParam) {
  override suspend fun doWork(): Result {
    val context = applicationContext
    // IO 코루틴에서 실행
    return withContext(Dispatchers.IO) {
      Log.d("SyncWorker", "SyncWorker is working")
      val currentTime = System.currentTimeMillis()
      val database = SensorDatabase.getInstance(context)
      // 데이터베이스에서 보낼 데이터 만들기 (시간순 정렬)
      val sensorValues = database.wearDao().loadAllDataBefore(currentTime).sortedBy {
        it.timestamp
      }
      val byteData = ByteArray(sensorValues.size * 40)
      Log.d("SyncWorker", "SensorData size: ${sensorValues.size}, Bytearray Size: ${byteData.size}")
      for (i in sensorValues.indices) {
        val sensorValue = sensorValues[i]
        // 차례대로 Byte에 쓰기
        // 4바이트 float들 쓰기
        val floats = sensorValue.let {
          arrayOf(it.accX, it.accY, it.accZ, it.gyroX, it.gyroY, it.gyroZ, it.pressure)
        }
        for (k in floats.indices) {
          writeIntToByteArray(byteData, i * 40 + k * 4, floats[k].toRawBits())
        }
        // 4바이트 int 쓰기
        writeIntToByteArray(byteData, i * 40 + 28, sensorValue.heartRate)
        // 8바이트 Long 쓰기
        writeLongToByteArray(byteData, i * 40 + 32, sensorValue.timestamp)
      }
      val asset = Asset.createFromBytes(byteData)
      // Request 데이터 만들기
      val request = PutDataMapRequest.create("/sensor-data").run {
        dataMap.putAsset("data", asset)
        asPutDataRequest()
      }
      // 콜백->코루틴화 (코루틴 Return)
      // https://stackoverflow.com/questions/48552925/existing-3-function-callback-to-kotlin-coroutines
      val result = suspendCoroutine { cont ->
        val putTask = Wearable.getDataClient(context).putDataItem(request)
        putTask.addOnCompleteListener {
          // 성공하면 끝
          if (it.isSuccessful) {
            cont.resume(Result.success())
          } else {
            cont.resume(Result.failure())
          }
        }
        // 실패하면 다시 시도
        putTask.addOnCanceledListener {
          cont.resume(Result.retry())
        }
        putTask.addOnFailureListener {
          cont.resume(Result.retry())
        }
      }
      if (result == Result.success()) {
        database.wearDao().deleteDataBefore(currentTime)
      }
      result
    }
  }
  private fun writeIntToByteArray(byteArray: ByteArray, offset:Int, num:Int) {
    byteArray[offset] = (num shr 24).toByte()
    byteArray[offset + 1] = (num shr 16).toByte()
    byteArray[offset + 2] = (num shr 8).toByte()
    byteArray[offset + 3] = num.toByte()
  }
  private fun writeLongToByteArray(byteArray: ByteArray, offset:Int, num:Long) {
    byteArray[offset] = (num shr 56).toByte()
    byteArray[offset + 1] = (num shr 48).toByte()
    byteArray[offset + 2] = (num shr 40).toByte()
    byteArray[offset + 3] = (num shr 32).toByte()
    byteArray[offset + 4] = (num shr 24).toByte()
    byteArray[offset + 5] = (num shr 16).toByte()
    byteArray[offset + 6] = (num shr 8).toByte()
    byteArray[offset + 7] = num.toByte()
  }
}