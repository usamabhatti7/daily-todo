# Daily To-Do

By **UB Labs**

![Daily To-Do logo](logo/logo-512.png)

A small daily task tracker: one list per day, priorities, a progress bar, moving unfinished tasks forward, and clearing completed ones. Tasks are stored on your device only.

## Download for Windows

**[Download DailyTodo-Setup.exe](https://github.com/usamabhatti7/daily-todo/releases/latest/download/DailyTodo-Setup.exe)** (always the latest version)

1. Click the link above and save the file.
2. Double-click `DailyTodo-Setup.exe`. If Windows SmartScreen warns, click **More info**, then **Run anyway**. The app is not code-signed yet.
3. Open **Daily To-Do** from the Start menu.

No installer needed? Get [DailyTodo-Portable.exe](https://github.com/usamabhatti7/daily-todo/releases/latest/download/DailyTodo-Portable.exe). All versions and checksums are on the [Releases page](https://github.com/usamabhatti7/daily-todo/releases). To uninstall, use Windows **Settings > Apps > Installed apps**.

| Folder | What it is |
|---|---|
| `web/` | The app (HTML, CSS, JS, no external requests). Used by the Windows app and deployed to GitHub Pages. |
| `desktop/` | Windows .exe installer (Tauri). |
| `android/` | Native Android app (Kotlin, Jetpack Compose). |
| `logo/` | Logo source (SVG) and PNGs, including the 512x512 Play Store icon. |

## How releases are made

Pushing a version tag (for example `v1.1`) builds everything and attaches the Windows installer and a SHA256SUMS.txt checksum file to a GitHub Release.

## Build locally

**Windows .exe** (needs Node.js LTS and Rust with the C++ Build Tools):

```
cd desktop
npm install
npm run build
```

Installer: `desktop/src-tauri/target/release/bundle/nsis/`

**Android** (needs Android Studio): open the `android/` folder and press Run, or Build > Build APK(s).

## Releasing a new version

```
./scripts/bump-version.sh 1.2      # updates every version number
# add a "## v1.2" section to CHANGELOG.md
git commit -am "v1.2" && git tag v1.2 && git push && git push --tags
```

## Publishing to Google Play

Play needs a signed Android App Bundle, an app signing key you keep safe, a privacy policy URL, and (for new personal accounts) a closed test with 12 testers for 14 days. Check Google's Play Console help for current rules.

## Security

See [SECURITY.md](SECURITY.md). Verify a download with `certutil -hashfile <file> SHA256` and compare it with `SHA256SUMS.txt` on the release.

## License

Proprietary. Copyright (c) 2026 Usama Bhatti, trading as UB Labs. All rights reserved. See [LICENSE](LICENSE).
