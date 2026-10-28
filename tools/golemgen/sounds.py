"""
Synthesized sounds for Golem Garage: golem servos and footsteps, wheels and treads, metal hits, breakdown,
the Garage's lasers and chimes, and Cub chirps. Written as mono OGG (so they're positional) plus sounds.json.
"""
import json
import os
import subprocess
import tempfile
import wave

import numpy as np
from scipy import signal

SR = 44100
MODID = "golemgarage"
rng = np.random.default_rng(1234)


def t_of(dur):
    return np.arange(int(SR * dur)) / SR


def env(dur, attack=0.005, decay=0.2):
    t = t_of(dur)
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    return a * np.exp(-np.maximum(t - attack, 0) / max(decay, 1e-4))


def band(x, lo, hi, order=2):
    nyq = SR / 2
    lo = max(lo, 10) / nyq
    hi = min(hi, nyq - 100) / nyq
    b, a = signal.butter(order, [lo, hi], btype="band")
    return signal.lfilter(b, a, x)


def low(x, f, order=2):
    b, a = signal.butter(order, f / (SR / 2), btype="low")
    return signal.lfilter(b, a, x)


def noise(dur):
    return rng.standard_normal(int(SR * dur))


def pad(x, dur):
    n = int(SR * dur)
    if len(x) >= n:
        return x[:n]
    return np.concatenate([x, np.zeros(n - len(x))])


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def at(x, start, total):
    out = np.zeros(int(SR * total))
    i = int(SR * start)
    j = min(len(out), i + len(x))
    out[i:j] += x[:j - i]
    return out


def sweep(f0, f1, dur, shape="sine", curve="exp"):
    t = t_of(dur)
    if curve == "exp":
        f = f0 * (f1 / f0) ** (t / dur)
    else:
        f = f0 + (f1 - f0) * t / dur
    ph = 2 * np.pi * np.cumsum(f) / SR
    if shape == "sine":
        return np.sin(ph)
    if shape == "tri":
        return 2 / np.pi * np.arcsin(np.sin(ph))
    if shape == "saw":
        return 2 * ((ph / (2 * np.pi)) % 1) - 1
    return np.sign(np.sin(ph))


def tone(f, dur, shape="sine"):
    return sweep(f, f * 1.0001, dur, shape)


def metal(fund, dur, decay=0.25, partials=(1.0, 2.76, 5.40, 8.93), amps=(1, 0.6, 0.35, 0.2)):
    """Inharmonic struck-metal partials."""
    t = t_of(dur)
    x = np.zeros(len(t))
    for p, a in zip(partials, amps):
        x += a * np.sin(2 * np.pi * fund * p * t + rng.random() * 6) * np.exp(-t / (decay / (1 + p * 0.25)))
    return x


def thump(f, dur, decay=0.08):
    t = t_of(dur)
    fdrop = f * (1 + 1.5 * np.exp(-t / 0.02))
    return np.sin(2 * np.pi * np.cumsum(fdrop) / SR) * np.exp(-t / decay)


def servo(f0, f1, dur, level=0.3):
    whir = sweep(f0, f1, dur, "saw") * 0.5 + sweep(f0 * 2, f1 * 2, dur, "sine") * 0.3
    whir = band(whir, 150, 2500)
    e = np.sin(np.pi * np.clip(t_of(dur) / dur, 0, 1)) ** 0.7
    return whir * e * level


def norm(x, peak=0.85):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def fade(x, fin=0.005, fout=0.03):
    n = len(x)
    a = np.ones(n)
    i = int(SR * fin)
    o = int(SR * fout)
    if i:
        a[:i] = np.linspace(0, 1, i)
    if o:
        a[-o:] = np.linspace(1, 0, o)
    return x * a


# ------------------------------------------------------------------ the sounds
def golem_step(v):
    d = 0.42
    p = [1.0, 0.9, 1.12][v]
    x = mix(thump(55 * p, d, 0.09) * 1.0,
            band(noise(d), 80, 600) * env(d, 0.002, 0.04) * 0.6,
            at(metal(820 * p, 0.25, 0.09) * 0.25, 0.01, d),
            at(servo(260 * p, 420 * p, 0.18, 0.35), 0.06, d))
    return fade(norm(x))


def step_wheels(v):
    d = 0.38
    t = t_of(d)
    r = low(noise(d), 180, 3) * (0.6 + 0.4 * np.sin(2 * np.pi * (28 + v * 4) * t))
    hum = sweep(90, 120, d, "saw") * 0.15
    x = (r + low(hum, 400)) * np.sin(np.pi * t / d) ** 0.6
    return fade(norm(x, 0.7))


def step_treads(v):
    d = 0.45
    parts = [low(noise(d), 220) * np.sin(np.pi * t_of(d) / d) * 0.5]
    for i, s in enumerate((0.0, 0.11, 0.22)):
        parts.append(at(metal(600 + 90 * i + v * 50, 0.12, 0.04) * 0.4, s, d))
    return fade(norm(mix(*parts), 0.75))


def golem_hurt(v):
    d = 0.6
    f = [240, 300][v]
    x = mix(metal(f, d, 0.28) * 1.0, band(noise(d), 1500, 7000) * env(d, 0.001, 0.03) * 0.5,
            thump(70, d, 0.1) * 0.6)
    return fade(norm(x))


def golem_attack():
    d = 0.55
    whoosh = band(noise(d), 300, 2500) * np.sin(np.pi * np.clip(t_of(d) / 0.3, 0, 1)) ** 2
    whoosh[int(0.3 * SR):] = 0
    x = mix(whoosh * 0.6, at(metal(180, 0.4, 0.22) * 0.9, 0.22, d), at(thump(60, 0.3, 0.1), 0.22, d))
    return fade(norm(x))


def golem_breakdown():
    d = 1.6
    t = t_of(d)
    down = sweep(900, 45, 1.2, "saw")
    down = low(down, 2500) * np.exp(-t_of(1.2) / 0.9) * 0.6
    crackle = np.zeros(len(t))
    for _ in range(26):
        i = int(rng.random() * 1.2 * SR)
        crackle[i:i + 120] += rng.standard_normal(120) * rng.random()
    crackle = band(crackle, 2000, 9000) * 0.5
    clatter = mix(*[at(metal(250 + rng.random() * 400, 0.3, 0.1) * 0.5, 1.15 + i * 0.07, d) for i in range(4)])
    x = mix(at(down, 0, d), crackle, clatter, at(thump(50, 0.4, 0.15), 1.15, d))
    return fade(norm(x), 0.002, 0.15)


def golem_work():
    d = 0.22
    x = mix(servo(380, 620, 0.1, 1.0), at(servo(620, 420, 0.1, 0.8), 0.11, d), at(metal(1400, 0.08, 0.02) * 0.3, 0.0, d))
    return fade(norm(x, 0.6))


def golem_ambient(v):
    d = 0.7
    if v == 0:
        b = mix(tone(660, 0.09, "tri") * env(0.09, 0.005, 0.06), at(tone(880, 0.12, "tri") * env(0.12, 0.005, 0.08), 0.13, d))
    else:
        b = mix(sweep(500, 820, 0.14, "tri") * env(0.14, 0.01, 0.1), at(tone(560, 0.1, "tri") * env(0.1, 0.005, 0.06), 0.2, d))
    hum = low(sweep(110, 112, d, "saw"), 300) * np.sin(np.pi * t_of(d) / d) * 0.25
    return fade(norm(mix(pad(b, d) * 0.7, hum), 0.5))


def part_fit():
    d = 0.5
    x = mix(thump(80, d, 0.06), metal(950, d, 0.08) * 0.4)
    for i in range(5):
        x = mix(x, at(band(noise(0.012), 2000, 6000) * 0.5, 0.15 + i * 0.045, d))
    return fade(norm(x))


def power_low():
    d = 0.5
    x = mix(tone(880, 0.13, "square") * env(0.13, 0.005, 0.1), at(tone(587, 0.2, "square") * env(0.2, 0.005, 0.15), 0.17, d))
    return fade(norm(low(x, 3000), 0.45))


def garage_laser():
    d = 2.0
    t = t_of(d)
    trem = 0.75 + 0.25 * np.sin(2 * np.pi * 8 * t)
    hum = (np.sin(2 * np.pi * 220 * t) + 0.5 * np.sin(2 * np.pi * 440 * t) + 0.25 * np.sin(2 * np.pi * 660 * t)) * trem
    whine = np.sin(2 * np.pi * (3100 + 120 * np.sin(2 * np.pi * 0.7 * t)) * t) * 0.12
    sizzle = band(noise(d), 4000, 9000) * 0.08 * (0.6 + 0.4 * np.sin(2 * np.pi * 13 * t))
    x = (hum * 0.5 + whine + sizzle) * np.minimum(1, np.minimum(t / 0.2, (d - t) / 0.3))
    return norm(x, 0.55)


def garage_start():
    d = 1.3
    up = sweep(90, 900, 1.0, "saw")
    up = low(up, 3000) * np.linspace(0.2, 1, len(up)) * 0.6
    x = mix(up, at(metal(400, 0.5, 0.2) * 0.5, 0.95, d), at(thump(60, 0.4, 0.12), 0.0, d))
    return fade(norm(x), 0.01, 0.2)


def repair_done():
    d = 1.6
    notes = [1046.5, 1318.5, 1568.0, 2093.0]
    x = np.zeros(int(SR * d))
    for i, f in enumerate(notes):
        tt = t_of(1.0)
        bell = (np.sin(2 * np.pi * f * tt) + 0.4 * np.sin(2 * np.pi * f * 2.0 * tt) + 0.2 * np.sin(2 * np.pi * f * 3.01 * tt))
        bell *= np.exp(-tt / 0.45)
        x = mix(x, at(bell, i * 0.12, d))
    return fade(norm(x, 0.7), 0.002, 0.2)


def module_install():
    d = 0.4
    zap = band(noise(0.25), 3000, 8000) * env(0.25, 0.002, 0.08)
    x = mix(at(thump(120, 0.15, 0.03), 0, d), at(metal(1600, 0.1, 0.03) * 0.5, 0, d), at(zap * 0.6, 0.05, d),
            at(sweep(800, 2400, 0.15, "tri") * env(0.15, 0.005, 0.1) * 0.4, 0.06, d))
    return fade(norm(x, 0.7))


def cub_chirp(v):
    d = 0.35
    p = [1.0, 1.15, 0.9][v]
    one = sweep(1300 * p, 2500 * p, 0.07, "tri") * env(0.07, 0.004, 0.05)
    two = sweep(1500 * p, 2900 * p, 0.06, "tri") * env(0.06, 0.004, 0.04)
    x = mix(at(one, 0, d), at(two, 0.1 if v != 2 else 0.14, d))
    return fade(norm(x, 0.55))


def cub_happy():
    d = 0.6
    t = t_of(d)
    f = 1700 + 300 * np.sign(np.sin(2 * np.pi * 14 * t)) + 60 * np.sin(2 * np.pi * 6 * t)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * t / d) ** 0.5
    return fade(norm(low(x, 6000), 0.5))


def cub_sleep():
    d = 1.6
    t = t_of(d)
    breath = low(noise(d), 900) * (np.sin(np.pi * t / d) ** 2)
    hum = np.sin(2 * np.pi * 300 * t) * 0.15 * np.sin(np.pi * t / d) ** 3
    return fade(norm(breath + hum, 0.25))


# ------------------------------------------------------------------ output
SOUNDS = {
    # event: ([(file, function)], subtitle)
    "golem.step": ([("golem/step%d" % i, lambda i=i: golem_step(i)) for i in range(3)], None),
    "golem.step_wheels": ([("golem/wheels%d" % i, lambda i=i: step_wheels(i)) for i in range(2)], None),
    "golem.step_treads": ([("golem/treads%d" % i, lambda i=i: step_treads(i)) for i in range(2)], None),
    "golem.hurt": ([("golem/hurt%d" % i, lambda i=i: golem_hurt(i)) for i in range(2)], "Golem clanks"),
    "golem.attack": ([("golem/attack", golem_attack)], "Golem strikes"),
    "golem.breakdown": ([("golem/breakdown", golem_breakdown)], "Golem breaks down"),
    "golem.work": ([("golem/work", golem_work)], "Golem works"),
    "golem.ambient": ([("golem/ambient%d" % i, lambda i=i: golem_ambient(i)) for i in range(2)], "Golem beeps"),
    "golem.part_fit": ([("golem/part_fit", part_fit)], "Part fitted"),
    "golem.power_low": ([("golem/power_low", power_low)], "Golem out of power"),
    "golem.module": ([("golem/module", module_install)], "Module installed"),
    "garage.laser": ([("garage/laser", garage_laser)], "Garage lasers hum"),
    "garage.start": ([("garage/start", garage_start)], "Garage powers up"),
    "garage.done": ([("garage/done", repair_done)], "Repair complete"),
    "cub.chirp": ([("cub/chirp%d" % i, lambda i=i: cub_chirp(i)) for i in range(3)], "Cub chirps"),
    "cub.happy": ([("cub/happy", cub_happy)], "Cub trills happily"),
    "cub.sleep": ([("cub/sleep", cub_sleep)], "Cub snoozes"),
}


def write_ogg(x, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5", path], check=True)
    os.remove(wav)


def generate(assets, lang):
    sdir = os.path.join(assets, "sounds")
    defs = {}
    for event, (files, subtitle) in SOUNDS.items():
        names = []
        for name, fn in files:
            write_ogg(fn(), os.path.join(sdir, name + ".ogg"))
            names.append("%s:%s" % (MODID, name))
        entry = {"sounds": names}
        if subtitle:
            key = "subtitles.%s.%s" % (MODID, event)
            entry["subtitle"] = key
            lang[key] = subtitle
        defs[event] = entry
    with open(os.path.join(assets, "sounds.json"), "w") as fh:
        json.dump(defs, fh, indent=2)
        fh.write("\n")
    print("sounds: %d events, %d files" % (len(defs), sum(len(v[0]) for v in SOUNDS.values())))
    return list(SOUNDS.keys())
