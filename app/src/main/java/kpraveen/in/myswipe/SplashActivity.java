package kpraveen.in.myswipe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;


public class SplashActivity extends AppCompatActivity {

//    static {
//        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = getSharedPreferences("application", Context.MODE_PRIVATE);
        boolean isLoggedIn;

        finish();

        isLoggedIn = sharedPref.getBoolean("loggedIn", false);
        if (isLoggedIn) {
            String accessToken = sharedPref.getString("accessToken", "");
            TheApplication.accessToken = accessToken;
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }

    }
}
