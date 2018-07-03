package kpraveen.in.myswipe;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
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
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class WifiJob extends JobService {

    private BroadcastReceiver wifiReceiver;
    private CountDownTimer timer;
    private boolean scanResultReceived = false;

    private long resulTime = 0;
    private long requestTime = 0;

    static void scheduleJob(Context context, int jobId, long latency) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, WifiJob.class);
            JobInfo jobInfo = new JobInfo.Builder(jobId, componentName)
                    .setMinimumLatency(latency)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).
                            build();
            if (jobInfo != null) {
                int ret = jobScheduler.schedule(jobInfo);
                Log.d(TheApplication.TAG, "WifiJob Scheduled latency " + latency);
            }
        }
    }

    static void schedulePeriodic(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, WifiJob.class);
            JobInfo jobInfo = new JobInfo.Builder(TheApplication.WIFI_PERIODIC_JOB, componentName)
                    .setPeriodic(UserConfiguration.instance.wifiJobInterval)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).
                            build();
            if (jobInfo != null) {
                int ret = jobScheduler.schedule(jobInfo);
                Log.d(TheApplication.TAG, "Wifi Job Scheduled periodic");
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

    }


    public void postData() {
        String ssid = "";
        WifiManager wifiManager = (WifiManager) WifiJob.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null) {
                ssid = info.getSSID();
                if (ssid.equals("<unknown ssid>")) {
                    ssid = "";
                } else if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
            }

        }

        JSONArray signalData = new JSONArray();
        List<ScanResult> results = wifiManager.getScanResults();
        for (int i = 0; i < results.size(); i++) {
            JSONObject obj = new JSONObject();
            ScanResult result = results.get(i);
            try {
                obj.put("ssid", result.SSID);
                obj.put("level", result.level);
                int signalLevel = WifiManager.calculateSignalLevel(result.level,100);
                obj.put("signalPercent", signalLevel);
                obj.put("BSSID", result.BSSID);
                //obj.put("venueName", result.venueName);
            } catch (JSONException e) {

            }
            signalData.put(obj);
        }

        JSONObject data = new JSONObject();
        try {
            data.put("type", "WifiJob");
            data.put("scanResultReceived", scanResultReceived);
            data.put("requestTime", requestTime);
            data.put("resultTime", resulTime);
            data.put("ssid", ssid);
            data.put("signalData", signalData);
            long time = System.currentTimeMillis();
            data.put("time", time);
            data.put("name", TheApplication.username);
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            data.put("justtime", df.format(System.currentTimeMillis()));
        } catch (JSONException e) {
            ;
        }
        SharedPreferences pref = WifiJob.this.getSharedPreferences("application", 0); // 0 - for private mode
        String accessToken = pref.getString("accessToken", "");
        String URL = TheApplication.baseUrl + "/api/activities?access_token=" + accessToken;
        Gson gson = new Gson();
        final String mRequestBody = data.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TheApplication.TAG, "wifi job post successfull ");
                //callback.onDone(true);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TheApplication.TAG, "wifi job posting error " + error.toString());
                String message = null;
                if (error instanceof NetworkError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof ServerError) {
                    message = "The server could not be found. Please try again after some time!!";
                } else if (error instanceof ParseError) {
                    message = "Parsing error! Please try again after some time!!";
                } else if (error instanceof NoConnectionError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof TimeoutError) {
                    message = "Connection TimeOut! Please check your internet connection.";
                } else if (error != null && error.networkResponse != null) {
                    //get status code here
                    final String statusCode = String.valueOf(error.networkResponse.statusCode);
                    //get response body and parse with appropriate encoding
                    try {
                        String body = new String(error.networkResponse.data, "UTF-8");
                        try {
                            JSONObject response = new JSONObject(body);
                            JSONObject response2 = (JSONObject) (response.get("error"));
                            message = response2.getString("message");
                        } catch (Throwable e) {
                        }
                    } catch (UnsupportedEncodingException e) {
                        // exception
                    }
                }
                if (message == null) {
                    message = "Error occured in wifi job posting";
                }
                Log.e(TheApplication.TAG, message);
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

    @Override
    public boolean onStartJob(final JobParameters params) {
        final String ssid = "";

        scanResultReceived = false;
        if (wifiReceiver == null) {
            wifiReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TheApplication.TAG, "wifi scan available ");
                    scanResultReceived = true;
                    resulTime = System.currentTimeMillis();
                }
            };
            registerReceiver(this.wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }

        requestTime = System.currentTimeMillis();

        WifiManager wifiManager = (WifiManager) WifiJob.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            wifiManager.startScan();
        } catch (SecurityException e) {

        }

        timer = new CountDownTimer(UserConfiguration.instance.wifiJobDuration, UserConfiguration.instance.wifiJobDuration) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                postData();
                jobFinished(params, false);
            }
        };

        timer.start();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        unregisterReceiver(this.wifiReceiver);
        scanResultReceived = false;
        resulTime = 0;
        if (timer != null) {
            timer.cancel();
        }
        return false;
    }
}
