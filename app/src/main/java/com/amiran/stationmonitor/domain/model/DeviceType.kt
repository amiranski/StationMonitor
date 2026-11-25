package com.amiran.stationmonitor.domain.model

/**
 * Kind of device. A `sealed interface` instead of an `enum class` because the unknown case
 * carries data: we keep the raw code (e.g. "EVC") so the UI can still show it.
 *
 * Swift equivalent: `enum DeviceType { case pos, bos, fcs, opt; case unknown(String) }`.
 */
sealed interface DeviceType {
    /** Code exactly as used by the API. */
    val code: String

    /** Point of sale, the cash register. */
    data object Pos : DeviceType {
        override val code = "POS"
    }

    /** Back-office server of the station. */
    data object Bos : DeviceType {
        override val code = "BOS"
    }

    /** Forecourt server, controls the fuel dispensers. */
    data object Fcs : DeviceType {
        override val code = "FCS"
    }

    /** Outdoor payment terminal. */
    data object Opt : DeviceType {
        override val code = "OPT"
    }

    /** A type added on the backend after this app version was released. */
    data class Unknown(override val code: String) : DeviceType

    companion object {
        fun fromApi(raw: String?): DeviceType = when (val code = raw?.trim()?.uppercase().orEmpty()) {
            Pos.code -> Pos
            Bos.code -> Bos
            Fcs.code -> Fcs
            Opt.code -> Opt
            else -> Unknown(code.ifEmpty { "?" })
        }
    }
}
