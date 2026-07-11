import 'package:flutter/material.dart';

import '../../domain/preferences/app_language.dart';

// The language picker options are intentionally shown as autonyms (each
// language in its own name, e.g. "Deutsch") rather than routed through the ARB
// catalog: an autonym is the same in every locale, which is the accepted i18n
// practice for a language selector so users can always recognise their language.
String appLanguageLabel(AppLanguage value) => switch (value) {
      AppLanguage.system => 'System default',
      AppLanguage.english => 'English',
      AppLanguage.spanish => 'Español',
      AppLanguage.german => 'Deutsch',
      AppLanguage.italian => 'Italiano',
      AppLanguage.estonian => 'Eesti',
    };

/// The in-app language picker. Port of the Kotlin
/// `ui/components/AppLanguageDropdown`, shared — as there — by the Display
/// settings section and the onboarding header.
class AppLanguageDropdown extends StatelessWidget {
  const AppLanguageDropdown({
    super.key,
    required this.selected,
    required this.onSelect,
  });

  final AppLanguage selected;
  final ValueChanged<AppLanguage> onSelect;

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: const InputDecoration(
        border: OutlineInputBorder(),
        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<AppLanguage>(
          value: selected,
          isExpanded: true,
          items: [
            for (final language in AppLanguage.values)
              DropdownMenuItem(
                value: language,
                child: Text(appLanguageLabel(language)),
              ),
          ],
          onChanged: (value) {
            if (value != null) onSelect(value);
          },
        ),
      ),
    );
  }
}
