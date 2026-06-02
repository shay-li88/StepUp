package com.example.stepup.utils;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

//מחלקת עזר עבור קריאות הרשת של האפליקציה, במטרה
// לחסוך במשאבי זיכרון ולמנוע יצירה מיותרת של מנועי רשת זהים.
    public static Retrofit getClient(String baseUrl) {
        if (retrofit == null) { // האם זו הפעם הראשונה שמישהו מבקש את המנוע?
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl) // הגדרת כתובת השרת
                    .addConverterFactory(GsonConverterFactory.create())
                    .build(); //יצירת האובייקט
        }
        return retrofit;
    }

}
