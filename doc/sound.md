# Sound

## Tones

| Tick   | Note | MIDI note number | Frequency (Hz) |
|--------|------|------------------|----------------|
| Strong | B6   | 95               | 1975.53        |
| Weak   | B5   | 83               | 987.77         |
| Sub    | B4   | 71               | 493.88         |

## Generation procedure

### Sine Wave

- Open Audacity
- Set "Project Rate (Hz)" to 48000
- Tracks -> Add New -> Mono Track
- Generate -> Tone -> Waveform Sine
    - Frequency (Hz): {tickTone}
    - Amplitude: 1
    - Duration: 0,050s
- Effect -> Fade Out
- Effect -> Fade Out
- File -> Export -> Export as WAV -> Encoding 32-bit float

### Square Wave

- Open Audacity
- Set "Project Rate (Hz)" to 48000
- Tracks -> Add New -> Mono Track
- Generate -> Tone -> Waveform Square
    - Frequency (Hz): {tickTone}
    - Amplitude: 1
    - Duration: 0,050s
- Effect -> Fade Out
- Effect -> Fade Out
- File -> Export -> Export as WAV -> Encoding 32-bit float

### Risset Drum

- Open Audacity
- Set "Project Rate (Hz)" to 48000
- Tracks -> Add New -> Mono Track
- Generate -> Risset Drum
    - Frequency (Hz): {tickTone}
    - Decay (seconds): 0.10
    - Center frequency of noise (Hz): 100.0
    - Width of noise band (Hz): 10.0
    - Amount of noise in mix (percent): 0.0
    - Amplitude (0 - 1): 1.000
- File -> Export -> Export as WAV -> Encoding 32-bit float

### Pluck

- Open Audacity
- Set "Project Rate (Hz)" to 48000
- Tracks -> Add New -> Mono Track
- Generate -> Pluck
    - Pluck MIDI pitch: {tickTone}
    - Fade-out type: Gradual
    - Duration: 0,100s
- File -> Export -> Export as WAV -> Encoding 32-bit float
