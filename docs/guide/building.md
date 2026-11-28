# Building from source

One source tree builds the mod for every supported Minecraft version (1.20.1 and 1.21.1), with
[Stonecutter](https://stonecutter.kikugie.dev/). Gradle itself needs Java 25 or newer to run (Stonecutter requires it);
it downloads the JDK each version compiles with (17 for 1.20.1, 21 for 1.21.1) if it isn't installed.

```bash
./gradlew build                        # every version: versions/<version>/build/libs/, and the Workbench
./gradlew :1.21.1:build                # one version
./gradlew :workbench:jar               # Workbench: workbench/build/libs/
./gradlew :1.21.1:runClient            # dev client for one version (Simple Voice Chat in run/1.21.1/mods)
./gradlew :1.20.1:runClient -Pselftest=true   # test world with every device, screenshots in run/1.20.1/screenshots
```

Each jar is named `venuesound-<mod version>+mc<minecraft version>.jar` and only accepts the
Minecraft versions it was built and tested for.

## How the versions share one source tree

- `src/` is committed in its **1.20.1** form. Code that differs between versions sits behind
  Stonecutter comments such as `//? if >=1.21 {` ... `//?} else {` ... `//?}`; the build rewrites
  them for each version.
- To work on another version in the IDE, switch the active version: `./gradlew "Set active project
  to 1.21.1"`. Switch back before committing: `./gradlew "Reset active project"`.
- Each version's settings (Fabric Loader, Fabric API, Java, and the `depends` ranges written into
  `fabric.mod.json`) are in `versions/<version>/gradle.properties`.
- Most of the code is shared unchanged: the console layout, sockets, signal engine, DSP, cable
  types and OSC have no Minecraft code. The differences are in registration, item data (NBT on
  1.20.1, data components on 1.21), networking payloads and rendering.

## Adding a Minecraft version

1. Add it to `versions(...)` in `settings.gradle.kts` and create `versions/<version>/gradle.properties`.
2. Build it and fix what changed, behind Stonecutter conditions.
3. Put that version's Simple Voice Chat jar in `run/<version>/mods` and run its self-test.
4. Add it to the `java-version` list in `.github/workflows/ci.yml` and `release.yml` if it needs a
   new Java, and add a `publish` matrix entry in `release.yml` (its game versions and Java).

## Other tools

- Textures: `python3 tools/make_textures.py`
- Banner and device renders: `tools/make_banner.py`, `tools/render_models.py`
- Item photos and icons (1.20.1): `./gradlew :1.20.1:runClient -Pphotos=true`, then `python3 tools/make_item_images.py`
- Demo video (1.20.1): `./gradlew :1.20.1:runClient -Pfilm=true`, then `python3 tools/make_video.py`
- Workbench screenshot: `java -jar workbench/build/libs/venuesound-workbench-*.jar --snapshot out.png`

Where things are: `block/MixerLayout.java` (every console control), `block/PortLayout.java`
(every socket), `audio/SignalEngine.java` (the signal chain, shared with the Workbench),
`client/` (renderers).
