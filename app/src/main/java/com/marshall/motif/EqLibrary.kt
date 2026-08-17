package com.marshall.motif

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class EqSnapshot(val name: String, val bands: List<Int>)

class EqLibrary(context: Context) {
    private val prefs = context.getSharedPreferences("marshall_eq", Context.MODE_PRIVATE)

    fun lastBands(): List<Int> {
        val raw = prefs.getString("last", null) ?: return listOf(0, 0, 0, 0, 0)
        return raw.split(",").mapNotNull { it.toIntOrNull() }.let { parsed ->
            (0 until 5).map { parsed.getOrElse(it) { 0 }.coerceIn(-6, 6) }
        }
    }

    fun saveLast(bands: List<Int>) {
        prefs.edit().putString("last", bands.take(5).joinToString(",")).apply()
    }

    fun activeName(): String? = prefs.getString("active", null)?.ifBlank { null }

    fun setActive(name: String?) {
        prefs.edit().putString("active", name.orEmpty()).apply()
    }

    fun snapshots(): List<EqSnapshot> {
        val json = prefs.getString("snapshots", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = o.optString("name").ifBlank { return@mapNotNull null }
                val bands = o.optJSONArray("bands") ?: return@mapNotNull null
                EqSnapshot(
                    name,
                    (0 until 5).map { bands.optInt(it, 0).coerceIn(-6, 6) },
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveSnapshot(name: String, bands: List<Int>) {
        val clean = name.trim().ifBlank { return }
        val next = snapshots().filterNot { it.name.equals(clean, ignoreCase = true) } +
            EqSnapshot(clean, (0 until 5).map { bands.getOrElse(it) { 0 }.coerceIn(-6, 6) })
        write(next.takeLast(8))
    }

    fun deleteSnapshot(name: String) {
        if (activeName() == name) setActive(null)
        write(snapshots().filterNot { it.name == name })
    }

    private fun write(items: List<EqSnapshot>) {
        val arr = JSONArray()
        items.forEach { snap ->
            arr.put(
                JSONObject()
                    .put("name", snap.name)
                    .put("bands", JSONArray(snap.bands)),
            )
        }
        prefs.edit().putString("snapshots", arr.toString()).apply()
    }
}
