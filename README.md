# DonutMaparts

See what is on a DonutSMP map before you buy it.

DonutMaparts is two things:

- **A website**, the mapart wall at **https://exdede.xyz/maparts**. Every map
  art the mod has caught on DonutSMP, over 20,000 pictures, each with the
  in-game map IDs it was found on. It is a web page, so it works the same for
  Java, Bedrock, console and phone players. No mod and no account needed.
- **A mod** for Minecraft Java Edition (Fabric, client-side). Hover a map on
  `/ah` or in a chest to see its picture, get an alert when a map you are
  hunting turns up, and add the new maps you walk past to the wall.

| I want to | Go here |
| --- | --- |
| See what is on a map, on any platform | [Guide](https://exdede.xyz/maparts/guide) |
| Look up a map ID | [Map ID lookup](https://exdede.xyz/maparts/lookup) |
| Browse map art by category | [The wall](https://exdede.xyz/maparts) |
| See the most popular maps | [Best of](https://exdede.xyz/maparts/best) |
| Download the mod | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/donutmaparts) |
| Install it on my launcher | [Install steps](https://exdede.xyz/maparts/install) |

## On Bedrock, console or phone

The mod is Java only. The website is not. Open it in any browser; a phone
next to the console does the job.

- Got the map's ID? It is on the map's tooltip in Java, otherwise ask the
  seller. Type it into the [lookup](https://exdede.xyz/maparts/lookup) and you
  get the picture saved for that map, plus the other map IDs the same picture
  was found on.
- A lookup can be linked directly:
  https://exdede.xyz/maparts/lookup?id=262563
- No ID? Browse [the wall](https://exdede.xyz/maparts) by category until you
  spot the picture.

What the website is not:

- It finds maps by map ID, wall number or category. There is no search by
  name or by builder.
- "Seen on 10 different maps" means the mod found that picture on 10 map IDs,
  and "spotted 380 times" is how often mod users ran into it. Neither is the
  number of copies on the server, so it cannot tell you how rare a map is.
- It is a catalogue of pictures taken off map items. No bases, no
  coordinates, no players, and nothing on it is for sale.

## Install the mod

Download it from
**[CurseForge](https://www.curseforge.com/minecraft/mc-mods/donutmaparts)**.
The same jars are on
[GitHub releases](https://github.com/exdede/Donutmaparts/releases/latest).
DonutMaparts is not on Modrinth.

There is one build per Minecraft version. Take the one that matches your game:

| Minecraft | Jar name ends in | Java |
| --- | --- | --- |
| 1.21.11 | `+1.21.11.jar` | 21 |
| 26.1 to 26.1.2 | `+26.1.2.jar` | 25 |
| 26.2 | `+26.2.jar` | 25 |
| 26.3 | `+26.3.jar` | 25 |

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.0 or newer for your
   Minecraft version.
2. Drop these into your `mods` folder, all for the same Minecraft version:
   - DonutMaparts
   - [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
   - [malilib](https://www.curseforge.com/minecraft/mc-mods/malilib)
   - (optional) [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) to reach the settings from the mods list
3. Launch. Join DonutSMP. Hover a map.

On Prism, the CurseForge app, the Modrinth app or Lunar? There are
[steps for each launcher](https://exdede.xyz/maparts/install).

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

## Map preview

Hover any filled map, in a chest, a shulker or the **Auction House**, and the
mod shows you the actual picture: inside the tooltip, in a bigger panel next to
the GUI, or both.

The preview is the map exactly as your game has it, drawn the same way an item
frame draws it, so a map that got griefed shows up griefed. Only when your game
has no data for a map at all does the mod fall back to the newest capture of it
on the DonutMaparts wall, marked "via exdede.xyz/maparts" because it may be
older than the map in game. That fallback waits a moment for the server first,
is DonutSMP-only by default, and can be turned off.

Everything is configurable in the **Map Preview** tab, and the unbound
"Toggle Map Preview" keybind flips it on and off.

## Adding to the wall

While you play on DonutSMP, the mod fingerprints the maps that render on your
screen and, if one is new, uploads it to the wall. No screenshots, no commands,
nothing to click. Duplicates are skipped server-side and the mod never sends
anything it should not.

Uploading is on by default. Turn off **General > Enabled** if you only want
previews and tracking; both keep working without it.

## Account linking and collections

Paste the link code from **https://exdede.xyz/maparts** into the settings
screen to tie your Minecraft account to your web account. Once linked, flip
on **auto collect** (off by default) and every new mapart the mod sees on
DonutSMP gets added to your online collection automatically, independent of
the tracking wishlist above.

## Settings

Open the config two ways:

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

**Map Preview tab:**

| Setting | Default | What it does |
| --- | --- | --- |
| Preview enabled | on | Master toggle for map previews |
| Preview mode | Inside tooltip | Inside tooltip, side panel, or both |
| Trigger | Always | Always, or only while holding Shift, Ctrl or Alt |
| Scope | All inventories | All inventories, containers only, or Auction House only |
| Tooltip size | 64 | Picture size inside the tooltip, 32 to 256 GUI pixels (128 = one map pixel per GUI pixel) |
| Panel size | 128 | Picture size in the side panel, 32 to 512. Shrinks automatically when there is no room |
| Panel position | Right of GUI | Left/right/above/below the GUI, any screen corner, or follow the cursor. Flips sides when there is no room |
| Panel margin | 6 | Gap between the panel and the GUI, cursor or screen edge |
| Panel padding | 4 | Space between the panel frame and the picture |
| Frame | Map paper | Vanilla map paper (like an item frame), flat border, or none |
| Flat border color / thickness | gray, 1 | ARGB color and 1 to 8 pixels |
| Background color | near black | Shows through transparent map pixels and around the panel |
| Label | None | Nothing, the map ID, or the map ID plus where the picture came from |
| Label color | light gray | ARGB color of the label |
| Hide map markers | on | Hide player arrows and banners on the preview |
| Wall fallback | on | Show the newest wall capture when your game has no data for the map |
| Wall fallback wait | 1000 ms | How long to wait for the server to send the map before asking the wall |
| Wall fallback only on DonutSMP | on | Map IDs from other servers would match the wrong art |
| Wall credit | on | Mark wall pictures "via exdede.xyz/maparts", since they may be older than the map in game |
| Show placeholder | on | "No preview" box when there is no picture, instead of nothing |

Plus three buttons: **Add ID**, **Bulk Add** and **Tracked IDs (N)**, the last
of which opens the list editor where you can remove entries one by one, and a
link code field to connect your web account.

## Privacy and takedowns

The mod only activates on DonutSMP. It sends a per-map fingerprint plus the map
pixels for new maps, nothing else. Map preview's wall fallback reads public
wall pictures by map ID, without your token or identity, and can be turned off. It never transmits a precomputed hash: the
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
| `versions/26.3` | 26.3 | 25 | Mojang official |

```bash
cd versions/1.21.11
JAVA_HOME=/path/to/jdk-21 ./gradlew build

cd versions/26.3
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

The built jar lands in that version's own `build/libs/`.

Why the split: Minecraft 26.1 is the first unobfuscated release, so Fabric
stopped publishing Yarn and Intermediary after 1.21.11. Builds for 26.1 and
later use Mojang's own names and the non-remapping `net.fabricmc.fabric-loom`
plugin, which is not compatible with the 1.21.11 build in the same Gradle
project. The pure-logic classes and their unit tests are identical across all
four, since they import nothing from Minecraft.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

MIT. See [LICENSE](LICENSE).
