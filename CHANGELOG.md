# Daily To-Do changelog

## v2.0
- Fixed the Android build error: Kotlin 1.9.25 now matches Compose compiler 1.5.15
- Dependabot no longer proposes Kotlin or Compose compiler updates on their own, because the two must be upgraded together by hand

## v1.9
- Android: added a Compose Preview so the screen can be seen and edited live inside Android Studio
- Android: added the Compose tooling libraries that Live Edit and Preview need (debug builds only for the tooling)

## v1.8
- Android: added the Gradle wrapper (Gradle 8.11.1) so Android Studio syncs and builds without extra setup
- The GitHub Android build now uses the same wrapper

## v1.7
- Publisher name is now UB Labs: shown in the Windows installer and Apps list, in the README, the license and the app footer
- Copyright line reads "Usama Bhatti, trading as UB Labs"
- The app's internal ID is unchanged, so installed copies update in place and keep their saved tasks

## v1.6
- Dependabot now ignores major version jumps (Kotlin 2, Android Gradle Plugin 9 and similar), which broke the Android build and need manual migration
- Only minor and patch dependency updates are proposed automatically

## v1.5
- Animations: the page fades and slides in when it opens
- New tasks slide in, deleted tasks slide away, and ticking a task gives the row and checkbox a small pop
- Moving between days slides the list left or right
- Finishing every task of the day sets off a confetti burst and a glow on the progress card
- The History tab animates its summary boxes and rows when it opens
- Buttons lift on hover and press down when clicked, and the logo wiggles on hover
- Everything switches off automatically if Windows "Show animations" is turned off (reduced motion)

## v1.4
- Easy install: the release now always contains DailyTodo-Setup.exe (installer) and DailyTodo-Portable.exe under fixed names
- README has a one-click Download for Windows link that always points to the latest version
- Release notes include install steps

## v1.3
- Licence changed to proprietary, all rights reserved to Usama Bhatti
- Security hardening: strict Content Security Policy in the Windows app and web version, no inline scripts or styles
- Saved data is validated when loaded, so a damaged or tampered file cannot break the app
- Android: backups of app data turned off, code shrinking enabled for release builds
- Builds: pinned dependency lockfile, least-privilege workflow permissions, no third-party release action, SHA256 checksums on releases
- Fixed: the release now includes the Windows installer (v1.2 releases may be missing it)
- Removed the Windows quick-install folder (the .exe installer replaces it)
- Added SECURITY.md, THIRD_PARTY.md and Dependabot updates

## v1.2 (Windows app and web)
- New History section: a permanent record of every task across all days
- Totals for all, done, open and removed tasks, plus search and filters
- Deleting a task or using "Clear completed" now keeps the task in History (marked Deleted or Cleared) instead of erasing it
- Restore a removed task, or delete it forever from History (asks you to click twice)
- Click a date in History to jump to that day
- Moving unfinished tasks forward now moves the original task, so History has no duplicates
- Android stays at v1.1 for now

## v1.1
- New logo (teal checklist) on every version: app icon, window icon, installer, browser tab, header, Play Store icon
- Version number shown in the app and in each build
- Android adaptive launcher icon with themed-icon support
- Android and Windows builds target the current requirements (Android API 36)

## v1.0
- First release: daily task list with priorities, progress bar, move unfinished tasks, clear completed
