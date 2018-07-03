package kpraveen.in.myswipe;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;


public class LocationService extends Service {

    private int FOREGROUND_NOTIFICATION = 129;

    private LocationListener networkListener;
    private LocationListener gpsListener;

    public Location lastLocationSent = null;

    private LocationManager locationManager;
    private boolean gpsRunning = false;
    private boolean networkRunning = false;
    public Location lastNetworkLocation = null;
    public Location lastGPSLocation = null;

    private float batteryPercentage = 0;
    private String mStartedBy = "";
    private long lastRequestTime = System.currentTimeMillis();
    private long lastLocationSentTime = 0;

    private CountDownTimer mTimer = null;
    private CountDownTimer mFinishTimer = null;
    private long networkStartTime = 0;
    private long networkStopTime = 0;
    private int numGPSResults = 0;
    private boolean liveLocation = false;
    private int mJobId = 999;
    private Location bestLocation;
    private boolean gpsEnabled = false;
    private boolean networkEnabled = false;
    private int numNetworkResults = 0;
    private double distanceFromOffice = 0;
    private boolean reachedOffice = false;
    public LocationService() {
    }

    public void postLocation(Location location) {
        MessageManager.postLocation(this, location);
    }

    public void locationReceived(Location location) {
        boolean send = false;
        Log.d(TheApplication.TAG, "Location Service location received  " + location.getProvider() + ", " + location.getAccuracy());
        postLocation(location);
        LocationHelper.locationReceived(this, location);
        lastLocationSent = location;
        lastLocationSentTime = System.currentTimeMillis();
        distanceFromOffice = LocationHelper.distanceFromOffice(this);
        Log.d(TheApplication.TAG, "New distance from office now is " + distanceFromOffice);
        if (!reachedOffice) {
            if (location.getAccuracy() < 30 && distanceFromOffice < UserConfiguration.instance.distanceAccuracy) {
                Log.d(TheApplication.TAG, "Reached office");
                reachedOffice = true;
                mFinishTimer = new CountDownTimer(120000, 12000) {
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        stopForeground(true);
                        stopGPS();
                        stopNetwork();
                        stopSelf();
                    }
                };
                mFinishTimer.cancel();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        gpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    numGPSResults++;
                    locationReceived(location);
                    lastGPSLocation = location;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //Log.d(ApplicationData.TAG, "GPS change");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TheApplication.TAG, "GPS is enabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TheApplication.TAG, "GPS is disabled");
            }
        };

        networkListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    numNetworkResults++;
                    locationReceived(location);
                    lastNetworkLocation = location;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TheApplication.TAG, "netowrok location changed");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TheApplication.TAG, "netowrok location is enabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TheApplication.TAG, "netwrok location is disabled");
            }
        };

        lastGPSLocation = null;
        lastNetworkLocation = null;
    }

    private void startGPS() {
        try {
            Log.d(TheApplication.TAG, "start GPS");
            numGPSResults = 0;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 1, gpsListener);
        } catch (SecurityException e) {
        }
        gpsRunning = true;
    }

    private void stopGPS() {
        try {
            Log.d(TheApplication.TAG, "stop GPS");
            locationManager.removeUpdates(gpsListener);
        } catch (SecurityException e) {
        }
        gpsRunning = false;
    }

    private void startNetwork() {
        try {
            Log.d(TheApplication.TAG, "start network");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 10, networkListener);
            networkRunning = true;
        } catch (SecurityException e) {
        }
    }

    private void stopNetwork() {

        try {
            Log.d(TheApplication.TAG, "stop network");
            locationManager.removeUpdates(networkListener);
        } catch (SecurityException e) {
        }
        networkRunning = false;
    }

    public void resetStopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new CountDownTimer(20*TheApplication.MINUTE, 60000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                stopGPS();
                stopNetwork();
                stopSelf();
            }
        };
        mTimer.start();
    }

    // check if reached office or stationary then stop
//    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//    Intent batteryStatus = LocationService.this.registerReceiver(null, ifilter);
//    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
//    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
//    batteryPercentage = 100 * level / (float) scale;
//                if (batteryPercentage < 10) {
//        stopSelf();
//    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mJobId = 999;
        mStartedBy = "";
        if (intent != null) {
            String str = intent.getStringExtra("startedBy");
            if (str != null) {
                mStartedBy = str;
            }
            boolean stopService = intent.getBooleanExtra("stopService", false);
            if (stopService) {
                stopForeground(true);
                stopGPS();
                stopNetwork();
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        Log.d(TheApplication.TAG, "LocationService service start. By " + mStartedBy);

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (UserConfiguration.instance.showForegroundNotification ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundNotification();
        }

        if (!gpsEnabled && !networkEnabled) {
            // Raise a notification
            stopSelf();
        }

        numGPSResults = 0;

        resetStopTimer();

        if (gpsEnabled) {
            if (!gpsRunning) {
                startGPS();
            }
        }

        if (networkEnabled) {
            if (!networkRunning) {
                startNetwork();
            }
        }

        lastRequestTime = System.currentTimeMillis();

        return START_NOT_STICKY;
    }

    private void startForegroundNotification() {
        String channelId = getString(R.string.foreground_service_channel);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ForegroundNotification", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, 0);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle("Team Application S1")
                        .setContentText("Tap here to open")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Team Service",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        Notification notification = notificationBuilder.build();
        startForeground(FOREGROUND_NOTIFICATION, notification);
    }


    @Override
    public void onDestroy() {
        Log.d(TheApplication.TAG, "on destroy location service");
        stopNetwork();
        stopGPS();
        //list.clear();
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    ;
}

