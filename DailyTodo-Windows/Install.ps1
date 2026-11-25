$dir = "$env:LOCALAPPDATA\DailyTodo"
New-Item -ItemType Directory -Force $dir | Out-Null
Copy-Item "$PSScriptRoot\daily-todo.html" "$dir\daily-todo.html" -Force
Copy-Item "$PSScriptRoot\daily-todo.ico" "$dir\daily-todo.ico" -Force
$edge = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"
if (-not (Test-Path $edge)) { $edge = "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe" }
if (-not (Test-Path $edge)) { Write-Host "Microsoft Edge was not found."; exit 1 }
$url = ([System.Uri]"$dir\daily-todo.html").AbsoluteUri
$ws = New-Object -ComObject WScript.Shell
$links = @("$env:APPDATA\Microsoft\Windows\Start Menu\Programs\Daily To-Do.lnk",
           "$([Environment]::GetFolderPath('Desktop'))\Daily To-Do.lnk")
foreach ($p in $links) {
  $s = $ws.CreateShortcut($p)
  $s.TargetPath = $edge
  $s.Arguments = "--app=$url"
  $s.IconLocation = "$dir\daily-todo.ico"
  $s.Save()
}
Write-Host "Installed. Open 'Daily To-Do' from the Start menu or your desktop."
