#!/bin/bash
mkdir -p "${GG_TMP:-/tmp/golemgarage}"
# Runs the behavior tests on a headless server for a few minutes and prints each golem's activity.
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
SECS=${1:-200}
rm -rf run/behaviortest
cp -r run/saves/golemtest_clean run/behaviortest
rm -rf run/behaviortest/datapacks/golemtest
python3 tools/testworld/behaviors.py run/behaviortest
sed -i 's/^level-name=.*/level-name=behaviortest/' run/server.properties
LOG=${GG_TMP:-/tmp/golemgarage}/behave.log
gradle runServer -Pgolemdebug --console=plain > "$LOG" 2>&1 &
GPID=$!
for i in $(seq 1 120); do grep -q "Done (" "$LOG" && break; sleep 2; done
sleep "$SECS"
for p in $(pgrep -x java); do
  if tr '\0' ' ' < /proc/$p/cmdline | grep -qE "launchTarget [a-z]*server"; then kill -TERM $p; fi
done
sleep 8
kill $GPID 2>/dev/null
sed -i 's/^level-name=.*/level-name=world/' run/server.properties
echo finished
