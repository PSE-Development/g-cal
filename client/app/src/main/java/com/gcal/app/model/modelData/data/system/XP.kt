package com.gcal.app.model.modelData.data.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Defines the contract for experience points.
 * Use the [XP.from] factory method to create instances.
 */
@Serializable
@SerialName("XP")
sealed interface XP {
    fun value(): Int

    companion object {
        /**
         * Creates an [XP] instance based on the provided [amount].
         * Returns [NoXp] if amount is 0 or less, otherwise [XpValue].
         */
        fun from(amount: Int): XP = if (amount <= 0) {
            NoXp
        } else {
            XpValue(amount)
        }
    }
}

/**
 * Standard implementation for positive experience points.
 */
@Serializable
@SerialName("XpValue")
data class XpValue(val amount: Int) : XP {
    override fun value(): Int = amount
}

/**
 * Sentinel object representing a state where no experience points are available or applicable.
 */
@Serializable
@SerialName("NoXp")
object NoXp : XP {

    private const val DEFAULT_VALUE = 0

    override fun value(): Int = DEFAULT_VALUE
}