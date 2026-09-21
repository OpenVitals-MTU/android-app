package tech.mmarca.openvitals.data.repository.contract

/** The daily drinking target, in liters. Clamped on write; read back for the stored value. */
interface HydrationGoalPreferences {

    var hydrationDailyGoalLiters: Double
}
