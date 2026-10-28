#!/bin/bash
mkdir -p "${GG_TMP:-/tmp/golemgarage}"
# Runs the client in the golemtest world on a virtual display and screenshots each scene.
# Usage: shoot.sh <outdir> [scene count]
OUT=${1:-${GG_TMP:-/tmp/golemgarage}/shots}
COUNT=${2:-12}
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
mkdir -p "$OUT"
rm -f "$OUT"/*.png
cp "$ROOT/tools/testworld/options.txt" "$ROOT/run/options.txt"
rm -rf "$ROOT/run/saves/golemtest"
cp -r "$ROOT/run/saves/golemtest_clean" "$ROOT/run/saves/golemtest"
export DISPLAY=:99
Xvfb :99 -screen 0 1280x720x24 >/dev/null 2>&1 &
XPID=$!
sleep 2
export LIBGL_ALWAYS_SOFTWARE=1
cd "$ROOT"
LOG=${GG_TMP:-/tmp/golemgarage}/client_run.log
gradle runClient -Pquickplay=golemtest --console=plain > "$LOG" 2>&1 &
GPID=$!
hidden=0
for s in $(seq 1 "$COUNT"); do
  for i in $(seq 1 240); do
    if grep -q "scene $s\b" "$LOG"; then break; fi
    if ! kill -0 $GPID 2>/dev/null; then echo "client exited"; break 2; fi
    sleep 1
  done
  if [ $hidden -eq 0 ]; then
    WID=$(xdotool search --name "Minecraft" | head -1)
    [ -n "$WID" ] && xdotool windowfocus "$WID" 2>/dev/null
    xdotool key F1
    hidden=1
  fi
  sleep 6
  import -display :99 -window root "$OUT/scene_$(printf %02d $s).png"
  echo "shot scene $s"
done
# GUI: right-click the golem in front of the camera, shoot both tabs.
xdotool mousemove 640 360 click 3
sleep 4
import -display :99 -window root "$OUT/gui_status.png"
xdotool mousemove 454 148 click 1
sleep 3
import -display :99 -window root "$OUT/gui_rules.png"
xdotool key Escape
sleep 1
for p in $(pgrep -x java); do
  if tr '\0' ' ' < /proc/$p/cmdline | grep -qE "launchTarget [a-z]*client"; then kill $p; fi
done
kill $GPID 2>/dev/null
kill $XPID 2>/dev/null
echo done
