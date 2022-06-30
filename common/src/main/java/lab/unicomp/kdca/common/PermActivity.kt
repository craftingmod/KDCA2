package lab.unicomp.kdca.common

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
      // 요청
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 13)
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
          finish()
          return
        } else {
          // 권한 거부
          finish()
          return
        }
      }
      else -> {
        // 이상한 응답
      }
    }
  }
}