package com.apagon.rhythm.core.time

import kotlin.time.Clock

/**
 * Multiplatform stand-in for java.lang.System so `System.currentTimeMillis()`
 * call sites compile unchanged in commonMain. Importing this object explicitly
 * shadows java.lang.System on the JVM.
 */
object System {
    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
