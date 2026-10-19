package com.maswadkar.lambandtiger.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.maswadkar.lambandtiger.R

@Composable
internal fun AgeScreen(store: AudienceStore) {
    var choice by rememberSaveable { mutableStateOf<AgeGroup?>(null) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState())
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.age_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.age_explanation))
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                AgeGroup.entries.forEach { group ->
                    val label = when (group) {
                        AgeGroup.UNDER_13 -> R.string.age_under_13
                        AgeGroup.AGE_13_15 -> R.string.age_13_15
                        AgeGroup.AGE_16_17 -> R.string.age_16_17
                        AgeGroup.AGE_18_PLUS -> R.string.age_18_plus
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("age-${group.name}")
                        .selectable(choice == group, role = Role.RadioButton, onClick = { choice = group })
                        .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = choice == group, onClick = null)
                        Spacer(Modifier.width(12.dp)); Text(stringResource(label))
                    }
                }
            }
            Button(onClick = { choice?.let(store::choose) }, enabled = choice != null,
                modifier = Modifier.fillMaxWidth().testTag("age-continue")) { Text(stringResource(R.string.age_continue)) }
            if (store.error) Text(stringResource(R.string.age_save_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
