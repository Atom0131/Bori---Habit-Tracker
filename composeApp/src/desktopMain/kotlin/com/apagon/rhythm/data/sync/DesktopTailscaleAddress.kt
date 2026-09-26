package com.apagon.rhythm.data.sync

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Resolves this machine's Tailscale IPv4 address, if Tailscale is installed
 * and connected (Stage 13). Tailscale assigns addresses from the CGNAT range
 * 100.64.0.0/10 and, on Linux, typically names its interface "tailscale0" —
 * matched by name first, with an IP-range fallback in case the interface
 * naming differs across distros/versions (not verified against a real
 * Tailscale install on this machine — Tailscale isn't set up here yet).
 * Returns null rather than throwing if nothing matches (not installed, not
 * logged in, VPN down); callers fall back to loopback, keeping Stage 4b's
 * local-dev two-instance workflow intact.
 */
fun findTailscaleAddress(): String? {
    val interfaces = runCatching { NetworkInterface.getNetworkInterfaces()?.toList() }.getOrNull() ?: return null

    interfaces.firstOrNull { it.displayName.contains("tailscale", ignoreCase = true) }
        ?.inetAddresses?.toList()
        ?.filterIsInstance<Inet4Address>()
        ?.firstOrNull()
        ?.let { return it.hostAddress }

    return interfaces.asSequence()
        .flatMap { it.inetAddresses.toList().asSequence() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { isTailscaleRange(it) }
        ?.hostAddress
}

/** 100.64.0.0/10: first octet 100, second octet in 64..127. */
private fun isTailscaleRange(addr: Inet4Address): Boolean {
    val bytes = addr.address
    return (bytes[0].toInt() and 0xFF) == 100 && (bytes[1].toInt() and 0xFF) in 64..127
}
