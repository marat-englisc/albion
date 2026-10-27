from pathlib import Path
import math, wave, struct

root = Path(__file__).resolve().parents[1]
# Original offline Albion completion cue, C5–E5. Standard library only.
raw = root / 'app/src/main/res/raw'
raw.mkdir(exist_ok=True)
rate = 44100
samples = []
for i in range(int(rate * 1.15)):
    t = i / rate
    value = 0.0
    for start, freq in [(0.0, 523.251), (0.16, 659.255)]:
        elapsed = t - start
        if elapsed >= 0:
            attack = 1 - math.exp(-elapsed * 110)
            decay = math.exp(-elapsed * 6.8)
            # Rounded partials and a rapid decay; no sharp notification transient.
            value += attack * decay * (math.sin(2*math.pi*freq*elapsed) + 0.16*math.sin(2*math.pi*freq*2*elapsed))
    fade = min(1.0, (1.15-t)/0.08)
    samples.append(int(max(-1, min(1, value * 0.27 * fade)) * 32767))
with wave.open(str(raw / 'albion_complete.wav'), 'wb') as f:
    f.setnchannels(1); f.setsampwidth(2); f.setframerate(rate)
    f.writeframes(struct.pack('<' + 'h'*len(samples), *samples))
