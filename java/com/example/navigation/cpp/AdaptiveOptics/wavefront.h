#pragma once
#include "ao_data_def.h"

class WavefrontSensor;
class Camera;

class _declspec(dllexport) Wavefront {
private:
    WavefrontSensor *m_ws;
    Camera *         m_camera;

    float *current_centroids;
    float *shifts;

public:
    // constructor
    Wavefront();

    // destructor
    ~Wavefront();

    bool SetReferenceWavefront(AoParams &ao_params, float *v1_x, float *v1_y, float *v2_x, float *v2_y, float *pupil_x, float *pupil_y, float *pupil_radius);

    bool GetCurrentWavefront(AoParams &ao_params, float *current_centroids, int *valid_num, float *all_shifts, float *tip, float *tilt, float *sphere, float *cylinder, float *orientation);

    bool GetCurrentWavefront(AoParams &ao_params, float *current_centroids, int *valid_num, float *valid_shifts, float *pupil_x, float *pupil_y, float *pupil_radius);

    bool DeterminePupilReference(AoParams &ao_params, float *pupil_x, float *pupil_y, float *pupil_radius);

    bool NullReferenceTipAndTilt(AoParams &ao_params);

    bool GetArraySize(int *byte_size);

    float GetPupilSize();

    bool GetResults(AoData &ao_data);

    bool Initialize(Camera *camera, AoData &ao_data, AoParams &ao_params, float *pupil_x, float *pupil_y, float *pupil_radius);

    bool Finalize();
};