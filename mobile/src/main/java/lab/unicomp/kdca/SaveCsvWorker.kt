package lab.unicomp.kdca

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lab.unicomp.kdca.common.data.SensorDatabase
import lab.unicomp.kdca.common.data.WearSensorData

class SaveCsvWorker(appContext: Context, workerParam: WorkerParameters) : CoroutineWorker(appContext, workerParam) {

  override suspend fun doWork(): Result {
    val uri = inputData.getString("file_uri") ?: return Result.failure()
    val isWear = inputData.getBoolean("is_wear", false)
    setForeground(createForegroundInfo())
    return withContext(Dispatchers.IO) {
      val database = SensorDatabase.getInstance(applicationContext)
      applicationContext.contentResolver.openOutputStream(Uri.parse(uri), "w")?.use { os ->
        os.bufferedWriter().use { writer ->
          if (isWear) {
            writer.write(arrayOf("acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z", "pressure", "heart_rate", "timestamp").joinToString(","))
            writer.newLine()
            val data = database.wearDao().getAll()
            for (d in data) {
              writer.write(arrayOf(d.accX, d.accY, d.accZ, d.gyroX, d.gyroY, d.gyroZ, d.pressure, d.heartRate, d.timestamp).joinToString(","))
              writer.newLine()
            }
          } else {
            writer.write(arrayOf("acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z", "pressure", "light", "timestamp").joinToString(","))
            writer.newLine()
            val data = database.phoneDao().getAll()
            for (d in data) {
              writer.write(arrayOf(d.accX, d.accY, d.accZ, d.gyroX, d.gyroY, d.gyroZ, d.pressure, d.light, d.timestamp).joinToString(","))
              writer.newLine()
            }
          }
        }
      }
      Result.success()
    }
  }

  private fun createForegroundInfo(): ForegroundInfo {
    val id = applicationContext.getString(R.string.worker_channel)
    val title = applicationContext.getString(R.string.worker_title)
    val desc = applicationContext.getString(R.string.worker_description)
    // This PendingIntent can be used to cancel the worker
    val intent = WorkManager.getInstance(applicationContext)
      .createCancelPendingIntent(getId())

    // Create a Notification channel if necessary
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_LOW)
        .setName(applicationContext.getString(R.string.worker_title))
        .build()
      NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(applicationContext, id)
      .setContentTitle(title)
      .setTicker(title)
      .setContentText(desc)
      .setSmallIcon(lab.unicomp.kdca.common.R.drawable.ic_run)
      .setOngoing(true)
      .build()

    return ForegroundInfo(1430,notification)
  }

}