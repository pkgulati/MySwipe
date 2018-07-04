package kpraveen.in.myswipe;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class MessageManager {


    public static String baseUrl = "https://iot.kpraveen.in";

    private String installationId = "";

    public static void postData(final Context context, JSONObject data) {
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            data.put("name", TheApplication.username);
            data.put("justtime", tf.format(System.currentTimeMillis()));
            data.put("yyyymmdd", df.format(System.currentTimeMillis()));
            data.put("time", System.currentTimeMillis());
        } catch (JSONException e) {
        }
        postData(context, baseUrl + "/api/activities?access_token=" + TheApplication.accessToken, data.toString());
    }

    public static void postSwipeMessage(final Context context, SwipeMessage message) {
        putData(context, baseUrl + "/api/SMS?access_token=" + TheApplication.accessToken, message.toJson());
    }

    public static void putData(final Context context, final String url, final String mRequestBody) {
        StringRequest stringRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TheApplication.TAG, "posting response" + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TheApplication.TAG, "posting error " + error.toString());
                Log.d(TheApplication.TAG, "request was " + mRequestBody);
                Log.d(TheApplication.TAG, "url was " + url);
                String message = null;
                if (error instanceof NetworkError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof ServerError) {
                    message = "The server returned error";
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
                    message = "Error occured in posting";
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
        int MY_SOCKET_TIMEOUT_MS = 10000;

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        TheApplication.requestQueue.add(stringRequest);
    }

    public static void postData(final Context context, final String url, final String mRequestBody) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TheApplication.TAG, "posting response" + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TheApplication.TAG, "posting error " + error.toString());
                Log.d(TheApplication.TAG, "request was " + mRequestBody);
                Log.d(TheApplication.TAG, "url was " + url);
                String message = null;
                if (error instanceof NetworkError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof ServerError) {
                    message = "The server returned error";
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
                    message = "Error occured in posting";
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
        int MY_SOCKET_TIMEOUT_MS = 10000;

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        TheApplication.requestQueue.add(stringRequest);
    }

    public static void processMessage(Context context, SwipeMessage message) {
        String str = message.text;
        int pos = str.indexOf("Punched on: ");
        if (pos >= 0 && str.length() > pos + 23) {
            Calendar cal = Calendar.getInstance();
            try {
                int year = Integer.parseInt(str.substring(pos + 18, pos + 22));
                int month = Integer.parseInt(str.substring(pos + 15, pos + 17));
                int day = Integer.parseInt(str.substring(pos + 12, pos + 14));
                int hour = Integer.parseInt(str.substring(pos + 27, pos + 29));
                int mins = Integer.parseInt(str.substring(pos + 30, pos + 32));

                if (year >= 2018 && month >= 1 && month < 12 && day >= 1 && day <= 31) {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month - 1);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, mins);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                }
                Log.d(TheApplication.TAG, "message received " + cal.getTimeInMillis() + " " + message.toJson());
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                String preferenceName = "swipeData" + df.format(cal.getTimeInMillis());
                SharedPreferences spref = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
                long swipeInTime = spref.getLong("swipeInTime", 0);
                long swipeOutTime = spref.getLong("swipeOutTime", 0);
                // actually we should diff
                if (hour <= 11) {
                    if (swipeInTime == 0 || cal.getTimeInMillis() < swipeInTime) {
                        SharedPreferences.Editor editor = spref.edit();
                        editor.putLong("swipeInTime", cal.getTimeInMillis());
                        editor.commit();
                    }
                }
                else if (hour >= 16) {
                    if (swipeOutTime ==0 || cal.getTimeInMillis() > swipeOutTime) {
                        SharedPreferences.Editor editor = spref.edit();
                        editor.putLong("swipeOutTime", cal.getTimeInMillis());
                        editor.commit();
                    }
                } else {
                    if (swipeInTime == 0) {
                        SharedPreferences.Editor editor = spref.edit();
                        editor.putLong("swipeInTime", cal.getTimeInMillis());
                        editor.commit();
                    } else {
                        SharedPreferences.Editor editor = spref.edit();
                        editor.putLong("swipeOutTime", cal.getTimeInMillis());
                        editor.commit();
                    }
                }
            } catch (NumberFormatException e) {

            }
        }
    }

    public static void readMessages(Context context, long startTime, long endTime) {

        String filter;
        if (UserConfiguration.instance.applySmsFilter) {
            filter = " BODY LIKE '%Punched on%' ";
        } else {
            filter = "";
        }
        filter = filter + " AND DATE >= " + startTime;
        filter = filter + " AND DATE <= " + endTime;

        TelephonyManager telephonyManager;
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        final long time = System.currentTimeMillis();
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, filter, null, "date ASC");
        int count = 0;
        final JSONArray array = new JSONArray();
        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                SwipeMessage message = new SwipeMessage();
                message.text = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                message.smsTime = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                message.smsSentTime = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE_SENT));
                message.serviceCenter = cursor.getString(cursor.getColumnIndex(Telephony.Sms.SERVICE_CENTER));
                message.address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                message.source = "Inbox";
                String str = TheApplication.username +  message.smsTime + message.text;
                message.id = UUID.nameUUIDFromBytes(str.getBytes()).toString();
                Log.d(TheApplication.TAG, "message  " + message.id + ":" + message.smsSentTime + " " + message.smsTime + " : " + message.text);

                MessageManager.postSwipeMessage(context, message);
                MessageManager.processMessage(context, message);
                count++;
                // use msgData
            } while (cursor.moveToNext());
        } else {
            Log.d(TheApplication.TAG, "no message recived");
        }
    }

    public static void locationToJson(Location location, JSONObject obj) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(location.getTime());
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            obj.put("latitude", location.getLatitude());
            obj.put("longitude", location.getLongitude());
            obj.put("accuracy", location.getAccuracy());
            obj.put("speed", location.getSpeed());
            obj.put("hasSpeed", location.hasSpeed());
            obj.put("name", TheApplication.username);
            obj.put("locationTime", location.getTime());
            obj.put("provider", location.getProvider());
            obj.put("justtime", tf.format(System.currentTimeMillis()));
            obj.put("yyyymmdd", df.format(System.currentTimeMillis()));
            obj.put("time", System.currentTimeMillis());
        } catch (JSONException e) {

        }
    }

    public static void postLocation(Context context, Location location, int jobId) {
        double distance = location.distanceTo(TheApplication.officeLocation);
        JSONObject data = new JSONObject();
        try {
            data.put("type", "LocationResult");
            data.put("appName", "MySwipe");
            data.put("jobId", jobId);
            data.put("distanceFromOffice", distance);
            data.put("appInstanceId", TheApplication.appInstanceId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        locationToJson(location, data);
        postData(context, baseUrl + "/api/activities?access_token=" + TheApplication.accessToken, data.toString());
    }
}
