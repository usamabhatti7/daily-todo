# Daily To-Do changelog

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
