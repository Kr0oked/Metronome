# Sound

## Sine wave frequencies

| Tick   | Frequency |
|--------|-----------|
| Strong | 2000hz    |
| Weak   | 1000hz    |
| Sub    | 500hz     |

# Generation procedure

- Open Audacity
- Set "Project Rate (Hz)" to 48000
- Tracks -> Add New -> Mono Track
- Generate -> Tone -> Waveform Sine, Frequency (Hz) {rateNeededForSound}, Amplitude 1, Duration 0,050s
- Effect -> Fade Out
- Effect -> Fade Out
- File -> Export -> Export as WAV -> Encoding 32-bit float
