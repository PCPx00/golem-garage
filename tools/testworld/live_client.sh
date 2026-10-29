#!/bin/bash
mkdir -p "${GG_TMP:-/tmp/golemgarage}"
# Starts the client in a fresh copy of the test world on display :99 and keeps it running
# (for driving it by hand with xdotool). Stop it with: tools/testworld/live_client.sh stop
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
if [ "$1" = "stop" ]; then
  for p in $(pgrep -x java); do
    tr '\0' ' ' < /proc/$p/cmdline | grep -qE "launchTarget [a-z]*client" && kill $p
  done
  pkill -f "Xvfb :99"
  exit 0
fi
cp "$ROOT/tools/testworld/options.txt" "$ROOT/run/options.txt"
rm -rf "$ROOT/run/saves/golemtest"
cp -r "$ROOT/run/saves/golemtest_clean" "$ROOT/run/saves/golemtest"
if [ "$1" = "features" ]; then
  rm -rf "$ROOT/run/saves/golemtest/datapacks/golemtest"
  python3 "$ROOT/tools/testworld/feature_world.py" "$ROOT/run/saves/golemtest"
fi
if [ "$1" = "showcase" ]; then
  rm -rf "$ROOT/run/saves/golemtest/datapacks/golemtest"
  python3 "$ROOT/tools/testworld/showcase_world.py" "$ROOT/run/saves/golemtest"
fi
if [ "$1" = "trailer" ]; then
  rm -rf "$ROOT/run/saves/golemtest/datapacks/golemtest"
  python3 "$ROOT/tools/testworld/trailer_world.py" "$ROOT/run/saves/golemtest"
fi
if [ "$1" = "garage" ]; then
  rm -rf "$ROOT/run/saves/golemtest/datapacks/gtest"
  python3 "$ROOT/tools/testworld/garage_world.py" "$ROOT/run/saves/golemtest"
fi
EXTRA=""
[ -n "$COMPAT" ] && EXTRA="-Pcompat"
export DISPLAY=:99
Xvfb :99 -screen 0 1280x720x24 >/dev/null 2>&1 &
sleep 2
export LIBGL_ALWAYS_SOFTWARE=1
cd "$ROOT"
gradle runClient -Pquickplay=golemtest -Pgolemdebug $EXTRA --console=plain > ${GG_TMP:-/tmp/golemgarage}/client_run.log 2>&1
