# yt-dlp packager (Seal)

Builds the single-file `yt-dlp` executable that Seal runs on-device
(`python libpython.so <yt-dlp> [args]`, see `youtubedl-android`) from the
vendored source tree at `../yt-dlp` (a full `yt-dlp/yt-dlp` checkout).

This replicates the relevant part of upstream `yt-dlp/Makefile`
(`lazy-extractors` + `yt-dlp.zip` + `yt-dlp` zipapp targets) in pure Python
so it works on Windows / Linux / macOS without `zip`/`touch`/`cat`.

## Layout

- `build_yt_dlp.py` — the builder. Reads `yt_dlp/version.py` for the version,
  regenerates `yt_dlp/extractor/lazy_extractors.py` (gitignored upstream),
  packs `yt_dlp/**/*.py` into a deterministic zip and prepends the
  `#!/usr/bin/env python3` shebang. Emits three files next to `--out`:
  `yt-dlp`, `yt-dlp.version`, `yt-dlp.sha256`.

## Manual usage

```bash
# from the Seal repo root
python tools/yt-dlp-packager/build_yt_dlp.py \
  --source yt-dlp \
  --out /tmp/ytdlp-test/yt-dlp

python /tmp/ytdlp-test/yt-dlp --version
python /tmp/ytdlp-test/yt-dlp --ignore-config --list-extractors | wc -l
```

`--no-lazy` skips regenerating `lazy_extractors.py` (slower startup, useful
when the tree already has it or for a quick sanity build).

Requirements: host Python >= 3.10 (matches `yt-dlp/pyproject.toml`
`requires-python`). No third-party packages needed — only stdlib
(`zipfile`, `hashlib`, `subprocess`).

## Gradle integration

`app/build.gradle.kts` registers `bundleYtDlpFromSource` (Exec):

- inputs: `../yt-dlp/yt_dlp`, `../yt-dlp/devscripts/make_lazy_extractors.py`;
- outputs: `$buildDir/generated/ytdlp/yt-dlp{,.version,.sha256}`,
  registered as an `assets` srcDir, so the APK contains
  `assets/yt-dlp/yt-dlp` (+ `.version` / `.sha256`);
- `preBuild.dependsOn(bundleYtDlpFromSource)`.

If the `yt-dlp/` checkout is absent or host Python is missing, the task
logs a warning and the build continues — the APK then falls back to the
`youtubedl-android` AAR's bundled `R.raw.ytdlp` plus the normal OTA updater
(`UpdateUtil.updateYtDlp`). When the checkout is present it becomes the
single source of truth: `BuildConfig.YT_DLP_BUNDLED_VERSION` is read from
`yt_dlp/version.py` at configuration time.

## Runtime installation

At startup (`App.onCreate`, after `YoutubeDL.init`), `YtDlpBundle`
(`app/.../util/YtDlpBundle.kt`) copies the asset over
`noBackupFilesDir/youtubedl-android/yt-dlp/yt-dlp` when the bundled version
is newer than the installed marker (SHA-256 verified, atomic replace).
This intentionally reuses the exact paths published by
`youtubedl-android` (`YoutubeDL.baseName` / `ytdlpDirName` / `ytdlpBin`),
so no fork of that library is required.

## Updating the vendored source

```bash
cd yt-dlp
git pull origin master
python devscripts/update-version.py   # refreshes yt_dlp/version.py
cd ..
python tools/yt-dlp-packager/build_yt_dlp.py --source yt-dlp --out /tmp/ytdlp/yt-dlp
```

`BuildConfig.YT_DLP_BUNDLED_VERSION` and the troubleshooting page pick up
the new version automatically on the next build. Keep an eye on
`yt-dlp/pyproject.toml` `pin` (`yt-dlp-ejs==0.8.0` etc.): the Android
Python env (`libpython.zip.so` in the AAR) ships its own copies of
`requests`/`urllib3`/`websockets`/`mutagen`/`certifi`, so a yt-dlp bump
that raises those minimums may require a matching AAR bump
(`gradle/libs.versions.toml` `youtubedlAndroid`).
