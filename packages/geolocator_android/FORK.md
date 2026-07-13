# Why this package is vendored

This is `geolocator_android`, forked from upstream **5.0.3** (Baseflow, MIT — see
`LICENSE`), with Google Play Services removed. `pubspec.yaml`'s `version:` is the
upstream version this fork is cut from, and `scripts/verify-geolocator-fork.sh`
refuses to release while upstream is ahead of it.

## The problem

`geolocator_android` depends on `com.google.android.gms:play-services-location`.
F-Droid's build scanner treats that as a non-free "usual suspect" and **deletes
the offending `android/build.gradle` outright**, after which Gradle fails to
configure `:geolocator_android` with a bare `NullPointerException`. There is no
way to keep the dependency and be built by F-Droid.

## Why removing it costs nothing

The app never used the fused provider. `activityRecordingLocationSettings()` sets
`forceLocationManager: true`, because the fused provider mixes in network and wifi
fixes that pass the accuracy gate without being the satellite fixes the recorder
filters on. Upstream only reaches for the fused client when
`forceAndroidLocationManager` is false, so that branch was already dead code here.

## The diff against upstream 5.0.3

1. `android/build.gradle` — dropped `implementation
   'com.google.android.gms:play-services-location:21.2.0'`.
2. `android/.../location/FusedLocationClient.java` — deleted.
3. `android/.../location/GeolocationManager.java` — dropped the two
   `com.google.android.gms.common` imports, deleted
   `isGooglePlayServicesAvailable()`, and made `createLocationClient()` always
   return `LocationManagerClient`.

Nothing else. Keep it that way: the smaller this diff, the cheaper the next
upstream merge.

## Where the guard runs

`scripts/verify-geolocator-fork.sh` checks two things, and they are enforced
differently because they age differently.

**Play Services absent + the `pubspec.yaml` override still in place.** Offline,
deterministic, and the one that actually breaks the F-Droid build. Always fatal,
everywhere — push, PR, nightly, release. It fails on the PR that reintroduced it,
which is the only cheap moment to catch it.

**Upstream is not ahead of the fork.** Needs pub.dev, and becomes true the day
Baseflow publishes — possibly a day nobody touched this repo. So it is enforced
by what is being built:

| build | staleness | why |
| --- | --- | --- |
| push / PR (`test.yml`) | not checked (`--offline`) | Upstream publishing must not turn `main` red for something no PR did. |
| nightly (`release.yml`) | **warns**, still ships (`--warn-stale`) | A nightly is a moving snapshot. Losing nightlies until someone ports the fork costs more than the staleness it nags about. |
| tagged release (`release.yml`) | **blocks** | This is the artifact users install and keep. It must not be cut from a fork that has silently stopped tracking upstream. |

The production-deployment path skips the staleness check: the tag pipeline already
ran it on the same commit, and upstream could publish between cutting that tag and
promoting it — a fork that was current when the artifact was built must not block
shipping that same artifact.

## Updating to a new upstream release

The release gate fails until this is done, which is the point — a stale fork is a
silently divergent one.

```bash
# 1. see what upstream changed
diff -ru "$(pub cache dir)/hosted/pub.dev/geolocator_android-<new>" packages/geolocator_android

# 2. re-apply the three edits above to the new sources
# 3. set version: <new> in this pubspec.yaml
# 4. sh scripts/verify-geolocator-fork.sh   # must pass
```

If upstream ever restructures the location clients, re-read `GeolocationManager`
before assuming the same three edits still apply.
