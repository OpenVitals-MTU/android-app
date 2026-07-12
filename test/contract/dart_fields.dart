import 'dart:io';

import 'package:analyzer/dart/analysis/features.dart';
import 'package:analyzer/dart/analysis/utilities.dart';
import 'package:analyzer/dart/ast/ast.dart';

/// Reads the FIELD NAMES a Dart class declares, straight from the source.
///
/// The source, not the generated `.freezed.dart` and not the generated
/// `messages.g.dart`: the Pigeon contract and the freezed factory ARE the two
/// declarations we are comparing, and generated files can only agree with
/// whatever they were generated from. A field that never made it into the
/// contract cannot appear in the code generated from it — which is exactly why
/// `SleepDataMsg` could silently lack `recordingMethod` while every layer
/// compiled.
///
/// Handles the two shapes this repo actually uses:
///
///   * a Pigeon message — plain `final` fields on a plain class;
///   * a domain model — a freezed `const factory X({required T a, T? b}) = _X;`
///     (the fields exist only as that factory's named parameters), or an
///     ordinary class taking `this.a` in its constructor.
Map<String, Set<String>> dartClassFields(String path) {
  final unit = parseFile(
    path: File(path).absolute.path,
    featureSet: FeatureSet.latestLanguageVersion(),
  ).unit;

  final classes = <String, Set<String>>{};
  for (final declaration in unit.declarations) {
    if (declaration is! ClassDeclaration) continue;
    final fields = <String>{};

    for (final member in declaration.body.members) {
      // Plain `final String? title;` — the Pigeon shape.
      if (member is FieldDeclaration && !member.isStatic) {
        for (final v in member.fields.variables) {
          fields.add(v.name.lexeme);
        }
      }
      // Constructor parameters — the freezed factory shape, and `this.x`.
      if (member is ConstructorDeclaration) {
        for (final p in member.parameters.parameters) {
          final name = _parameterName(p);
          if (name != null) fields.add(name);
        }
      }
    }
    // A private backing field is an implementation detail, not part of the
    // contract; and freezed's `_` private constructor contributes nothing.
    fields.removeWhere((f) => f.startsWith('_'));
    classes[declaration.namePart.typeName.lexeme] = fields;
  }
  return classes;
}

String? _parameterName(FormalParameter parameter) {
  var p = parameter;
  if (p is DefaultFormalParameter) p = p.parameter;
  if (p is FieldFormalParameter) return p.name.lexeme;
  if (p is SimpleFormalParameter) return p.name?.lexeme;
  if (p is SuperFormalParameter) return p.name.lexeme;
  return null;
}
