#!/bin/bash
mkdir -p "${GG_TMP:-/tmp/golemgarage}"
# Drives the live client. GUI coordinates are in GUI pixels (scale 3 at 1280x720).
# Usage: drive.sh click X Y | rclick X Y | key K | type TEXT | shot NAME | wait SECONDS | scroll X Y N
export DISPLAY=:99
S=2
while [ $# -gt 0 ]; do
  case "$1" in
    click) xdotool mousemove $(( $2 * S + 1 )) $(( $3 * S + 1 )) click 1; shift 3; sleep 0.6 ;;
    rclick) xdotool mousemove $(( $2 * S + 1 )) $(( $3 * S + 1 )) click 3; shift 3; sleep 0.6 ;;
    move) xdotool mousemove $(( $2 * S + 1 )) $(( $3 * S + 1 )); shift 3; sleep 0.6 ;;
    key) xdotool key "$2"; shift 2; sleep 0.5 ;;
    look) xdotool mousemove_relative -- "$2" "$3"; shift 3; sleep 0.6 ;;
    type) xdotool type --delay 60 "$2"; shift 2; sleep 0.5 ;;
    wait) sleep "$2"; shift 2 ;;
    shot) import -display :99 -window root "${GG_TMP:-/tmp/golemgarage}/$2.png"; shift 2 ;;
    *) echo "unknown $1"; exit 1 ;;
  esac
done
