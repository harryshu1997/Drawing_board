#include "wavefront_sensor.h"

#include <math.h>
#include <vector>
#include "algorithm/ImageProcessingToolBox/matrix_operator.h"
#include "algorithm/ImageProcessingToolBox/calculate_centroid.h"
#include <iostream>
#include <algorithm>

#define PI 3.1415926

WavefrontSensor::WavefrontSensor()
{
    intensity_threshold = 0.2;
    n_iterations        = 5;
    reduction_factor    = 0.8;
    n_lenslets          = 0;

    lenslets              = nullptr;
    defocus_gradient      = nullptr;
    astig_0_deg_gradient  = nullptr;
    astig_45_deg_gradient = nullptr;
    original_centroids    = nullptr;
}

WavefrontSensor::~WavefrontSensor()
{
    Finalize();
}

bool WavefrontSensor::Initialize(int n_iterations, float reduction_factor, float intensity_threshold)
{
    this->n_iterations        = n_iterations;
    this->reduction_factor    = reduction_factor;
    this->intensity_threshold = intensity_threshold;

    return true;
}

bool WavefrontSensor::SetReferenceCentroids(float *reference_centroids, int n_centroids, float search_box_radius)
{
    if(nullptr == reference_centroids || n_centroids == 0)
        return false;

    search_radius = search_box_radius;
    n_lenslets    = n_centroids;
    SetSize(n_lenslets);

    // set all lenslets data
    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        lenslets[lenslet_index].Enabled           = true;
        lenslets[lenslet_index].ReferenceRow      = reference_centroids[lenslet_index * 2];
        lenslets[lenslet_index].ReferenceCol      = reference_centroids[lenslet_index * 2 + 1];
        lenslets[lenslet_index].search_box_radius = search_radius;
    }

    return true;
}

bool WavefrontSensor::FindReferenceCentroids(unsigned char *img, int rows, int cols, int max_spacing, float *v1_x, float *v1_y, float *v2_x, float *v2_y)
{
    float *vectors = new float[4];
    FindLatticeVectors(img, rows, cols, max_spacing, vectors);
    *v1_x = vectors[0];
    *v1_y = vectors[1];
    *v2_x = vectors[2];
    *v2_y = vectors[3];

    n_lenslets = 0;
    std::vector<int> reference_rows;
    std::vector<int> reference_cols;

    search_radius = sqrt(vectors[0] * vectors[0] + vectors[1] * vectors[1]) / 2;
    if(search_radius > sqrt(vectors[2] * vectors[2] + vectors[3] * vectors[3]) / 2)
        search_radius = sqrt(vectors[2] * vectors[2] + vectors[3] * vectors[3]) / 2;

    // find the maximum as the origin (startting point) to sweep entile image
    int origin_row, origin_col;

    int pieces = 4;
    for(int mm = 0; mm < pieces; mm++) {
        for(int nn = 0; nn < pieces; nn++) {
            // find spots in every small piece
            int max = 0;
            for(int i = rows * mm / pieces; i < rows * (mm + 1) / pieces; i++) {
                for(int j = cols * nn / pieces; j < cols * (nn + 1) / pieces; j++) {
                    if(max < img[j + i * cols]) {
                        max        = img[j + i * cols];
                        origin_row = i;
                        origin_col = j;
                    }
                }
            }

            // make sure sweeping scope is large enough and ideal location array is in image size
            int tmp_row, tmp_col, max_row, max_col;
            int max_n_along_axis = sqrt(rows * rows + cols * cols) / pieces / search_radius;
            for(int i = -max_n_along_axis; i < max_n_along_axis; i++) {
                for(int j = -max_n_along_axis; j < max_n_along_axis; j++) {
                    tmp_row = origin_row + i * vectors[0] + j * vectors[2];
                    tmp_col = origin_col + i * vectors[1] + j * vectors[3];

                    float ratio = 1.0;
                    while(ratio < 2.0) {
                        int top_row      = tmp_row - search_radius * ratio;
                        int bottom_row   = tmp_row + search_radius * ratio;
                        int left_column  = tmp_col - search_radius * ratio;
                        int right_column = tmp_col + search_radius * ratio;

                        // check if this ROI is in image and count number of lenslets
                        if(top_row >= rows * mm / pieces && bottom_row < rows && left_column >= cols * nn / pieces && right_column < cols) {
                            max = 0;
                            for(int m = top_row; m <= bottom_row; m++) {
                                for(int n = left_column; n <= right_column; n++) {
                                    if(max < img[n + m * cols]) {
                                        max     = img[n + m * cols];
                                        max_row = m;
                                        max_col = n;
                                    }
                                }
                            }

                            // rule out dim regions
                            if(max > intensity_threshold * pow((float)2, pixel_bits)) {
                                n_lenslets++;
                                reference_rows.push_back(max_row);
                                reference_cols.push_back(max_col);
                                // reference_rows.push_back((tmp_row+max_row)/2);
                                // reference_cols.push_back((tmp_col+max_col)/2);
                                ratio = 100.1;
                            }
                            else {
                                ratio *= 1.1;
                            }
                        }
                        else
                            ratio = 100.1;
                    }
                }
            }
        }
    }

    // refine the centroids
    SetSize(n_lenslets);

    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        lenslets[lenslet_index].Enabled           = true;
        lenslets[lenslet_index].ReferenceRow      = (float)reference_rows[lenslet_index];
        lenslets[lenslet_index].ReferenceCol      = (float)reference_cols[lenslet_index];
        lenslets[lenslet_index].search_box_radius = search_radius;
    }

    // get centroids of the possible spots
    CalculateCentroids(img, pixel_bits, cols, rows, n_iterations, reduction_factor, intensity_threshold, true);

    // update the results as the reference
    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        lenslets[lenslet_index].Enabled      = lenslets[lenslet_index].isValid;
        lenslets[lenslet_index].ReferenceRow = lenslets[lenslet_index].CentroidRow;
        lenslets[lenslet_index].ReferenceCol = lenslets[lenslet_index].CentroidCol;
    }
    RemoveDuplicatedLenslets();

    delete[] vectors;

    return true;
}

bool WavefrontSensor::FindCurrentCentroids(unsigned char *img, int rows, int cols)
{
    CalculateCentroids(img, pixel_bits, cols, rows, n_iterations, reduction_factor, 0.8 * intensity_threshold, true);
    ReviseCurrentCentroids(img, rows, cols);

    return true;
}

bool WavefrontSensor::ReviseCurrentCentroids(unsigned char *img, int rows, int cols)
{
    std::vector<float> vec_mean, vec_tmp;
    float              sum = 0;
    int                row, col;
    int                cnt;

    if(lenslets != nullptr) {
        float     tmp_max = 0;
        int       id_max;
        const int len = 6;
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].isValid) {
                sum   = 0;
                cnt   = 0;
                int r = lenslets[lenslet_index].CentroidRow;
                int c = lenslets[lenslet_index].CentroidCol;

                // mean value of each centroid
                for(row = r - len; row <= r + len; row++) {
                    for(col = c - len; col <= c + len; col++) {
                        sum += (float)img[cols * row + col];
                        cnt++;
                    }
                }
                vec_mean.push_back(sum / cnt);
                if(sum / cnt > tmp_max) {
                    tmp_max = sum / cnt;
                    id_max  = lenslet_index;
                }

                if(sum / cnt + len > (int)img[cols * r + c]
                   || (int)img[cols * (r - len) + c] + len > (int)img[cols * r + c]
                   || (int)img[cols * (r + len) + c] + len > (int)img[cols * r + c]
                   || (int)img[cols * r + c - len] + len > (int)img[cols * r + c]
                   || (int)img[cols * r + c + len] + len > (int)img[cols * r + c]) {
                    lenslets[lenslet_index].isValid = false;
                }
            }
        }

        if(vec_mean.size() < 1)
            return true;

        // extract a small area
        int id = 0;
        sum    = 0;
        vec_tmp.clear();
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].isValid) {
                if(abs(lenslets[lenslet_index].CentroidRow - lenslets[id_max].CentroidRow) < search_radius * 5
                   && abs(lenslets[lenslet_index].CentroidCol - lenslets[id_max].CentroidCol) < search_radius * 5) {
                    vec_tmp.push_back(vec_mean[id]);
                    sum += vec_mean[id];
                }
                id++;
            }
        }

        if(vec_tmp.size() < 1)
            return true;
        sum /= vec_tmp.size();

        // select reasonable centroids
        vec_mean.clear();
        vec_mean.assign(vec_tmp.begin(), vec_tmp.end());
        sort(vec_mean.begin(), vec_mean.end());

        // threshold based on gradient
        float threshold = 0;
        float g_tmp = 0, g_max = 0;
        float g_mean = (vec_mean[vec_mean.size() - 1] - vec_mean[0]) / (vec_mean.size() - 1);
        float std    = 0;

        for(int i = 1; i < vec_mean.size(); i++) {
            g_tmp = vec_mean[i] - vec_mean[i - 1];
            std += (g_tmp - g_mean) * (g_tmp - g_mean);
            if(g_tmp > g_max) {
                g_max     = g_tmp;
                threshold = vec_mean[i];
                id        = i;
            }
        }
        //std::cout << threshold << std::endl;
        float g_mean1 = g_max;
        float g_mean2 = g_max;
        if(id < (vec_mean.size() - 1) && id > 1) {
            g_mean1 = vec_mean[id - 1] - vec_mean[id - 2];
            g_mean2 = vec_mean[id + 1] - vec_mean[id];
        }

        // threshold only when it is a brupt jumping
        std = sqrt(std / vec_mean.size());
        if(g_max > g_mean + std * 3 && g_max > g_mean1 + std * 2 && g_max > g_mean2 + std * 2 && threshold > sum * 1.01 + g_max) {
            // repeat iterating
            id = 0;
            for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
                if(lenslets[lenslet_index].isValid) {
                    if(abs(lenslets[lenslet_index].CentroidRow - lenslets[id_max].CentroidRow) < search_radius * 5
                       && abs(lenslets[lenslet_index].CentroidCol - lenslets[id_max].CentroidCol) < search_radius * 5) {
                        if(vec_tmp[id] > threshold - 0.001) {
                            lenslets[lenslet_index].isValid = false;
                        }
                        id++;
                    }
                }
            }
        }
    }

    return true;
}

bool WavefrontSensor::ReviseRefereceCentroids(int center_x, int center_y, int radius, float *pupil_center_x, float *pupil_center_y, float *pupil_radius)
{
    *pupil_center_x = 0.0;
    *pupil_center_y = 0.0;
    *pupil_radius   = 0.0;

    if(nullptr == lenslets || n_lenslets == 0)
        return false;

    float row_min = search_radius * 20000, row_max = 0.0, col_min = search_radius * 20000, col_max = 0.0;

    // update the useful points in pupil size as the reference
    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        if(lenslets[lenslet_index].Enabled) {
            // only look in a chosen circle ROI
            if((lenslets[lenslet_index].ReferenceCol - center_x) * (lenslets[lenslet_index].ReferenceCol - center_x)
                   + (lenslets[lenslet_index].ReferenceRow - center_y) * (lenslets[lenslet_index].ReferenceRow - center_y)
               > radius * radius) {
                lenslets[lenslet_index].Enabled = false;
            }
            else {
                if(row_min > lenslets[lenslet_index].ReferenceRow)
                    row_min = lenslets[lenslet_index].ReferenceRow;
                if(row_max < lenslets[lenslet_index].ReferenceRow)
                    row_max = lenslets[lenslet_index].ReferenceRow;
                if(col_min > lenslets[lenslet_index].ReferenceCol)
                    col_min = lenslets[lenslet_index].ReferenceCol;
                if(col_max < lenslets[lenslet_index].ReferenceCol)
                    col_max = lenslets[lenslet_index].ReferenceCol;
            }
            //lenslets[lenslet_index].Enabled = lenslets[lenslet_index].isValid;
        }
    }
    *pupil_center_y = (row_min + row_max) / 2;
    *pupil_center_x = (col_min + col_max) / 2;


    // calculate radius for zernike
    float max, tmp;
    max = 0.0;
    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        if(lenslets[lenslet_index].Enabled) {
            tmp = (lenslets[lenslet_index].ReferenceCol - *pupil_center_x) * (lenslets[lenslet_index].ReferenceCol - *pupil_center_x)
                  + (lenslets[lenslet_index].ReferenceRow - *pupil_center_y) * (lenslets[lenslet_index].ReferenceRow - *pupil_center_y);
            if(tmp > max)
                max = tmp;
        }
    }
    *pupil_radius = sqrt(max) + search_radius;

    // for internal use
    GenerateDefocusAndAstigDisplacements(*pupil_center_x, *pupil_center_y);

    return true;
}

bool WavefrontSensor::DeterminCurrentPupil(int center_x, int center_y, int radius, float *pupil_center_x, float *pupil_center_y, float *pupil_radius)
{
    *pupil_center_x = 0.0;
    *pupil_center_y = 0.0;
    *pupil_radius   = 0.0;

    if(nullptr == lenslets || n_lenslets == 0)
        return false;

    // update the useful points in pupil size as the reference
    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        if(lenslets[lenslet_index].Enabled) {
            // only look in a chosen circle ROI
            if((lenslets[lenslet_index].ReferenceCol - center_x) * (lenslets[lenslet_index].ReferenceCol - center_x)
                   + (lenslets[lenslet_index].ReferenceRow - center_y) * (lenslets[lenslet_index].ReferenceRow - center_y)
               > radius * radius) {
                lenslets[lenslet_index].isValid = false;
            }
        }
    }

    // calculate radius for zernike
    float distance = 0.0;
    float diameter = 0.0;
    int   id1 = 0, id2 = 0;

    for(int i = 0; i < n_lenslets - 1; i++) {
        for(int j = i + 1; j < n_lenslets; j++) {
            if(lenslets[i].isValid && lenslets[j].isValid) {
                distance = sqrt((lenslets[i].ReferenceRow - lenslets[j].ReferenceRow) * (lenslets[i].ReferenceRow - lenslets[j].ReferenceRow)
                                + (lenslets[i].ReferenceCol - lenslets[j].ReferenceCol) * (lenslets[i].ReferenceCol - lenslets[j].ReferenceCol));

                if(distance > diameter) {
                    diameter = distance;
                    id1      = i;
                    id2      = j;
                }
            }
        }
    }

    *pupil_radius = diameter / 2.0 + search_radius;
    *pupil_center_y = (lenslets[id1].ReferenceRow + lenslets[id2].ReferenceRow) / 2;
    *pupil_center_x = (lenslets[id1].ReferenceCol + lenslets[id2].ReferenceCol) / 2;
    
    return true;
}

bool WavefrontSensor::CalculateCentroids(void *imageDataPtr, int bitsPerPixel, int ImageWidth, int ImageHeight, int numIterations, float boxReductionFactor,
                                         float thresholdIntensityFactor,  // value [0.0,1.0] for rejecting low intensity centroids
                                         bool  isCalculationFractional)
{
    if(imageDataPtr == nullptr || n_lenslets == 0)
        return false;

    bool spotFound = false;

    float CentroidRow, CentroidCol;
    float ROITop, ROIBottom, ROILeft, ROIRight;

    unsigned int CentroidMaxIntensity = 0;
    int thresholdIntensity   = (int)(thresholdIntensityFactor * pow((float)2, (int)bitsPerPixel));

    for(int i = 0; i < n_lenslets; i++) {
        if(!lenslets[i].Enabled)
            continue;

        // use the entire search box initialially
        ROITop    = lenslets[i].ReferenceRow - lenslets[i].search_box_radius;
        ROIBottom = lenslets[i].ReferenceRow + lenslets[i].search_box_radius;
        ROILeft   = lenslets[i].ReferenceCol - lenslets[i].search_box_radius;
        ROIRight  = lenslets[i].ReferenceCol + lenslets[i].search_box_radius;

        // calculate centroid. Checking return value is important which indicates whether spot exists or not
        float airyPixel = 2.5;
        spotFound = CalculateCentroid(imageDataPtr, bitsPerPixel, ImageWidth, ImageHeight, 
                                      ROILeft, ROITop, ROIRight, ROIBottom, 
                                      numIterations, boxReductionFactor, 
                                      airyPixel, isCalculationFractional,
                                      &CentroidCol,  // Col is x in (x,y) coordinate system
                                      &CentroidRow,  // Row is y in (x,y) coordinate system
                                      &CentroidMaxIntensity);

        // std::cout << "CentroidMaxIntensity = " << CentroidMaxIntensity << std::endl;
        if(spotFound && (CentroidMaxIntensity >= thresholdIntensity)) {
            lenslets[i].isValid     = true;
            lenslets[i].CentroidCol = CentroidCol;
            lenslets[i].CentroidRow = CentroidRow;
        }
        else {
            // if the spot is not found OR the intensity is below the
            // threshold the spot displacement is forced to zero
            lenslets[i].isValid     = false;
            lenslets[i].CentroidCol = lenslets[i].ReferenceCol;
            lenslets[i].CentroidRow = lenslets[i].ReferenceRow;
        }
    }

    return true;
}

bool WavefrontSensor::RemoveDuplicatedLenslets()
{
    int                cnt = 0;
    std::vector<float> reference_rows;
    std::vector<float> reference_cols;

    float distance   = 0.0;
    float tmp_radius = search_radius * 20;

    for(int i = 0; i < n_lenslets - 1; i++) {
        for(int j = i + 1; j < n_lenslets; j++) {
            if(lenslets[i].Enabled && lenslets[j].Enabled) {
                distance = sqrt((lenslets[i].ReferenceRow - lenslets[j].ReferenceRow) * (lenslets[i].ReferenceRow - lenslets[j].ReferenceRow)
                                + (lenslets[i].ReferenceCol - lenslets[j].ReferenceCol) * (lenslets[i].ReferenceCol - lenslets[j].ReferenceCol));

                // erase points that are too near
                if(distance < search_radius) {
                    lenslets[j].Enabled = false;
                    lenslets[j].isValid = false;
                }
                else if(distance < tmp_radius)
                    tmp_radius = distance;
            }
        }
    }
    search_radius = tmp_radius / 2;

    // resize
    cnt = 0;
    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        if(lenslets[lenslet_index].Enabled) {
            cnt++;
            reference_rows.push_back(lenslets[lenslet_index].ReferenceRow);
            reference_cols.push_back(lenslets[lenslet_index].ReferenceCol);
        }
    }

    n_lenslets = cnt;
    SetSize(n_lenslets);

    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        lenslets[lenslet_index].Enabled           = true;
        lenslets[lenslet_index].ReferenceRow      = reference_rows[lenslet_index];
        lenslets[lenslet_index].ReferenceCol      = reference_cols[lenslet_index];
        lenslets[lenslet_index].search_box_radius = search_radius;
    }

    return true;
}

bool WavefrontSensor::GetReferenceCentroids(float *reference_centroids, int *num)
{
    int enabledCount = 0;
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].Enabled) {
                reference_centroids[2 * enabledCount]     = lenslets[lenslet_index].ReferenceRow;
                reference_centroids[2 * enabledCount + 1] = lenslets[lenslet_index].ReferenceCol;
                enabledCount++;
            }
        }
    *num = enabledCount;

    return true;
}

bool WavefrontSensor::GetCurrentCentroids(float *current_centroids, int *num)
{
    int validCount = 0;
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].isValid) {
                current_centroids[2 * validCount]       = lenslets[lenslet_index].CentroidRow;
                current_centroids[2 * validCount + 1] = lenslets[lenslet_index].CentroidCol;
                validCount++;
            }
        }
    *num = validCount;

    return true;
}

bool WavefrontSensor::SetFocus(float focus_in_D, float lenslet_focal_length)
{
    float one_sphere1, one_sphere2;

    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        if(lenslets[lenslet_index].Enabled) {
            one_sphere1 = defocus_gradient[2 * lenslet_index] * lenslet_focal_length;
            one_sphere2 = defocus_gradient[2 * lenslet_index + 1] * lenslet_focal_length;

            lenslets[lenslet_index].ReferenceRow = original_centroids[2 * lenslet_index] + focus_in_D * one_sphere1;
            lenslets[lenslet_index].ReferenceCol = original_centroids[2 * lenslet_index + 1] + focus_in_D * one_sphere2;
        }
    }

    return true;
}

bool WavefrontSensor::GetFocusShifts(float focus_in_D, float lenslet_focal_length, float *shifts)
{
    float one_sphere1, one_sphere2;

    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        if(lenslets[lenslet_index].Enabled) {
            one_sphere1 = defocus_gradient[2 * lenslet_index] * lenslet_focal_length;
            one_sphere2 = defocus_gradient[2 * lenslet_index + 1] * lenslet_focal_length;

            shifts[2 * lenslet_index]     = focus_in_D * one_sphere1;
            shifts[2 * lenslet_index + 1] = focus_in_D * one_sphere2;
        }
    }

    return true;
}

bool WavefrontSensor::GetSphereAndCylinder(float lenslet_focal_length, float *sphere, float *cylinder, float *orientation)
{
    float row_shift, col_shift;
    float proj_defocus_gradient = 0.0, proj_astig_0_gradient = 0.0, proj_astig_45_gradient = 0.0;
    float norm_defocus = 0.0, norm_astig0 = 0.0, norm_astig45 = 0.0;

    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        if(lenslets[lenslet_index].isValid) {
            row_shift = lenslets[lenslet_index].CentroidRow - lenslets[lenslet_index].ReferenceRow;
            col_shift = lenslets[lenslet_index].CentroidCol - lenslets[lenslet_index].ReferenceCol;

            proj_defocus_gradient += row_shift * defocus_gradient[2 * lenslet_index] + col_shift * defocus_gradient[2 * lenslet_index + 1];
            norm_defocus += defocus_gradient[2 * lenslet_index] * defocus_gradient[2 * lenslet_index] + defocus_gradient[2 * lenslet_index + 1] * defocus_gradient[2 * lenslet_index + 1];

            proj_astig_0_gradient += row_shift * astig_0_deg_gradient[2 * lenslet_index] + col_shift * astig_0_deg_gradient[2 * lenslet_index + 1];
            norm_astig0 +=
                astig_0_deg_gradient[2 * lenslet_index] * astig_0_deg_gradient[2 * lenslet_index] + astig_0_deg_gradient[2 * lenslet_index + 1] * astig_0_deg_gradient[2 * lenslet_index + 1];

            proj_astig_45_gradient += row_shift * astig_45_deg_gradient[2 * lenslet_index] + col_shift * astig_45_deg_gradient[2 * lenslet_index + 1];
            norm_astig45 +=
                astig_45_deg_gradient[2 * lenslet_index] * astig_45_deg_gradient[2 * lenslet_index] + astig_45_deg_gradient[2 * lenslet_index + 1] * astig_45_deg_gradient[2 * lenslet_index + 1];
        }
    }

    proj_defocus_gradient  = proj_defocus_gradient / norm_defocus / lenslet_focal_length;
    proj_astig_0_gradient  = proj_astig_0_gradient / norm_astig0 / lenslet_focal_length;
    proj_astig_45_gradient = proj_astig_45_gradient / norm_astig45 / lenslet_focal_length;

    *cylinder    = sqrt(3.0 * (proj_astig_0_gradient * proj_astig_0_gradient + proj_astig_45_gradient * proj_astig_45_gradient));
    *orientation = 180.0 / PI * atan2(-proj_astig_45_gradient, proj_astig_0_gradient) / 2;
    *sphere      = proj_defocus_gradient - *cylinder / 2.0;

    return true;
}

/*This function generates internal vectors used for estimating the spot displacements that would be produced
by the Zernike polynomials: defocus and astigmatisms.
This internal function is only meant to be called from within ReviseRefereceCentroids.*/
bool WavefrontSensor::GenerateDefocusAndAstigDisplacements(float pupil_center_x, float pupil_center_y)
{
    float a, b;
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            original_centroids[2 * lenslet_index]     = lenslets[lenslet_index].ReferenceRow;
            original_centroids[2 * lenslet_index + 1] = lenslets[lenslet_index].ReferenceCol;

            a = lenslets[lenslet_index].ReferenceRow - pupil_center_y;
            b = lenslets[lenslet_index].ReferenceCol - pupil_center_x;

            defocus_gradient[2 * lenslet_index]     = a;
            defocus_gradient[2 * lenslet_index + 1] = b;

            astig_0_deg_gradient[2 * lenslet_index]     = -a;
            astig_0_deg_gradient[2 * lenslet_index + 1] = b;

            astig_45_deg_gradient[2 * lenslet_index]     = b;
            astig_45_deg_gradient[2 * lenslet_index + 1] = a;
        }

    return true;
}

bool WavefrontSensor::GetTipAndTilt(float *tip, float *tilt)
{
    int   enabledCount = 0;
    float tmp1, tmp2;

    tmp1 = 0.0;
    tmp2 = 0.0;
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].isValid) {
                tmp1 += lenslets[lenslet_index].CentroidRow - lenslets[lenslet_index].ReferenceRow;
                tmp2 += lenslets[lenslet_index].CentroidCol - lenslets[lenslet_index].ReferenceCol;
                enabledCount++;
            }
        }

    *tip  = tmp1 / enabledCount;
    *tilt = tmp2 / enabledCount;

    return true;
}

bool WavefrontSensor::NullReferenceTipAndTilt()
{
    float tip, tilt;
    GetTipAndTilt(&tip, &tilt);

    // resetting the tip and tilt values for all spots (enabled or not)
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            lenslets[lenslet_index].ReferenceRow = lenslets[lenslet_index].ReferenceRow + tip;
            lenslets[lenslet_index].ReferenceCol = lenslets[lenslet_index].ReferenceCol + tilt;
        }

    return true;
}

bool WavefrontSensor::GetShifts(float *shifts)
{
    int enabledCount = 0;
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].Enabled) {
                shifts[2 * enabledCount]     = lenslets[lenslet_index].CentroidRow - lenslets[lenslet_index].ReferenceRow;
                shifts[2 * enabledCount + 1] = lenslets[lenslet_index].CentroidCol - lenslets[lenslet_index].ReferenceCol;
                enabledCount++;
            }
        }

    return true;
}

bool WavefrontSensor::GetStrictShifts(float *shifts)
{
    int   enabledCount = 0;
    float row_std = 0, col_std = 0;
    float row_mean = 0, col_mean = 0;
    float tip = 0, tilt = 0;

    // get std and mean of valid shifts
    if(lenslets != nullptr) {
        for(unsigned int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].isValid) {
                tip  = lenslets[lenslet_index].CentroidRow - lenslets[lenslet_index].ReferenceRow;
                tilt = lenslets[lenslet_index].CentroidCol - lenslets[lenslet_index].ReferenceCol;
                enabledCount++;

                row_std += tip * tip;
                col_std += tilt * tilt;
                row_mean += tip;
                col_mean += tilt;
            }
        }
        row_std = sqrt(row_std / enabledCount);
        col_std = sqrt(col_std / enabledCount);
        row_mean /= enabledCount;
        col_mean /= enabledCount;

        // expection detection
        enabledCount = 0;
        for(unsigned int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].Enabled) {
                tip  = lenslets[lenslet_index].CentroidRow - lenslets[lenslet_index].ReferenceRow;
                tilt = lenslets[lenslet_index].CentroidCol - lenslets[lenslet_index].ReferenceCol;
                if(abs(tip - row_mean) < row_std * 3 && abs(tilt - col_mean) < col_std * 3) {
                    shifts[2 * enabledCount]     = tip;
                    shifts[2 * enabledCount + 1] = tilt;
                }
                else {
                    lenslets[lenslet_index].isValid = false;
                    shifts[2 * enabledCount]        = 0;
                    shifts[2 * enabledCount + 1]    = 0;
                }
                enabledCount++;
            }
        }

        // smooth shifts
        float smooth_row = 0, smooth_col = 0;
        float weight, weight_sum         = 0;
        enabledCount = 0;
        for(unsigned int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].Enabled) {
                if(abs(shifts[2 * enabledCount]) + abs(shifts[2 * enabledCount + 1]) < 0.00000001) {
                    smooth_row = 0;
                    smooth_col = 0;
                    weight_sum = 0;
                    for(unsigned int i = 0; i < n_lenslets; i++) {
                        if(i != lenslet_index && lenslets[i].isValid) {
                            weight = 1.0 / ((lenslets[lenslet_index].ReferenceRow - lenslets[i].ReferenceRow) / search_radius * (lenslets[lenslet_index].ReferenceRow - lenslets[i].ReferenceRow) / search_radius + (lenslets[lenslet_index].ReferenceCol - lenslets[i].ReferenceCol) / search_radius * (lenslets[lenslet_index].ReferenceCol - lenslets[i].ReferenceCol) / search_radius);
                            smooth_row += weight * (lenslets[i].CentroidRow - lenslets[i].ReferenceRow);
                            smooth_col += weight * (lenslets[i].CentroidCol - lenslets[i].ReferenceCol);
                            weight_sum += weight;
                        }
                    }
                    shifts[2 * enabledCount]     = smooth_row / weight_sum;
                    shifts[2 * enabledCount + 1] = smooth_col / weight_sum;
                }
                enabledCount++;
            }
        }
    }

    return true;
}

bool WavefrontSensor::GetValidShifts(float *shifts, int *num)
{
    int enabledCount = 0;
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].isValid) {
                shifts[2 * enabledCount]     = lenslets[lenslet_index].CentroidRow - lenslets[lenslet_index].ReferenceRow;
                shifts[2 * enabledCount + 1] = lenslets[lenslet_index].CentroidCol - lenslets[lenslet_index].ReferenceCol;
                enabledCount++;
            }
        }
    *num = enabledCount;

    return true;
}

bool WavefrontSensor::GetMaxShift(float *max_shift)
{
    float tmp1, tmp2;
    *max_shift = 0.0;

    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].Enabled) {
                tmp1 = abs(lenslets[lenslet_index].CentroidRow - lenslets[lenslet_index].ReferenceRow) / lenslets[lenslet_index].search_box_radius;
                tmp2 = abs(lenslets[lenslet_index].CentroidCol - lenslets[lenslet_index].ReferenceCol) / lenslets[lenslet_index].search_box_radius;

                if(tmp1 > *max_shift)
                    *max_shift = tmp1;
                if(tmp2 > *max_shift)
                    *max_shift = tmp2;
            }
        }

    return true;
}

void WavefrontSensor::SetSize(const int num)
{
    Finalize();

    lenslets = new Lenslet[num];

    defocus_gradient      = new float[num * 2];
    astig_0_deg_gradient  = new float[num * 2];
    astig_45_deg_gradient = new float[num * 2];
    original_centroids    = new float[num * 2];
}

int WavefrontSensor::GetNumberOfCentroids()
{
    return n_lenslets;
}

int WavefrontSensor::GetNumberOfEnabledCentroids()
{
    int enabledCount = 0;
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].Enabled)
                enabledCount++;
        }

    return enabledCount;
}

int WavefrontSensor::GetNumberOfValidCentroids()
{
    int validCount = 0;
    if(lenslets != nullptr)
        for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
            if(lenslets[lenslet_index].isValid)
                validCount++;
        }

    return validCount;
}

bool WavefrontSensor::GetReferenceCentroid(int lenslet_index, float *row, float *col)
{
    if(lenslet_index >= n_lenslets || !lenslets[lenslet_index].Enabled)
        return false;

    *row = lenslets[lenslet_index].ReferenceRow;
    *col = lenslets[lenslet_index].ReferenceCol;

    return true;
}

bool WavefrontSensor::GetCurrentCentroid(int lenslet_index, float *row, float *col)
{
    if(lenslet_index >= n_lenslets || !lenslets[lenslet_index].isValid)
        return false;

    *row = lenslets[lenslet_index].CentroidRow;
    *col = lenslets[lenslet_index].CentroidCol;

    return true;
}

bool WavefrontSensor::ResetReferenceCentroids()
{
    // set all lenslets data
    for(int lenslet_index = 0; lenslet_index < n_lenslets; lenslet_index++) {
        lenslets[lenslet_index].Enabled = true;
    }

    return true;
}

float WavefrontSensor::GetPupilRadius()
{
    float distance = 0.0;
    float diameter = 0.0;

    for(int i = 0; i < n_lenslets - 1; i++) {
        for(int j = i + 1; j < n_lenslets; j++) {
            if(lenslets[i].isValid && lenslets[j].isValid) {
                distance = sqrt((lenslets[i].ReferenceRow - lenslets[j].ReferenceRow) * (lenslets[i].ReferenceRow - lenslets[j].ReferenceRow)
                                + (lenslets[i].ReferenceCol - lenslets[j].ReferenceCol) * (lenslets[i].ReferenceCol - lenslets[j].ReferenceCol));

                if(distance > diameter) {
                    diameter = distance;
                }
            }
        }
    }

    return diameter / 2.0 + search_radius;
}

float WavefrontSensor::GetSearchRadius()
{
    return search_radius;
}

bool WavefrontSensor::Finalize()
{
    if(lenslets != nullptr) {
        delete[] lenslets;
        lenslets = nullptr;
    }

    if(defocus_gradient != nullptr) {
        delete[] defocus_gradient;
        defocus_gradient = nullptr;
    }

    if(astig_0_deg_gradient != nullptr) {
        delete[] astig_0_deg_gradient;
        astig_0_deg_gradient = nullptr;
    }

    if(astig_45_deg_gradient != nullptr) {
        delete[] astig_45_deg_gradient;
        astig_45_deg_gradient = nullptr;
    }

    if(nullptr != original_centroids) {
        delete[] original_centroids;
        original_centroids = nullptr;
    }

    return true;
}