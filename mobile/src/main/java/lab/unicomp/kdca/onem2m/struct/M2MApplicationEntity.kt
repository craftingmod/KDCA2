package lab.unicomp.kdca.onem2m.struct

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * https://github.com/craftingmod/ncube-thyme-typescript/blob/master/src/onem2m/m2m_ae.ts
 */
@Serializable
data class M2MApplicationEntity(
  @SerialName("pi")
  val parentID: String,
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
  @SerialName("et")
  @Serializable(M2MInstantSerializer::class)
  val expireTime: Instant,
  @SerialName("api")
  val apiString: String,
  @SerialName("poa")
  val pointOfAccess: List<String> = listOf(),
  @SerialName("aei")
  val applicationEntityId: String,
)