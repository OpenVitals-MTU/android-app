import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';

void main() {
  Future<List<String>> tableNames(OpenVitalsDatabase db) async {
    final rows = await db
        .customSelect(
          "SELECT name FROM sqlite_master WHERE type='table' AND name='beverages'",
        )
        .get();
    return rows.map((row) => row.read<String>('name')).toList();
  }

  test('legacy version one migrates to beverage schema version three', () async {
    final db = OpenVitalsDatabase(NativeDatabase.memory());
    addTearDown(db.close);

    final migration = OpenVitalsDatabase.migration1To3;
    expect(migration.startVersion, 1);
    expect(migration.endVersion, 3);
    expect(migration.sql, contains('CREATE TABLE IF NOT EXISTS `beverages`'));

    await migration.migrate(db);
    expect(await tableNames(db), contains('beverages'));
  });

  test('legacy version two migrates to beverage schema version three', () async {
    final db = OpenVitalsDatabase(NativeDatabase.memory());
    addTearDown(db.close);

    final migration = OpenVitalsDatabase.migration2To3;
    expect(migration.startVersion, 2);
    expect(migration.endVersion, 3);
    expect(migration.sql, contains('CREATE TABLE IF NOT EXISTS `beverages`'));

    await migration.migrate(db);
    expect(await tableNames(db), contains('beverages'));
  });
}
