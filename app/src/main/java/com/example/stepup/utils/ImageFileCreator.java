package com.example.stepup.utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageFileCreator {
    //היא מחלקת עזר שאחראית לקחת מידע גולמי של תמונה קישור מקומי Uri, או Bitmap)
    // ולהמיר אותו פיזית לקובץ זמני (.jpg) בתיקיית המטמון (Cache) של המכשיר
    // כדי ששרת הענן יוכל לקרוא ולהעלות אותו.
    private static final String TAG = "ImageFileCreator";

    public static File createTempFileFromUri(Uri uri, Context context) {
        Log.d(TAG, "Creating temp file from URI: " + uri);
        try { //המרה מקישור גלריה לקובץ פיזי

            // 1. פתיחת צינור לקריאת המידע של התמונה מתוך הגלריה בעזרת ContentResolver
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            // 2. יצירת קובץ זמני ריק בתוך תיקיית המטמון הפרטית של האפליקציה (CacheDir)
            File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());

            // 3. פתיחת צינור כתיבה אל הקובץ הזמני החדש
            FileOutputStream out = new FileOutputStream(tempFile);

            // 4. לולאת העתקה: קוראים מהגלריה וכותבים לקובץ הזמני עד שאין יותר מידע
            byte[] buf = new byte[4096];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            // 5. סגירת הצינורות ושחרור הזיכרון
            out.close();
            inputStream.close();
            Log.d(TAG, "Temp file created: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "createTempFileFromUri: failed: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "createTempFileFromUri: failed: " + e.getMessage());
            return null;
        }
    }

    public static File createTempFileFromBitmap(Bitmap bitmap, Context context) {
        try {
            // 1. יצירת קובץ זמני ריק בתיקיית ה-Cache הפרטית
            File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());
            // 2. פתיחת צינור כתיבה לקובץ הזמני
            FileOutputStream out = new FileOutputStream(tempFile);

            // 3. פעולה קריטית: לקיחת הפיקסלים, דחיסתם
            // לפורמט JPEG (באיכות 90%) וכתיבתם לקובץ פיזית
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close(); //סגירת הכל
            Log.d(TAG, "Temp file created from bitmap: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            Log.d(TAG, "Temp file creation failed: " + e.getMessage());
            return null;
        }
    }
}

