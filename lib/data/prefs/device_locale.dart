import '../../domain/preferences/unit_system.dart';

/// The unit-system default derived from the device locale.
///
/// This is the seam that used to be a bare `Platform.localeName` read inside
/// `PreferencesRepository`. Pulled out as pure functions over a locale *string*
/// so the default is a function of an argument rather than of the machine the
/// test happens to run on.

/// The three countries that do not use the metric system for everyday
/// measurements: the United States, Liberia and Myanmar.
const Set<String> imperialCountries = {'US', 'LR', 'MM'};

/// Extracts the upper-cased country from a POSIX-style locale name, e.g.
/// `'en_US.UTF-8'` -> `'US'`. Returns an empty string when the locale carries
/// no country (`'en'`), which simply means "not an imperial country".
String countryFromLocaleName(String localeName) {
  final underscore = localeName.indexOf('_');
  if (underscore < 0) return '';
  var country = localeName.substring(underscore + 1);
  final terminator = country.indexOf(RegExp(r'[.@]'));
  if (terminator >= 0) country = country.substring(0, terminator);
  return country.toUpperCase();
}

/// The unit system a device in [localeName] should start out with, used only
/// when the user has never chosen one.
UnitSystem unitSystemForLocale(String localeName) =>
    imperialCountries.contains(countryFromLocaleName(localeName))
        ? UnitSystem.imperial
        : UnitSystem.metric;
