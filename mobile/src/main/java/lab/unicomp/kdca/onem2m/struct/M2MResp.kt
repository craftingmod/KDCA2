package lab.unicomp.kdca.onem2m.struct

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class M2M_AERes(
  @SerialName("m2m:ae")
  val ae: M2MApplicationEntity? = null,
  @SerialName("m2m:dbg")
  val debugMessage: String? = null,
)

@Serializable
data class M2M_CBRes(
  @SerialName("m2m:cb")
  val cb: M2MCommonServiceEntityBase?,
  @SerialName("m2m:dbg")
  val debugMessage: String? = null,
)

@Serializable
data class M2M_CINRes(
  @SerialName("m2m:cin")
  val cin: M2MContentInstance?,
  @SerialName("m2m:dbg")
  val debugMessage: String? = null,
)

@Serializable
data class M2M_CNTRes(
  @SerialName("m2m:cnt")
  val cnt:M2MContainerBase?,
  @SerialName("m2m:dbg")
  val debugMessage: String? = null,
)

