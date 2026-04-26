package tech.mmarca.openvitals.core.presentation

data class DisplayValue(
    val value: String,
    val unit: String,
) {
    val text: String get() = if (unit.isBlank()) value else "$value $unit"
}
