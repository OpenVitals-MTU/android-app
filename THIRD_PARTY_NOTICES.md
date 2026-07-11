# Third Party Notices

## Bundled Bell Sounds

Bundled mindfulness timer bell sounds are included for offline timer playback.

- `bowl_struck.ogg`: `SingingBowl1.ogg` by BambooBeast, Wikimedia Commons, public domain. Source: https://commons.wikimedia.org/wiki/File:SingingBowl1.ogg
- `bowl_rubbed.ogg`: `SingingBowl2.ogg` by BambooBeast, Wikimedia Commons, public domain. Source: https://commons.wikimedia.org/wiki/File:SingingBowl2.ogg
- `bowl_bright.ogg`: `singing bowl strike sound` by inoshirodesign, Freesound, Creative Commons 0. Source: https://freesound.org/people/inoshirodesign/sounds/271370/
- `bowl_temple.ogg`: `temple bowl` by midge-f, Freesound, Creative Commons 0. Source: https://freesound.org/people/midge-f/sounds/810426/
- `bowl_harmony.ogg`: `Stereo Harmony Bells` by newagesoup, Freesound, Creative Commons 0. Source: https://freesound.org/people/newagesoup/sounds/400809/

## Bundled Background Sounds

Bundled mindfulness timer background sounds are included for offline timer playback.

- `ambient_meditation.ogg`: 20-second excerpt from `MindRapMeditation.mp3` by mellowarrior, Freesound, Creative Commons 0. Source: https://freesound.org/people/mellowarrior/sounds/169530/
- `ambient_chimes.ogg`: 20-second excerpt from `wind chimes birds squirrel.wav` by nixonvote, Freesound, Creative Commons 0. Source: https://freesound.org/people/nixonvote/sounds/516561/
- `ambient_dreamscape.ogg`: 1-minute excerpt from `DreamScape` by Bigvegie, Freesound, Creative Commons 0. Source: https://freesound.org/people/Bigvegie/sounds/591378/

## Bundled Mapsforge Render Theme

Offline `.map` / `.maps` map packs are rendered with Mapsforge's stock render theme. The
Android app resolves these resources from the `org.mapsforge:mapsforge-themes` artifact at
runtime; the Flutter app has no equivalent jar, so `assets/mapsforge/` contains a verbatim
copy of that artifact's `default.xml` render theme together with the symbol and pattern
images it references.

- `assets/mapsforge/default.xml`, `assets/mapsforge/symbols/**`, `assets/mapsforge/patterns/**`:
  extracted from `org.mapsforge:mapsforge-themes` 0.25.0. Mapsforge is licensed under the
  GNU Lesser General Public License v3.0. Source: https://github.com/mapsforge/mapsforge

## Offline Map Data

Offline base maps are rendered from map packs the user imports; no map data is bundled with
the app.

- PMTiles packs are styled with `assets/offline_maps/protomaps_base_style.json`, which
  targets the Protomaps basemaps vector tile schema. Source: https://github.com/protomaps/basemaps
- Rendered PMTiles maps display the attribution `© OpenStreetMap contributors, Protomaps`.
  OpenStreetMap data is licensed under the Open Database License (ODbL).
  Source: https://www.openstreetmap.org/copyright
