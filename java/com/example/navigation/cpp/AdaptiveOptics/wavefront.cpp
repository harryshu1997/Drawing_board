#include "wavefront.h"
#include "wavefront_sensor.h"
#include "system/Camera/camera.h"

Wavefront::Wavefront()
{
    m_ws = new WavefrontSensor;
}

Wavefront::~Wavefront()
{
    if(m_ws != nullptr) {
        delete m_ws;
        m_ws = nullptr;
    }

    Finalize();
}

bool Wavefront::Initialize(Camera *camera, AoData &ao_data, AoParams &ao_params, float *pupil_x, float *pupil_y, float *pupil_radius)
{
    // chech parameters
    if(camera->GetImageHeight() != ao_data.height || camera->GetImageWidth() != ao_data.width)
        return false;

    m_camera = camera;
    m_ws->SetReferenceCentroids(ao_data.reference_centroids, ao_data.n_centroids, ao_data.search_radius);
    m_ws->ReviseRefereceCentroids(ao_params.center_x, ao_params.center_y, ao_params.radius, pupil_x, pupil_y, pupil_radius);

    return true;
}

bool Wavefront::SetReferenceWavefront(AoParams &ao_params, float *v1_x, float *v1_y, float *v2_x, float *v2_y, float *pupil_x, float *pupil_y, float *pupil_radius)
{
    unsigned char *frame;
    int            rows, cols;

    m_ws->Initialize(ao_params.n_iterations, ao_params.reduction_factor, ao_params.intensity_threshold);

    // estimate reference wavefront
    m_camera->GetOneFrame(&frame, &rows, &cols, false);
    m_ws->FindReferenceCentroids(frame, rows, cols, ao_params.max_spacing, v1_x, v1_y, v2_x, v2_y);
    m_ws->ReviseRefereceCentroids(ao_params.center_x, ao_params.center_y, ao_params.radius, pupil_x, pupil_y, pupil_radius);

    return true;
}

bool Wavefront::GetCurrentWavefront(AoParams &ao_params, float *current_centroids, int *valid_num, float *all_shifts, float *tip, float *tilt, float *sphere, float *cylinder, float *orientation)
{
    unsigned char *frame;
    int            rows, cols;

    m_ws->Initialize(ao_params.n_iterations, ao_params.reduction_factor, ao_params.intensity_threshold);

    // estimate current wavefront
    m_camera->GetOneFrame(&frame, &rows, &cols, false);
    m_ws->FindCurrentCentroids(frame, rows, cols);
    m_ws->GetCurrentCentroids(current_centroids, valid_num);
    m_ws->GetShifts(all_shifts);
    m_ws->GetTipAndTilt(tip, tilt);
    m_ws->GetSphereAndCylinder(ao_params.lenslet_focal_length_in_mm * 0.001, sphere, cylinder, orientation);

    return true;
}

bool Wavefront::GetCurrentWavefront(AoParams &ao_params, float *current_centroids, int *valid_num, float *valid_shifts, float *pupil_x, float *pupil_y, float *pupil_radius)
{
    unsigned char *frame;
    int            rows, cols;

    m_ws->Initialize(ao_params.n_iterations, ao_params.reduction_factor, ao_params.intensity_threshold);

    // estimate current wavefront
    m_camera->GetOneFrame(&frame, &rows, &cols, false);
    m_ws->FindCurrentCentroids(frame, rows, cols);
    
    m_ws->DeterminCurrentPupil(ao_params.center_x, ao_params.center_y, ao_params.radius, pupil_x, pupil_y, pupil_radius);
    m_ws->GetCurrentCentroids(current_centroids, valid_num);
    m_ws->NullReferenceTipAndTilt();
    m_ws->GetValidShifts(valid_shifts, valid_num);

    return true;
}

bool Wavefront::DeterminePupilReference(AoParams &ao_params, float *pupil_x, float *pupil_y, float *pupil_radius)
{
    unsigned char *frame;
    int            rows, cols;

    m_ws->ResetReferenceCentroids();
    m_ws->Initialize(ao_params.n_iterations, ao_params.reduction_factor, ao_params.intensity_threshold);

    // estimate current wavefront
    m_camera->GetOneFrame(&frame, &rows, &cols, false);
    m_ws->FindCurrentCentroids(frame, rows, cols);
    m_ws->ReviseRefereceCentroids(ao_params.center_x, ao_params.center_y, ao_params.radius, pupil_x, pupil_y, pupil_radius);

    return true;
}

bool Wavefront::NullReferenceTipAndTilt(AoParams &ao_params)
{
    unsigned char *frame;
    int            rows, cols;

    m_ws->Initialize(ao_params.n_iterations, ao_params.reduction_factor, ao_params.intensity_threshold);

    // estimate current wavefront
    m_camera->GetOneFrame(&frame, &rows, &cols, false);
    m_ws->FindCurrentCentroids(frame, rows, cols);
    m_ws->NullReferenceTipAndTilt();

    return true;
}

bool Wavefront::GetArraySize(int *size)
{
    *size = m_ws->GetNumberOfEnabledCentroids() * 2;

    return true;
}

float Wavefront::GetPupilSize()
{
    return m_ws->GetPupilRadius();
}

bool Wavefront::GetResults(AoData &ao_data)
{
    int num = 0;
    // ao_data.n_centroids = m_ws->GetNumberOfEnabledCentroids();
    ao_data.search_radius = m_ws->GetSearchRadius();
    m_ws->GetReferenceCentroids(ao_data.reference_centroids, &num);
    ao_data.n_centroids = num;

    return true;
}

bool Wavefront::Finalize()
{
    return true;
}