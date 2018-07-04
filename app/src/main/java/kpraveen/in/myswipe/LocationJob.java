package kpraveen.in.myswipe;

import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class LocationJob extends JobService {

    private CountDownTimer mTimer;
    private int FOREGROUND_NOTIFICATION = 165;
    private LocationListener networkListener;
    private LocationListener gpsListener;
    public Location lastNetworkLocation = null;
    private Boolean jobRunning = false;
    public Location lastGPSLocation = null;
    private LocationManager locationManager;
    private float batteryPercentage = 0;
    private JobParameters mParams;
    private CountDownTimer mStopTimer;
    private boolean gpsEnabled = false;
    private boolean networkEnabled = false;
    private int mJobId = 0;
    private long jobStartTime = System.currentTimeMillis();

    public static int LOCATION_JOB_NOW = 700;
    public static int LOCATION_JOB_ONE = 701;
    public static int LOCATION_JOB_TWO = 702;
    public static int LOCATION_JOB_FCM = 703;
    public static int LOCATION_JOB_FALLBACK = 704;
    public static int LOCATION_PERIODIC_JOB = 708;

    public Location lastLocation;
    private double distanceFromOffice = -1;
    private CountDownTimer mRestartTimer;
    private boolean gpsRunning = false;
    private boolean networkRunning = false;

    private boolean yetToReachOffice;

    private CountDownTimer mTestTimer;
    private boolean reachedOffice = false;

    static void schedulePeriodic(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, LocationJob.class);
            JobInfo jobInfo = new JobInfo.Builder(LocationJob.LOCATION_PERIODIC_JOB, componentName)
                    .setPeriodic(UserConfiguration.instance.periodicJobInterval)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).
                            build();
            if (jobInfo != null) {
                int ret = jobScheduler.schedule(jobInfo);
                Log.d(TheApplication.TAG, "Periodic Location Job got scheduled");
            }
        }
    }

    static void scheduleJob(Context context, int jobId, long latency) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, LocationJob.class);
            JobInfo jobInfo = new JobInfo.Builder(jobId, componentName)
                    .setMinimumLatency(latency)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).
                            build();
            if (jobInfo != null) {
                int ret = jobScheduler.schedule(jobInfo);
                Log.d(TheApplication.TAG, "LocationJob Scheduled " + jobId + " latency " + latency);
            }
        }
    }


    static void scheduleNow(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, LocationJob.class);
            JobInfo jobInfo = new JobInfo.Builder(LOCATION_JOB_NOW, componentName)
                    .setMinimumLatency(1)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).
                            build();
            if (jobInfo != null) {
                int ret = jobScheduler.schedule(jobInfo);
                Log.d(TheApplication.TAG, "LocationJob Scheduled ");
            }
        }
    }

    public void locationReceived(Location location) {
        Log.d(TheApplication.TAG, "LocationJob location received     " + location.getProvider() + " --> " + location.getLatitude() + ", " + location.getLongitude() + " , " + location.getAccuracy());
        lastLocation = location;
        MessageManager.postLocation(this, location, mJobId);
        LocationHelper.locationReceived(this, location);
        distanceFromOffice = LocationHelper.distanceFromOffice(LocationJob.this);
        Log.d(TheApplication.TAG, "New distance from office now is " + distanceFromOffice);
        if (!reachedOffice) {
            if (location.getAccuracy() < UserConfiguration.instance.maxLocationAccuracy && distanceFromOffice < UserConfiguration.instance.reachedDistanceLimit) {
                Log.d(TheApplication.TAG, "Reached office");
                reachedOffice = true;
                resetStopTimer();
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, gpsListener);
        } catch (SecurityException e) {
            Log.e(TheApplication.TAG, "Check permission for GPS");
        }
        gpsRunning = true;
    }

    private void stopGPS() {
        try {
            Log.d(TheApplication.TAG, "stop GPS");
            locationManager.removeUpdates(gpsListener);
        } catch (SecurityException e) {
            Log.d(TheApplication.TAG, "Check permission for GPS");
        }
        gpsRunning = false;
    }

    private void startNetwork() {
        try {
            Log.d(TheApplication.TAG, "start Network");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, networkListener);
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

    public void startRestartTimer() {
        if (mRestartTimer != null) {
            mRestartTimer.cancel();
        }
        mRestartTimer = new CountDownTimer(60000, 60000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (gpsEnabled) {
                    startGPS();
                }

                if (networkEnabled) {
                    startNetwork();
                }
            }
        };
    }

    public void resetStopTimer() {
        if (mStopTimer != null) {
            mStopTimer.cancel();
        }
        mStopTimer = new CountDownTimer(UserConfiguration.instance.locationJobDuration, UserConfiguration.instance.locationJobDuration) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                Log.d(TheApplication.TAG, "LocationJob Timer");
                // may be location service updated reached office
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                String preferenceName = "swipeData" + df.format(System.currentTimeMillis());
                SharedPreferences pref = LocationJob.this.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
                reachedOffice = pref.getBoolean("reachedOffice", false);
                if (reachedOffice) {
                    Log.d(TheApplication.TAG, "reached office already ");
                    finishThisJob();
                    return;
                }

                distanceFromOffice = LocationHelper.distanceFromOffice(LocationJob.this);
                Log.d(TheApplication.TAG, "distance from office " + distanceFromOffice);
                if (distanceFromOffice < 0) {
                    finishThisJob();
                    return;
                }

                if (distanceFromOffice < UserConfiguration.instance.reachedDistanceLimit) {
                    Log.d(TheApplication.TAG, "Reached office");
                    finishThisJob();
                    return;
                }

                if (distanceFromOffice > UserConfiguration.instance.farDistance) {
                    Log.d(TheApplication.TAG, "Far from office, stop job");
                    finishThisJob();
                    return;
                }

                if (LocationHelper.isStationary(LocationJob.this, UserConfiguration.instance.stationaryCheckMinutes)) {
                    Log.d(TheApplication.TAG, "stationary for 15 mins so stop the job");
                    finishThisJob();
                    return;
                }

                if (lastLocation == null) {
                    Log.d(TheApplication.TAG, "not receiving locations, so stop the job");
                    finishThisJob();
                    return;
                }

                if (UserConfiguration.instance.useLocationService && distanceFromOffice < UserConfiguration.instance.nearDistance) {
                    Log.d(TheApplication.TAG, "near office start service");
                    Intent startIntent = new Intent(LocationJob.this, LocationService.class);
                    startIntent.putExtra("startedBy", "LocationJob");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        LocationJob.this.startForegroundService(startIntent);
                    } else {
                        LocationJob.this.startService(startIntent);
                    }
                }


                lastLocation = null;

                if (UserConfiguration.instance.extendTimerWhenNear &&
                        distanceFromOffice < UserConfiguration.instance.nearDistance2
                        && (System.currentTimeMillis() -  jobStartTime < 6*TheApplication.MINUTE)) {
                    // continue locations
                    Log.d(TheApplication.TAG, "continue sending location");
                    resetStopTimer();
                    return;
                }
                stopGPS();
                stopNetwork();
                finishThisJob();
            }
        };
        mStopTimer.start();
    }

    private void scheduleNextJob() {
        Log.d(TheApplication.TAG, "Schedule next job");
        Calendar calendar = Calendar.getInstance();
        int nextJobId = (mJobId == LOCATION_JOB_ONE) ? LOCATION_JOB_TWO : LOCATION_JOB_ONE;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (hour <= 6) {
            int day = calendar.get(Calendar.DAY_OF_YEAR);
            calendar.set(Calendar.DAY_OF_YEAR, day);
            calendar.set(Calendar.HOUR_OF_DAY, 7);
            calendar.set(Calendar.MINUTE, 30);
            scheduleJob(LocationJob.this, nextJobId, (calendar.getTimeInMillis()-System.currentTimeMillis()));
        } else if (hour <= 11) {
            if (reachedOffice) {
                scheduleJob(LocationJob.this, nextJobId, 2*AlarmManager.INTERVAL_HOUR);
            } else {
                scheduleJob(LocationJob.this, nextJobId, AlarmManager.INTERVAL_HALF_HOUR);
            }
        } else if (hour <= 20) {
            if (reachedOffice) {
                scheduleJob(LocationJob.this, nextJobId, 3 * AlarmManager.INTERVAL_HOUR);
            } else {
                scheduleJob(LocationJob.this, nextJobId, 2*AlarmManager.INTERVAL_HOUR);
            }
        } else {
            int day = calendar.get(Calendar.DAY_OF_YEAR);
            calendar.set(Calendar.DAY_OF_YEAR, day+1);
            calendar.set(Calendar.HOUR_OF_DAY, 6);
            calendar.set(Calendar.MINUTE, 30);
            scheduleJob(LocationJob.this, nextJobId, (calendar.getTimeInMillis()-System.currentTimeMillis()));
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {

        Log.d(TheApplication.TAG, "Start LocationJob " + params.getJobId());
        Log.d(TheApplication.TAG, "office location " + TheApplication.officeLocation.getLatitude() + "," + TheApplication.officeLocation.getLongitude());
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = 100 * level / (float) scale;

        JSONObject data = new JSONObject();
        try {
            data.put("type", "SwipeLocationJob");
            data.put("deviceManufacturer", Build.MANUFACTURER);
            data.put("deviceModel", Build.MODEL);
            data.put("deviceProduct", Build.PRODUCT);
            data.put("batteryPercentage", batteryPercentage);
            data.put("deviceVersionRelease", Build.VERSION.RELEASE);
            data.put("deviceSDKVersion", Build.VERSION.SDK_INT);
            data.put("versionCode", BuildConfig.VERSION_CODE);
            data.put("versionName", BuildConfig.VERSION_NAME);
            data.put("buildTimestamp", BuildConfig.BUILD_TIMESTAMP);
            data.put("appName", "MySwipe");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MessageManager.postData(this, data);

        if (batteryPercentage < TheApplication.minBatteryPercentage) {
            Log.d(TheApplication.TAG, "battery too low");
            return false;
        }

        if (jobRunning) {
            Log.d(TheApplication.TAG, "job already running");
            return false;
        }

        distanceFromOffice = LocationHelper.distanceFromOffice(this);
        Log.d(TheApplication.TAG, "distance from office at start of job " + distanceFromOffice);
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        String preferenceName = "swipeData" + df.format(System.currentTimeMillis());
        SharedPreferences pref = LocationJob.this.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        reachedOffice = pref.getBoolean("reachedOffice", false);
        scheduleNextJob();

        jobRunning = true;

        mParams = params;
        mJobId = params.getJobId();

        lastNetworkLocation = null;
        lastGPSLocation = null;
        jobStartTime = System.currentTimeMillis();

        //startTestTimer();

        resetStopTimer();

        if (gpsEnabled) {
            startGPS();
        }

        if (networkEnabled) {
            startNetwork();
        }

        return true;
    }


    private int pointIndex = 0;
    private double startLatitude = 12.855620;
    private double startLongitude = 77.630501;

    private double endLatitude = 12.847427;
    private double endLongitude = 77.663121;

    private void startTestTimer() {

        mTestTimer = new CountDownTimer(AlarmManager.INTERVAL_FIFTEEN_MINUTES, 20000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (pointIndex < 10) {
                    pointIndex++;
                }
                Location location = new Location("test");
                location.setLatitude(startLatitude + (endLatitude - startLatitude) * pointIndex / 10);
                location.setLongitude(startLongitude + (endLongitude - startLongitude) * pointIndex / 10);
                location.setTime(System.currentTimeMillis());
                locationReceived(location);
            }

            @Override
            public void onFinish() {

            }
        };
        mTestTimer.start();
    }

    public void finishThisJob() {
        LocationHelper.saveLocations(this);
        stopGPS();
        stopNetwork();
        jobRunning = false;
        if (mStopTimer != null) {
            mStopTimer.cancel();
        }
        if (mTestTimer != null) {
            mTestTimer.cancel();
        }
        if (mRestartTimer != null) {
            mRestartTimer.cancel();
        }
        if (mParams != null) {
            Log.d(TheApplication.TAG, "finishThisJob LocationJob " + mParams.getJobId());
            jobFinished(mParams, false);
        } else {
            Log.d(TheApplication.TAG, "Some how job Param is null");
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TheApplication.TAG, "LocationJob : onStopJob");
        mParams = null;
        jobRunning = false;
        stopGPS();
        stopNetwork();
        if (mStopTimer != null) {
            mStopTimer.cancel();
        }
        if (mRestartTimer != null) {
            mRestartTimer.cancel();
        }
        if (mTestTimer != null) {
            mTestTimer.cancel();
        }
        return false;
    }

}

