# DonutMaparts

A client-side Fabric mod that quietly catalogs every mapart you see on
**DonutSMP** onto one public wall: **https://exdede.xyz/maparts**

While you play, the mod fingerprints the maps that render on your screen and, if
one is new, uploads it to the wall. No screenshots, no commands, nothing to
click. Duplicates are skipped server-side and the mod never sends anything it
should not.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.21.11**.
2. Drop these into your `mods` folder:
   - [DonutMaparts](https://modrinth.com/mod/donutmaparts) ([CurseForge mirror](https://www.curseforge.com/minecraft/mc-mods/donutmaparts))
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [malilib](https://modrinth.com/mod/malilib)
   - (optional) [Mod Menu](https://modrinth.com/mod/modmenu) to reach the settings from the mods list
3. Launch. Join DonutSMP. That is it.

Browse the wall any time at **https://exdede.xyz/maparts**.

## Settings

Open the config three ways:

- **Mod Menu**: click the gear on the DonutMaparts card.
- **Keybind**: Options > Controls > Key Binds > "DonutMaparts" > "Open Settings" (unbound by default, bind whatever you like).

Options:

| Setting | Default | What it does |
| --- | --- | --- |
| Enabled | on | Master toggle for capture and upload |
| Toast notifications | on | Occasional "catalogued N maparts" toast so you know it is working |
| Debug mode | off | Slot coloring, tooltips, and verbose console logging (development tooling) |

## Privacy and takedowns

The mod only activates on DonutSMP. It sends a per-map fingerprint plus the map
pixels for new maps, nothing else. It never transmits a precomputed hash: the
server recomputes everything and decides what is actually new.

Want a mapart of yours off the wall? Contact me via
**https://exdede.xyz/contact** and it comes down, no questions asked.

## Building from source

Requires JDK 21.

```bash
cd mod
JAVA_HOME=/path/to/jdk-21 ./gradlew build
```

The built jar lands in `build/libs/`.

## License

MIT. See [LICENSE](LICENSE).
