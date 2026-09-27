package tech.mmarca.openvitals.core.presentation

import tech.mmarca.openvitals.domain.preferences.UnitSystem

/** One place for the Celsius and Fahrenheit arithmetic every temperature field shares. */
object TemperatureUnits {
    private const val FahrenheitFreezingPoint = 32.0
    private const val FahrenheitPerCelsius = 1.8

    fun celsiusToFahrenheit(celsius: Double): Double = celsius * FahrenheitPerCelsius + FahrenheitFreezingPoint

    fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - FahrenheitFreezingPoint) / FahrenheitPerCelsius

    /** A typed value in the user's unit, as Celsius for storage. */
    fun toCelsius(value: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) fahrenheitToCelsius(value) else value

    /** A stored Celsius value in the user's unit, for a text field. */
    fun fromCelsius(celsius: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) celsiusToFahrenheit(celsius) else celsius

    /** The unit label the app prints next to a temperature. */
    fun label(system: UnitSystem): String = if (system == UnitSystem.IMPERIAL) "deg F" else "deg C"
}
