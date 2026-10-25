---
name: tvOS Home — CleanYourTV fork
colors:
  primary: "#0A84FF"
  on-primary: "#FFFFFF"
  surface: "#0B0B0F"
  on-surface: "#FFFFFF"
  surface-raised: "#16161C"
  on-surface-raised: "#F2F2F7"
  surface-glass: "#FFFFFF"
  on-surface-glass: "#FFFFFF"
  text-primary: "#FFFFFF"
  text-secondary: "#A1A1AA"
  text-tertiary: "#6E6E76"
  scrim: "#000000"
  ring: "#FFFFFF"
  accent-sky: "#4FC3F7"
  accent-warm: "#FFB74D"
  accent-danger: "#FF453A"
  accent-success: "#32D74B"
typography:
  display-xl: "72sp / 76 line / -1.5 tracking / 200 weight"
  display-lg: "48sp / 52 line / -1.0 tracking / 300 weight"
  title-xl: "28sp / 32 line / -0.4 tracking / 500 weight"
  title-lg: "22sp / 26 line / -0.2 tracking / 500 weight"
  body: "16sp / 22 line / 0 tracking / 400 weight"
  label: "13sp / 16 line / +0.6 tracking / 600 weight / UPPERCASE"
  meta: "12sp / 16 line / +0.2 tracking / 500 weight"
spacing:
  unit: "4dp"
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "24dp"
  xl: "40dp"
  screen-edge: "48dp"
  dock-gap: "20dp"
rounded:
  knob: "18dp"
  sm: "8dp"
  md: "14dp"
  lg: "18dp"
  tile: "14dp"
  pill: "999dp"
components:
  widget-card: "background {colors.surface-raised}; corner {rounded.lg}; text {colors.text-primary}"
  widget-card-focus: "background {colors.surface-raised}; corner {rounded.lg}; glow {colors.accent-sky}; ring {colors.ring}"
  app-tile: "background {colors.surface-glass}; corner {rounded.tile}"
  app-tile-focus: "ring {colors.ring}; scale 1.06"
  dock-band: "background {colors.surface-glass}; corner {rounded.lg}"
  top-pill: "background {colors.surface-glass}; corner {rounded.pill}; text {colors.on-surface-glass}"
omitted:
  shadows: "TV panels at 2 m read box shadows as smudges; depth comes from graded scrim and a 1dp rim, not shadow"
---

## Overview

Audience: someone sitting 2–3 m from a 1080p Android TV, driving it with a five-button D-pad remote and
often a phone in the other hand. The home screen is looked at, not read: the first second decides whether
it feels clean.

Reads as: **calm, weightless, filmic**.

Taste direction: the **tvOS "shelf" model**, borrowed honestly. What is taken from tvOS: a black field that
lets the wallpaper do the talking, one hero zone on top, a dock band that floats rather than fills, content
art rising to meet the eye, and the focused item growing toward the viewer. What is deliberately not
taken: tvOS's own fonts, its SF Symbols artwork, its icon rasters, its parallax-aerial videos (a 1 GB TV
would stutter), and its top-bar chrome — a TV that switches inputs and shows Wi-Fi needs those pills
visible, so they stay.

Refused direction: "iOS icons on a black grid". Recolouring a phone launcher does not read as tvOS; the
layout rhythm and the depth grading do.

## Signature Element

**The weather card is the first tile and the only live one.** It sits at the head of the app row,
one-tile wide, with a sky gradient that changes with the actual sun position (dawn amber, day blue, dusk
rose, night indigo) and a thin horizon line at its base. Everything else on screen opens an app; this one
tells you something. It earns its place because it is the one piece of information a person checks while
walking past a TV, and because Open-Meteo gives it without an account or a key.

## Colors

Token set is in the front matter. Proportion discipline **80 / 15 / 5**: 80 % of the screen is the
background field and wallpaper scrim, 15 % is content (tiles, dock, widget), 5 % is accent — the focus
ring, the weather card's sky, and the one coloured number on the air-quality pill.

Background is not flat black: `surface` (#0B0B0F) with a radial lift toward the top-centre so the panel
never looks like a switched-off TV. Every surface token has its on-surface partner; a component picks the
pair and never mixes (`widget-card` uses `surface-raised` + `text-primary`, never `surface-raised` +
`accent-sky` as body text).

Focus ring is its own token, pure `ring` (#FFFFFF), because it must stay visible on the sky-blue weather
card, the black dock, and any wallpaper a theme brings. Contrast floor: WCAG AA — body text ≥ 4.5:1 on its
surface, the focus ring ≥ 3:1 against both the tile it outlines and the field behind it.

Semantics are the only saturated colours allowed outside the weather card: `accent-danger` for a failed
weather fetch, `accent-success` for "updated just now".

## Typography

One family: **Outfit** (already bundled in the reference launcher under the OFL, so no new font asset and
no licence question). Its geometric forms with a light weight carry the "large thin clock" idea tvOS uses,
and it has the Latin coverage the built-in locales need.

Scale is in the front matter. Rules:
- Weight, not size, carries hierarchy at the top: `display-xl` at 200 for the clock, so it reads as air
  rather than as a headline.
- `label` is the only uppercase style, used for the greeting and day names. Tracking is added because
  uppercase at 13sp sets too tight otherwise.
- Numbers are tabular for the clock and the temperature so they do not jitter as digits change.
- Line length: tile names never wrap; the tile is measured to fit its label, and a name too long is
  ellipsised at the tile edge rather than shrunk below `body`.

## Layout

Base unit 4dp, scale in the front matter. Three horizontal zones, fixed:

| Zone | Height | Contents |
|---|---|---|
| Top bar | 96dp from the top edge, `screen-edge` insets | Greeting + clock + date on the left; Inputs / Wi-Fi / Settings pills on the right |
| Shelf | fills the middle | Focused item's name in `title-xl`, its colour glow bleeding up behind it |
| Dock | 1 row, `dock-gap` between tiles, 32dp above the bottom edge | App tiles, `fill_dock` so a short dock never shows a hole |

The grid of remaining apps scrolls under the dock; Down from the dock reveals it. The scroll owner is the
grid RecyclerView alone — the top bar and dock never scroll, so the clock never leaves the screen.

Tile width is derived from `columns`, never hard-coded; the widget is exactly one tile wide so it lines up
with the grid columns in every theme.

**Order.** The weather card occupies slot 0 of the grid and the app tiles fill the slots after it in reading
order. An off-by-one here is invisible in code review and hides the apps a person actually opens, so the
slot index is written once and tested.

Safe area: a 5 % margin on all four sides because televisions overscan. Nothing important sits inside it.

**Scaling to the panel.** Every length above is authored for a 1080p panel. One factor scales them all:
`min(width, height) / 1080`, floored at 0.62 and capped at 1.0. A 720p set (the POLYTRON 2K this was built on
is 1280x720 at 213 dpi) therefore draws at 0.667, and a 4K set does not grow a 300 dp clock. Keeping one
factor rather than a second table of numbers is what lets the dock, the shelf and the grid stay in proportion
across both panel classes.

## Elevation & Depth

No shadows anywhere (see `omitted`). Depth is three things:

1. **Scrim grade** — a vertical gradient from 70 % black at the top to 92 % at the bottom, over the
   wallpaper. It gives the picture depth and keeps text readable on any wallpaper.
2. **Glass** — dock band, top pills, and the focus highlight are the same translucent white at 8–14 % over
   the scrim, with a 1dp rim at 22 %. One blur radius only, 24dp, and only where the GPU can afford it:
   `build()` checks `Build.VERSION.SDK_INT` and the `ro.hardware` class, and falls back to a flat 10 % fill
   with no blur. A 1 GB set must never pay for a blur it cannot draw.
3. **Focus scale** — the focused tile grows 6 % and lifts 2dp. That is the whole "toward the viewer" cue.

## Shapes

One radius knob, 18dp, with `sm` 8dp, `md` 14dp and the tile radius 14dp derived from it. Pills are fully
round. The dock band is `lg`, concentric with its corner tiles: 18dp band radius, 14dp tile radius, 20dp
gap, so the gap reads the same on all four sides.

## Components

- **app-tile** — the app's icon on a glass slab, with its name always present under the tile: 62 % opacity
  while unfocused, full `text-primary` while focused. The name is not revealed on focus only; a grid of
  unlabelled icons cannot be read from a sofa. Icons, not banners: a 320x180 banner squeezed into a square
  tile loses its artwork and stretches its background, which is what the first build shipped. States:
  default, focused (`surface` lightened one step + `ring` + scale 1.06), pressed (scale 1.0, no delay),
  disabled (tile at 40 % and not focusable; a disabled app is skipped by the D-pad).
- **widget-card** — one-tile weather card. States: loading (sky gradient, no numbers, a 2dp progress line),
  loaded (temp, city, condition, three day highs), error (`accent-danger` dot, "Weather unavailable", the
  last good reading kept for one hour), focused (ring + glow in `accent-sky`). Its tap opens nothing: OK
  on it refreshes.
- **dock-band** — glass strip behind the dock apps, running the full safe width with the apps aligned to its
  leading edge, so the band never shrinks around a short dock into a single island. State change only on
  focus (rim brightens one step).
- **top-pill** — Inputs, Wi-Fi, Settings. Focused: fill to 18 %, text to full white.
- **shelf-title** — the focused item's name; cross-fades over 180 ms when focus moves, never slides.
- **empty-state** — when no app is installed beyond the widget: the widget alone, centred, with one line
  "No apps yet — sideload one, then press Home".

## Motion & Interaction

Profiles: `snap` 120 ms / accelerate-decelerate; `settle` 220 ms / spring, damping 0.82, stiffness 420;
`crossfade` 180 ms / linear.

What animates: the focus highlight (spring from tile to tile, one continuous object, never a per-tile
border), the focused tile's scale, the shelf title's cross-fade, the weather card's numbers on refresh.
What never animates: the clock and date (they change, they do not move), the wallpaper, the dock band, and
anything on first layout — the first frame is final.

`reduced_motion` (a config key, on for MediaTek MT5895-class boards) replaces `settle` with `snap` and
drops the focus scale entirely. Every animation is optional; none is load-bearing for usability.

D-pad contract: Up/Down move exactly one row; the ends of the top bar never fall through to tiles; the
last focused tile is restored on return; the weather card is reachable with Left from the first app and
never traps focus.

## Do's and Don'ts

- Do let the wallpaper be the only large area of colour on screen.
- Do keep one accent per screen state (the focus ring), plus the weather card's sky.
- Do measure every text line against its tile before shipping it, at 1080p and at 720p.
- Don't use iOS system colours as a palette — `#0A84FF` here is a link/interactive blue, not a fill.
- Don't put a card, a badge or a border on anything that does not need one; the dock and the one widget
  are the only raised surfaces.
- Don't animate the clock, the date, or the wallpaper.
- Don't render a blur on a device class that cannot afford it; the flat fallback is a first-class look.
- Don't copy tvOS icon artwork, its font, or its aerial videos. Inspired by, never traced from.

## Accessibility Constraints & Accepted Debt

Constraints honoured: D-pad-only operation for every element; content descriptions on the weather card
("Palembang, 29 degrees, showers, updated 15:13") and every pill; focus order is reading order; contrast
meets AA; `reduced_motion` honoured; no element requires a long press to be useful.

Accepted debt:
- No screen-reader traversal of the grid beyond the focused tile: TalkBack on this launcher reads the
  focused tile and its position only. Reason: the grid is a custom D-pad surface; a full accessibility
  tree costs more RAM than a 1 GB set should spend.
- The card's own type is sized as a fraction of the card (0.30 of its height for the temperature down to
  0.078 for a label) rather than from the `typography:` tokens. Reason: the card is one column wide and its
  height follows the grid, so a token size that fits a 1080p card overflows a 720p one. The token scale still
  governs every TextView on the screen.
- Focus scale is 6 % rather than tvOS's larger lift. Reason: at 6 columns on a 720p panel a bigger scale
  collides with its neighbour.
- The weather card is one tile wide, so it shows the current temperature and condition only, not the
  three-day strip; the full page lives in the weather app. Reason: a wider card would break the grid rhythm
  the rest of the row depends on.

## Research Log

- **tvOS shelf layout** — observed from Apple's tvOS product pages and the tvOS HIG section on Focus and
  Selection (accessed 2026-10-25). Taken: the black field, the single hero zone, the floating dock, the
  focused item growing toward the viewer, and the one-continuous-highlight rule. Not taken: system fonts,
  icon artwork, aerial video wallpapers.
- **Apple HIG, Designing for tvOS** — the reminder that a TV UI is read at a distance and driven by a D-pad:
  minimum tap targets, focus always visible, no reliance on hover. Taken as the sizing floor (tiles ≥
  180dp at 6 columns) and the focus-ring contrast rule.
- **The reference launcher in this repo's upstream (`launcher/`)** — read its `config.xml` and `MainActivity`
  for the config-key vocabulary, the Home-guard behaviour and the focus-bounce constants it already tuned
  on real TVs. Taken: the config key names (so a theme keeps working), the `reduced_motion` idea, and the
  `focus_overshoot` spring values. Not taken: its 4-column-only grid and its tile art treatment.
- **Skipped: Apple SF Symbols and SF Pro.** Licence coverage is not transferable to an Android APK; Outfit
  under the OFL already ships in the repo.
- **Skipped: a live tvOS capture.** No Apple TV on hand, and a screenshot of someone else's UI is not a
  licence to trace it. The directions above are from published guidance, not from a captured screen.
