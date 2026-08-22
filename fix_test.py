import re

with open('app/src/test/java/com/audiotune/studio/audio/dsp/DspReferenceAccuracyTest.kt', 'r') as f:
    code = f.read()

# Fix compressor
code = code.replace("processor.attackMs = 1f // Fast attack to settle quicker", "processor.attackMs = 5f")
code = code.replace("processor.releaseMs = 10f", "processor.releaseMs = 100f")
code = code.replace("val expectedOut = if (inRms > -12.0) {", "val expectedOut = if (levelDb > -12.0) {\n                inRms - ((levelDb - -12.0) * (1.0 - 1.0 / 4.0))\n            } else {")
code = code.replace("-12.0 + (inRms - -12.0) / 4.0", "inRms") # This line is replaced above actually
# Let's do a better replace for compressor expectedOut
code = re.sub(
r"val expectedOut = if \(inRms > -12\.0\) \{[^{}]*\} else \{[^{}]*\}",
"""val expectedOut = if (levelDb > -12.0) {
                inRms - ((levelDb - -12.0) * (1.0 - 1.0 / 4.0))
            } else {
                inRms
            }""", code)

# Fix NoiseGate
code = code.replace("if (inRms < -40.0) {", "if (levelDb < -40.0) {")
code = code.replace("processor.attackMs = 1f\n        processor.releaseMs = 10f", "processor.attackMs = 5f\n        processor.releaseMs = 100f")

# Fix Expander
code = re.sub(
r"if \(inRms < -40\.0\) \{[^{}]*\} else \{",
"""if (levelDb < -40.0) {
                val expectedGainChange = (levelDb - -40.0) * (2f - 1f)
                val expectedOutput = inRms + expectedGainChange
                assertTrue("Expander should attenuate properly below threshold. Expected: $expectedOutput, Actual: $outRms", abs(outRms - expectedOutput) < 2.0)
            } else {""", code)

with open('app/src/test/java/com/audiotune/studio/audio/dsp/DspReferenceAccuracyTest.kt', 'w') as f:
    f.write(code)
