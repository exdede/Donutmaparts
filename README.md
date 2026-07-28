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
   - [DonutMaparts](https://modrinth.com/mod/donutmaparts) [CurseForge mirror](https://www.curseforge.com/minecraft/mc-mods/donutmaparts)
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [malilib](https://modrinth.com/mod/malilib)
   - (optional) [Mod Menu](https://modrinth.com/mod/modmenu) to reach the settings from the mods list
3. Launch. Join DonutSMP. That is it.

Browse the wall any time at **https://exdede.xyz/maparts**.

## Mapart tracking

Hunting a specific mapart? Put its map ID on the tracking list and let the mod
watch for you.

Open **Settings > Tracking**, then either add one ID at a time or paste a whole
batch separated by commas or newlines. Duplicates and anything that is not a
map ID get dropped, so pasting the same list twice is harmless.

From then on, whenever you open any inventory, chest, shulker or ender chest,
the mod checks every slot. If a tracked map is in there it:

- plays a sound, one of six you can pick from, or none at all
- posts a toast naming the map and where it turned up
- pulses a gold highlight on that exact slot until you close the screen

Each map alerts once per time you open a container, so a chest full of tracked
maps does not turn into a slot machine. Flip on **auto remove** and an ID drops
off the list the moment you find it.

Tracking works on any server and in singleplayer, and keeps working with
uploads turned off. It sends nothing anywhere. Your list never leaves your
client.

Want alerts only for certain containers? Flip off chest, ender chest, shulker
box, Auction House or "other" individually in the Tracking tab. All five
start on, so nothing changes until you narrow it yourself.

## Account linking and collections

Paste the link code from **https://exdede.xyz/maparts** into the settings
screen to tie your Minecraft account to your web account. Once linked, flip
on **auto collect** (off by default) and every new mapart the mod sees on
DonutSMP gets added to your online collection automatically, independent of
the tracking wishlist above.

## Settings

Open the config three ways:

- **Mod Menu**: click the gear on the DonutMaparts card.
- **Keybind**: Options > Controls > Key Binds > "DonutMaparts" > "Open Settings" (unbound by default, bind whatever you like).

**General tab:**

| Setting | Default | What it does |
| --- | --- | --- |
| Enabled | on | Master toggle for capture and upload |
| Toast notifications | on | Occasional "catalogued N maparts" toast so you know it is working |
| Debug mode | off | Slot coloring, tooltips, and verbose console logging (development tooling) |

**Tracking tab:**

| Setting | Default | What it does |
| --- | --- | --- |
| Tracking enabled | on | Master toggle for the tracking alerts |
| Auto remove on match | off | Drop an ID from the list as soon as it is found |
| Alert sound enabled | on | Play a sound on a match |
| Alert sound | Pling | Pling, Bell, XP Pickup, Level Up, Anvil Land or Arrow Hit |
| Tracking toasts | on | Show a toast on a match, independent of the upload toasts above |
| Track: plain chest | on | Alert for matches found in a plain chest |
| Track: ender chest | on | Alert for matches found in your ender chest |
| Track: shulker box | on | Alert for matches found in a shulker box |
| Track: Auction House | on | Alert for matches found in the Auction House |
| Track: other | on | Alert for matches found in any other container |
| Auto collect | off | Add newly seen maps to your online collection while on DonutSMP (needs a linked account) |

Plus three buttons: **Add ID**, **Bulk Add** and **Tracked IDs (N)**, the last
of which opens the list editor where you can remove entries one by one, and a
link code field to connect your web account.

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

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

MIT. See [LICENSE](LICENSE).
