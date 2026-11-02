package com.shterneregen.securelan.lantools.model

import com.shterneregen.securelan.common.net.address.IpLiteral
import java.util.UUID

/**
 * User-owned identity, unrelated to authenticated room peers. Persistence belongs to applications.
 *
 * @property id Stable local UUID, independent of changing addresses or advertisements.
 * @property displayName Trimmed nonempty user label, up to 128 characters without control characters.
 * @property addressHint Last known numeric address; neither an online signal nor an identity proof.
 * @property hostnameHint Optional name up to 253 characters, without whitespace/control characters.
 * @property wake Optional WoL configuration; saving a device does not require knowing its MAC.
 */
data class SavedLanDevice(
    val id: UUID,
    val displayName: String,
    val addressHint: IpLiteral? = null,
    val hostnameHint: String? = null,
    val wake: WakeConfiguration? = null,
) {
    init {
        require(displayName.length in 1..128 && displayName == displayName.trim() && displayName.none(Char::isISOControl))
        require(hostnameHint == null || hostnameHint.length in 1..253 && hostnameHint.none { it.isWhitespace() || it.isISOControl() })
    }
}

/**
 * Persistable preferences; validate again before network access. Interface keys are only hints.
 *
 * @property mac Syntactically valid MAC; the service additionally requires a nonzero unicast target.
 * @property broadcast Optional numeric broadcast override, checked against the selected subnet.
 * @property preferredInterfaceKey Saved user preference, not an OS default-route recommendation.
 * @property port Destination UDP port, default 9; validated before any network access.
 */
data class WakeConfiguration(
    val mac: MacAddress,
    val broadcast: IpLiteral? = null,
    val preferredInterfaceKey: String? = null,
    val port: Int = 9,
)
