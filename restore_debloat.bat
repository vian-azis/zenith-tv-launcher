@echo off
echo ===================================================
echo [EMERGENCY RESTORE] Zenith TV System Debloat Rollback
echo ===================================================
echo Restoring disabled packages via ADB...

adb shell pm enable com.google.android.apps.tv.launcherx
adb shell pm enable com.google.android.feedback
adb shell pm enable com.android.dynsystem
adb shell pm enable com.android.cts.ctsshim
adb shell pm enable com.android.cts.priv.ctsshim
adb shell pm enable com.tcl.browser
adb shell pm enable com.tcl.guard
adb shell pm enable com.tcl.keyhelp
adb shell pm enable com.tcl.messagebox
adb shell pm enable com.google.android.videos
adb shell pm enable com.google.android.play.games

echo Done! All packages re-enabled.
pause

