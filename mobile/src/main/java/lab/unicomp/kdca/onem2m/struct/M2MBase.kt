package lab.unicomp.kdca.onem2m.struct

import kotlinx.datetime.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 복붙용 코드
 * 설명은 https://github.com/craftingmod/ncube-thyme-typescript/blob/master/src/onem2m/m2m_base.ts 참고
 */
@Serializable
data class M2MBase(
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
  @SerialName("lbl")
  val labels:List<String>,
)

object M2MType {
  val ApplicationEntity = 2
  val Container = 3
  val ContentInstance = 4
  val CSEBase = 5
  val Subscribe = 23
}

object M2MInstantSerializer : KSerializer<Instant> {
  override val descriptor = PrimitiveSerialDescriptor("M2MInstantSerializer", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Instant) {
    val padNum = { num: Int, size: Int ->
      val str = num.toString()
      if (str.length < size) {
        "0".repeat(size - str.length) + str
      } else {
        str
      }
    }
    encoder.encodeString(
      value.toLocalDateTime(TimeZone.UTC).let { date ->
        StringBuilder().apply {
          append(padNum(date.year, 4))
          append(padNum(date.monthNumber, 2))
          append(padNum(date.dayOfMonth, 2))
          append("T")
          append(padNum(date.hour, 2))
          append(padNum(date.minute, 2))
          append(padNum(date.second, 2))
        }.toString()
      }
    )
  }

  override fun deserialize(decoder: Decoder): Instant {
    val time = decoder.decodeString()
    try {
      val dateTime = LocalDateTime.parse(
        StringBuilder().apply {
          append(time.substring(0, 4))
          append("-")
          append(time.substring(4, 6))
          append("-")
          append(time.substring(6, 8))
          append(time.substring(8, 11))
          append(":")
          append(time.substring(11, 13))
          append(":")
          append(time.substring(13, 15))
        }.toString()
      )
      return dateTime.toInstant(TimeZone.UTC)
    } catch (err:Exception) {
      err.printStackTrace()
      return Instant.DISTANT_PAST
    }
  }
}