# DonutMaparts

A client-side Fabric mod that quietly catalogs every mapart you see on
**DonutSMP** onto one public wall: **https://exdede.xyz/maparts**

While you play, the mod fingerprints the maps that render on your screen and, if
one is new, uploads it to the wall. No screenshots, no commands, nothing to
click. Duplicates are skipped server-side and the mod never sends anything it
should not.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.21.11**, **26.1** or **26.2**, and take the matching build of the mod.
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

One self-contained Gradle build per Minecraft version, under `versions/`:

| Directory | Minecraft | JDK | Mappings |
| --- | --- | --- | --- |
| `versions/1.21.11` | 1.21.11 | 21 | Yarn 1.21.11+build.6 |
| `versions/26.1` | 26.1.2 | 25 | Mojang official |
| `versions/26.2` | 26.2 | 25 | Mojang official |

```bash
cd versions/1.21.11
JAVA_HOME=/path/to/jdk-21 ./gradlew build

cd versions/26.2
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

The built jar lands in that version's own `build/libs/`.

Why the split: Minecraft 26.1 is the first unobfuscated release, so Fabric
stopped publishing Yarn and Intermediary after 1.21.11. Builds for 26.1 and
later use Mojang's own names and the non-remapping `net.fabricmc.fabric-loom`
plugin, which is not compatible with the 1.21.11 build in the same Gradle
project. The pure-logic classes and their unit tests are identical across all
three, since they import nothing from Minecraft.

## License

MIT. See [LICENSE](LICENSE).
