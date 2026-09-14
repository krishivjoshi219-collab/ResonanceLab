# ResonanceLab — Tap. Listen. Know. | RevenueCat Shipaton 2026 (Next Gen)

> Pocket NDT lab: tap any wall, tank or slab → live 48 kHz resonance, hollow/solid/void verdict, sonar distance + liquid level, certified PDF. Free to try, Pro to certify.
>
> **Stack:** 100% Kotlin + Jetpack Compose + RevenueCat Paywalls + Entitlements (`pro_access`).
> **Track:** Next Gen Award — student entry. No Play release required. Judges: use `Paywall → Test Mode Unlock` for instant Pro, no purchase needed.
> **Demo video:** [Add YouTube/Vimeo link here, <2 min] <!-- Required for Next Gen -->
> **License:** MIT — see `LICENSE` (public open-source repo, required for Next Gen).

ResonanceLab is a native Android instrument for non-destructive acoustic testing, void detection and sonar ranging.

## Why Next Gen judges should care

1. **Clear, useful idea:** renters tap walls for voids, makers check prints, field techs gauge tanks without opening. Works offline, no cloud, no PII.
2. **Instant demo, zero mic — no one else does this:** `Try instantly — no mic needed` plays realistic Hollow door / Solid wall / Void tile taps with full spectra. Judges on emulator see magic in <2s. Permission screen has `Continue in Demo Mode`.
3. **Resonance DNA identity:** every verdict gets a glyph (◯ Ring / ■ Block / ◈ Crack) + `peakHz Q • #Shipaton` footer. Memorable in video, shareable for #BuildInPublic.
4. **Thoughtful RevenueCat use:** free tier gives magic in 10s (waterfall + spectrum + tap classification + calibration + all 3 demo taps). Pro (`pro_access`) unlocks Active Sonar ToF, Liquid estimator, PDF + CSV export. Paywall uses live `Offerings.current` (no hardcoded prices), trial eligibility, annual/monthly/lifetime, restore + Test Mode Unlock.
5. **Care in craft:** calm lab instrument theme (`#0B0E14` bg, `#11161F` surface, `#38BDF8` accent, 16dp cards), custom sonar-pulse launcher icon, `InstrumentCard` + `SectionHeader` + `StatusBanner` + `EmptyState` + `SignalQualityMeter`, subtle `#232E44` chart grid.

## 90-second video script (steal this)

0-10s: "Every wall sounds different. Tap. Listen. Know." → tap all 3 demo buttons, verdicts snap Hollow/Solid/Void.
10-40s: grant mic, tap real table twice, show peak + Q + DNA glyph settle.
40-70s: Test Mode Unlock → sonar chirp distance → liquid slider → PDF certificate.
70-90s: "Free to try, Pro to certify — built with RevenueCat." Show paywall with live offerings.

## What it does

- **Live spectrum:** 48 kHz capture, 2048-pt FFT, Hann window, ~93 fps waterfall + magnitude curve.
- **Classification:** hollow / solid / void / ambient from peak Hz, Q-factor, decay rate, centroid, SNR.
- **Active sonar (Pro):** 4–16 kHz LFM chirp, matched-filter echo, time-of-flight to distance.
- **Liquid level (Pro):** quarter-wave air-column shift to fill % / height cm.
- **Export (Pro):** PDF inspection certificate + raw CSV spectra, share via FileProvider.
- **Billing:** RevenueCat `pro_access` entitlement + Test Mode Unlock switch for judges.

## DSP notes

- Q = `f0 / Δf(-3 dB)`; decay = least-squares dB/s over history; SNR = peak - calibrated floor.
- Low SNR (<10 dB) linearly penalizes confidence.
- Impact gate: `snr>=6 dB && rms>floor+3 dB && rms>-70 dBFS` — otherwise `AMBIENT_NOISE`.
- Sonar distance: `d = t*c/2`, c=343.2 m/s; reject conf<0.3.
- Liquid: `L_air = c/4f`, `fill = 1 - L_air/H`; H=10–100 cm slider.

## Architecture

- `audio/AudioRecordManager` — capture loop, FFT dispatch, state flows.
- `audio/AudioConfig` — 48 kHz, 2048 FFT, 512 hop.
- `dsp/FastFourierTransform` — radix-2 FFT, magnitude spectrum, no alloc in loop.
- `dsp/HannWindow` — precomputed coeffs, in-place apply.
- `dsp/SpectralMetricsCalculator` — peak, centroid, Q (-3 dB), decay, SNR, RMS.
- `dsp/MaterialClassifier` — threshold + confidence model with SNR penalty.
- `dsp/LiquidLevelEstimator` — `f = c/4L` inversion, clamped 0–100%.
- `audio/SonarChirpGenerator` — LFM sweep builder + correlator input.
- `ui/viewmodel/ResonanceViewModel` — capture, tab, snapshot history, export, paywall state.
- `ui/components/*` — `SpectrogramCanvas` (Bitmap waterfall), `SpectrumBarChart`, `MaterialStateCard`, `CalibrationBar`, `SonarChirpPanel`, `LiquidLevelPanel`, `PaywallSheet`, `UiKit`.
- `util/MeasurementFormat + AcousticValidation` — formatting + usability guards (pure, unit-testable).

## Judge test in 60s

1. Grant mic → tap Record → tap table twice. See peak + Q + verdict settle.
2. Top bar camera → EXPORT_LOGS → share snapshot PDF (Pro via Test Mode Unlock).
3. ACTIVE_SONAR → Emit chirp → distance cm. LIQUID_LEVEL → move height slider → fill %.
4. Paywall → toggle Test Mode Unlock → all Pro unlocks, restore works offline.

## Build / run

- Android Studio Ladybug+, minSdk 26+, JDK 17.
- `local.properties`: `sdk.dir=...`
- Debug run: `./gradlew :app:installDebug` (or open in Android Studio → Run)
- Release: `./gradlew :app:bundleRelease`
- Tests: `./gradlew testDebugUnitTest`
- **No local build?** Push to `main` → GitHub Actions builds the debug APK in the cloud (`.github/workflows/build-apk.yml`) → download it from Actions → Artifacts → `ResonanceLab-debug-apk`. No secrets required (demo RevenueCat key).

## Permissions

- `RECORD_AUDIO` (runtime), `POST_NOTIFICATIONS` only if foreground-service capture is enabled.
- No network except RevenueCat + Play Billing.

## Known limits / roadmap

- Single-mic phone DSP — not a calibrated NDT probe; keep 5–20 cm tap distance.
- Noise floor is per-session; recalibrate on room change via Calibration card.
- Next: multi-tap averaging, secondary-peak inharmonicity, cloud history, CSV import.
