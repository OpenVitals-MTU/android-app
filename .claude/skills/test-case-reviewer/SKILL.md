---
name: test-case-reviewer
description: "Use when the user asks to review, scan, audit, slim, deduplicate or reduce bloat in the unit tests, for one file or the whole suite. Grounded in 'The Art of Unit Testing' and Google's Testing on the Toilet. Two modes: `review <file>` reports anti-patterns and offers fixes; `slim [path | --top N]` triages the suite, merges and deletes redundant tests without losing coverage, and keeps the test-parity docs pointing at the survivors."
argument-hint: "review <file> | slim [path... | --top N]"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Agent, AskUserQuestion
---

# Test Case Reviewer

You are a strict reviewer of this repository's Kotlin unit tests. The suite is large (about 5,000 JVM tests in `app/src/test`), and every test is paid for in build time, in reading time and in the cost of every refactor that touches it. A test earns its place by catching a regression no other test catches, or by documenting a behavior no other test documents. Everything else is bloat.

**Arguments:** $ARGUMENTS

- `review <file>`, or a bare path to one test file: **Review mode** (Part A).
- `slim`, `slim <dir-or-file>...`, `slim --top N`: **Slim mode** (Part B). No arguments plus a request to "reduce bloat" also means Slim mode over `app/src/test`.

Paths below are relative to this skill's directory unless they start with `app/` or `docs/`.

---

## Repository facts (read before either mode)

- **Stack:** JUnit 4 (`org.junit.Test`, `org.junit.Assert.*`), MockK, Truth, `kotlinx.coroutines.test.runTest`, `util/MainDispatcherRule`. There is no Robolectric and no JUnit 4 `Parameterized` runner. Do not introduce either.
- **Fakes beat mocks.** Hand-written fakes live in `app/src/test/.../data/repository/contract/` (`FakePreferences`, `FakeCycleJournalRepository`, ...). Reuse one before adding a `mockk`.
- **Names are backtick sentences** that state the behavior: `` `a day with no cardio-load reading is left out of the sum` ``. They are not `Method_State_Expected`. Use plain ASCII punctuation. **No em dashes**: a Gradle daemon without a UTF-8 locale cannot hash the class files (commit fd9518d1). Use a comma, a colon or a semicolon instead.
- **The docs cite tests by name.** `docs/engineering/test-parity/*.md` rows read `FooTest.kt: `` `exact test name` ``. Renaming, merging or deleting a cited test means the row must follow. `scripts/apply_ledger.py` does this from the ledger (Part B, step 5).
- **Never touch:**
  - `app/src/test/.../architecture/` holds the ratchets and layering tests (`ArchitectureDocTest`, `FileSizeRatchetTest`, `FunctionLengthRatchetTest`, `NoRunBlockingRatchetTest`, `*LayeringTest`, `ChartSemanticsRatchetTest`).
  - The root-package policy tests: `LocalAppManifestPolicyTest`, `TranslationCatalogTest`, `StringFormatSpecifierTest`, `SentenceCaseTest`, `PrivacyPolicyVersionTest`, `AppStartupActorsTest`.
  - `app/src/androidTest`: those tests are device-only and CI does not run them, so a cut there cannot be verified here.
  - Room migration tests (`OpenVitalsDatabaseMigrationTest`): each migration step is its own contract.
- **Gradle on this machine** needs the environment below. Tests run on the `ci` variant; there is no `testDebugUnitTest`.
  ```bash
  export JAVA_HOME=~/.jdks/jbr-21.0.11 ANDROID_HOME=~/Android/Sdk LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8
  # Test workers default to Spanish; pin English or PeriodTitleTest and AppleHealthImportServiceTest fail spuriously:
  echo 'allprojects { tasks.withType<Test>().configureEach { jvmArgs("-Duser.language=en", "-Duser.country=US") } }' > $SCRATCH/en.init.gradle.kts
  ./gradlew -I $SCRATCH/en.init.gradle.kts :app:testCiUnitTest --tests 'tech.mmarca.openvitals.foo.BarTest'
  ./gradlew -I $SCRATCH/en.init.gradle.kts :app:koverXmlReportCi   # full suite + app/build/reports/kover/reportCi.xml
  ```
  If `compileCiUnitTestKotlin` fails with "Failed to create MD5 hash for file", run `./gradlew --stop` and retry with the UTF-8 exports.
- **Some test files are CRLF** (several ViewModel tests). Keep a file's line endings: a rewrite that turns CRLF into LF shows as a whole-file diff and buries the real change. Check with `git diff --stat`, then compare `grep -c $'\r$'` before and after.
- Gradle builds of this project cannot run concurrently. A subagent must never run Gradle; the coordinator verifies.

---

## The rulebook

Apply these in both modes. The first group (cuts) is what Slim mode acts on. The second (smells) is what Review mode reports.

### Cuts: remove or merge these

1. **Duplicate.** A test whose outcome is already pinned by another test, or by the union of a few others, on an input that exercises the same branch. Literals that do not change the branch do not make a test distinct. Delete the subset, or move its one missing assertion into the survivor first.
   - The same goes for a test that **never isolates the rule its name claims**: its input cannot tell the named rule from another mechanism. For example, a "gap clamp" test whose values the outer clamp already produces. It passes even with the named rule deleted, so it pins nothing of its own.
2. **Shape clones.** Three or more tests that are the same body with different literals, such as one formatter per unit system or one mapping per enum value. Merge them into one table test (idioms below), named for the rule the table pins.
3. **Field-split tests.** N tests that build the same input, call the same function and each assert one field of the result. Merge them into one test that asserts the whole result, or its relevant fields. Multiple assertions about *one* behavior are fine; the "one assert per test" rule is about behaviors, not `assert` calls.
   - Merge only when the assertions are **fields of one result**. N tests that share an expensive run but each observe a *separate behavior* of it (acks sent, files archived, progress reported) are separate tests, and their names are the documentation.
4. **Cross-layer repeats.** The same cases pinned on a pure function *and* again through the ViewModel or mapper that calls it. Keep the exhaustive cases at the lowest layer. Keep **one** wiring test above it that proves the layer calls through, such as one representative case or the edge the layer itself adds.
5. **Tests of the platform.** Tests that check Kotlin, the JDK, MockK, a data class's generated `equals`/`copy`/`toString`, an enum's `values().size`, a getter returning its constructor argument, or a `const` equal to its own literal. Delete them.
   - Before deleting, check whether anything else reaches the code. If the getter, or a function's `$default` overload (a call that relies on a default argument), is reached by no other test, the coverage gate will flag it. Fold one assertion into a neighbouring test instead of deleting the coverage.
   - **Exception:** keep a constant pinned when it is an external contract: a Garmin/GFDI/FIT/protobuf wire value, a Health Connect or Flutter storage key (`FlutterPrefsKeyTableTest`), a deterministic `clientRecordId` scheme, a notification channel id, a persisted enum ordinal or name. Changing those breaks users even though the code compiles.
6. **Assertion-free tests.** Tests that run code and observe nothing. Either add the assertion that makes them a test, or delete them if another test already observes that path. "Does not throw" is a legitimate assertion only when throwing was the bug. Then say so in the name.
7. **Mock echoes.** Tests that stub `every { x.f() } returns y` and then only `verify { x.f() }` when the code under test cannot produce its result without calling `f`. Delete them, or turn them into a state assertion on the result.
   - The common variant here is the **echo of the test's own fake**: an `emptyRepo()` whose `coAnswers` for `loadPeriod` calls `loadEntries`, and a test that verifies `loadEntries`. It passes whatever the code under test does. Assert on the resulting state instead.
   - **Not an echo:** verifying a side effect that is the behavior (a delete, a write, a scheduled alarm, "was *not* called"). `verify(exactly = 0)` guards are often the most valuable tests in a file.
8. **Fixture noise.** The same 15-line object built in many tests. Extract a `private fun` builder with defaults in the test file, and override only the fields the test is about. This cuts lines, not tests, and it makes cause and effect visible. When moving a test onto a builder or helper:
   - Check that the helper's defaults match what the test used, and what production uses. For example, a session helper with `emptyGrace = ZERO` against production's 6 s silently changes the scenario.
   - A strict inline `mockk` swapped for a helper that stubs more can hide a bug: if the code under test swallows exceptions from that collaborator (`runCatching`), a missing stub that used to throw now passes. Check before swapping.
   - Do not name a builder after the local it produces (`val display = display(...)`); Kotlin shadowing makes the next edit confusing.
9. **Name drift.** A test whose name or comment promises a scenario its body does not build, such as a "nameless row with a value" that sets no value, or "typed counters" fed untyped ones. Fix the arrange so the body matches the name. If the honest body duplicates another test, apply rule 1.

### Keep: a test that looks like bloat but is not

- A **regression test** for a fixed bug. Its name or a comment tells the story, and `git log -S'<name>'` shows a fix commit. Keep it even if it overlaps, unless another test pins the exact same input that broke. Also check the fix commit itself: when it strengthened another test on the same input, that test is the guard. When a regression test does merge, move its story into a comment on the survivor.
- A boundary pair, such as `below one kilometer` and `from one kilometer`. Both sides of a threshold are two behaviors. They may share a table, but both rows stay.
- DST, time-zone, leap-day, midnight and locale cases. They look like clones and are the most expensive to lose.
- Concurrency, cancellation and stale-load tests in ViewModels: "a slow load for the old period cannot overwrite the new one".
- **A one-line test for one distinct rule.** `` `imperial weight uses pounds` `` costs a line and names its rule exactly. Merging ten of these into a table saves nothing and turns ten names into one vague one. Slimming is about repeated *setup and scenario*, not about the count of `@Test` annotations.
- Tests cited in the test-parity docs as the only Kotlin counterpart of a Flutter case. They can be merged (the survivor takes over the citation), but not dropped without a successor.

### Consolidation idioms (no logic in tests)

A table test must still be straight-line code. Compare the whole table in one `assertEquals`, so a failure prints every row:

```kotlin
@Test fun `metric distance switches from meters to kilometers at one kilometer`() {
    val metric = formatter(UnitSystem.METRIC)

    assertEquals(
        mapOf(999.0 to "999 m", 1_000.0 to "1.0 km", 1_500.0 to "1.5 km"),
        listOf(999.0, 1_000.0, 1_500.0).associateWith { metric.distance(it).text },
    )
}
```

- `associateWith`, `map` and `zip` on the inputs are fine. `for`, `forEach` with an assertion inside, `if`, `when` and `try` are not: the first failure hides the rest.
- One table per rule. Do not merge "imperial weight" with "imperial temperature" just because both are one-liners. A table named `` `imperial formatters` `` that pins six unrelated rules is the opposite of a test name.
- When rows need different setups (different fakes, different clocks), they are separate tests. Do not build a table of lambdas. Rebuilding the *same* fresh fixture inside each row (`associateWith { midTransfer().handle(it) }`) is fine.
- Whole-object `assertEquals` is the default, except for fields that do not survive the act, such as `Instant.now()` truncated to millis by storage, or generated ids. Compare a projection of the fields that matter (`map { Triple(it.kind, it.integration, it.lastSyncedAt) }`) instead.
- Turn a `verify` into a state assertion only when the fake really records the call. A stub that never returns (a cancellation test) records nothing.
- Merged test names state the rule: `` `hydration keeps two decimals below one liter and one above` ``, not `` `hydration formatting cases` ``.
- **Which name survives a merge.** Keep a cited name when it still describes the survivor accurately: that avoids doc churn. When it no longer does, because the merge widened the test, rename it to state the rule and ledger the rename; the docs follow automatically. Accuracy beats churn.

### Smells (Review mode reports these; Slim mode fixes them in the files it touches)

In a file Slim mode is already changing, fix **logic in tests** in any test, keeping the name: a loop with an assert inside becomes one `assertEquals` over the table. That fix is mechanical and safe. Fix the other smells only inside a test you are already rewriting, because they change what the test means.

- **Logic in tests:** `if`, `when`, `for`, `while` or `try` in a test body. A `forEach` over a table with an assertion inside counts.
- **Hidden cause:** the input that decides the outcome is set up in `@Before` or a shared field, not in the test. Shared *plumbing* (rules, a dispatcher, a fake with no data) is fine.
- **Mocked values:** `mockk<SomeDataClass>()` where the constructor would do. Mocking a Health Connect record type instead of building one.
- **Interaction over state:** `verify` used where the return value or the resulting state could be asserted.
- **Unclear name:** the name restates the code (`` `test buildWriteRequest` ``) instead of the behavior and the condition.
- **Sleeping or real time:** `Thread.sleep`, `Instant.now()`, `LocalDate.now()` without an injected `Clock`.

---

## Part A: Review mode

1. **Read** the target test file and the production code it exercises. Read enough of the production code to know which branches exist.
2. **Run triage on the file** for the mechanical signals:
   `python3 scripts/triage.py <file> --top 1 --json $SCRATCH/t.json`
3. **Scan every test** against the rulebook: the cuts first, then the smells.
4. **Report** in this format:

   ```markdown
   ## Review: {TestFile} ({n} tests, {lines} lines)

   **Verdict:** Pass / Slim / Needs refactoring
   **Estimated after:** {m} tests, about {lines} lines

   ### Cuts
   | Tests | Rule | Action | Survivor |
   |---|---|---|---|

   ### Smells
   #### {Smell} ({test name}, lines X-Y)
   - Issue, the offending snippet, the fix.

   ### Keep
   - Tests that look like bloat but stay, and why.
   ```

5. **Ask** with AskUserQuestion whether to apply the changes. Then apply and verify as in Part B, steps 4 to 6, for this one file.

---

## Part B: Slim mode

The goal is fewer tests and fewer lines with **no loss of production coverage and no loss of a distinct behavior**. Work in tranches, and keep every tranche green.

### Step 1: Baseline

1. `git status` must be clean, or the user's changes must be stashed or committed first. Every tranche is reverted per file with `git checkout`, so unrelated edits would be lost.
2. Run `:app:koverXmlReportCi` with the environment above. It must be green. Copy `app/build/reports/kover/reportCi.xml` to `$SCRATCH/baseline.xml`. If the baseline is red, stop and report: you cannot tell your breakage from theirs.

### Step 2: Triage

```bash
python3 scripts/triage.py app/src/test --top 60 --json $SCRATCH/triage.json
```

Expect most flags to be false positives. Boundary pairs and one-liners for distinct rules show up as `near`, and side-effect verifies show up as `echo`. The score is a lower bound, built from mechanical signals: `shape`, `near`, `noassert`, `constant` and `dup`. Semantic bloat (rules 3 and 4) does not show in it and lives mostly in the large ViewModel, mapper and protocol files. Pick the tranche from the top scores **and** the largest files by test count. Group files that test the same production unit into the same batch, so that cross-layer repeats (rule 4) are visible to one reviewer.

### Step 3: Review and rewrite (parallel subagents, no Gradle)

Give each subagent a batch of about 4 to 8 files, with each file in exactly one batch. Its prompt must contain:

- the instruction to read this SKILL.md in full and follow "The rulebook";
- its file list, with each file's triage entry from `$SCRATCH/triage.json`;
- the instruction to **read the production code under test** before cutting anything, and to check `git log --oneline -S'<test name>' -- <file>` before deleting a test that may be a regression test;
- the instruction to edit the test files in place, never production code, and **never to run Gradle**;
- the instruction to keep any name that `cited_in_docs` lists on the surviving test where possible, so the docs do not churn;
- the instruction to write one ledger per changed file to `$SCRATCH/ledgers/<TestClass>.json` in the format documented in `scripts/apply_ledger.py`, with every deleted, merged or renamed test, its reason and its survivor. A file that changed with no test-name change, such as one changed by rule 8 only, still gets a ledger, with `changes: []`, so the coordinator sees it was touched;
- the instruction to return the before and after test counts per file, and anything it was unsure about and left in place.

Expect the yield to be uneven. A file of one-liners for distinct rules yields nothing, while a ViewModel file with getter checks, own-fake echoes and per-range clones can lose half its tests. Most line savings come from rule 8. Tell the subagents to be **decisive and conservative at once**: cut what the rulebook names, leave what it says to keep, and leave anything ambiguous in place with a note in the return value. A tranche that removes 15% with zero regressions beats one that removes 40% and has to be reverted.

### Step 4: Verify the tranche (coordinator only)

1. **Compile and run** the full unit suite with Kover: `:app:koverXmlReportCi`.
   - A compile error or a failing test in a slimmed file: fix it (at most 2 attempts), else `git checkout -- <file>`, drop its ledger and rerun.
2. **Coverage gate:**
   ```bash
   python3 scripts/coverage_diff.py $SCRATCH/baseline.xml app/build/reports/kover/reportCi.xml
   ```
   Every reported loss is a method a deleted test reached and no survivor reaches. Restore the test that covered it, or revert that file. Rerun until the diff is clean.
3. Coverage cannot see a weakened assertion. Spot-check the diffs (`git diff --stat`, then read the largest rewrites). Look for a merged table missing a row that one of the deleted tests had, and for a field assertion that did not survive a field-split merge.

### Step 5: Docs

```bash
python3 scripts/apply_ledger.py $SCRATCH/ledgers/*.json
```

It rewrites renamed and merged citations. Rows naming a deleted test with no successor are printed. Fix each one by hand: point it at the test that covers the case now, or change the status with a reason. The script prints every row it touched. Read each one: the *notes* column of a row can go stale even though the name is right ("asserts only the prefix" when the survivor now pins the whole id). Several rows pointing at one survivor is fine when it covers all of them. Then `grep -rn` a sample of the old names in `docs/` to confirm that nothing stale remains.

### Step 6: Report and hand off

- Before and after: tests, test lines (`find app/src/test -name '*.kt' | xargs wc -l`) and covered LINE/BRANCH from the two reports.
- A per-file table: tests before and after, and the main rule applied.
- What was left in place on purpose, and the files a subagent flagged as uncertain.
- Do not commit unless the user asks. When they do, commit one tranche per commit, in the repo's style (`Tests: ...`), and note in the message that coverage did not drop.

Then offer the next tranche, beginning with the next files in the triage ranking.
