# Music Visualizer

A RuneLite plugin that paints music onto the game world. Nearby scenery or floor tiles flash to OSRS MIDI notes or Windows PC playback audio, within a configurable radius of the player. OSRS music and scenery remain the defaults.

## PC audio and floor tiles

- Select **Audio source → Source → PC audio** to visualize sound playing through the Windows default playback device. The plugin uses WASAPI loopback inside RuneLite; no microphone, recording-device selection, Stereo Mix, or companion app is required. Adjust **Audio sensitivity** and use the status overlay to check the signal level.
- Select **Display → Flash targets → Floor tiles** or **Both**. Tiles share the radius, colors, selection mode, and decay settings, with a separate **Floor tile opacity** setting (default 60).
- **Display → Visual activity** controls density for either audio source: 0 disables flashes, 25 preserves the original one-target density, 50 (default) lights roughly the cube root of the nearby tile count and four scenery objects per event, and 100 lights the entire scanned floor and up to twelve scenery objects. Floor coverage grows on a proportional curve so small steps above 25 stay gentle even in large scenes. Floor targets spread across the area instead of forming scan-order strips. Repeated flashes refresh a target instead of stacking opacity. Radius now extends to 50 tiles; opacity and decay remain independent controls. Whole-floor coverage is limited to loaded tiles inside that radius, and larger areas cost more rendering time.

PC mode automatically reconnects when the default playback device changes. Apps explicitly routed to a different output are not included. Exclusive-mode or protected playback may not be available. OSRS music mode remains available on other platforms.

PC audio is analyzed at the playback device's native sample rate using a windowed FFT. Energy changes in bass, midrange, and treble trigger flashes, and spectral peaks determine the colors. This estimates musical activity rather than transcribing individual notes or instruments. Capture and analysis run on a background worker; scene targeting runs on RuneLite's client thread. No audio is saved or transmitted.

Floor highlights use projected tile polygons, like tile-marker overlays; they are not depth-tested changes to ground materials. Unloaded tiles and tiles outside the current plane or radius are excluded. Targets are cleared on scene transitions, and multi-tile scenery objects are deduplicated.

## How it works

OSRS music is MIDI-driven and deterministic. The plugin:

1. Detects the active track via `Client.getActiveMidiRequests()`.
2. Asks the running RuneLite client for the track's raw cache bytes via `Client.getIndex(...).loadData(archiveId, 0)`, converts Jagex's packed format to standard SMF using a ported version of `net.runelite.cache.definitions.loaders.TrackLoader`, and parses the result with `javax.sound.midi`.
3. Runs a background scheduler that "plays through" the score in lockstep with wall-clock time since the track started — **without emitting audio**. The actual sound still comes from OSRS.
4. On each note-on event, picks one nearby object and flashes it. The color is derived from the note's pitch on a chromatic color wheel.

## Colors

**Display → Color scheme** selects Rainbow (the original pitch wheel), Ocean, Sunset, Aurora, Ember, Pastel, Forest, Ice, Neon, Autumn, Rose gold, Monochrome, complementary color pairs, or flag-inspired palettes. Flag presets include red/white/blue, Ukraine, Ireland, Italy, France, Canada/Japan, Brazil, rainbow pride, trans pride, bi pride, and pan pride. These use recognizable flag colors rather than drawing flag patterns. **Custom gradient** blends between your chosen **Custom gradient start** and **Custom gradient end** colors across the twelve pitch classes. Set both endpoints to the same color for a solid color. All schemes work with both audio sources and target types, and preserve the separate floor/scenery opacity settings. New flashes use the selected scheme immediately.

The following describes the default Rainbow scheme:

Additional presets include Jewel tones, Amethyst, Emerald, Sapphire, Ruby, Earth tones, Desert, Moss and stone, Copper and teal, Retro arcade, Synthwave, Vaporwave, Vintage, Cherry blossom, Lavender and mint, Peach and cream, Twilight, Moonlight, Citrus, and Tropical.

Each flash's color comes from the MIDI note's pitch, in three steps:

1. **Drop the octave.** Take `note % 12`, which collapses every octave onto the same 12-element wheel. Middle C and the C two octaves above it both hash to index 0 — that's why melodic motifs that repeat at different octaves show up as the same color cluster.
2. **Map the index to a hue on a chromatic color wheel.** Index 0 (C) sits at 0° (red), and each semitone adds 30°. So C♯ is orange, D is yellow, E is green, G is sky blue, B is pink-red, etc.
3. **Hold saturation and brightness constant** at 0.85 and 1.0, so only the hue varies. Notes close in pitch land close on the color wheel; notes that clash musically (a tritone apart) land on opposite sides — which is also where your eye reads "opposite color."

This mapping is sometimes called a **chromatic circle** and has a long history in music visualization (Scriabin famously used a version of it).

## Sync

When OSRS MIDI mode starts partway through a song, flashes start from the MIDI's beginning because the current playback position is unavailable. The next track change resyncs automatically. This limitation does not apply to PC audio capture.

A `Sync offset (ms)` slider lets you nudge the OSRS MIDI visualization forward or backward to match what you hear. Each new track resyncs automatically. MIDI channel selection and MIDI sync offset do not apply to PC audio.

## Development

Use JDK 17 with the included Gradle wrapper:

```sh
./gradlew test jar
./gradlew run
```

On Windows use `gradlew.bat`. The `run` task launches the development client with assertions enabled. The JAR bundles JNA 5.9.0 and its native bridge resources for Windows loopback capture. JNA's bundled license files are retained. This native integration needs RuneLite Plugin Hub review before distribution there.

Automated tests cover audio formats, native sample rates, silence, stereo/surround audio, event rate limiting, and floor/scenery selection. `LoopbackSmoke.main` is an optional manual Windows check: it captures four seconds of current playback, prints only levels/event counts, and verifies worker shutdown. It does not play or save audio. In-game rendering and device switching should also be checked manually.

## Data collection

This plugin runs entirely on your machine and sends no data anywhere.

## Special thanks to

- [runelite/runelite](https://github.com/runelite/runelite) — the `net.runelite.cache.definitions.loaders.TrackLoader` class does the heavy lifting of converting Jagex's packed music format into standard SMF bytes.
- [Rune-Status/lequietriot-RS-MIDI-Dumper](https://github.com/Rune-Status/lequietriot-RS-MIDI-Dumper) — `net/openrs/cache/track/Track.java` (Adam @ sigterm.info, 2017) is the reference decoder that pointed us at the right loader in the runelite-cache library.

## License

BSD-2-Clause. See [LICENSE](LICENSE).
