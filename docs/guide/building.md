# Building from source

Java 17.

```bash
./gradlew build                        # mod jar: build/libs/
./gradlew :workbench:jar               # Workbench: workbench/build/libs/
./gradlew runClient                    # dev client (put Simple Voice Chat in run/mods)
./gradlew runClient -Pselftest=true    # test world with every device, screenshots in run/screenshots
```

- Textures: `python3 tools/make_textures.py`
- Banner and device renders: `tools/make_banner.py`, `tools/render_models.py`
- Workbench screenshot: `java -jar workbench/build/libs/venuesound-workbench-*.jar --snapshot out.png`

Where things are: `block/MixerLayout.java` (every console control), `block/PortLayout.java`
(every socket), `audio/SignalEngine.java` (the signal chain, shared with the Workbench),
`client/` (renderers).
