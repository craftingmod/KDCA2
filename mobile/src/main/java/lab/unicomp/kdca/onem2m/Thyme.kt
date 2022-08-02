package lab.unicomp.kdca.onem2m

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import lab.unicomp.kdca.onem2m.struct.*

class Thyme(
  // 서버 주소
  val address: String,
  val m2mResourceId: String,
) {
  companion object {
    private const val TAG = "Thyme"
    private const val API = "1.0.0-thyme-kotlin"
    const val SERVER = "http://203.253.128.161:7579"
    const val DEVICE_CODE = "3"
  }
  val headers = mapOf(
    Headers.ACCEPT to "application/json",
    "X-M2M-RI" to m2mResourceId,
  )

  fun getContentType(type:Int):String {
    return "application/vnd.onem2m-res+json;ty=$type"
  }

  suspend fun getCommonServiceEntityInfo(cseName: String) : M2MCommonServiceEntityBase? {
    return Fuel.get("$address/$cseName")
      .header(headers)
      .header(Headers.CONTENT_TYPE to getContentType(M2MType.ContentInstance))
      .header("X-M2M-Origin" to "SOrigin")
      .timeout(5000)
      .awaitObjectResult(
        deserializable = kotlinxDeserializerOf(
          loader = M2M_CBRes.serializer(),
          json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
          }
        )
      ).fold(
        success = {
          if (it.debugMessage != null) {
            Log.e(TAG, "[getCommonServiceEntityInfo] m2mError: ${it.debugMessage}")
          }
          it.cb
        },
        failure = {
          Log.d(TAG, "[getCommonServiceEntityInfo] error: ${it.exception}")
          null
        }
      )
  }

  inner class ThymeCSE(private val cse:M2MCommonServiceEntityBase) {
    suspend fun createApplicationEntity(resourceName:String, description:String = ""):M2MApplicationEntity? {
      return requestAE(
        fnname = "createApplicationEntity",
        fuel = Fuel.post("$address/${cse.resourceName}")
          .jsonBody(
            Json.encodeToString(M2M_AEreq_Frame.serializer(), M2M_AEreq_Frame(
              M2M_AEreq(
                resourceName = resourceName,
                api = API,
                labels = description.isNotEmpty().let {
                  if (it) listOf(description) else emptyList()
                },
              )
            ))
          ),
        origin = "S$resourceName",
      ).second
    }

    suspend fun getApplicationEntity(resourceName:String):M2MApplicationEntity? {
      return requestAE(
        "getApplicationEntity",
        Fuel.get("$address/${cse.resourceName}/${resourceName}"),
        "S$resourceName"
      ).second
    }

    suspend fun deleteApplicationEntity(m2mae:M2MApplicationEntity):M2MApplicationEntity? {
      return requestAE(
        "deleteApplicationEntity",
        Fuel.delete("$address/${cse.resourceName}/${m2mae.resourceName}"),
        m2mae.applicationEntityId
      ).second
    }

    suspend fun getThymeAE(aeName: String):ThymeAE? {
      return (getApplicationEntity(aeName) ?: createApplicationEntity(aeName))?.let {
        ThymeAE(cse, it, emptyList())
      }
    }

    private suspend fun requestAE(fnname:String, fuel:Request, origin: String, ignoreResult: Boolean = false):Pair<Boolean, M2MApplicationEntity?> {
      val req = fuel.header(headers)
        .header(Headers.CONTENT_TYPE to getContentType(M2MType.ApplicationEntity))
        .header("X-M2M-Origin" to origin)
        .timeout(5000)
      if (ignoreResult) {
        return if (req.awaitStringResponseResult().second.statusCode == 200) {
          Pair(true, null)
        } else {
          Pair(false, null)
        }
      } else {
        return req.awaitObjectResult(
          deserializable = kotlinxDeserializerOf(
            loader = M2M_AERes.serializer(),
            json = Json {
              ignoreUnknownKeys = true
              coerceInputValues = true
            }
          )
        ).fold(
          success = {
            if (it.debugMessage != null) {
              Log.e(TAG, "[$fnname] m2mError: ${it.debugMessage}")
            }
            Pair(it.debugMessage == null, it.ae)
          },
          failure = {
            Log.d(TAG, "[$fnname] error: ${it.exception}")
            Pair(false, null)
          }
        )
      }
    }
  }

  open inner class ThymeAE(protected open val cse:M2MCommonServiceEntityBase, protected open val ae:M2MApplicationEntity, protected open val cnts:List<M2MContainerBase>) {


    protected open fun getURL():String {
      return "$address/${cse.resourceName}/${ae.resourceName}" + when (cnts.isEmpty()) {
        true -> ""
        false -> cnts.joinToString("/", prefix = "/") { it.resourceName }
      }
    }

    suspend fun createContainer(resourceName:String, maxBufferSize:Int = 10240, description:String = ""):M2MContainerBase? {
      return requestCNT(
        "createContainer",
        Fuel.post(getURL())
          .jsonBody(
            Json.encodeToString(M2M_CNTreq_Frame.serializer(), M2M_CNTreq_Frame(
              M2M_CNTreq(
                resourceName = resourceName,
                labels = description.isNotEmpty().let {
                  if (it) listOf(description) else emptyList()
                },
                maxBufferSize = maxBufferSize,
              )
            ))
          ),
      )
    }

    suspend fun getContainer(resourceName:String):M2MContainerBase? {
      val url = getURL()
      return requestCNT(
        "getContainer",
        Fuel.get("$url/$resourceName"),
      )
    }

    suspend fun deleteContainer(m2mCnt:M2MContainerBase):M2MContainerBase? {
      val url = getURL()
      return requestCNT(
        "deleteContainer",
        Fuel.delete("$url/${m2mCnt.resourceName}"),
      )
    }

    suspend fun getThymeCNT(cntName: String):ThymeCNT? {
      return (getContainer(cntName) ?: createContainer(cntName, 1024 * 1024 * 500))?.let {
        ThymeCNT(cse, ae, cnts.toMutableList().apply {
          add(it)
        })
      }
    }

    private suspend fun requestCNT(fnname:String, fuel:Request):M2MContainerBase? {
      return fuel.header(headers)
        .header(Headers.CONTENT_TYPE to getContentType(M2MType.Container))
        .header("X-M2M-Origin" to ae.applicationEntityId)
        .timeout(5000)
        .awaitObjectResult(
          deserializable = kotlinxDeserializerOf(
            loader = M2M_CNTRes.serializer(),
            json = Json {
              ignoreUnknownKeys = true
              coerceInputValues = true
            }
          )
        ).fold(
          success = {
            if (it.debugMessage != null) {
              Log.e(TAG, "[$fnname] m2mError: ${it.debugMessage}")
            }
            it.cnt
          },
          failure = {
            Log.d(TAG, "[$fnname] error: ${it.exception}")
            null
          }
        )
    }
  }

  inner class ThymeCNT(override val cse:M2MCommonServiceEntityBase, override val ae:M2MApplicationEntity, override val cnts:List<M2MContainerBase>) : ThymeAE(cse, ae, cnts) {

    suspend fun putContent(content:String): Boolean {
      val url = getURL()
      return requestCIN(
        "putContentInstance",
        Fuel.post(url)
          .jsonBody(
            Json.encodeToString(M2M_CINreq_Frame.serializer(), M2M_CINreq_Frame(
              M2M_CINreq(
                resourceValue = content,
              )
            ))
          ),
        ignoreResult = true
      ).first
    }

    suspend fun getLatestContent():M2MContentInstance? {
      val url = getURL()
      return requestCIN(
        "getLatestContentInstance",
        Fuel.get("$url/latest"),
      ).second
    }

    private suspend fun requestCIN(fnname:String, fuel:Request, ignoreResult:Boolean = false):Pair<Boolean, M2MContentInstance?> {
      val req = fuel.header(headers)
        .header(Headers.CONTENT_TYPE to getContentType(M2MType.ContentInstance))
        .header("X-M2M-Origin" to ae.applicationEntityId)
        .timeout(5000)
      return if (ignoreResult) {
        req.awaitStringResult().fold(
          success = {
            Pair(!it.contains("m2m:dbg"), null)
          },
          failure = {
            Pair(false, null)
          }
        )
      } else {
        req.awaitObjectResult(
          deserializable = kotlinxDeserializerOf(
            loader = M2M_CINRes.serializer(),
            json = Json {
              ignoreUnknownKeys = true
              coerceInputValues = true
            }
          )
        ).fold(
          success = {
            if (it.debugMessage != null) {
              Log.e(TAG, "[$fnname] m2mError: ${it.debugMessage}")
            }
            Pair(it.debugMessage == null, it.cin)
          },
          failure = {
            Log.d(TAG, "[$fnname] error: ${it.exception}")
            Pair(false, null)
          }
        )
      }
    }
  }


}