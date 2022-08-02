package lab.unicomp.kdca

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.kittinunf.fuel.Fuel
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import lab.unicomp.kdca.common.data.SensorDatabase
import lab.unicomp.kdca.onem2m.Thyme
import java.math.BigDecimal
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min


class MobiusSyncWorker(appContext: Context, workerParam: WorkerParameters) : CoroutineWorker(appContext, workerParam) {
  override suspend fun doWork(): Result {
    val context = applicationContext
    val prefStore = DataStoreModule(context)
    val sid = prefStore.sid.first()
    val phoneLastSent = prefStore.phoneLastSent.first()
    val wearLastSent = prefStore.wearLastSent.first()
    if (sid.isEmpty()) {
      return Result.failure()
    }
    // IO 코루틴에서 실행
    return withContext(Dispatchers.IO) {
      Log.d("MobiusSyncWorker", "MobiusSyncWorker is working")
      val currentTime = System.currentTimeMillis()
      val database = SensorDatabase.getInstance(context)
      // 모비우스에서 불러오기
      val thyme = Thyme(
        address = "http://203.253.128.161:7579",
        m2mResourceId = "12345",
      )
      val ubiTest = thyme.getCommonServiceEntityInfo("Mobius")?.let { cseBase ->
        thyme.ThymeCSE(cseBase).getThymeAE("S$sid")
      }
      val phoneMobius = ubiTest?.getThymeCNT("phone")
      val watchMobius = ubiTest?.getThymeCNT("watch")
      if (phoneMobius == null || watchMobius == null) {
        Log.e("MobiusSyncWorker", "Mobius isn't responding. fail.")
        return@withContext Result.retry()
      }
      val durationSplit = 15 * 60 * 1000
      // 일단 폰부터
      val phoneAcc = phoneMobius.getThymeCNT("mAcc") ?: return@withContext Result.retry()
      val phoneGyro = phoneMobius.getThymeCNT("mGyr") ?: return@withContext Result.retry()
      val phonePressure = phoneMobius.getThymeCNT("mPre") ?: return@withContext Result.retry()
      val phoneLight = phoneMobius.getThymeCNT("mLi") ?: return@withContext Result.retry()
      // val phoneTotal = phoneMobius.getThymeCNT("mTotal") ?: return@withContext Result.retry()
      // val phoneTimeI = phoneMobius.getThymeCNT("Time") ?: return@withContext Result.retry()
      // Log.d("MobiusSyncWorker", "first Item: ${database.phoneDao().getAll().firstOrNull()}")
      var phoneLastTime = if (phoneLastSent > 0) phoneLastSent else database.phoneDao().loadFirst().firstOrNull()?.timestamp ?: return@withContext Result.retry()
      // var phoneLastTime = phoneTimeI.getLatestContent()?.contentValue?.toLong() ?: database.phoneDao().loadFirst().firstOrNull()?.timestamp ?: return@withContext Result.retry()
      // 15분마다 자르기
      while (phoneLastTime < currentTime) {
        if (phoneLastTime + durationSplit > currentTime) {
          break
        }
        Log.d("MobiusSyncWorker", "[Phone] Uploading between ${
          Instant.fromEpochMilliseconds(phoneLastTime)} and ${Instant.fromEpochMilliseconds(phoneLastTime + durationSplit)}")
        // 사이 15분 데이터 수집
        val betweenData = database.phoneDao().loadDataBetween(phoneLastTime, phoneLastTime + durationSplit).sortedBy { it.timestamp }
        if (betweenData.isEmpty()) {
          phoneLastTime = min(currentTime, phoneLastTime + durationSplit)
          continue
        }
        // 넣을 데이터 푸싱
        val timestamp = betweenData.first().timestamp
        val accData = mergeTimestamp(timestamp, betweenData.joinToString(",") {
          "[" + arrayOf(it.accX, it.accY, it.accZ).joinToString(",") { n -> roundFloat(n, 3) } + "]"
        }.let {"[$it]"})
        val gyroData = mergeTimestamp(timestamp, betweenData.joinToString(",") {
          "[" + arrayOf(it.gyroX, it.gyroY, it.gyroZ).joinToString(",") { n -> roundFloat(n, 3) } + "]"
        }.let {"[$it]"})
        val pressureData = mergeTimestamp(timestamp, betweenData.joinToString(",") { roundFloat(it.pressure, 2) }.let {"[$it]"})
        val lightData = mergeTimestamp(timestamp, betweenData.joinToString(",") { it.light.toString() }.let {"[$it]"})
        /*
        val totalData = betweenData.joinToString(",") {
          arrayOf(it.accX, it.accY, it.accZ, it.gyroX, it.gyroY, it.gyroZ, it.pressure, it.light, it.timestamp).joinToString(",").let { t -> "[$t]"}
        }.let {it -> "[$it]"}
         */
        // 전부 넣기
        phoneAcc.putContent(accData)
        phoneGyro.putContent(gyroData)
        phonePressure.putContent(pressureData)
        phoneLight.putContent(lightData)
        // phoneTotal.putContent(totalData)
        // phoneTimeI.putContent(betweenData.last().timestamp.toString())
        prefStore.setPhoneLastSent(betweenData.last().timestamp)
        // 다음 루프
        phoneLastTime = min(currentTime, phoneLastTime + durationSplit)
      }
      // 그 다음 워치
      val watchAcc = watchMobius.getThymeCNT("wAcc") ?: return@withContext Result.retry()
      val watchGyro = watchMobius.getThymeCNT("wGyr") ?: return@withContext Result.retry()
      val watchPressure = watchMobius.getThymeCNT("wPre") ?: return@withContext Result.retry()
      val watchHeartrate = watchMobius.getThymeCNT("wHR") ?: return@withContext Result.retry()
      // val watchTotal = watchMobius.getThymeCNT("wTotal") ?: return@withContext Result.retry()
      // val watchTimeI = watchMobius.getThymeCNT("Time") ?: return@withContext Result.retry()
      // var watchLastTime = watchTimeI.getLatestContent()?.contentValue?.toLong() ?: database.wearDao().loadFirst().firstOrNull()?.timestamp ?: return@withContext Result.retry()
      var watchLastTime = if (wearLastSent > 0) wearLastSent else database.wearDao().loadFirst().firstOrNull()?.timestamp ?: return@withContext Result.retry()
      // 15분마다 자르기
      while (watchLastTime < currentTime) {
        if (watchLastTime + durationSplit > currentTime) {
          break
        }
        Log.d("MobiusSyncWorker", "[Watch] Uploading between ${
          Instant.fromEpochMilliseconds(watchLastTime)} and ${Instant.fromEpochMilliseconds(watchLastTime + durationSplit)}")
        // 사이 15분 데이터 수집
        val betweenData = database.wearDao().loadDataBetween(watchLastTime, watchLastTime + durationSplit).sortedBy { it.timestamp }
        if (betweenData.isEmpty()) {
          watchLastTime = min(currentTime, watchLastTime + durationSplit)
          continue
        }
        // 넣을 데이터 푸싱
        val timestamp = betweenData.first().timestamp
        val accData = mergeTimestamp(timestamp, betweenData.joinToString(",") {
          "[" + arrayOf(it.accX, it.accY, it.accZ).joinToString(",") { n -> roundFloat(n, 3) } + "]"
        }.let {"[$it]"})
        val gyroData = mergeTimestamp(timestamp, betweenData.joinToString(",") {
          "[" + arrayOf(it.gyroX, it.gyroY, it.gyroZ).joinToString(",") { n -> roundFloat(n, 3) } + "]"
        }.let {"[$it]"})
        val pressureData = mergeTimestamp(timestamp, betweenData.joinToString(",") { roundFloat(it.pressure, 2) }.let {"[$it]"})
        val heartRateData = mergeTimestamp(timestamp, betweenData.joinToString(",") { it.heartRate.toString() }.let {"[$it]"})
        /*
        val totalData = betweenData.joinToString(",") {
          arrayOf(it.accX, it.accY, it.accZ, it.gyroX, it.gyroY, it.gyroZ, it.pressure, it.heartRate, it.timestamp).joinToString(",").let { t -> "[$t]"}
        }.let {"[$it]"}
         */
        // 전부 넣기
        watchAcc.putContent(accData)
        watchGyro.putContent(gyroData)
        watchPressure.putContent(pressureData)
        watchHeartrate.putContent(heartRateData)
        // watchTotal.putContent(totalData)
        // watchTimeI.putContent(betweenData.last().timestamp.toString())
        prefStore.setWearLastSent(betweenData.last().timestamp)
        // 다음 루프
        watchLastTime = min(currentTime, watchLastTime + durationSplit)
      }
      Result.success()
    }
  }

  private fun mergeTimestamp(timestamp:Long, data:String): String {
    return "{\"t\":$timestamp,\"v\":$data}"
  }

  private fun roundFloat(float: Float, degree: Int = 2): String {
    return BigDecimal(float.toDouble()).setScale(degree, BigDecimal.ROUND_HALF_EVEN).toPlainString()
  }
}