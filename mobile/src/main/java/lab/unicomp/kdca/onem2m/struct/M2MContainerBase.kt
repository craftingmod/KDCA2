package lab.unicomp.kdca.onem2m.struct

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * https://github.com/craftingmod/ncube-thyme-typescript/blob/master/src/onem2m/m2m_cnt.ts
 */
@Serializable
data class M2MContainerBase(
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
  // val labels: List<String>?,
  @SerialName("et")
  @Serializable(M2MInstantSerializer::class)
  val expireTime: Instant,
  @SerialName("mni")
  val maxInstances: Long,
  @SerialName("mbs")
  val maxBufferSize: Long,
  @SerialName("mia")
  val maxInstanceAge: Int,
  @SerialName("cr")
  val creatorId: String,
  @SerialName("cni")
  val currentNumberOfInstances: Int,
  @SerialName("cbs")
  val currentByteSize: Int,
)