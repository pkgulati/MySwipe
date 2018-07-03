package kpraveen.in.myswipe;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.widget.Toast;


import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private boolean channelCreated = false;
    private LocationListener gpsListener;
    private static int nextJobId = 300;
    private static long lastFCMReceivedTime = 0;

    public AppFirebaseMessagingService() {
    }

    final Handler mHandler = new Handler();

    // Helper for showing tests
    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AppFirebaseMessagingService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean checkPermission() {
        int permission = PermissionChecker.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PermissionChecker.PERMISSION_GRANTED) {
            permission = PermissionChecker.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return (permission == PermissionChecker.PERMISSION_GRANTED ? true : false);
    }

    public float distance(double lat_a, double lng_a, double lat_b, double lng_b) {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b - lat_a);
        double lngDiff = Math.toRadians(lng_b - lng_a);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue();
    }

    private BroadcastReceiver wifiReceiver;

    private AsyncTask<Void, Void, Void> locationTask = new AsyncTask<Void, Void, Void>() {

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    };

    private void populate(JSONObject activity, String ssid) {

        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = 100 * level / (float) scale;

        try {
            activity.put("wifissid", ssid);
            activity.put("batteryPercentage", batteryPercentage);
            activity.put("gpsEnabled", gpsEnabled);
            activity.put("networkEnabled", gpsEnabled);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {

            JSONObject req = new JSONObject(remoteMessage.getData());

            String type;
            try {
                type = req.getString("type");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            Log.d(TheApplication.TAG, "Swipe app FCM message received " + type);
            if (type.equals("InformationUpdateRequest")) {
                Log.d(TheApplication.TAG, "===========InformationUpdateRequest========= ");
                String activityId;
                try {
                    activityId = req.getString("activityId");
                } catch (JSONException e) {
                    activityId = "";
                }
                JSONObject activity = new JSONObject();
                try {
                    activity.put("type", "FCMResponse");
                    activity.put("activityId", activityId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String ssid = "";
                populate(activity, ssid);

                lastFCMReceivedTime = System.currentTimeMillis();
            } else if (type.equals("status")) {
                String requestId;
                try {
                    requestId = req.getString("requestId");
                } catch (JSONException e) {
                    requestId = "";
                }
                String appName = getResources().getString(R.string.app_name);
                JSONObject activity = new JSONObject();
                try {
                    activity.put("type", "FCMStatus");
                    activity.put("requestId", requestId);
                    activity.put("buildSdkVersion", Build.VERSION.SDK_INT);
                    activity.put("deviceId", TheApplication.deviceToken);
                    activity.put("versionCode", BuildConfig.VERSION_CODE);
                    activity.put("versionName", BuildConfig.VERSION_NAME);
                    activity.put("buildTimestamp", BuildConfig.BUILD_TIMESTAMP);
                    activity.put("appName", appName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String ssid = "";
                populate(activity, ssid);

                JSONArray jobs = new JSONArray();
                JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                if (jobScheduler != null) {
                    List<JobInfo> list = jobScheduler.getAllPendingJobs();
                    for (int i = 0; i < list.size(); i++) {
                        JobInfo info = list.get(i);
                        JSONObject job = new JSONObject();
                        try {
                            job.put("jobId", info.getId());
                            job.put("service", info.getService());
                            job.put("job", info.toString());
                            job.put("latency", info.getMinLatencyMillis());
                            job.put("interval", info.getIntervalMillis());
                            job.put("isPeriodic", info.isPeriodic());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        jobs.put(job);
                    }
                }
                try {
                    activity.put("jobs", jobs);
                    SharedPreferences pref = this.getSharedPreferences("Location", 0); // 0 - for private mode
                    long time = pref.getLong("time", 0);
                    float baseLocationMinutes = (System.currentTimeMillis() - time) / 60000;
                    activity.put("baseLocationTime", time);
                    activity.put("baseLocationAge", baseLocationMinutes);
                    activity.put("baseLocationLatitude", pref.getFloat("latitude", 0));
                    activity.put("baseLocationLongitude", pref.getFloat("longitude", 0));
                    activity.put("baseLocationAccuracy", pref.getFloat("accuracy", 0));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                MessageManager.postData(this, activity);
            }
            else if (type.equals("startLocationJob")) {
                LocationJob.scheduleJob(this, LocationJob.LOCATION_JOB_FCM, 1);
            }
            else if (type.equals("startFCMJob")) {
                FCMJob.scheduleJob(this, LocationJob.LOCATION_JOB_FCM, 1);
            }
            else if (type.equals("scheduleJob")) {
                String jobName;
                try {
                    jobName = req.getString("jobName");
                } catch (JSONException e) {
                    jobName = "";
                }
                if (!jobName.isEmpty()) {
                    long latency = 0;
                    long time = 0;
                    try {
                        time = req.getLong("time");
                        latency = time - System.currentTimeMillis();
                    } catch (JSONException e) {
                        latency = 1;
                    }
                    if (latency < 1) {
                        latency = 1;
                    }

                    int jobId;
                    try {
                        jobId = req.getInt("jobId");
                    } catch (JSONException e) {
                        jobId = 100;
                    }
                    boolean status = true;
                    if (jobName.equals("LocationJob")) {
                        LocationJob.scheduleJob(this, jobId, latency);
                    } else if (jobName.equals("FCMJob")) {
                        FCMJob.scheduleJob(this, jobId, latency);
                    } else if (jobName.equals("WifiJob")) {
                        WifiJob.scheduleJob(this, jobId, latency);
                    } else {
                        status = false;
                    }
                    JSONObject activity = new JSONObject();
                    try {
                        activity.put("type", "JobScheduled");
                        activity.put("jobName", jobName);
                        activity.put("time", time);
                        activity.put("status", status);
                        activity.put("scheduledTime", System.currentTimeMillis() + latency);
                        activity.put("jobId", jobId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (type.equals("startWifiJob")) {
                WifiJob.scheduleJob(this, TheApplication.WIFI_FCM_JOB, 1);
            } else if (type.equals("configure")) {
               UserConfiguration.fetch(this, new DoneCallback() {
                   @Override
                   public void onDone(boolean flag) {
                        LocationJob.scheduleJob(AppFirebaseMessagingService.this, LocationJob.LOCATION_JOB_FCM, 1);
                   }
               });
            } else if (type.equals("startLocationService")) {
                Intent startIntent = new Intent(this, LocationService.class);
                startIntent.putExtra("startedBy", "FCMRequest");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(startIntent);
                } else {
                    this.startService(startIntent);
                }
            } else if (type.equals("cancelJob")) {
                try {
                    String jobIdStr = req.getString("jobId");
                    int jobId = Integer.parseInt(jobIdStr);
                    JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                    if (jobScheduler != null) {
                        jobScheduler.cancel(jobId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (type.equals("notification")) {
                String abcd = type;
                Log.d(TheApplication.TAG, "notification");
            } else if (type.equals("cancelJobs")) {
                JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                if (jobScheduler != null) {
                    jobScheduler.cancelAll();
                }
            } else if (type.equals("killApp")) {
                int pid = android.os.Process.myPid();
                android.os.Process.killProcess(pid);
                System.exit(0);
            } else {
                Log.d(TheApplication.TAG, "Unknonw message type " + type);
                //toast("Unknonw message type " + type);
            }

        }
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }

    }

    @Override
    public void onDestroy() {
        if (this.wifiReceiver != null) {
            unregisterReceiver(AppFirebaseMessagingService.this.wifiReceiver);
        }
        super.onDestroy();
    }

    private void sendNotification(String title, String messageBody) {

        String channelId = "TeamNotification";
        if (!channelCreated) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel(channelId,
                        "Channel human readable title",
                        NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            channelCreated = true;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        // replaces current notification
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setContentIntent(notificationPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);

        Notification notification = notificationBuilder.build();
        notificationManager.notify(25, notification);

    }

}
