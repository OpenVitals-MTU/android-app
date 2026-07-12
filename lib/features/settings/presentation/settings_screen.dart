import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../../../core/diagnostics/diagnostics_build_config.dart';
import '../../../core/presentation/external_link.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/ov_card.dart';
import 'settings_section.dart';

/// Settings nav-suite branch body (rendered inside the adaptive scaffold), ported
/// from the Kotlin `SettingsScreen` root (`settingsScreenContent(section = null)`).
/// Lists the settings sections plus a privacy footer.
class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 920),
        child: ListView(
          padding: const EdgeInsets.symmetric(vertical: 8),
          children: [
            // The debug-diagnostics section is surfaced only in
            // diagnostics-enabled builds — the Kotlin hub filters out
            // DEBUG_DIAGNOSTICS unless BuildConfig.OPENVITALS_DIAGNOSTICS
            // (debug OR ci OR nightly), and kDiagnosticsEnabled is its analogue.
            for (final section in SettingsSection.values)
              if (kDiagnosticsEnabled ||
                  section != SettingsSection.debugDiagnostics)
                Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                  child: _SettingsCategoryCard(section: section),
                ),
            SectionHeader(l10n.sectionSupport),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: _SupportCard(),
            ),
            SectionHeader(l10n.sectionPrivacy),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: OpenVitalsCard(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.settingsPrivacyOnDevice,
                        style: theme.textTheme.titleSmall,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        l10n.privacyPolicyBody2,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 24),
              child: _SettingsVersionText(),
            ),
          ],
        ),
      ),
    );
  }
}

class _SettingsCategoryCard extends StatelessWidget {
  const _SettingsCategoryCard({required this.section});

  final SettingsSection section;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return OpenVitalsCard(
      onTap: () => context.push(section.route),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(section.icon, color: theme.colorScheme.onSurface, size: 24),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    section.localizedTitle(l10n),
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    section.localizedSummary(l10n),
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right,
                color: theme.colorScheme.onSurfaceVariant),
          ],
        ),
      ),
    );
  }
}

/// The hub support card. Port of Kotlin `SupportOpenVitalsCard`: a heart-marked
/// header plus three outlined link buttons (report an issue, join discussions,
/// open the funding page) that open their URLs in the system browser.
class _SupportCard extends StatelessWidget {
  const _SupportCard();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.only(top: 2),
                  child: Icon(
                    Icons.favorite_border,
                    size: 20,
                    color: theme.colorScheme.primary,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.settingsSupportTitle,
                        style: theme.textTheme.titleSmall,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        l10n.settingsSupportBody,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            _SupportLinkButton(
              label: l10n.settingsSupportIssuesAction,
              url: supportIssuesUrl,
            ),
            const SizedBox(height: 8),
            _SupportLinkButton(
              label: l10n.settingsSupportDiscussionAction,
              url: supportDiscussionUrl,
            ),
            const SizedBox(height: 8),
            _SupportLinkButton(
              label: l10n.settingsSupportAction,
              url: supportUrl,
            ),
          ],
        ),
      ),
    );
  }
}

class _SupportLinkButton extends StatelessWidget {
  const _SupportLinkButton({required this.label, required this.url});

  final String label;
  final String url;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      child: OutlinedButton.icon(
        onPressed: () => openExternalUrl(context, url),
        icon: const Icon(Icons.open_in_new, size: 18),
        label: Text(label),
      ),
    );
  }
}

/// The hub version footer. Port of Kotlin `SettingsVersionText`: reads the app
/// version and build number from the platform package metadata via
/// package_info_plus and renders the localized "Version X (Y)" string.
class _SettingsVersionText extends StatelessWidget {
  const _SettingsVersionText();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return FutureBuilder<PackageInfo>(
      future: PackageInfo.fromPlatform(),
      builder: (context, snapshot) {
        final info = snapshot.data;
        final text = info == null
            ? ''
            : l10n.settingsAppVersion(
                info.version,
                int.tryParse(info.buildNumber) ?? 0,
              );
        return Text(
          text,
          textAlign: TextAlign.center,
          style: theme.textTheme.bodySmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        );
      },
    );
  }
}
