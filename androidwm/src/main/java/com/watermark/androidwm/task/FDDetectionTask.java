/*
 *    Copyright 2018 Yizheng Huang
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.watermark.androidwm.task;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.watermark.androidwm.listener.DetectFinishListener;
import com.watermark.androidwm.utils.BitmapUtils;

import static com.watermark.androidwm.utils.BitmapUtils.pixel2ARGBArray;
import static com.watermark.androidwm.utils.BitmapUtils.getBitmapPixels;
import static com.watermark.androidwm.utils.Constant.ERROR_BITMAP_NULL;
import static com.watermark.androidwm.utils.Constant.ERROR_DETECT_FAILED;
import static com.watermark.androidwm.utils.Constant.LSB_IMG_PREFIX_FLAG;
import static com.watermark.androidwm.utils.Constant.LSB_IMG_SUFFIX_FLAG;
import static com.watermark.androidwm.utils.Constant.LSB_TEXT_PREFIX_FLAG;
import static com.watermark.androidwm.utils.Constant.LSB_TEXT_SUFFIX_FLAG;
import static com.watermark.androidwm.utils.Constant.MAX_IMAGE_SIZE;
import static com.watermark.androidwm.utils.Constant.WARNNING_BIG_IMAGE;
import static com.watermark.androidwm.utils.StringUtils.binaryToString;
import static com.watermark.androidwm.utils.StringUtils.copyFromIntArray;
import static com.watermark.androidwm.utils.StringUtils.getBetweenStrings;
import static com.watermark.androidwm.utils.StringUtils.intArrayToStringJ;
import static com.watermark.androidwm.utils.StringUtils.replaceNinesJ;

/**
 * This is a task for watermark image detection.
 * In FD mode, all the task will return a bitmap;
 *
 * @author huangyz0918 (huangyz0918@gmail.com)
 */
@SuppressWarnings("PMD")
public class FDDetectionTask extends AsyncTask<Bitmap, Void, DetectionReturnValue> {

    private DetectFinishListener listener;

    public FDDetectionTask(DetectFinishListener listener) {
        this.listener = listener;
    }

    @Override
    protected DetectionReturnValue doInBackground(Bitmap... bitmaps) {
        Bitmap markedBitmap = bitmaps[0];
        DetectionReturnValue resultValue = new DetectionReturnValue();

        if (markedBitmap == null) {
            listener.onFailure(ERROR_BITMAP_NULL);
            return null;
        }

        if (markedBitmap.getWidth() > MAX_IMAGE_SIZE || markedBitmap.getHeight() > MAX_IMAGE_SIZE) {
            listener.onFailure(WARNNING_BIG_IMAGE);
            return null;
        }

        int[] pixels = getBitmapPixels(markedBitmap);
        int[] colorArray = pixel2ARGBArray(pixels);

        // TODO: the two arrays make the maxsize smaller than 1024.
        double[] colorArrayD = copyFromIntArray(colorArray);

        for (int i = 0; i < colorArrayD.length; i++) {
            colorArrayD[i] = (int) colorArrayD[i] % 10;
        }

        replaceNinesJ(colorArray);
        String binaryString = intArrayToStringJ(colorArray);
        String resultString;

        if (binaryString.contains(LSB_TEXT_PREFIX_FLAG) && binaryString.contains(LSB_TEXT_SUFFIX_FLAG)) {
            resultString = getBetweenStrings(binaryString, true, listener);
            resultString = binaryToString(resultString);
            resultValue.setWatermarkString(resultString);
        } else if (binaryString.contains(LSB_IMG_PREFIX_FLAG) && binaryString.contains(LSB_IMG_SUFFIX_FLAG)) {
            binaryString = getBetweenStrings(binaryString, false, listener);
            resultString = binaryToString(binaryString);
            resultValue.setWatermarkBitmap(BitmapUtils.stringToBitmap(resultString));
        }

        return resultValue;
    }

    @Override
    protected void onPostExecute(DetectionReturnValue detectionReturnValue) {
        if (detectionReturnValue == null) {
            listener.onFailure(ERROR_DETECT_FAILED);
            return;
        }

        if (detectionReturnValue.getWatermarkString() != null &&
                !"".equals(detectionReturnValue.getWatermarkString()) ||
                detectionReturnValue.getWatermarkBitmap() != null) {
            listener.onSuccess(detectionReturnValue);
        } else {
            listener.onFailure(ERROR_DETECT_FAILED);
        }
        super.onPostExecute(detectionReturnValue);
    }
}