#!/bin/bash
mkdir -p "${GG_TMP:-/tmp/golemgarage}"
# Creates run/saves/golemtest: a creative superflat world with the test datapack.
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
mkdir -p run
echo "eula=true" > run/eula.txt
cat > run/server.properties <<'PROPS'
level-type=minecraft\:flat
gamemode=creative
generate-structures=false
spawn-protection=0
online-mode=false
level-name=world
spawn-monsters=false
spawn-animals=false
PROPS
rm -rf run/world
gradle runServer --console=plain > ${GG_TMP:-/tmp/golemgarage}/mkworld.log 2>&1 &
GPID=$!
for i in $(seq 1 120); do
  if grep -q "Done (" ${GG_TMP:-/tmp/golemgarage}/mkworld.log 2>/dev/null; then break; fi
  sleep 2
done
sleep 5
for p in $(pgrep -x java); do
  if tr '\0' ' ' < /proc/$p/cmdline | grep -qE "launchTarget [a-z]*server"; then kill -TERM $p; SP=$p; fi
done
for i in $(seq 1 30); do
  if [ -z "$SP" ] || ! kill -0 $SP 2>/dev/null; then break; fi
  sleep 2
done
kill $GPID 2>/dev/null || true
wait $GPID 2>/dev/null || true
mkdir -p run/saves
rm -rf run/saves/golemtest run/saves/golemtest_clean
mv run/world run/saves/golemtest_clean
python3 tools/testworld/scenes.py run/saves/golemtest_clean
ls run/saves/golemtest
