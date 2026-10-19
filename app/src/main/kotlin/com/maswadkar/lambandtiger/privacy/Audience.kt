package com.maswadkar.lambandtiger.privacy

import android.content.Context
import androidx.compose.runtime.*

/** Coarse local-only answer. No DOB, profiling, age-based rewards or adult advertising. */
internal enum class AgeGroup { UNDER_13, AGE_13_15, AGE_16_17, AGE_18_PLUS }
internal val LocalAudience = staticCompositionLocalOf<AudienceStore?> { null }
internal class AudienceStore(context: Context) {
    private val preferences = context.getSharedPreferences("audience", Context.MODE_PRIVATE)
    var group by mutableStateOf(runCatching { AgeGroup.entries.firstOrNull { it.name == preferences.getString("group", null) } }.getOrNull())
        private set
    var error by mutableStateOf(false); private set
    fun choose(value: AgeGroup) {
        error = !preferences.edit().putString("group", value.name).commit()
        if (!error) group = value
    }
    fun changePlayer() {
        error = !preferences.edit().remove("group").commit()
        if (!error) group = null
    }
}
