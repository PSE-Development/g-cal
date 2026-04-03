package com.gcal.app.model.modelData.data.system

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Custom serializer for [LocalDateTime] using ISO 8601 format.
 * Facilitates JSON communication between the app and external API.
 */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val stringDate = value.format(isoFormatter)
        encoder.encodeString(stringDate)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val stringDate = decoder.decodeString()
        return LocalDateTime.parse(stringDate, isoFormatter)
    }
}