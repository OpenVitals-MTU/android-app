package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The only place a user sees which health data they handed over. A category without its
 * status line, or a Grant button that asks for nothing, hides a switched-off feature behind a broken one.
 */
class PermissionCategoryCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersARowPerCategoryWithGrantedOptionalAndUnsupportedStatus() {
        // Sleep is granted, heart is not, and mindfulness is absent from this build: three facts, three labels.
        setContent(granted = SLEEP_PERMISSIONS)

        composeRule.onNodeWithText(string(R.string.onboarding_category_activity_sleep))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_status_granted))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.onboarding_category_heart_recovery))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_status_optional))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.onboarding_category_mindfulness))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_status_not_supported))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun aGrantButtonRequestsThatCategorysPermissions() {
        // The request must carry the category's permission set; asking for nothing leaves the row unchanged forever.
        var requested: Set<String>? = null
        setContent(granted = SLEEP_PERMISSIONS, onGrantHeart = { requested = it })

        composeRule.onNodeWithText(string(R.string.action_grant))
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle { assertEquals(HEART_PERMISSIONS, requested) }
    }

    private fun setContent(
        granted: Set<String>,
        onGrantHeart: (Set<String>) -> Unit = {},
    ) {
        val sleep = SettingsPermissionCategory(
            id = "activity_sleep",
            titleRes = R.string.onboarding_category_activity_sleep,
            descriptionRes = R.string.onboarding_category_activity_sleep_desc,
            permissions = SLEEP_PERMISSIONS,
        )
        val heart = SettingsPermissionCategory(
            id = "heart_recovery",
            titleRes = R.string.onboarding_category_heart_recovery,
            descriptionRes = R.string.onboarding_category_heart_recovery_desc,
            permissions = HEART_PERMISSIONS,
        )
        val mindfulness = SettingsPermissionCategory(
            id = "mindfulness",
            titleRes = R.string.onboarding_category_mindfulness,
            descriptionRes = R.string.onboarding_category_mindfulness_desc,
            permissions = setOf("android.permission.health.READ_MINDFULNESS"),
            available = false,
            unavailableReasonRes = R.string.onboarding_category_mindfulness_unavailable,
        )
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    listOf(sleep, heart, mindfulness).forEach { category ->
                        PermissionCategoryCard(
                            category = category,
                            grantedPermissions = granted,
                            availability = HealthConnectAvailability.AVAILABLE,
                            onGrant = {
                                if (category.id == "heart_recovery") {
                                    onGrantHeart(category.permissions)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val SLEEP_PERMISSIONS = setOf(
            "android.permission.health.READ_SLEEP",
            "android.permission.health.WRITE_SLEEP",
        )
        val HEART_PERMISSIONS = setOf(
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_RESTING_HEART_RATE",
        )
    }
}
