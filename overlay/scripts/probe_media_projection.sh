#!/usr/bin/env bash
# Emulator-only MediaProjection consent discovery. Does not accept any system
# consent automatically; captures a test-only UI hierarchy for CI diagnostics.
set -euo pipefail
pkg=dev.goodlight999.taobaolingo.overlay
dir="${RUNNER_TEMP:-/tmp}"
adb shell appops set "$pkg" SYSTEM_ALERT_WINDOW allow
adb shell am force-stop "$pkg"
adb shell am start -W -n "$pkg/.MainActivity"
sleep 2
adb shell uiautomator dump /sdcard/taobaolingo-main.xml >/dev/null
adb pull /sdcard/taobaolingo-main.xml "$dir/taobaolingo-main.xml" >/dev/null
python3 - "$dir/taobaolingo-main.xml" <<'PY'
import sys
import re
import subprocess
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
found = []
for node in root.iter('node'):
    if node.attrib.get('text') == '画面翻訳を開始':
        bounds = node.attrib.get('bounds', '')
        xy = [int(x) for x in re.findall(r'\d+', bounds)]
        if len(xy) == 4: found.append(((xy[0]+xy[2])//2, (xy[1]+xy[3])//2))
if len(found) != 1:
    raise SystemExit('Could not locate exactly one start-translation button')
subprocess.run(['adb','shell','input','tap',str(found[0][0]),str(found[0][1])],check=True)
PY
sleep 3
adb shell uiautomator dump /sdcard/taobaolingo-projection.xml || true
adb pull /sdcard/taobaolingo-projection.xml "$dir/taobaolingo-projection.xml" || true
adb exec-out screencap -p > "$dir/taobaolingo-projection.png" || true
echo 'Captured the Android MediaProjection consent UI for automated test development.'
