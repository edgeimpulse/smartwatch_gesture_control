#include <jni.h>
#include <string>
#include <vector>
#include "edge-impulse-sdk/classifier/ei_run_classifier.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_wizbulb_presentation_MainActivity_getFeatureCount(JNIEnv* env, jobject) {
    return EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_wizbulb_presentation_MainActivity_runInference(
        JNIEnv* env,
        jobject,
        jfloatArray data
) {
    jsize length = env->GetArrayLength(data);
    jfloat* inputPtr = env->GetFloatArrayElements(data, nullptr);
    std::vector<float> rawFeatures(inputPtr, inputPtr + length);
    env->ReleaseFloatArrayElements(data, inputPtr, 0);

    if ((int)rawFeatures.size() != EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE) {
        std::string err = "Expected " + std::to_string(EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE)
                        + " floats, got " + std::to_string(rawFeatures.size());
        return env->NewStringUTF(err.c_str());
    }

    ei_impulse_result_t result;
    signal_t signal;
    numpy::signal_from_buffer(rawFeatures.data(), rawFeatures.size(), &signal);

    EI_IMPULSE_ERROR res = run_classifier(&signal, &result, false);
    if (res != EI_IMPULSE_OK) {
        std::string err = "Classifier error: " + std::to_string(res);
        return env->NewStringUTF(err.c_str());
    }

    // Return best label and confidence as "label:confidence"
    uint32_t best = 0;
    for (uint32_t i = 1; i < EI_CLASSIFIER_LABEL_COUNT; i++) {
        if (result.classification[i].value > result.classification[best].value) best = i;
    }

    std::string output = std::string(result.classification[best].label)
                       + ":" + std::to_string(result.classification[best].value);
    return env->NewStringUTF(output.c_str());
}
