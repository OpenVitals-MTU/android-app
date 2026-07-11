/// A formatted numeric value paired with its unit, mirroring the Kotlin
/// `DisplayValue` data class.
class DisplayValue {
  const DisplayValue(this.value, this.unit);

  final String value;
  final String unit;

  String get text => unit.trim().isEmpty ? value : '$value $unit';

  @override
  bool operator ==(Object other) =>
      other is DisplayValue && other.value == value && other.unit == unit;

  @override
  int get hashCode => Object.hash(value, unit);

  @override
  String toString() => 'DisplayValue($value, $unit)';
}
