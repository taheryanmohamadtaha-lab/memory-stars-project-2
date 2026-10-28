package com.memorystars.leitner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.memorystars.leitner.domain.Leitner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val name: String = "",
    val onboarded: Boolean = false,
    val themeMode: Int = 0, // 0 سیستم، 1 روشن، 2 تاریک
    val themeId: String = "rainbow",
    val goal: Int = 10,
    val intervals: List<Int> = Leitner.DEFAULT_INTERVALS,
    val bonusXp: Int = 0,
    val sessions: Int = 0,
    val goalDay: String = "",
    val reduceMotion: Boolean = false,
    val bigFont: Boolean = false
)

object K {
    val name = stringPreferencesKey("name")
    val onboarded = booleanPreferencesKey("onboarded")
    val themeMode = intPreferencesKey("themeMode")
    val themeId = stringPreferencesKey("themeId")
    val goal = intPreferencesKey("goal")
    val intervals = stringPreferencesKey("intervals")
    val bonusXp = intPreferencesKey("bonusXp")
    val sessions = intPreferencesKey("sessions")
    val goalDay = stringPreferencesKey("goalDay")
    val reduceMotion = booleanPreferencesKey("reduceMotion")
    val bigFont = booleanPreferencesKey("bigFont")
}

fun parseIntervals(s: String?): List<Int> {
    val l = s?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
    return if (l != null && l.size == 5 && l.all { it in 1..365 }) l else Leitner.DEFAULT_INTERVALS
}

class SettingsStore(private val ctx: Context) {
    val flow: Flow<AppSettings> = ctx.ds.data.map { p ->
        AppSettings(
            name = p[K.name] ?: "",
            onboarded = p[K.onboarded] ?: false,
            themeMode = p[K.themeMode] ?: 0,
            themeId = p[K.themeId] ?: "rainbow",
            goal = p[K.goal] ?: 10,
            intervals = parseIntervals(p[K.intervals]),
            bonusXp = p[K.bonusXp] ?: 0,
            sessions = p[K.sessions] ?: 0,
            goalDay = p[K.goalDay] ?: "",
            reduceMotion = p[K.reduceMotion] ?: false,
            bigFont = p[K.bigFont] ?: false
        )
    }

    suspend fun edit(block: (MutablePreferences) -> Unit) {
        ctx.ds.edit { block(it) }
    }
}
