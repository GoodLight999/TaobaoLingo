#!/usr/bin/env bash
# Usage: probe.sh <label> <apk>   — install, launch, auto-accept consent, capture evidence.
set -u
L="$1"; APK="$2"; PKG=com.taobao.taobao; OUT="out/$L"; mkdir -p "$OUT"
adb uninstall "$PKG" >/dev/null 2>&1 || true
adb install -r -g "$APK" > "$OUT/install.txt" 2>&1; cat "$OUT/install.txt"
adb shell pm list packages | grep -q "$PKG" || { echo "INSTALL FAILED ($L)"; exit 0; }
adb shell dumpsys package "$PKG" | grep -iE "signatures|versionName|primaryCpuAbi" > "$OUT/pkg.txt" || true
adb logcat -c
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
tap_words() {  # tap first node whose text/desc matches any consent word
  adb exec-out uiautomator dump /dev/tty 2>/dev/null | sed 's/UI hierchary dumped.*//' > /tmp/ui.xml || return 1
  python3 - <<'PY'
import re,subprocess,xml.etree.ElementTree as ET
W=["同意并继续","同意","Agree","AGREE","允许","Allow","ALLOW","知道了","跳过","Skip","确定","OK"]
try: r=ET.parse('/tmp/ui.xml').getroot()
except Exception: raise SystemExit(1)
for w in W:
  for n in r.iter('node'):
    t=(n.get('text') or '')+'|'+(n.get('content-desc') or '')
    if w in t and n.get('clickable','true')!='x':
      a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds')))
      subprocess.run(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)])
      print('tapped',repr(t)); raise SystemExit(0)
raise SystemExit(1)
PY
}
for i in $(seq 1 12); do sleep 8; tap_words && sleep 3; done
sleep 30
adb exec-out screencap -p > "$OUT/home.png"
adb exec-out uiautomator dump /dev/tty 2>/dev/null > "$OUT/home_ui.xml" || true
# try the 我的淘宝 / account tab
python3 - "$OUT/home_ui.xml" <<'PY' || true
import re,sys,subprocess,xml.etree.ElementTree as ET
try: r=ET.fromstring(open(sys.argv[1]).read().split('UI hierchary')[0])
except Exception: sys.exit()
for n in r.iter('node'):
  t=(n.get('text') or '')+(n.get('content-desc') or '')
  if '我的淘宝' in t or t.strip()=='我的':
    a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds'))); subprocess.run(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)]); print('tapped account'); break
PY
sleep 20
adb exec-out screencap -p > "$OUT/account.png"
adb logcat -d -v threadtime > "$OUT/logcat.txt"
grep -iE "mtop|SecurityGuard|sgmain|x-sign|FAIL_SYS|ERRCODE|retCode|SG_ERROR|signature|FATAL|ANR" "$OUT/logcat.txt" | head -400 > "$OUT/logcat_key.txt" || true
grep -oE "(FAIL_SYS_[A-Z_]+|SG_ERROR_CODE[^,]*|ANDROID_SYS_[A-Z_]+)" "$OUT/logcat.txt" | sort | uniq -c | sort -rn > "$OUT/error_codes.txt" || true
echo "== $L error codes =="; cat "$OUT/error_codes.txt"
