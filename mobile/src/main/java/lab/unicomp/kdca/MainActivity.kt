package lab.unicomp.kdca

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
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
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import lab.unicomp.kdca.onem2m.Thyme
import java.util.*

class MainActivity : AppCompatActivity() {
  private lateinit var requestPermissionLauncher:ActivityResultLauncher<String>

  private val storage = SimpleStorage(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val pref = DataStoreModule(applicationContext)
    // 권한 받는 리시버!
    requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        // 권한 허용 받았음!
        ContextCompat.startForegroundService(this, Intent(this, SensorService::class.java))
      } else {
        // 안 받았음
        finish()
      }
    }
    val requestDBFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
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

    findViewById<Button>(R.id.save_db_btn).setOnClickListener {
      lifecycleScope.launch {
        val sid = pref.sid.first()
        val time = Clock.System.now().epochSeconds
        requestDBFileLauncher.launch(
          if (sid.isNotEmpty()) {
            "sensorData_S${sid}_$time.db"
          } else {
            "sensorData_S000_$time.db"
          }
        )
      }
    }

    findViewById<Button>(R.id.show_warning_btn).setOnClickListener {
      MaterialAlertDialogBuilder(this).apply {
        setTitle(R.string.warning_title)
        setMessage(resources.openRawResource(
          if (resources.configuration.locales.get(0).language == "ko") {
            R.raw.warning_ko
          } else {
            R.raw.warning_en
          }
        ).bufferedReader().use {it.readText()})
        setPositiveButton(android.R.string.ok) { dialog, _ ->
          dialog.dismiss()
        }
      }.show()
    }

    findViewById<Button>(R.id.register_btn).setOnClickListener {
      CustomTabsIntent.Builder().build().launchUrl(
        this, Uri.parse("http://example.org/register")
      )
    }

    findViewById<Button>(R.id.survey_btn).setOnClickListener {
      CustomTabsIntent.Builder().build().launchUrl(
        this, Uri.parse("http://example.org/")
      )
    }

    val sidLayout = findViewById<ConstraintLayout>(R.id.sid_input_layout)
    val sidSuggestText = findViewById<TextView>(R.id.sid_suggest_text)
    val sidInput = findViewById<TextInputLayout>(R.id.sid_field)
    val sidInputBtn = findViewById<Button>(R.id.sid_register_btn)
    sidInputBtn.isEnabled = false

    sidInput.editText?.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

      }

      override fun afterTextChanged(s: Editable) {
        sidInputBtn.isEnabled = s.length >= 3
      }
    })

    sidInputBtn.setOnClickListener {
      sidInput.isEnabled = false
      it.isEnabled = false
      val value = sidInput.editText?.text ?: ""
      if (value.isNotEmpty()) {
        lifecycleScope.launch {
          pref.setSid(value.toString())
        }
        sidLayout.visibility = View.GONE
        sidSuggestText.text = resources.getString(R.string.welcome_sid).format(value.toString())
      }
    }

    lifecycleScope.launch {
      val sid = pref.sid.first()
      if (sid.isNotEmpty()) {
        sidLayout.visibility = View.GONE
        sidSuggestText.text = resources.getString(R.string.welcome_sid).format(sid)
      } else {
        sidLayout.visibility = View.VISIBLE
        sidSuggestText.text = resources.getString(R.string.suggest_sid_input)
      }
    }
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