package com.maswadkar.lambandtiger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.maswadkar.lambandtiger.privacy.*
import org.junit.*
import org.junit.Assert.*

class AudienceFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun resetPlayer() { rule.runOnIdle { rule.activity.ads.updateAudience(false); rule.activity.audience.changePlayer() } }
    @Test fun neutralSelectionPersistsAndChangingPlayerResetsGate() {
        rule.onNodeWithTag("age-continue").assertIsNotEnabled()
        AgeGroup.entries.forEach { rule.onNodeWithTag("age-${it.name}").assertIsNotSelected() }
        rule.runOnIdle { assertFalse(rule.activity.ads.eligible); assertFalse(rule.activity.ads.bannerAllowed) }
        rule.onNodeWithTag("age-UNDER_13").performClick()
        rule.onNodeWithTag("age-continue").performClick()
        rule.runOnIdle { assertEquals(AgeGroup.UNDER_13, AudienceStore(rule.activity).group) }
        rule.onNodeWithTag("age-continue").assertDoesNotExist()
        rule.runOnIdle { rule.activity.ads.updateAudience(false); rule.activity.audience.changePlayer() }
        rule.onNodeWithTag("age-continue").assertIsNotEnabled()
        AgeGroup.entries.forEach { rule.onNodeWithTag("age-${it.name}").assertIsNotSelected() }
        rule.onNodeWithTag("age-AGE_18_PLUS").performClick()
        rule.onNodeWithTag("age-continue").performClick()
        rule.runOnIdle { assertEquals(AgeGroup.AGE_18_PLUS, AudienceStore(rule.activity).group) }
    }
    @Test fun unreadableStoredGroupIsUnknown() {
        rule.runOnIdle {
            rule.activity.getSharedPreferences("audience",0).edit().putString("group","invalid").commit()
            assertNull(AudienceStore(rule.activity).group)
        }
        rule.onNodeWithTag("age-continue").assertIsNotEnabled()
    }
}
