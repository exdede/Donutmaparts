# Changelog

## 0.5.0

### Tracking groups

Tracked map IDs now live in named, color-coded groups. One mapart is usually
many map IDs (every printed copy has its own), so a group is one art and
all of its copies.

- Paste IDs from clipboard: copy a list of any size and click once. No more
  splitting a big list into parts because the paste box was too small.
- Type IDs still works for a few IDs at a time. Either way you name the group.
- Groups screen: one row per group with its color, name and map count.
  Remove a whole group in one click, or remove all of them. Click the color
  to change it, click the name to rename it.
- Hover a group to see what the map looks like on the DonutMaparts wall,
  before you have found it in game.
- A found map glows in its group's color, and the toast says which group
  it belongs to.
- "Auto remove on match" now removes the whole group once any copy is found.
- Your existing tracked IDs move into a group called "Tracked" on first launch.

### Map preview

- Maps the wall took down now say "Removed from wall" instead of "No
  preview", so it is clear the map was captured and later removed, not
  that nobody has ever seen it.
- Wall previews show up faster: the wait for the server's own map data is
  0.3 seconds instead of 1 (for new installs, existing settings are kept),
  and more previews load at once.

### Linking your account

- The General tab now says where link codes come from: log in at
  exdede.xyz/account, generate a code, then click Link Account while on
  DonutSMP. Hover the button for the steps.

## 0.4.0 for Minecraft 26.3

Added to the 0.4.0 release on 2026-11-12. Same features as the other 0.4.0
builds.

- New build for Minecraft 26.3 (malilib 0.30.2, Mod Menu 21.0.0).
- The Add ID, Bulk Add and Link Account boxes take typing as soon as they
  open on 26.3. Without this they showed a cursor but ignored the keyboard
  until clicked, because 26.3 changed how the game reads typed text.

## 0.4.0

### Map preview

Hover a filled map in any inventory, including the Auction House, and see
the actual picture before you buy it.

- The picture shows inside the item tooltip, in a bigger side panel next to
  the open GUI, or both.
- The preview is the map as your game has it, drawn the same way an item
  frame draws it, so it is always the current state of the map.
- Only when your game has no data for a map at all does the mod fall back to
  the newest capture of it on the DonutMaparts wall, after a short wait for
  the server, and marks it "via exdede.xyz/maparts" since it may be older
  than the map in game. DonutSMP only by default, and it can be switched off.
- New Map Preview settings tab: preview size, panel size (shrinks to fit)
  and position (next to the GUI, any screen corner, or following the
  cursor), hold Shift, Ctrl or Alt to show, where previews appear
  (everywhere, containers only, or Auction House only), frame style (vanilla
  map paper, flat border with color and thickness, or none), background
  color, padding, map ID label and its color, hiding map markers, and the
  wall fallback's wait and credit line.
- One button resets every preview setting back to default.
- New unbound "Toggle Map Preview" keybind.

## 0.3.1

### Fixed

- Account linking now works. The code entry box only accepted 16 characters,
  so pasting a 32 character link code silently dropped half of it and the OK
  button then appeared to do nothing at all. If linking has never worked for
  you, this was why.
- A link code the mod can tell is malformed now says so, instead of leaving
  the dialog sitting there with no explanation.
- The message shown when linking is unavailable now tells you which of the
  three reasons applies: the mod is switched off, you are not on DonutSMP, or
  the connection to the backend has not finished yet. Previously all three
  claimed you were not on DonutSMP.

## 0.3.0

### Tracking scope toggles

Narrow where tracking alerts fire instead of all-or-nothing.

- New per-container toggles in the Tracking tab: plain chest, ender chest,
  shulker box, Auction House, and everything else, each on its own switch.
- All five default on, so upgrading changes nothing until you deliberately
  narrow scope.
- A plain chest and an ender chest share the same underlying container type
  and are told apart by the screen's title text.

### Account linking and mapart collection

- New link code entry in the settings screen: paste the code from the site
  to tie your Minecraft account to your web account.
- Optional auto-collect toggle, off by default, adds newly seen maps to your
  online collection while you are on DonutSMP. Independent of the tracked-ID
  wishlist and of the tracking master toggle.

### Fixed

- The not-connected toast now names DonutSMP explicitly instead of a generic
  "server" message.
- Client-side link code validation matches the backend's widened format.

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
