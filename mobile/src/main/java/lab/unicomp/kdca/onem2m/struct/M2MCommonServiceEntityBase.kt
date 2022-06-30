package lab.unicomp.kdca.onem2m.struct

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/**
 * https://github.com/craftingmod/ncube-thyme-typescript/blob/master/src/onem2m/m2m_cb.ts
 */
@Serializable
data class M2MCommonServiceEntityBase(
  @SerialName("pi")
  val parentID: String?,
  @SerialName("ri")
  val resourceID: String,
  @SerialName("ty")
  val resourceType: Int,
  @SerialName("ct")
  @Serializable(M2MInstantSerializer::class)
  val creationTime: Instant,
  @SerialName("rn")
  val resourceName: String,
  @SerialName("lt")
  @Serializable(M2MInstantSerializer::class)
  val lastModifiedTime: Instant,
  // @SerialName("lbl")
  // val labels:List<String>?,
  @SerialName("cst")
  val cseType: Int,
  @SerialName("csi")
  val cseId: String,
  @SerialName("srt")
  val supportedResourceType: List<Int>,
  @SerialName("poa")
  val pointOfAccess: List<String>,
)