package lab.unicomp.kdca.onem2m.struct

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class M2M_AEreq_Frame(
  @SerialName("m2m:ae")
  val aeData:M2M_AEreq,
)

@Serializable
data class M2M_AEreq(
  @SerialName("rn")
  val resourceName: String,
  @SerialName("api")
  val api: String,
  @SerialName("lbl")
  val labels: List<String>,
  @SerialName("rr")
  val rr: Boolean = true,
)

@Serializable
data class M2M_CNTreq_Frame(
  @SerialName("m2m:cnt")
  val cntData:M2M_CNTreq,
)

@Serializable
data class M2M_CNTreq(
  @SerialName("rn")
  val resourceName: String,
  @SerialName("lbl")
  val labels: List<String>,
  @SerialName("mbs")
  val maxBufferSize: Int = 10240,
)

@Serializable
data class M2M_CINreq_Frame(
  @SerialName("m2m:cin")
  val cntData:M2M_CINreq,
)

@Serializable
data class M2M_CINreq(
  @SerialName("con")
  val resourceValue: String,
)
