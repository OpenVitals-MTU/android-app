package tech.mmarca.openvitals.features.onboarding

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.OnboardingCategoryId
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.AppLanguageDropdown
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.InstructionSteps
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsFilledButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import tech.mmarca.openvitals.ui.components.StepBar
import tech.mmarca.openvitals.ui.components.StepHero
import tech.mmarca.openvitals.ui.components.StepInlineActionButton
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private const val HC_PACKAGE = "com.google.android.apps.healthdata"
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$HC_PACKAGE"

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val unableToOpenPermissions = stringResource(R.string.onboarding_unable_open_permissions)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkState()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    fun launchRequest(permissions: Set<String>) {
        if (permissions.isEmpty()) return
        viewModel.beginPermissionRequest(permissions)
        permissionLauncher.launch(permissions)
    }

    fun openHealthConnectSettings() {
        if (!openHealthConnectPermissionSettings(context)) {
            Toast.makeText(context, unableToOpenPermissions, Toast.LENGTH_LONG).show()
        }
    }

    // A request that achieved nothing — a refusal, or Health Connect silently
    // refusing to ask again after two cancels — falls through to the settings
    // page, where the toggle still works.
    LaunchedEffect(state.openSettingsEvent) {
        if (state.openSettingsEvent > 0L) {
            openHealthConnectSettings()
        }
    }

    BackHandler(enabled = !state.isFirstStep) {
        viewModel.back()
    }

    if (state.isCheckingPermissions) {
        FullScreenLoading()
        return
    }

    fun completeOnboarding() {
        viewModel.completeOnboarding()
        onOnboardingComplete()
    }

    if (state.availability != HealthConnectAvailability.AVAILABLE) {
        UnavailableContent(state = state, onSelectLanguage = viewModel::selectAppLanguage)
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xxl),
        ) {
            when (state.step) {
                OnboardingStep.CATEGORIES -> CategoriesStep(
                    state = state,
                    onSelectLanguage = viewModel::selectAppLanguage,
                    onRequest = { id -> launchRequest(viewModel.missingRequestableFor(id)) },
                    onRequestAll = { launchRequest(viewModel.missingRequestableForCategories()) },
                )
                OnboardingStep.MINDFULNESS -> MindfulnessStep(
                    state = state,
                    onOptInChanged = viewModel::setMindfulnessOptIn,
                    onRequest = { launchRequest(viewModel.missingRequestableFor(OnboardingCategoryId.MINDFULNESS)) },
                )
                OnboardingStep.CYCLE_TRACKING -> CycleStep(
                    state = state,
                    onRequest = { launchRequest(viewModel.missingRequestableFor(OnboardingCategoryId.CYCLE_TRACKING)) },
                )
                OnboardingStep.ADDITIONAL_ACCESS -> AdditionalAccessStep(
                    state = state,
                    onRequest = { launchRequest(viewModel.missingRequestableFor(OnboardingCategoryId.ADDITIONAL_ACCESS)) },
                    onOpenHealthConnect = ::openHealthConnectSettings,
                )
            }
        }
        StepBar(
            nextLabel = when {
                state.isLastStep -> stringResource(R.string.action_finish)
                state.step == OnboardingStep.CATEGORIES || state.currentStepSatisfied ->
                    stringResource(R.string.onboarding_action_next)
                else -> stringResource(R.string.onboarding_action_skip)
            },
            onNext = when {
                !state.canAdvance -> null
                state.isLastStep -> ::completeOnboarding
                else -> viewModel::next
            },
            backLabel = stringResource(R.string.onboarding_action_back),
            onBack = if (state.isFirstStep) null else viewModel::back,
        )
    }
}

// ── Step one: header, feature cards, category rows ──────────────────────────

@Composable
private fun CategoriesStep(
    state: OnboardingUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onRequest: (OnboardingCategoryId) -> Unit,
    onRequestAll: () -> Unit,
) {
    OnboardingHeader(state = state, onSelectLanguage = onSelectLanguage)

    Spacer(Modifier.height(Spacing.xxl))
    FeatureCard(
        icon = Icons.Outlined.Lock,
        title = stringResource(R.string.onboarding_privacy_title),
        body = stringResource(R.string.onboarding_privacy_body),
    )
    Spacer(Modifier.height(Spacing.md))
    FeatureCard(
        icon = Icons.Outlined.HealthAndSafety,
        title = stringResource(R.string.onboarding_health_connect_title),
        body = stringResource(R.string.onboarding_health_connect_body),
    )
    Spacer(Modifier.height(Spacing.md))
    FeatureCard(
        icon = Icons.Outlined.Info,
        title = stringResource(R.string.health_disclaimer_title),
        body = stringResource(R.string.health_disclaimer_body),
    )

    Spacer(Modifier.height(Spacing.xxl))
    Text(
        text = stringResource(R.string.onboarding_step_categories_title),
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = stringResource(R.string.onboarding_step_categories_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // One dialog for every outstanding category, above the per-row buttons that
    // stay for granting them one at a time.
    if (state.categoriesMissingPermissions.isNotEmpty()) {
        Spacer(Modifier.height(Spacing.lg))
        OpenVitalsFilledButton(
            onClick = onRequestAll,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_action_grant_all))
        }
    }

    Spacer(Modifier.height(Spacing.lg))
    // Outstanding rows first, ordered ONCE when the step opens rather than on
    // every grant: a list that re-sorts live moves the next row out from under
    // the finger that just tapped one.
    val rowOrder = remember { mutableStateOf<List<OnboardingCategoryId>>(emptyList()) }
    if (rowOrder.value.isEmpty() && state.categoryRows.isNotEmpty()) {
        rowOrder.value = state.categoryRows
            .sortedBy { row ->
                when {
                    !row.available -> 2
                    row.fullyGranted -> 1
                    else -> 0
                }
            }
            .map { it.id }
    }
    rowOrder.value.mapNotNull { id -> state.categoryRows.firstOrNull { it.id == id } }
        .forEach { row ->
            PermissionCategoryRow(row = row, onRequest = { onRequest(row.id) })
            Spacer(Modifier.height(Spacing.md))
        }

    if (!state.requiredGranted) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.onboarding_core_required),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Step two: mindfulness opt-in ────────────────────────────────────────────

@Composable
private fun MindfulnessStep(
    state: OnboardingUiState,
    onOptInChanged: (Boolean) -> Unit,
    onRequest: () -> Unit,
) {
    StepHero(
        icon = Icons.Outlined.SelfImprovement,
        title = stringResource(R.string.onboarding_step_mindfulness_title),
        body = stringResource(R.string.onboarding_step_mindfulness_body),
    )
    Spacer(Modifier.height(Spacing.xxl))
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LayoutMetrics.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_mindfulness_opt_in_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.onboarding_mindfulness_opt_in_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.mindfulnessOptIn, onCheckedChange = onOptInChanged)
        }
    }
    val row = state.mindfulnessRow
    if (state.mindfulnessOptIn && row != null) {
        Spacer(Modifier.height(Spacing.md))
        PermissionCategoryRow(row = row, onRequest = onRequest)
    }
}

// ── Step three: cycle tracking ──────────────────────────────────────────────

@Composable
private fun CycleStep(
    state: OnboardingUiState,
    onRequest: () -> Unit,
) {
    StepHero(
        icon = Icons.Outlined.CalendarMonth,
        title = stringResource(R.string.onboarding_step_cycle_title),
        body = stringResource(R.string.onboarding_step_cycle_body),
    )
    val row = state.cycleRow
    if (row != null) {
        Spacer(Modifier.height(Spacing.xxl))
        PermissionCategoryRow(row = row, onRequest = onRequest)
    }
}

// ── Step four: additional access + routes walkthrough ───────────────────────

@Composable
private fun AdditionalAccessStep(
    state: OnboardingUiState,
    onRequest: () -> Unit,
    onOpenHealthConnect: () -> Unit,
) {
    StepHero(
        icon = Icons.Outlined.Tune,
        title = stringResource(R.string.onboarding_step_additional_title),
        body = stringResource(R.string.onboarding_step_additional_body),
    )
    val row = state.additionalAccessRow
    if (row != null) {
        Spacer(Modifier.height(Spacing.xxl))
        PermissionCategoryRow(row = row, onRequest = onRequest)
    }
    if (state.routesOutstanding) {
        Spacer(Modifier.height(Spacing.md))
        OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LayoutMetrics.cardPadding)) {
                Text(
                    text = stringResource(R.string.onboarding_routes_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.onboarding_routes_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                InstructionSteps(
                    steps = listOf(
                        stringResource(R.string.onboarding_routes_step1),
                        stringResource(R.string.onboarding_routes_step2),
                        stringResource(R.string.onboarding_routes_step3),
                    ),
                )
                Spacer(Modifier.height(Spacing.md))
                StepInlineActionButton(
                    text = stringResource(R.string.onboarding_open_health_permissions),
                    onClick = onOpenHealthConnect,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

// ── Shared pieces ───────────────────────────────────────────────────────────

@Composable
internal fun OnboardingHeader(
    state: OnboardingUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppLanguageDropdown(
            selected = state.appLanguage,
            onSelect = onSelectLanguage,
            modifier = Modifier.align(Alignment.End),
        )
        Spacer(Modifier.height(Spacing.lg))
        Image(
            painter = painterResource(R.drawable.open_vitals_logo_wide),
            contentDescription = null,
            modifier = Modifier
                .width(152.dp)
                .height(104.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(Spacing.xxl))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, body: String) {
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.xxl),
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionCategoryRow(
    row: OnboardingRow,
    onRequest: () -> Unit,
) {
    val containerColor = if (row.fullyGranted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = Emphasis.disabled)
    } else {
        androidx.compose.ui.graphics.Color.Unspecified
    }
    OpenVitalsCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = LayoutMetrics.cardPadding,
                vertical = LayoutMetrics.metricTilePadding,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(row.titleRes()), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = row.statusText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (row.fullyGranted) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.onboarding_status_granted),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (!row.available) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.onboarding_status_not_supported),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(row.descriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!row.fullyGranted && row.available) {
                Spacer(Modifier.height(Spacing.sm))
                OpenVitalsTonalButton(
                    onClick = onRequest,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = if (row.partial) {
                            stringResource(R.string.action_review)
                        } else {
                            stringResource(R.string.action_grant)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingRow.statusText(): String = when {
    !available -> stringResource(R.string.onboarding_status_not_supported)
    fullyGranted -> stringResource(R.string.onboarding_status_granted)
    partial -> stringResource(R.string.onboarding_status_partially_granted, grantedCount, total)
    required -> stringResource(R.string.onboarding_status_required)
    else -> stringResource(R.string.onboarding_status_optional)
}

private fun OnboardingRow.titleRes(): Int = when (id) {
    OnboardingCategoryId.ACTIVITY -> R.string.onboarding_hc_category_activity
    OnboardingCategoryId.BODY -> R.string.onboarding_hc_category_body
    OnboardingCategoryId.NUTRITION -> R.string.onboarding_hc_category_nutrition
    OnboardingCategoryId.SLEEP -> R.string.onboarding_hc_category_sleep
    OnboardingCategoryId.VITALS -> R.string.onboarding_hc_category_vitals
    OnboardingCategoryId.CYCLE_TRACKING -> R.string.onboarding_hc_category_cycle
    OnboardingCategoryId.MINDFULNESS -> R.string.onboarding_hc_category_mindfulness
    OnboardingCategoryId.ADDITIONAL_ACCESS -> R.string.onboarding_hc_category_additional
}

private fun OnboardingRow.descriptionRes(): Int = when (id) {
    OnboardingCategoryId.ACTIVITY -> R.string.onboarding_hc_category_activity_desc
    OnboardingCategoryId.BODY -> R.string.onboarding_hc_category_body_desc
    OnboardingCategoryId.NUTRITION -> R.string.onboarding_hc_category_nutrition_desc
    OnboardingCategoryId.SLEEP -> R.string.onboarding_hc_category_sleep_desc
    OnboardingCategoryId.VITALS -> R.string.onboarding_hc_category_vitals_desc
    OnboardingCategoryId.CYCLE_TRACKING -> R.string.onboarding_hc_category_cycle_desc
    OnboardingCategoryId.MINDFULNESS -> R.string.onboarding_hc_category_mindfulness_desc
    OnboardingCategoryId.ADDITIONAL_ACCESS -> R.string.onboarding_hc_category_additional_desc
}

// ── Health Connect unavailable ──────────────────────────────────────────────

@Composable
internal fun UnavailableContent(
    state: OnboardingUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingHeader(state = state, onSelectLanguage = onSelectLanguage)
        Spacer(Modifier.height(Spacing.xxxl))
        when (state.availability) {
            HealthConnectAvailability.NEEDS_PROVIDER_UPDATE -> {
                OpenVitalsCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_health_connect_update),
                        modifier = Modifier.padding(LayoutMetrics.cardPadding),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
                OpenVitalsButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, PLAY_STORE_URL.toUri()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.onboarding_install_health_connect))
                }
            }
            HealthConnectAvailability.NEEDS_PLAY_STORE -> UnavailableCard(
                text = stringResource(R.string.onboarding_health_connect_needs_play_store),
            )
            else -> UnavailableCard(
                text = stringResource(R.string.onboarding_health_connect_not_supported),
            )
        }
    }
}

@Composable
private fun UnavailableCard(text: String) {
    OpenVitalsCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
