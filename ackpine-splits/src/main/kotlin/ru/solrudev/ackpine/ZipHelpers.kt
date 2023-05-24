package ru.solrudev.ackpine

import java.util.zip.ZipInputStream

@JvmSynthetic
internal fun ZipInputStream.entries() = sequence {
	while (true) {
		val entry = nextEntry ?: break
		yield(entry)
	}
}.constrainOnce()