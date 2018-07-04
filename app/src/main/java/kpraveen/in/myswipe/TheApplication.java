package kpraveen.in.myswipe;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

public class TheApplication extends Application {

    public static String MessageReceived = "MESSAGE_RECEIVED";
    public static String appInstanceId = "";
    public static RequestQueue requestQueue;
    public static String TAG = "pkgdebug";
    public static long locationJobDuration = 40000;
    public static float minBatteryPercentage = 15.0f;
    public static int WIFI_PERIODIC_JOB = 61;
    public static int WIFI_FCM_JOB = 62;
    public static final String baseUrl = "http://iot.kpraveen.in";
    public static String accessToken = "";
    public static String deviceToken = "";
    public static String username = "";
    public static boolean loggedIn;
    public static Location officeLocation;
    public final static int MINUTE = 60000;
    public static String userId = "";

    public static void registerDevice() {

        String URL = TheApplication.baseUrl + "/api/deviceRegistrations?access_token=" + TheApplication.accessToken;
        JSONObject data = new JSONObject();
        try {
            data.put("deviceToken", deviceToken);
            data.put("appName", "MySwipe");
            data.put("time", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String mRequestBody = data.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TheApplication.TAG, "posting device success");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TheApplication.TAG, "posting device token error " + error.toString());
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.d("app", "Unsupported Encoding ");
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };
        int MY_SOCKET_TIMEOUT_MS = 20000;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        TheApplication.requestQueue.add(stringRequest);
    }

    public static void scheduleJobs(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            boolean periodicJobSceduled = false;
            boolean locationJobScheduled = false;
            List<JobInfo> list = jobScheduler.getAllPendingJobs();
            for (int i = 0; i < list.size(); i++) {
                JobInfo info = list.get(i);
                if (info.getId() == LocationJob.LOCATION_PERIODIC_JOB) {
                    periodicJobSceduled = true;
                }
                if (info.getId() == LocationJob.LOCATION_JOB_ONE) {
                    locationJobScheduled = true;
                }
                if (info.getId() == LocationJob.LOCATION_JOB_TWO) {
                    locationJobScheduled = true;
                }
            }
            if (!periodicJobSceduled) {
                LocationJob.schedulePeriodic(context);
            }
            if (!locationJobScheduled) {
                LocationJob.scheduleJob(context, LocationJob.LOCATION_JOB_ONE, 1);
            }
        }
    }

    public static void fetchdata(final Context context, final String yyyymmdd, final DoneCallback callback) {
        Log.d(TheApplication.TAG, "fetch user config");
        JSONObject filter = new JSONObject();
        JSONObject where = new JSONObject();
        try {
            where.put("yyyymmdd", yyyymmdd);
            filter.put("where", where);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String url = TheApplication.baseUrl + "/api/users/me/swipeData?filter=";
        try {
            url = url + URLEncoder.encode(filter.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        url = url + "&access_token=" + TheApplication.accessToken;
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TheApplication.TAG, "data received " + response);
                try {
                    JSONArray array  = new JSONArray(response);
                    if (array.length() > 0) {
                        JSONObject data = array.getJSONObject(0);
                        String preferenceName = "swipeData" + yyyymmdd;
                        Log.d(TheApplication.TAG, "Fetching data for " + preferenceName);
                        SharedPreferences spref = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = spref.edit();
                        try {
                            boolean override = data.getBoolean("override");
                            if (override) {
                                editor.clear();
                            }
                        } catch (JSONException e) {
                        }
                        try {
                            editor.putString("statusRemarks", data.getString("statusRemarks"));
                        } catch (JSONException e) {
                        }
                        try {
                            editor.putLong("inTime", data.getLong("inTime"));
                        } catch (JSONException e) {
                        }

                        try {
                            long swipeInTime = data.getLong("swipeInTime");
                            if (swipeInTime > 0) {
                                editor.putLong("swipeInTime", swipeInTime);
                            } else if (swipeInTime == -1) {
                                editor.remove("swipeInTime");
                            }
                        } catch (JSONException e) {
                        }
                        try {
                            long swipeOutTime = data.getLong("swipeOutTime");
                            if (swipeOutTime > 0) {
                                editor.putLong("swipeOutTime", swipeOutTime);
                            } else if (swipeOutTime == -1) {
                                editor.remove("swipeOutTime");
                            }
                        }
                        catch (JSONException e) {

                        }

                        try {
                            long leavingTime = data.getLong("leavingTime");
                            if (leavingTime >= 0) {
                                editor.putLong("leavingTime", leavingTime);
                            }
                        }  catch (JSONException e) {

                        }

                        try {
                            long reachedOfficeTime = data.getLong("reachedOfficeTime");
                            if (reachedOfficeTime >= 0) {
                                editor.putLong("reachedOfficeTime", reachedOfficeTime);
                                editor.putBoolean("reachedOffice", true);
                            }
                        }
                        catch (JSONException e) {

                        }

                        editor.commit();
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


    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences pref = this.getSharedPreferences("application", Context.MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        appInstanceId = pref.getString("appInstanceId", "");
        if (appInstanceId == null || appInstanceId.isEmpty()) {
            appInstanceId = UUID.randomUUID().toString();
            SharedPreferences.Editor edit = pref.edit();
            edit.putString("appInstanceId", appInstanceId);
            edit.commit();
        }

        UserConfiguration.load(this);
        officeLocation = new Location("dummy");
        officeLocation.setLatitude(UserConfiguration.instance.latitude);
        officeLocation.setLongitude(UserConfiguration.instance.longitude);
        TheApplication.accessToken = pref.getString("accessToken", "");
        TheApplication.deviceToken = pref.getString("deviceId", "");
        TheApplication.username = pref.getString("username", "");
        TheApplication.loggedIn = pref.getBoolean("loggedIn", false);
        TheApplication.userId = pref.getString("userId", "");
        LocationHelper.load(this);
        Log.d(TAG, "MySwipe " +  TheApplication.username);
        Log.d(TAG, "deviceToken " +  TheApplication.deviceToken);
        Log.d(TAG, "accesToken " +  TheApplication.accessToken);
        Log.d(TAG, "userId " +  TheApplication.userId);

    }

}
