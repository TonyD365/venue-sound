"""Synthesises the demo video's background music (original, no samples, free to use).

    python3 tools/make_music.py out.wav [seconds]

A bright, steady I-vi-IV-V progression in C at 112 BPM: warm pad, bass, a plucked arpeggio,
kick, clap and hi-hats, with a fade in and out.
"""

import sys
import wave

import numpy as np

RATE = 44100
BPM = 112
BEAT = 60 / BPM
BAR = 4 * BEAT


def midi(n: float) -> float:
    return 440.0 * 2 ** ((n - 69) / 12)


def env(n: int, attack: float, release: float) -> np.ndarray:
    t = np.arange(n) / RATE
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    r = np.clip((n / RATE - t) / max(release, 1e-4), 0, 1)
    return a * r


def tone(freq: float, dur: float, kind: str = "pad") -> np.ndarray:
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    if kind == "pad":
        w = sum(np.sin(2 * np.pi * freq * d * t) / (k + 1) for k, d in enumerate([1, 1.003, 0.997, 2.0, 2.004]))
        return w * env(n, 0.35, 0.6) * 0.18
    if kind == "bass":
        w = np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(2 * np.pi * freq * 2 * t)
        return w * env(n, 0.01, 0.12) * 0.45
    # pluck
    w = np.sin(2 * np.pi * freq * t) + 0.4 * np.sin(2 * np.pi * freq * 3 * t) * np.exp(-t * 18)
    return w * np.exp(-t * 7) * env(n, 0.004, 0.05) * 0.22


def kick() -> np.ndarray:
    n = int(0.35 * RATE)
    t = np.arange(n) / RATE
    f = 45 + 110 * np.exp(-t * 28)
    return np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-t * 9) * 0.9


def noise(dur: float, decay: float, level: float, seed: int) -> np.ndarray:
    n = int(dur * RATE)
    rng = np.random.default_rng(seed)
    x = rng.standard_normal(n)
    x = np.diff(x, prepend=0)  # brighter
    return x * np.exp(-np.arange(n) / RATE * decay) * level


def add(buf: np.ndarray, sig: np.ndarray, at: float, pan: float = 0.0) -> None:
    i = int(at * RATE)
    if i >= buf.shape[0]:
        return
    j = min(buf.shape[0], i + len(sig))
    left = np.cos((pan + 1) * np.pi / 4) * np.sqrt(2)
    right = np.sin((pan + 1) * np.pi / 4) * np.sqrt(2)
    buf[i:j, 0] += sig[: j - i] * left
    buf[i:j, 1] += sig[: j - i] * right


def main() -> None:
    out = sys.argv[1]
    seconds = float(sys.argv[2]) if len(sys.argv) > 2 else 52.0
    buf = np.zeros((int((seconds + 2) * RATE), 2))
    # C, Am, F, G with a little voicing movement
    chords = [[60, 64, 67, 72], [57, 60, 64, 69], [53, 57, 60, 65], [55, 59, 62, 67]]
    roots = [36, 33, 29, 31]
    bar = 0
    while bar * BAR < seconds:
        c = bar % 4
        start = bar * BAR
        for note in chords[c]:
            add(buf, tone(midi(note), BAR + 0.3, "pad"), start, pan=-0.3 + 0.2 * (note % 4))
        for beat in range(4):
            add(buf, tone(midi(roots[c]), BEAT * 0.9, "bass"), start + beat * BEAT)
        if bar >= 2:  # arpeggio comes in after the intro
            arp = chords[c] + [chords[c][1] + 12]
            for s in range(8):
                add(buf, tone(midi(arp[s % len(arp)] + 12), BEAT * 0.5, "pluck"), start + s * BEAT / 2, pan=0.4 if s % 2 else -0.4)
        if 2 <= bar:  # drums
            for beat in range(4):
                add(buf, kick(), start + beat * BEAT)
                if beat in (1, 3):
                    add(buf, noise(0.2, 22, 0.25, bar * 4 + beat), start + beat * BEAT)
                for h in range(2):
                    add(buf, noise(0.05, 90, 0.09, 1000 + bar * 8 + beat * 2 + h), start + beat * BEAT + h * BEAT / 2, pan=0.5)
        bar += 1
    buf = buf[: int(seconds * RATE)]
    # fade in 1.5 s, out 3 s, then normalise with a little headroom
    n = buf.shape[0]
    t = np.arange(n) / RATE
    fade = np.clip(t / 1.5, 0, 1) * np.clip((seconds - t) / 3.0, 0, 1)
    buf *= fade[:, None]
    buf /= max(1e-6, np.abs(buf).max()) / 0.85
    pcm = (buf * 32767).astype("<i2")
    with wave.open(out, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())
    print(f"wrote {out} ({seconds:.0f} s)")


if __name__ == "__main__":
    main()
