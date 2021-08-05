#pragma once

#include "ao_data_def.h"

class _declspec(dllexport) WavefrontSensor {
private:
    // parameters
    float        intensity_threshold = 0.2;
    int pixel_bits          = 8;
    int          n_iterations        = 5;
    float        reduction_factor    = 0.5;

    // lenslets
    float    search_radius;
    int      n_lenslets = 0;
    Lenslet *lenslets;

    // defocus and astig
    float *defocus_gradient, *astig_0_deg_gradient, *astig_45_deg_gradient;
    float *original_centroids;

public:
    // constructor
    WavefrontSensor();

    // destructor
    ~WavefrontSensor();

    // Initialize
    bool Initialize(int n_iterations, float reduction_factor, float intensity_threshold);
    bool Finalize();

    bool SetReferenceCentroids(float *reference_centroids, int n_centroids, float search_box_radius);

    // Find reference centroids
    bool FindReferenceCentroids(unsigned char *img, int rows, int cols, int max_spacing, float *v1_x, float *v1_y, float *v2_x, float *v2_y);
    bool ReviseRefereceCentroids(int center_x, int center_y, int radius, float *pupil_center_x, float *pupil_center_y, float *pupil_radius);
    bool DeterminCurrentPupil(int center_x, int center_y, int radius, float *pupil_center_x, float *pupil_center_y, float *pupil_radius);

    // Find current centroids
    bool FindCurrentCentroids(unsigned char *img, int rows, int cols);
    bool ReviseCurrentCentroids(unsigned char *img, int rows, int cols);

    float GetPupilRadius();
    float GetSearchRadius();
    int   GetNumberOfCentroids();
    int   GetNumberOfEnabledCentroids();
    int   GetNumberOfValidCentroids();

    bool GetReferenceCentroid(int lenslet_index, float *row, float *col);
    bool GetCurrentCentroid(int lenslet_index, float *row, float *col);
    bool GetReferenceCentroids(float *reference_centroids, int *num);
    bool GetCurrentCentroids(float *current_centroids, int *num);
    bool GetSphereAndCylinder(float lenslet_focal_length, float *sphere, float *cylinder, float *orientation);
    bool GetTipAndTilt(float *tip, float *tilt);
    bool NullReferenceTipAndTilt();
    bool GetShifts(float *shifts);
    bool GetStrictShifts(float *shifts);
    bool GetValidShifts(float *shifts, int *num);
    bool GetMaxShift(float *max_shift);
    bool SetFocus(float focus_in_D, float lenslet_focal_length);
    bool GetFocusShifts(float focus_in_D, float lenslet_focal_length, float *shifts);
    bool ResetReferenceCentroids();

private:
    void SetSize(const int num);

    // Iterative centroiding
    bool CalculateCentroids(void *imageDataPtr, int bitsPerPixel, int image_Width, int image_Height, int numIterations, float boxReductionFactor,
                            float thresholdIntensityFactor,  // value [0.0,1.0] for rejecting low intensity centroids
                            bool  isCalculationFractional);

    bool RemoveDuplicatedLenslets();
    bool GenerateDefocusAndAstigDisplacements(float pupil_center_x, float pupil_center_y);
};