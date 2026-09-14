# ResonanceLab — Tap. Listen. Know. | RevenueCat Shipaton 2026

> Pocket NDT lab: tap any wall, tank or slab → live 48 kHz resonance, hollow/solid/void verdict, sonar distance + liquid level, certified PDF. Free to try, Pro to certify.
>
> **Stack:** 100% Kotlin + Jetpack Compose + RevenueCat Paywalls + Entitlements (`pro_access`).
> **Status:** First public Play release during Shipaton submission window. Judges: use `Paywall → Judge Bypass` for instant Pro, no purchase needed.

ResonanceLab is a production-grade native Android instrument for non-destructive acoustic testing, void detection and sonar ranging, built to win on craft, monetization fit and real-world utility.

---

## 🔬 Core DSP & Physics Architecture

### 1. Audio Capture Pipeline
- **Engine**: Native `AudioRecord` streaming at 48,000 Hz, 16-bit PCM Mono.
- **Buffer Safety**: Pre-allocated circular ring buffer (16,384 samples) with zero heap allocations during the audio loop to prevent Garbage Collection (GC) frame drops.
- **Sliding Window**: 2048-sample analysis frames with a 512-sample hop size, yielding ~93.75 frames/second for fluid 60+ FPS visualization.

### 2. Spectral Analysis Engine
- **Windowing**: Hann (Hanning) window $w[n] = 0.5 \left(1 - \cos\left(\frac{2\pi n}{N - 1}\right)\right)$ applied to minimize spectral leakage.
- **Radix-2 FFT**: In-place Cooley-Tukey Radix-2 Fast Fourier Transform with precomputed bit-reversal and trigonometric twiddle factor tables, computing 1024 magnitude bins ($0\text{ Hz}$ to $24\text{ kHz}$ at $\Delta f = 23.4375\text{ Hz/bin}$).
- **Peak Frequency Interpolation**: Sub-bin parabolic quadratic peak estimation:
  $$\delta = \frac{\alpha - \gamma}{2(\alpha - 2\beta + \gamma)}, \quad f_{\text{peak}} = (k_{\max} + \delta) \cdot \Delta f$$
- **Spectral Centroid**: Center of mass of the frequency spectrum:
  $$\text{Centroid} = \frac{\sum k \cdot \Delta f \cdot M[k]}{\sum M[k]}$$
- **Quality Factor ($Q$)**: Sharpness of resonant modes computed via $-3\text{dB}$ half-power bandwidth:
  $$Q = \frac{f_{\text{peak}}}{f_{\text{high},-3\text{dB}} - f_{\text{low},-3\text{dB}}}$$
- **Energy Decay Rate**: Least-squares linear regression slope of RMS sound pressure ($\text{dB/s}$) during transient ring-down.

### 3. Non-Destructive Material Classification
- **`HOLLOW / CAVITY`**: Sharp sustained resonance modes ($f > 1.1\text{ kHz}$, high $Q > 10.0$, low mechanical damping decay rate $< 32\text{ dB/s}$).
- **`SOLID / SUBSTRATE`**: Rapid energy dissipation, high internal damping ($> 38\text{ dB/s}$), low $Q < 6.5$, broad low-frequency smear ($< 950\text{ Hz}$).
- **`DELAMINATION / VOID`**: Secondary harmonic split modes and acoustic boundary impedance mismatch.

---

## 💎 Monetization & Entitlements (RevenueCat)

Managed via `BillingManager.kt` utilizing the RevenueCat SDK:
- **Entitlement ID**: `pro_access`
- **Free Tier**: Real-time Waterfall Spectrogram, Live Void Tap Detection, FFT Spectrum Curve, Calibration.
- **Pro Tier**:
  1. **Active Sonar Chirp Mode**: Linear Frequency Modulated (LFM) sweep pulse ($4\text{ kHz} \to 16\text{ kHz}$) with matched filter cross-correlation echo Time-of-Flight (ToF) distance calculation.
  2. **Acoustic Liquid Level Estimator**: Quarter-wave acoustic column resonance shift calculation for closed/open container fill level gauging.
  3. **Standardized PDF Certification & CSV Export**: ISO-compliant PDF inspection reports with embedded spectral plots and raw FFT CSV datasets.
- **Judge / Reviewer Debug Bypass**: In-app switch toggle inside `PaywallSheet.kt` and top bar to immediately test Pro features without purchase or sandbox credentials.

---

## 🎨 UI/UX Cybernetic Design

- **Theme**: Obsidian Void (`#070B12`), Deep Slate (`#0D1524`), Cyber Cyan (`#00F0FF`), Electric Emerald (`#00FF9D`), Neon Amber (`#FFB800`), Quantum Violet (`#9D4EDD`), Plasma Pink (`#FF0055`).
- **Waterfall Spectrogram**: zero-alloc Bitmap ring buffer on Compose `Canvas`, 60fps, subtle lab grid.
- **Responsive Telemetry Grid**: animated confidence + peak/Q/decay/SNR pills.

## Why It Wins (Shipaton Angles)

### HAMM — Monetization that fits
- Free: live waterfall + spectrum + tap classification + calibration. Enough to feel magic in 10s.
- Pro (`pro_access` via RevenueCat): Active Sonar ToF, Liquid estimator, PDF + CSV export.
- Paywall uses **live `Offerings.current`** (no hardcoded prices), shows trial eligibility, annual/monthly/lifetime, restore + judge bypass. Annual anchored as BEST VALUE for 58% saving story.
- Growth loop: free tap → snapshot history → watermarked share → “Unlock certified PDF” → Pro.

### Design — Calm lab instrument
- Dark `#0B0E14` / `#11161F`, single sky accent `#38BDF8`, 16dp cards, sans semibold headers. No neon shouting.
- Patterns: `InstrumentCard` + `SectionHeader` + `StatusBanner` + `EmptyState` + `SignalQualityMeter`. Subtle `#232E44` chart grid, amber peak dot.

### Peace / Real-world use
- Renters tap walls for voids, makers check prints, field techs gauge tanks without opening. No cloud, no PII, works offline after install.

## Judge Test in 60s
1. Grant mic → tap Record → tap table twice. See peak + Q + verdict settle.
2. Top bar camera → EXPORT_LOGS → share snapshot PDF (Pro via Bypass).
3. ACTIVE_SONAR → Emit chirp → distance cm. LIQUID_LEVEL → move height slider → fill %.
4. Paywall → toggle Judge Bypass → all Pro unlocks, restore works offline.

# ResonanceLab — Acoustic Material Analysis (Technical Deep Dive)

Professional-grade Android instrument for non-destructive acoustic inspection.
Tap a surface, read resonance, classify material, estimate distance / liquid level, export certified reports.

## What It Does

- **Live spectrum:** 48 kHz capture, 2048-pt FFT, Hann window, ~93 fps waterfall + magnitude curve.
- **Classification:** hollow / solid / void / ambient from peak Hz, Q-factor, decay rate, centroid, SNR.
- **Active sonar (Pro):** 4–16 kHz LFM chirp, matched-filter echo, time-of-flight to distance.
- **Liquid level (Pro):** quarter-wave air-column shift to fill % / height cm.
- **Export (Pro):** PDF inspection certificate + raw CSV spectra, share via FileProvider.
- **Billing:** RevenueCat Pro entitlement + judge/demo bypass switch.

## Architecture

- `audio/AudioRecordManager` — capture loop, FFT dispatch, state flows.
- `audio/AudioConfig` — 48 kHz, 2048 FFT, 512 hop, NYQUIST + BIN_RESOLUTION helpers.
- `dsp/FastFourierTransform` — radix-2 FFT, magnitude spectrum, no alloc in loop.
- `dsp/HannWindow` — precomputed coeffs, in-place apply.
- `dsp/SpectralMetricsCalculator` — peak, centroid, Q (-3 dB), decay (linear fit), SNR, RMS.
- `dsp/MaterialClassifier` — threshold + confidence model with SNR penalty.
- `dsp/LiquidLevelEstimator` — `f = c/4L` inversion, clamped 0–100%.
- `audio/SonarChirpGenerator` — LFM sweep builder + correlator input.
- `ui/viewmodel/ResonanceViewModel` — capture, tab, snapshot history, export, paywall state.
- `ui/components/*` — `SpectrogramCanvas` (Bitmap waterfall), `SpectrumBarChart`, `MaterialStateCard`, `CalibrationBar`, `SonarChirpPanel`, `LiquidLevelPanel`, `PaywallSheet`, `UiKit` (cards, banners, empty states).
- `util/MeasurementFormat + AcousticValidation` — formatting + usability guards (pure, unit-testable).

## DSP Notes

- Q = `f0 / Δf(-3 dB)`; decay = least-squares dB/s over history; SNR = peak - calibrated floor.
- Low SNR (<10 dB) linearly penalizes confidence — see `AcousticValidation.confidencePenaltyForLowSnr`.
- Impact gate: `snr>=6 dB && rms>floor+3 dB && rms>-70 dBFS` — otherwise `AMBIENT_NOISE`.
- Sonar distance: `d = t*c/2`, c=343.2 m/s; reject conf<0.3.
- Liquid: `L_air = c/4f`, `fill = 1 - L_air/H`; H=10–100 cm slider.

## UI System

- Dark lab theme: `#0B0E14` bg, `#11161F` surface, `#38BDF8` primary, 16 dp cards, 12 dp pills.
- Typography: Default sans, semibold headers, muted 12–13 sp body.
- Patterns: `InstrumentCard` + `SectionHeader` + `StatusBanner` + `EmptyState` + `SignalQualityMeter`.
- Charts: subtle `#232E44` grid, single accent line, amber peak dot.

## Build / Run

- Android Studio Ladybug+, minSdk 26+, JDK 17.
- `local.properties`: `sdk.dir=...`
- Debug run: `./gradlew :app:installDebug`
- Release: `./gradlew :app:bundleRelease`
- Tests: `./gradlew testDebugUnitTest`

## Permissions

- `RECORD_AUDIO` (runtime), `POST_NOTIFICATIONS` only if foreground-service capture is enabled.
- No network except RevenueCat + Play Billing.

## Known Limits / Roadmap

- Single-mic phone DSP — not a calibrated NDT probe; keep 5–20 cm tap distance.
- Noise floor is per-session; recalibrate on room change via Calibration card.
- Next: multi-tap averaging, secondary-peak inharmonicity, cloud history, CSV import.
