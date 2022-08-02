package lab.unicomp.kdca

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      ContextCompat.startForegroundService(context, Intent(
        context, SensorService::class.java
      ))
    }
  }

}