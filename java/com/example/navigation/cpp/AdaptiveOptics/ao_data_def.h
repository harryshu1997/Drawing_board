#pragma once

/****************************************
            struct Lenslet
*****************************************/
struct Lenslet {
    // flag for lenslet in reference centroid calculation
    bool Enabled;

    // flag indicating valid image content
    bool isValid;

    // search box radius
    float search_box_radius;

    // reference and detected coords (column,row) correspond to (x,y)
    float ReferenceRow, ReferenceCol;

    float CentroidRow, CentroidCol;

    // default constructor initializing variables to zero
    Lenslet()
    {
        Enabled = isValid = true;
        search_box_radius = 0.0;
        ReferenceRow = ReferenceCol = CentroidRow = CentroidCol = 0.0;
    }
};

struct AoData {
    // camera info
    int height, width;

    // reference wavefront info
    int    n_centroids;
    float *reference_centroids;
    float  search_radius;

    // dm info and ao calibration results
    int    n_zernike, n_actuators;
    float *control_matrix;

    AoData()
    {
        height = width = 0;

        n_centroids         = 0;
        reference_centroids = nullptr;
        search_radius       = 0.0;

        n_zernike      = 0;
        n_actuators    = 0;
        control_matrix = nullptr;
    }
};

struct AoParams {
    // wavefront parameters
    int   center_x, center_y, radius;
    int   max_spacing;
    int   n_iterations;
    float reduction_factor;
    float intensity_threshold;
    float lenslet_focal_length_in_mm;
    int   zernike_coeff_num;
    float pixel_size;
    float pupil_size_mm;
    float focal_length_mm;
    float camera_pixel_size_mm;

    // ao calibration paramters
    int   condition_number;
    int   n_explore_steps;
    int   n_repetitions;
    float max_shift;

    // ao control parameters
    float bleed;
    float gain;

    AoParams()
    {
        center_x                   = 512;
        center_y                   = 512;
        radius                     = 1024;
        max_spacing                = 50;
        n_iterations               = 5;
        reduction_factor           = 0.5;
        intensity_threshold        = 0.2;
        lenslet_focal_length_in_mm = 15.0;
        zernike_coeff_num          = 26;
        pupil_size_mm              = 3;
        focal_length_mm            = 10;
        camera_pixel_size_mm       = 0.098;

        condition_number = 20;
        n_explore_steps  = 50;
        n_repetitions    = 10;
        max_shift        = 0.8;

        bleed = 0.2;
        gain  = 0.5;
    }
};