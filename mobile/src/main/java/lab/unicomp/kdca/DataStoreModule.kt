package lab.unicomp.kdca

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.prefStore by preferencesDataStore(name = "prefStore")

class DataStoreModule(private val context: Context) {
  private val sidKey = stringPreferencesKey("sid")
  private val phoneLastSentKey = longPreferencesKey("phoneLastSent")
  private val wearLastSentKey = longPreferencesKey("wearLastSent")
  private val lastSyncKey = longPreferencesKey("mobiusLastSync")

  val sid = context.prefStore.data.catch { exception ->
    if (exception is IOException) {
      emit(emptyPreferences())
    } else {
      throw exception
    }
  }.map { pref ->
    pref[sidKey] ?: ""
  }

  suspend fun setSid(sid:String) {
    context.prefStore.edit { pref ->
      pref[sidKey] = sid
    }
  }

  val phoneLastSent = context.prefStore.data.catch { exception ->
    if (exception is IOException) {
      emit(emptyPreferences())
    } else {
      throw exception
    }
  }.map { pref ->
    pref[phoneLastSentKey] ?: 0
  }

  suspend fun setPhoneLastSent(sent:Long) {
    context.prefStore.edit { pref ->
      pref[phoneLastSentKey] = sent
    }
  }

  val wearLastSent = context.prefStore.data.catch { exception ->
    if (exception is IOException) {
      emit(emptyPreferences())
    } else {
      throw exception
    }
  }.map { pref ->
    pref[wearLastSentKey] ?: 0
  }

  suspend fun setWearLastSent(sent:Long) {
    context.prefStore.edit { pref ->
      pref[wearLastSentKey] = sent
    }
  }

  val lastSync = context.prefStore.data.catch { exception ->
    if (exception is IOException) {
      emit(emptyPreferences())
    } else {
      throw exception
    }
  }.map { pref ->
    pref[lastSyncKey] ?: 0
  }

  suspend fun setLastSync(lastSync:Long) {
    context.prefStore.edit { pref ->
      pref[lastSyncKey] = lastSync
    }
  }
}