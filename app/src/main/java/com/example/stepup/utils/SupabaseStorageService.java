package com.example.stepup.utils;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface SupabaseStorageService {
    @Multipart
    // הסרתי את הסלאש הראשון בתחילת הכתובת.
    // ב-Retrofit, אם ה-Base URL נגמר ב-/, ה-Path לא צריך להתחיל ב-/.
    @POST("storage/v1/object/{bucket}/{filename}")
    Call<ResponseBody> uploadFile(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Path("bucket") String bucket,
            // הוספתי (encoded = true) - זה קריטי אם שם הקובץ מכיל תיקיות (כמו "profiles/user1.jpg")
            // כדי ש-Retrofit לא יהפוך את הסלאש של התיקייה לתו מיוחד.
            @Path(value = "filename", encoded = true) String filename,
            @Part MultipartBody.Part file
    );
}