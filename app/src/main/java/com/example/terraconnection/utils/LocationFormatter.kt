package com.example.terraconnection.utils

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.text.format.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.LinkedHashMap

object LocationFormatter {
    private const val COORD_DECIMALS = 4
    private const val CACHE_SIZE = 100
    private const val CACHE_TTL_MS = 15 * 60 * 1000L

    private data class CacheEntry(val value: String?, val timestamp: Long)

    private val cache = object : LinkedHashMap<String, CacheEntry>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > CACHE_SIZE
        }
    }

    private fun key(lat: Double, lon: Double): String {
        val factor = Math.pow(10.0, COORD_DECIMALS.toDouble())
        val latRounded = Math.round(lat * factor) / factor
        val lonRounded = Math.round(lon * factor) / factor
        return "$latRounded,$lonRounded"
    }

    suspend fun getGeneralAreaName(context: Context, latitude: Double, longitude: Double): String? {
        val cacheKey = key(latitude, longitude)
        val now = System.currentTimeMillis()
        synchronized(cache) {
            val entry = cache[cacheKey]
            if (entry != null && now - entry.timestamp <= CACHE_TTL_MS) {
                return entry.value
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                val area = listOfNotNull(
                    address?.subLocality,
                    address?.locality,
                    address?.adminArea,
                    address?.countryName
                ).firstOrNull()
                synchronized(cache) { cache[cacheKey] = CacheEntry(area, now) }
                area
            } catch (e: Exception) {
                synchronized(cache) { cache[cacheKey] = CacheEntry(null, now) }
                null
            }
        }
    }

    fun isRecent(timestampMillis: Long, windowMillis: Long = 2 * 60 * 1000): Boolean {
        return System.currentTimeMillis() - timestampMillis <= windowMillis
    }

    fun formatRelative(timeMillis: Long): CharSequence {
        return DateUtils.getRelativeTimeSpanString(
            timeMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
    }
}


