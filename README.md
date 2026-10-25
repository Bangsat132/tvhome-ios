# tvhome-ios — an Apple TV style home screen for Android TV, with a live weather card

A home screen for an Android TV or Google TV set, built as its own app: a black field, a floating glass dock,
one continuous focus highlight, and a weather card that is the first tile and the only live one.

Built for and tested against a **POLYTRON 2K** (HKC `nagai`, Android TV 14 / SDK 34, 1080p), replacing this
repo's upstream CleanYourTV launcher. The design contract is [`DESIGN.md`](DESIGN.md): tokens, layout, motion
and the rules a reviewer can check against a screenshot.

## What it does

- **Field** — not flat black: a radial lift that follows the sun, then a scrim grade so any wallpaper keeps
  type readable.
- **Top bar** — greeting, a thin clock, the date on the left; Inputs, Wi-Fi and Settings as glass pills on the
  right. Fixed: it never scrolls, so the clock never leaves the screen.
- **Dock** — one row of app tiles on a floating glass band, filled from the grid so a short dock never shows a
  hole.
- **Weather card** — the first tile of the grid, one tile wide, live from [Open-Meteo](https://open-meteo.com/)
  (no account, no key). Temperature, condition, the city and three day marks; a sky gradient whose colour
  follows the actual sun position. It refreshes every 30 minutes while Home is on screen, and 5 minutes after
  a failure.
- **Grid** — the rest of the apps, D-pad first, alphabetical, one row at a time.

## Build and install

The APK is built by GitHub Actions, not on the TV owner's machine:

```bash
gh workflow run Build --repo <you>/tvhome-ios
gh run watch --repo <you>/tvhome-ios
gh run download <run id> --repo <you>/tvhome-ios -n tvhome-ios-apk -D out
```

Then sideload it and make it Home:

```bash
adb connect <tv ip>:5555
adb install -r out/app-release.apk
adb shell cmd package set-home-activity io.github.bangsat132.tvhome/.HomeActivity
adb shell pm grant io.github.bangsat132.tvhome android.permission.ACCESS_FINE_LOCATION  # the Wi-Fi pill's name
```

Press Home. With the stock launcher still enabled, Android may open it instead: disable it only after
`adb shell dumpsys activity activities | grep mFocusedApp` shows this app, and check the TV's Wi-Fi is not owned
by `com.google.android.tungsten.setupwraith` before disabling that.

## Settings

The launcher reads a `config.json` from its own storage, over its built-in defaults:

```json
{
  "dock_apps": ["com.netflix.ninja", "com.google.android.youtube.tv"],
  "hidden_apps": [],
  "columns": 5,
  "fahrenheit": false,
  "reduced_motion": false
}
```

The city is picked on the TV: with no city set, focus the weather card and press OK to open the picker.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Open-Meteo only: the city search and the forecast. |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | The Wi-Fi pill's state. |
| `ACCESS_FINE_LOCATION` | The Wi-Fi network *name*, which Android gates behind location. Granted by `pm grant`, never asked at runtime. |

No analytics, no ads, no background service, no telemetry.

## Licence

MIT, same as the project it grew from. The bundled Outfit typeface is under the SIL Open Font License; see
[`launcher/FONT-LICENSE-OFL.txt`][ofl] in the upstream project for the full text.

[ofl]: https://github.com/Gathin23/cleanyourtv/blob/main/launcher/FONT-LICENSE-OFL.txt
