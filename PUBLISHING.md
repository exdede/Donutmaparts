# Publishing checklist — v0.3.0

Working notes for this release. Not part of the mod, delete or ignore once
published if you want a clean tree.

## GitHub

- [ ] Merge `feat/tracking-scope-and-auto-collection` into `main`, push.
- [ ] Tag `v0.3.0`, push tag.
- [ ] Build all three jars (`versions/1.21.11`, `versions/26.1`, `versions/26.2`).
- [ ] Create a GitHub release on tag `v0.3.0`, attach the three jars, paste
      the 0.3.0 section of [CHANGELOG.md](CHANGELOG.md) as the release body.
- [ ] Repo "About" description (right sidebar, gear icon next to About):

  > Fabric mod that catalogs maparts you see on DonutSMP to a public wall, and can alert you when a map you're hunting turns up in a chest, ender chest or shulker box.

  Topics to add if you want them searchable: `minecraft`, `fabric`, `fabric-mod`, `donutsmp`, `maparts`.

## Modrinth

New project, so it queues for manual review after your first version upload
(usually a few days, can stretch to a week). Submit once and wait, don't
resubmit.

**Project setup:**
- Name: `DonutMaparts`
- Slug: `donutmaparts` (matches the mod ID, matches the CurseForge slug below)
- Summary (short, shows in search results):

  > Catalogs maparts you see on DonutSMP to a public wall, with an in-game tracker for map IDs you're hunting.

- Categories: `utility`, `management` (pick what's closest in Modrinth's list, no perfect match exists)
- License: MIT
- Source: `https://github.com/exdede/Donutmaparts`
- Issues: `https://github.com/exdede/Donutmaparts/issues`

**Description (project body, full markdown):**

```markdown
DonutMaparts quietly catalogs every mapart you see on **DonutSMP** onto one
public wall: https://exdede.xyz/maparts

While you play, the mod fingerprints the maps that render on your screen and,
if one is new, uploads it to the wall. No screenshots, no commands, nothing
to click. Duplicates are skipped server-side.

## Mapart tracking

Put a map ID on your tracking list from the Tracking tab, one at a time or
pasted in bulk, and the mod watches for it. Open any inventory, chest,
shulker box or ender chest and it checks every slot: a match plays a sound,
posts a toast, and pulses a gold highlight on the slot until you close the
screen.

Scope it down if you want: toggle chest, ender chest, shulker box, Auction
House and "other" independently. All five start on.

Tracking works on any server, in singleplayer, and with uploads switched
off entirely. Nothing about it is sent anywhere.

## Account linking

Link your Minecraft account to your web account from the settings screen,
then optionally turn on auto-collect to add newly seen maps to your online
collection as you play DonutSMP.

## Requirements

- [Fabric API](https://modrinth.com/mod/fabric-api)
- [malilib](https://modrinth.com/mod/malilib)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional, for settings access from the mods list)

Three separate builds are published, one per Minecraft version: **1.21.11**,
**26.1**, **26.2**. Grab the one matching your game version.

## Privacy

The mod only activates on DonutSMP. It sends a per-map fingerprint plus
pixels for maps the server hasn't seen before, nothing else. Want a mapart
of yours off the wall? https://exdede.xyz/contact and it comes down, no
questions asked.

Source: https://github.com/exdede/Donutmaparts
```

- [ ] Upload the three jars as one version (`0.3.0`), each tagged with its
      matching Minecraft game version and loader `Fabric`.
- [ ] Icon: reuse `versions/*/src/main/resources/assets/donutmaparts/icon.png`.

## CurseForge

CurseForge review is faster than Modrinth's, usually same-day to a couple
days for a first submission.

- Project name: `DonutMaparts`
- Slug: `donutmaparts`
- Categories: `Map and Information`, `Utility & QOL`
- License: MIT
- Same description markdown as the Modrinth block above, CurseForge's editor
  renders standard markdown fine.
- Same three jars, one file per Minecraft version, same requirement links
  (Fabric API, malilib, Mod Menu as optional).
- Relations: mark Fabric API and malilib as **required dependencies**, Mod
  Menu as **optional dependency**, so CurseForge's installer pulls them in.

## After all three are live

Cross-link: add the Modrinth and CurseForge URLs to `README.md`'s Install
section (it already has placeholder links pointing at
`modrinth.com/mod/donutmaparts` and the CurseForge mirror, just confirm they
resolve once both projects clear review) and to the GitHub repo's About
section (the field below the description holds a second link if needed).
