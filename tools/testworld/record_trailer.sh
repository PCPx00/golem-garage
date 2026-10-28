#!/bin/bash
mkdir -p "${GG_TMP:-/tmp/golemgarage}"
# Watches the client log and records each "SHOT <name> <ticks>" to ${GG_TMP:-/tmp/golemgarage}/trailer/<name>.mp4.
OUT=${GG_TMP:-/tmp/golemgarage}/trailer
mkdir -p $OUT
LOG=${GG_TMP:-/tmp/golemgarage}/client_run.log
tail -n0 -F $LOG | while read -r line; do
  if [[ "$line" =~ SHOT\ ([a-z]+)\ ([0-9]+) ]]; then
    name=${BASH_REMATCH[1]}; ticks=${BASH_REMATCH[2]}
    [ "$name" = "$last" ] && continue
    last=$name
    secs=$(python3 -c "print(round($ticks/20+0.3,2))")
    ffmpeg -y -loglevel error -f x11grab -framerate 20 -video_size 1280x720 -i :99 -t $secs \
      -c:v libx264 -preset veryfast -crf 16 -pix_fmt yuv444p $OUT/$name.mp4 &
    echo "recording $name for $secs s"
  fi
  [[ "$line" =~ "trailer done" ]] && { sleep 2; echo done; exit 0; }
done
