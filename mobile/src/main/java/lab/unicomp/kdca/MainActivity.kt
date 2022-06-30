package lab.unicomp.kdca

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.callback.FilePickerCallback
import com.anggrayudi.storage.callback.StorageAccessCallback
import com.anggrayudi.storage.file.openOutputStream
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lab.unicomp.kdca.onem2m.Thyme

class MainActivity : AppCompatActivity() {
  private lateinit var requestPermissionLauncher:ActivityResultLauncher<String>

  private val storage = SimpleStorage(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    // 권한 받는 리시버!
    requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        // 권한 허용 받았음!
      } else {
        // 안 받았음
        finish()
      }
    }
    val requestPhoneFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
      if (uri == null) {
        return@registerForActivityResult
      }
      WorkManager.getInstance(this).enqueue(
        OneTimeWorkRequest.Builder(SaveCsvWorker::class.java)
          .setInputData(
            Data.Builder()
              .putString("file_uri", uri.toString())
              .putBoolean("is_wear", false)
              .build()
          )
          .build()
      )
    }
    val requestWearFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
      if (uri == null) {
        return@registerForActivityResult
      }
      WorkManager.getInstance(this).enqueue(
        OneTimeWorkRequest.Builder(SaveCsvWorker::class.java)
          .setInputData(
            Data.Builder()
              .putString("file_uri", uri.toString())
              .putBoolean("is_wear", true)
              .build()
          )
          .build()
      )
    }

    findViewById<Button>(R.id.save_phone_btn).setOnClickListener {
      requestPhoneFileLauncher.launch("phone_collect.csv")
    }
    findViewById<Button>(R.id.save_wear_btn).setOnClickListener {
      requestWearFileLauncher.launch("wear_collect.csv")
    }
    // 디렉터리

  }

  private fun setupSimpleStorage() {
    storage.filePickerCallback = object : FilePickerCallback {
      override fun onCanceledByUser(requestCode: Int) {
        Toast.makeText(baseContext, "Canceled save", Toast.LENGTH_SHORT).show()
      }

      override fun onStoragePermissionDenied(requestCode: Int, files: List<DocumentFile>?) {

      }

      override fun onFileSelected(requestCode: Int, files: List<DocumentFile>) {
        lifecycleScope.launch {
          withContext(Dispatchers.IO) {
            files.first().openOutputStream(this@MainActivity, false)?.also { os ->
              csvWriter().open(os) {
                writeRow(arrayOf(""))
              }
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
      // BODY_SENSORS 권한 없음
      MaterialAlertDialogBuilder(this)
        .setTitle(R.string.perm_dialog_title)
        .setMessage(R.string.perm_dialog_description)
        .setIcon(R.drawable.ic_circle_check)
        .setPositiveButton(R.string.ok) { _, _ ->
          requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
        .setNegativeButton(R.string.cancel) { _, _ ->
          finish()
        }
        .show()
    } else {
      // BODY_SENSORS 권한 있음
      ContextCompat.startForegroundService(this, Intent(this, SensorService::class.java))
    }
  }
}