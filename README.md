# ResonanceLab (Acoustic Material Testing & Sonar App)

ResonanceLab is a production-grade native Android application engineered for non-destructive acoustic material testing (NDT), structural void detection, and active sonar echo location using Jetpack Compose, high-performance Digital Signal Processing (DSP), and RevenueCat monetization.

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
- **Waterfall Spectrogram**: 2D rolling bitmap buffer rendered on Compose `Canvas` with continuous heatmap color interpolation.
- **Responsive Telemetry Grid**: Real-time HUD cards with animated confidence meters and metric pills.
