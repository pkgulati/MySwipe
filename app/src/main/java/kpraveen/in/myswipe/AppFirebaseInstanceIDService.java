package kpraveen.in.myswipe;

import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class AppFirebaseInstanceIDService extends FirebaseInstanceIdService {
    public AppFirebaseInstanceIDService() {
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String deviceToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TheApplication.TAG, "Set new deviceToken : " + deviceToken);
        TheApplication.deviceToken = deviceToken;
        SharedPreferences pref = getApplicationContext().getSharedPreferences("application", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("deviceId", deviceToken);
        editor.commit();
        if (TheApplication.loggedIn) {
            TheApplication.registerDevice();
        }
    }
}

