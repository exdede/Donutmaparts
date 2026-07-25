# Changelog

## 0.2.0

### Mapart tracking

Keep a list of map IDs you are hunting, and the mod shouts when one turns up.

- New **Tracking** tab in the settings screen, alongside the existing General tab.
- Add IDs one at a time, or paste a whole batch separated by commas or newlines.
  Duplicates and junk are dropped, so pasting a list twice is harmless.
- While any inventory is open, every slot is checked once per tick. A match
  plays a sound, posts a toast naming the map and where it turned up, and
  pulses a gold highlight on that slot until you close the screen.
- Each map alerts once per time you open a container, so a chest full of
  tracked maps does not turn into a slot machine.
- Optional auto remove, off by default, drops an ID from the list the moment it
  is found and saves straight away so the removal survives a crash.
- Six alert sounds to pick from, or turn the sound off and keep just the toast
  and the highlight. Tracking toasts have their own toggle, separate from the
  upload toasts.
- Tracking runs on any server and in singleplayer, and keeps working with
  uploads switched off. It sends nothing anywhere. The list never leaves your
  client.

### Minecraft 26.1 and 26.2 support

- Separate builds for 1.21.11, 26.1 and 26.2, one per download.
- 26.1 was the first unobfuscated Minecraft release, so Fabric stopped
  publishing Yarn mappings after 1.21.11. The 26.x builds are ported to
  Mojang's official mappings and the non-remapping Loom plugin. Nothing about
  this is visible in game, but it is why the downloads are split.
- The 1.21.11 build is unchanged in behaviour and still needs JDK 21. The 26.x
  builds need JDK 25 to compile.

### Fixed

- The tracking scan is wrapped so an unexpected error inside it can no longer
  interrupt mapart capture or the upload batch flush on that tick.
- Auto remove writes the config file once per tick instead of once per matched
  slot.
- The inventory slot overlay returns immediately when both debug mode and
  tracking are off, instead of inspecting every slot every frame.

## 0.1.0

First public release.

- Watches maps that render on your client while you are on DonutSMP, waits for
  each one to finish drawing, fingerprints it, and uploads anything new to
  https://exdede.xyz/maparts
- Skips anything this install has already sent, and batches uploads instead of
  firing one request per map.
- Unsent maps are saved to disk on disconnect and retried on the next launch.
- Settings via keybind or Mod Menu. Master toggle, upload toasts, debug mode.
- Inert on every server except DonutSMP, by design.
