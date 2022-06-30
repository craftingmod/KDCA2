package lab.unicomp.kdca

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import lab.unicomp.kdca.databinding.ActivityMainBinding

class MainActivity : Activity() {

  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
      AlertDialog.Builder(this)
        .setTitle(R.string.perm_noti_title)
        .setMessage(R.string.perm_noti_description)
        .setPositiveButton(R.string.ok) { _, _ ->
          ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 13)
        }
        .setNegativeButton(R.string.exit) { _, _ ->
          finish()
        }
        .show()
    }
  }

  override fun onResume() {
    super.onResume()
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
      startService(Intent(this, SensorService::class.java))
    }
  }
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      13 -> {
        // 결과창
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // 권한 허가
          return
        } else {
          // 권한 거부
          finish()
        }
      }
      else -> {
        // 이상한 응답
      }
    }
  }
}