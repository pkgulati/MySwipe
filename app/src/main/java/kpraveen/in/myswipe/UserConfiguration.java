package kpraveen.in.myswipe;

import android.app.AlarmManager;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class UserConfiguration {

    public static UserConfiguration instance = new UserConfiguration();
    private static boolean loaded = false;
    public boolean showForegroundNotification = false;
    public String homeWifiSsid = "XYZXYZABC123AAA";
    public long wifiJobDuration = 20000;
    public long wifiJobInterval = 4*AlarmManager.INTERVAL_HOUR;

    public double latitude = 12.967609;
    public double longitude = 77.641135;

    public double reachedDistanceLimit = 100;
    public long locationJobDuration = 40000;
    public long locationStorageInterval = 120000;
    public int locationStorageCount = 20;
    public double stationaryCheckDistance = 800;
    public int stationaryCheckMinutes = 15;
    public double farDistance = 5000;
    public double nearDistance = 2000;
    public double nearDistance2 = 2000;
    public long swipeAdjustment = 600000;
    public long locationTimeAdjustment = 300000;
    public boolean applySmsFilter = true;
    public float maxLocationAccuracy = 700;
    public long periodicJobInterval = 4 * AlarmManager.INTERVAL_HOUR;
    public float minBatteryPercentage = 15.0f;
    public boolean useLocationService = false;
    public boolean extendTimerWhenNear = true;
    public int alarm1 = 0;
    public int alarm2 = 0;
    public int alarm3 = 0;
    public int alarm4 = 0;
    public boolean useJobForAlarm = true;

    public static void load(Context context) {
        if (!loaded) {
            SharedPreferences pref = context.getSharedPreferences("application", Context.MODE_PRIVATE);
            String stringValue = pref.getString("configuration", "");
            if (!stringValue.isEmpty()) {
                Gson gson = new Gson();
                UserConfiguration config = gson.fromJson(stringValue, UserConfiguration.class);
                if (config != null) {
                    instance = config;
                }
            }
            loaded = true;
        }
    };

    public static void setConfiguration(JSONObject cfg) {
        Gson gson = new Gson();
        UserConfiguration config = gson.fromJson(cfg.toString(), UserConfiguration.class);
        if (config != null) {
            instance = config;
        }
    }

    public synchronized static void fetch(final Context context, final DoneCallback callback) {
        Log.d(TheApplication.TAG, "fetch user config");
        String url = TheApplication.baseUrl + "/api/users/me/swipeConfiguration?access_token=";
        url = url + TheApplication.accessToken;
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TheApplication.TAG, "data received " + response);
                try {
                    JSONObject data = new JSONObject(response);
                    SharedPreferences pref = context.getSharedPreferences("application", Context.MODE_PRIVATE);
                    String currentValue = pref.getString("configuration", "{}");
                    if (!currentValue.equals(data.toString())) {
                        Log.d(TheApplication.TAG, "configuration changed");
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("configuration", response);
                        editor.commit();
                        UserConfiguration.setConfiguration(data);
                        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                        if (jobScheduler != null) {
                            jobScheduler.cancelAll();
                        }
                        TheApplication.scheduleJobs(context);
                        LocationService.setDailyAlarms(context);
                   }
                } catch (JSONException e) {
                }
                if (callback != null) {
                    callback.onDone(true);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TheApplication.TAG, "configuration fetch Error " + error.toString());
                if (callback != null) {
                    callback.onDone(false);
                }
            }
        });
        int MY_SOCKET_TIMEOUT_MS = 10000;
        request.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        TheApplication.requestQueue.add(request);
    }

}
