package tech.mmarca.openvitals.data.repository.contract

/** Where a workout's distance comes from. */
interface SessionDistancePreferences {
    /** A workout with a route takes the route's length as its distance. Off by default. */
    var preferRouteDistance: Boolean
}
