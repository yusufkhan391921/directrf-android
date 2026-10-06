package com.example.directrf.data.preferences

import android.content.SharedPreferences
import com.example.directrf.domain.model.CallRecord
import org.json.JSONArray
import org.json.JSONObject

data class SavedAppPreferences(
    val displayName: String? = null,
    val callHistory: List<CallRecord> = emptyList()
)

interface DirectRfPreferences {
    fun load(): SavedAppPreferences
    fun saveProfile(displayName: String)
    fun addCallRecord(record: CallRecord)
}

class SharedPreferencesDirectRfPreferences(
    private val preferences: SharedPreferences
) : DirectRfPreferences {
    override fun load(): SavedAppPreferences {
        val history = runCatching {
            val encoded = preferences.getString(KEY_CALL_HISTORY, "[]") ?: "[]"
            val items = JSONArray(encoded)
            (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                CallRecord(
                    remoteName = item.getString("remoteName"),
                    endedAtMillis = item.getLong("endedAtMillis"),
                    durationSeconds = item.getLong("durationSeconds"),
                    outcome = item.getString("outcome")
                )
            }
        }.getOrDefault(emptyList())

        return SavedAppPreferences(
            displayName = preferences.getString(KEY_DISPLAY_NAME, null),
            callHistory = history
        )
    }

    override fun saveProfile(displayName: String) {
        preferences.edit().putString(KEY_DISPLAY_NAME, displayName).apply()
    }

    override fun addCallRecord(record: CallRecord) {
        val history = load().callHistory.toMutableList()
        history.add(0, record)
        val encoded = JSONArray().apply {
            history.take(MAX_HISTORY).forEach { call ->
                put(
                    JSONObject()
                        .put("remoteName", call.remoteName)
                        .put("endedAtMillis", call.endedAtMillis)
                        .put("durationSeconds", call.durationSeconds)
                        .put("outcome", call.outcome)
                )
            }
        }
        preferences.edit().putString(KEY_CALL_HISTORY, encoded.toString()).apply()
    }

    private companion object {
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_CALL_HISTORY = "call_history"
        const val MAX_HISTORY = 50
    }
}