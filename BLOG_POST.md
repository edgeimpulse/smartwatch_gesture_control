# Gesture-controlled smart lights with a Galaxy Watch and Edge Impulse

I clapped twice. The lights went off.

No voice command. No phone. No hub. Just a Galaxy Watch 4 on my wrist, an ML model running on its processor, and a UDP packet fired across the local network. The whole thing took an afternoon to build — and most of that time was collecting training data.

Here is how it works.

---

## The idea

Smartwatches are underrated as gesture remotes. They sit on your wrist, they have an IMU, and — crucially — they are already there when you want to adjust the lights without hunting for your phone. The catch is that getting a custom ML model onto a Wear OS watch is supposed to be painful: ABI mismatches, stripped-down runtimes, JNI boilerplate, toolchains that argue with each other.

Edge Impulse removes most of that pain. The C++ Android deployment exports a self-contained inference library you compile straight into the app. No pre-built TFLite binaries to wrestle with. I decided to find out how far that could take me.

---

## Collecting data

The Galaxy Watch 4 exposes accelerometer and gyroscope data via the standard Android `SensorManager` API at up to 50 Hz. I built a small data-forwarding app that streamed both axes over UDP to the Edge Impulse data ingestion endpoint — six channels in total: `acc_x`, `acc_y`, `acc_z`, `gyr_x`, `gyr_y`, `gyr_z`.

I recorded five gesture classes:

- **double_clap** — two sharp claps
- **brighten** — wrist flick upward
- **dim** — wrist flick downward
- **color_cycle** — slow circular wrist motion
- **idle** — wrist at rest, normal movement

About 30 samples per class, two seconds per sample. The whole collection session took roughly 20 minutes. Longer than the actual coding.

---

## Training

Inside Edge Impulse Studio the setup is straightforward. A 2-second input window at 50 Hz gives 100 data points per channel — enough to capture a full gesture without drowning the model in noise. I used the Spectral Analysis DSP block, which extracts frequency-domain features from each axis, then fed those into a small dense neural network.

Training with INT8 quantisation kept the model compact. Final accuracy on the test set came out at **92%**, with most of the misses being `idle` samples that contained slight wrist movement — a known hard case that better data collection would address.

---

## The tricky bit

Galaxy Watch 4 runs a 32-bit ARMv7 userspace — `armeabi-v7a` — even though the underlying Exynos W920 processor is 64-bit capable. Wear OS 3 on the Watch 4 never enables 64-bit mode, which limits your options.

The deeper problem is that Edge Impulse runs inference through **TFLite Micro** — the embedded variant of TensorFlow Lite designed for microcontrollers. Unlike the full TFLite runtime, there are no pre-built TFLite Micro libraries for Android. It always has to be compiled from source.

Edge Impulse's C++ Android deployment handles this entirely by exporting TFLite Micro source code alongside the model. CMake compiles everything from scratch targeting whatever ABI you specify. One line in `build.gradle.kts`:

```kotlin
ndk { abiFilters.add("armeabi-v7a") }
```

That is the entire ABI configuration. Everything else is handled by the build.

---

## Wiring it up

The inference pipeline in the watch app runs in two stages.

First, a motion detector monitors the accelerometer variance over a rolling 10-sample window at 50 ms intervals. At rest, variance sits near zero. A gesture spikes it above a threshold almost immediately — no model needed for this step, just arithmetic. This keeps the CPU nearly idle when nothing is happening.

When motion is detected, the app waits one second to let the gesture fully develop, then passes the last two seconds of IMU data to the Edge Impulse classifier via a JNI call:

```kotlin
val result     = runInference(sensorCollector.buildInputArray(featureCount))
val label      = result?.substringBefore(":")
val confidence = result?.substringAfter(":")?.toFloatOrNull() ?: 0f
```

Results below 65% confidence are discarded. Everything else maps to a smart light command — toggle, bright, dim, or colour cycle — sent as a UDP packet to the bulb's local IP.

A partial wake lock keeps the sensor pipeline alive when the screen is off, which is the normal state for a watch.

---

## Results

End-to-end latency from gesture completion to bulb response is around 1.1 seconds — the one-second capture window dominates. That is perfectly comfortable for light control; you are not trying to play a video game.

The model runs reliably across normal daily movement. The variance gate filters out walking and typing almost completely, so false triggers are rare in practice. Running continuously off-screen the watch loses perhaps 5–10% more battery than standby — acceptable for a demonstration, worth optimising for daily use.

---

## What's next

The obvious improvement is tighter training data — more samples, more variation in wrist angle and movement speed. Beyond that, the same architecture would support scene presets (a slow double-tap for "movie mode", a Z-motion for "goodnight"), which would make this genuinely useful rather than just a satisfying demo.

The full source code is on GitHub at **[YOUR REPO URL]**, including the Edge Impulse deployment, the Wear OS app, and a helper script to swap in a retrained model in one command.

If you build something with it — or train a better model — post it in the Edge Impulse community forum. I want to see what gestures other people think are natural.

---

> **Before publishing, fill in:**
> - Replace `[YOUR REPO URL]` with the actual GitHub link
> - Confirm the **92%** accuracy figure matches your Edge Impulse Studio test result
> - Optionally measure and replace the **5–10% battery** estimate with a real number
