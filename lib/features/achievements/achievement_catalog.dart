import 'package:flutter/material.dart';

import '../../ui/theme/app_colors.dart';

/// Meters in one mile, used to convert the lifetime-distance badge targets.
const double _metersPerMile = 1609.344;

/// The badge family, used for filtering + accent/icon selection.
/// Port of Kotlin `AchievementCategory`.
enum AchievementCategory {
  dailySteps('Daily steps', Icons.directions_walk, AppColors.steps),
  lifetimeDistance('Lifetime distance', Icons.straighten, AppColors.distance),
  dailyFloors('Daily floors', Icons.stairs, AppColors.floors),
  lifetimeFloors('Lifetime floors', Icons.workspace_premium, AppColors.elevation);

  const AchievementCategory(this.label, this.icon, this.color);

  final String label;
  final IconData icon;
  final Color color;
}

/// Which repository-derived metric a badge is evaluated against.
/// Port of Kotlin `AchievementMetric`.
enum AchievementMetric {
  dailySteps,
  lifetimeDistanceMeters,
  dailyFloors,
  lifetimeFloors,
}

/// A single badge definition. Port of Kotlin `AchievementDefinition`.
class AchievementDefinition {
  const AchievementDefinition({
    required this.id,
    required this.name,
    required this.category,
    required this.metric,
    required this.target,
  });

  final String id;
  final String name;
  final AchievementCategory category;
  final AchievementMetric metric;
  final double target;
}

AchievementDefinition _dailySteps(String id, String name, int steps) =>
    AchievementDefinition(
      id: id,
      name: name,
      category: AchievementCategory.dailySteps,
      metric: AchievementMetric.dailySteps,
      target: steps.toDouble(),
    );

AchievementDefinition _lifetimeDistance(String id, String name, double miles) =>
    AchievementDefinition(
      id: id,
      name: name,
      category: AchievementCategory.lifetimeDistance,
      metric: AchievementMetric.lifetimeDistanceMeters,
      target: miles * _metersPerMile,
    );

AchievementDefinition _dailyFloors(String id, String name, int floors) =>
    AchievementDefinition(
      id: id,
      name: name,
      category: AchievementCategory.dailyFloors,
      metric: AchievementMetric.dailyFloors,
      target: floors.toDouble(),
    );

AchievementDefinition _lifetimeFloors(String id, String name, int floors) =>
    AchievementDefinition(
      id: id,
      name: name,
      category: AchievementCategory.lifetimeFloors,
      metric: AchievementMetric.lifetimeFloors,
      target: floors.toDouble(),
    );

const List<(String, String, int)> _dailyStepTargets = [
  ('boat_shoes', 'Boat Shoes', 5000),
  ('sneakers', 'Sneakers', 10000),
  ('minions_stuart', 'Minions Stuart', 12345),
  ('urban_boots', 'Urban Boots', 15000),
  ('high_tops', 'High Tops', 20000),
  ('minions_kevin', 'Minions Kevin', 22222),
  ('classics', 'Classics', 25000),
  ('trail_shoes', 'Trail Shoes', 30000),
  ('minions_bob', 'Minions Bob', 32100),
  ('hiking_boots', 'Hiking Boots', 35000),
  ('cleats', 'Cleats', 40000),
  ('snow_boots', 'Snow Boots', 45000),
  ('cowboy_boots', 'Cowboy Boots', 50000),
  ('platform_shoes', 'Platform Shoes', 55000),
  ('blue_suede_shoes', 'Blue Suede Shoes', 60000),
  ('ruby_slippers', 'Ruby Slippers', 65000),
  ('spring_loaders', 'Spring Loaders', 70000),
  ('genie_shoes', 'Genie Shoes', 75000),
  ('futuristic_kicks', 'Futuristic Kicks', 80000),
  ('rocket_boots', 'Rocket Boots', 90000),
  ('olympian_sandals', 'Olympian Sandals', 100000),
];

const List<(String, String, double)> _lifetimeDistanceTargets = [
  ('marathon', 'Marathon', 26.0),
  ('penguin_march', 'Penguin March', 70.0),
  ('london_underground', 'London Underground', 250.0),
  ('hawaiian_islands', 'Hawaiian Islands', 350.0),
  ('serengeti', 'Serengeti', 500.0),
  ('italy', 'Italy', 736.0),
  ('new_zealand', 'New Zealand', 990.0),
  ('great_barrier_reef', 'Great Barrier Reef', 1600.0),
  ('japan', 'Japan', 1869.0),
  ('india', 'India', 1997.0),
  ('monarch_migration', 'Monarch Migration', 2500.0),
  ('sahara', 'Sahara', 2983.0),
  ('nile', 'Nile', 4132.0),
  ('africa', 'Africa', 5000.0),
  ('great_wall', 'Great Wall', 5500.0),
  ('russian_railway', 'Russian Railway', 5772.0),
  ('earth', 'Earth', 7900.0),
  ('pole_to_pole', 'Pole to Pole', 12430.0),
];

const List<(String, String, int)> _dailyFloorTargets = [
  ('happy_hill', 'Happy Hill', 10),
  ('redwood_forest', 'Redwood Forest', 25),
  ('lighthouse', 'Lighthouse', 50),
  ('ferris_wheel', 'Ferris Wheel', 75),
  ('skyscraper', 'Skyscraper', 100),
  ('rollercoaster', 'Rollercoaster', 125),
  ('stadium', 'Stadium', 150),
  ('bridge', 'Bridge', 175),
  ('castle', 'Castle', 200),
  ('waterfall', 'Waterfall', 300),
  ('canyon', 'Canyon', 400),
  ('volcano', 'Volcano', 500),
  ('mountain', 'Mountain', 600),
  ('rainbow', 'Rainbow', 700),
];

const List<(String, String, int)> _lifetimeFloorTargets = [
  ('helicopter', 'Helicopter', 500),
  ('skydiver', 'Skydiver', 1000),
  ('hot_air_balloon', 'Hot Air Balloon', 2000),
  ('747', '747', 4000),
  ('cloud', 'Cloud', 8000),
  ('spaceship', 'Spaceship', 14000),
  ('shooting_star', 'Shooting Star', 20000),
  ('astronaut', 'Astronaut', 28000),
  ('satellite', 'Satellite', 35000),
];

/// The full badge catalog (62 definitions), ported verbatim from the Kotlin
/// `AchievementCatalog.kt` `AchievementDefinitions`.
final List<AchievementDefinition> achievementDefinitions = [
  for (final (id, name, steps) in _dailyStepTargets)
    _dailySteps(id, name, steps),
  for (final (id, name, miles) in _lifetimeDistanceTargets)
    _lifetimeDistance(id, name, miles),
  for (final (id, name, floors) in _dailyFloorTargets)
    _dailyFloors(id, name, floors),
  for (final (id, name, floors) in _lifetimeFloorTargets)
    _lifetimeFloors(id, name, floors),
];
