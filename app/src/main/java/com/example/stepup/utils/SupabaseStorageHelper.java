package com.example.stepup.utils;

import android.util.Log;
import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseStorageHelper {
    private static final String supabaseUrl = "https://rgxefgovfehmniygqynk.supabase.co";
    private static final String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJneGVmZ292ZmVobW5peWdxeW5rIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU2ODgwNDUsImV4cCI6MjA4MTI2NDA0NX0.Ejt0OaVVY8JAe7kqxUHTGlj-Y233oa3RXGP2KQu_IZo";
    private static final String SUPABASE_BUCKET = "shaylisbucket";
    private static final String TAG = "SupabaseStorageHelper";

    public interface OnResultCallback {
        void onResult(boolean success, String url, String error);
    }

    public static void uploadPicture(final File file, final String filePath, OnResultCallback callback) {
        try {
            OkHttpClient client = new OkHttpClient.Builder().build();

            // תיקון קטן כאן: וודא שיש סלאש בסוף ה-Base URL
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(supabaseUrl + "/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            SupabaseStorageService service = retrofit.create(SupabaseStorageService.class);

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", filePath, requestFile);

            String bearer = "Bearer " + supabaseKey;

            Call<ResponseBody> call = service.uploadFile(
                    supabaseKey,
                    bearer,
                    SUPABASE_BUCKET,
                    filePath,
                    body
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        callback.onResult(true, getFileSupabaseUrl(filePath), null);
                    } else {
                        callback.onResult(false, null, "Error: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    callback.onResult(false, null, t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onResult(false, null, e.getMessage());
        }
    }

    public static String getFileSupabaseUrl(String filePath) {
        // לוודא שהנתיב נבנה נכון עבור Glide
        return supabaseUrl + "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + filePath;
    }
}