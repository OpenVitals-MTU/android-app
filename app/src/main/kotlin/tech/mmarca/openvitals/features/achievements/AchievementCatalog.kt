package tech.mmarca.openvitals.features.achievements

private const val MetersPerMile = 1_609.344

enum class AchievementCategory {
    DAILY_STEPS,
    LIFETIME_DISTANCE,
    DAILY_FLOORS,
    LIFETIME_FLOORS,
}

enum class AchievementMetric {
    DAILY_STEPS,
    LIFETIME_DISTANCE_METERS,
    DAILY_FLOORS,
    LIFETIME_FLOORS,
}

data class AchievementDefinition(
    val id: String,
    val name: String,
    val category: AchievementCategory,
    val metric: AchievementMetric,
    val target: Double,
)

private val dailyStepBadges = listOf(
    dailySteps("boat_shoes", "Boat Shoes", 5_000),
    dailySteps("sneakers", "Sneakers", 10_000),
    dailySteps("minions_stuart", "Minions Stuart", 12_345),
    dailySteps("urban_boots", "Urban Boots", 15_000),
    dailySteps("high_tops", "High Tops", 20_000),
    dailySteps("minions_kevin", "Minions Kevin", 22_222),
    dailySteps("classics", "Classics", 25_000),
    dailySteps("trail_shoes", "Trail Shoes", 30_000),
    dailySteps("minions_bob", "Minions Bob", 32_100),
    dailySteps("hiking_boots", "Hiking Boots", 35_000),
    dailySteps("cleats", "Cleats", 40_000),
    dailySteps("snow_boots", "Snow Boots", 45_000),
    dailySteps("cowboy_boots", "Cowboy Boots", 50_000),
    dailySteps("platform_shoes", "Platform Shoes", 55_000),
    dailySteps("blue_suede_shoes", "Blue Suede Shoes", 60_000),
    dailySteps("ruby_slippers", "Ruby Slippers", 65_000),
    dailySteps("spring_loaders", "Spring Loaders", 70_000),
    dailySteps("genie_shoes", "Genie Shoes", 75_000),
    dailySteps("futuristic_kicks", "Futuristic Kicks", 80_000),
    dailySteps("rocket_boots", "Rocket Boots", 90_000),
    dailySteps("olympian_sandals", "Olympian Sandals", 100_000),
)

private val lifetimeDistanceBadges = listOf(
    lifetimeDistance("marathon", "Marathon", 26.0),
    lifetimeDistance("penguin_march", "Penguin March", 70.0),
    lifetimeDistance("london_underground", "London Underground", 250.0),
    lifetimeDistance("hawaiian_islands", "Hawaiian Islands", 350.0),
    lifetimeDistance("serengeti", "Serengeti", 500.0),
    lifetimeDistance("italy", "Italy", 736.0),
    lifetimeDistance("new_zealand", "New Zealand", 990.0),
    lifetimeDistance("great_barrier_reef", "Great Barrier Reef", 1_600.0),
    lifetimeDistance("japan", "Japan", 1_869.0),
    lifetimeDistance("india", "India", 1_997.0),
    lifetimeDistance("monarch_migration", "Monarch Migration", 2_500.0),
    lifetimeDistance("sahara", "Sahara", 2_983.0),
    lifetimeDistance("nile", "Nile", 4_132.0),
    lifetimeDistance("africa", "Africa", 5_000.0),
    lifetimeDistance("great_wall", "Great Wall", 5_500.0),
    lifetimeDistance("russian_railway", "Russian Railway", 5_772.0),
    lifetimeDistance("earth", "Earth", 7_900.0),
    lifetimeDistance("pole_to_pole", "Pole to Pole", 12_430.0),
)

private val dailyFloorBadges = listOf(
    dailyFloors("happy_hill", "Happy Hill", 10),
    dailyFloors("redwood_forest", "Redwood Forest", 25),
    dailyFloors("lighthouse", "Lighthouse", 50),
    dailyFloors("ferris_wheel", "Ferris Wheel", 75),
    dailyFloors("skyscraper", "Skyscraper", 100),
    dailyFloors("rollercoaster", "Rollercoaster", 125),
    dailyFloors("stadium", "Stadium", 150),
    dailyFloors("bridge", "Bridge", 175),
    dailyFloors("castle", "Castle", 200),
    dailyFloors("waterfall", "Waterfall", 300),
    dailyFloors("canyon", "Canyon", 400),
    dailyFloors("volcano", "Volcano", 500),
    dailyFloors("mountain", "Mountain", 600),
    dailyFloors("rainbow", "Rainbow", 700),
)

private val lifetimeFloorBadges = listOf(
    lifetimeFloors("helicopter", "Helicopter", 500),
    lifetimeFloors("skydiver", "Skydiver", 1_000),
    lifetimeFloors("hot_air_balloon", "Hot Air Balloon", 2_000),
    lifetimeFloors("747", "747", 4_000),
    lifetimeFloors("cloud", "Cloud", 8_000),
    lifetimeFloors("spaceship", "Spaceship", 14_000),
    lifetimeFloors("shooting_star", "Shooting Star", 20_000),
    lifetimeFloors("astronaut", "Astronaut", 28_000),
    lifetimeFloors("satellite", "Satellite", 35_000),
)

val AchievementDefinitions: List<AchievementDefinition> =
    dailyStepBadges + lifetimeDistanceBadges + dailyFloorBadges + lifetimeFloorBadges

private fun dailySteps(id: String, name: String, steps: Long) =
    AchievementDefinition(
        id = id,
        name = name,
        category = AchievementCategory.DAILY_STEPS,
        metric = AchievementMetric.DAILY_STEPS,
        target = steps.toDouble(),
    )

private fun lifetimeDistance(id: String, name: String, miles: Double) =
    AchievementDefinition(
        id = id,
        name = name,
        category = AchievementCategory.LIFETIME_DISTANCE,
        metric = AchievementMetric.LIFETIME_DISTANCE_METERS,
        target = miles * MetersPerMile,
    )

private fun dailyFloors(id: String, name: String, floors: Int) =
    AchievementDefinition(
        id = id,
        name = name,
        category = AchievementCategory.DAILY_FLOORS,
        metric = AchievementMetric.DAILY_FLOORS,
        target = floors.toDouble(),
    )

private fun lifetimeFloors(id: String, name: String, floors: Int) =
    AchievementDefinition(
        id = id,
        name = name,
        category = AchievementCategory.LIFETIME_FLOORS,
        metric = AchievementMetric.LIFETIME_FLOORS,
        target = floors.toDouble(),
    )
