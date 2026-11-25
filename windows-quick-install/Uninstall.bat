@echo off
rmdir /s /q "%LOCALAPPDATA%\DailyTodo"
del "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Daily To-Do.lnk"
del "%USERPROFILE%\Desktop\Daily To-Do.lnk"
echo Removed.
pause
