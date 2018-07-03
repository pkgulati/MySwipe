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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class FCMJob extends JobService {

    public static int FCM_JOB_ID = 801;
    private static int FCM_PERIODIC_JOB = 808;
    private LocationListener networkListener;
    private LocationListener gpsListener;
    public Location lastNetworkLocation = null;
    public Location lastGPSLocation = null;
    private LocationManager locationManager;
    private float batteryPercentage = 0;
    private JobParameters mParams;
    private CountDownTimer mLocationTimer;
    private boolean gpsEnabled = false;
    private boolean networkEnabled = false;
    private int numGPSResults = 0;
    private int mJobId = 0;
    private boolean gpsRunning = false;
    private Boolean jobRunning = false;
    private long lastRequestTime = System.currentTimeMillis();
    private long lastLocationSentTime = 0;
    public Location lastLocationSent = null;
    private int numNetworkResults = 0;
    private boolean networkRunning = false;
    private BroadcastReceiver receiver;

    public static long jobCounter = 0;
    private Location bestLocation;
    private Location lastLocation;

    private CountDownTimer networkStartTimer;

    static void schedulePeriodic(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, LocationJob.class);
            JobInfo jobInfo = new JobInfo.Builder(FCMJob.FCM_PERIODIC_JOB, componentName)
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

            List<JobInfo> list = jobScheduler.getAllPendingJobs();
            for (int i = 0; i < list.size(); i++) {
                JobInfo info = list.get(i);
                if (info.getId() == jobId) {
                    Log.d(TheApplication.TAG, "Already scheduled or running");
                    return;
                }
            }

            ComponentName componentName = new ComponentName(context, FCMJob.class);
            JobInfo jobInfo = new JobInfo.Builder(jobId, componentName)
                    .setMinimumLatency(latency)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).
                            build();
            if (jobInfo != null) {
                int ret = jobScheduler.schedule(jobInfo);
                Log.d(TheApplication.TAG, "FCMJob Scheduled " + jobId + " latency " + latency);
            }
        }
    }

    private void startGPS() {
        try {
            Log.d(TheApplication.TAG, "start GPS");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, gpsListener);
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
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 1, networkListener);
        } catch (SecurityException e) {
        }
        networkRunning = true;
    }

    private void stopNetwork() {
        try {
            Log.d(TheApplication.TAG, "stop network");
            locationManager.removeUpdates(networkListener);
        } catch (SecurityException e) {
        }
        networkRunning = false;
    }

    public void locationReceived(Location location) {
        Log.d(TheApplication.TAG, "FCMJob location received     " + location.getProvider() + " --> " + location.getLatitude() + ", " + location.getLongitude() + " , " + location.getAccuracy());
        lastLocation = location;
        MessageManager.postLocation(this, location);
        LocationHelper.locationReceived(this, location);
        if (numGPSResults >= 3) {
            finishThisJob();
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

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TheApplication.TAG, "FCM Job Boadcast Received Extend Timer");
                if (!gpsRunning) {
                    if (gpsEnabled) {
                        startGPS();
                    }
                }

                if (!networkRunning) {
                    if (networkEnabled) {
                        startNetwork();
                    }
                }

                resetStopTimer();
            }
        };
        lastGPSLocation = null;
        lastNetworkLocation = null;
    }

    public void finishThisJob() {
        Log.d(TheApplication.TAG, "finishThisJob FCMJob");
        stopGPS();
        stopNetwork();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        if (mLocationTimer != null) {
            mLocationTimer.cancel();
        }
        jobRunning = false;
        if (networkStartTimer != null) {
            networkStartTimer.cancel();;
        }
        if (mParams != null) {
            jobFinished(mParams, false);
        } else {
            Log.d(TheApplication.TAG, "Some how job Param is null");
        }
    }

    public void resetStopTimer() {
        if (mLocationTimer != null) {
            mLocationTimer.cancel();
        }
        mLocationTimer = new CountDownTimer(60000, 60000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                Log.d(TheApplication.TAG, "FCM job : finish job, timer completed");
                finishThisJob();
            }
        };
        mLocationTimer.start();
    }

    @Override
    public boolean onStartJob(final JobParameters params) {

        jobCounter++;
        Log.d(TheApplication.TAG, "Start FCMJob " + params.getJobId() + " jobCounter " + jobCounter);

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercentage = 100 * level / (float) scale;

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        JSONObject activity = new JSONObject();
        try {
            activity.put("type", "FCMJobStart");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            activity.put("gpsEnabled", gpsEnabled);
            activity.put("jobRunning", jobRunning);
            activity.put("batteryPercentage", batteryPercentage);
            activity.put("jobCounter", jobCounter);
            activity.put("oldJobId", mJobId);
            activity.put("newJobId", params.getJobId());
            activity.put("networkEnabled", networkEnabled);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        if (batteryPercentage < UserConfiguration.instance.minBatteryPercentage) {
            return false;
        }

        if (!networkEnabled && !gpsEnabled) {
            return false;
        }

        resetStopTimer();

        if (jobRunning) {
            Log.d(TheApplication.TAG, "job already running");
            return false;
        }

        IntentFilter filter = new IntentFilter();
        bestLocation = null;
        jobRunning = true;

        mParams = params;
        mJobId = params.getJobId();
        lastLocationSent = null;
        lastRequestTime = System.currentTimeMillis();

        if (!gpsRunning) {
            if (gpsEnabled) {
                startGPS();
            }
        }

        startNetworkTimer();

        return true;
    }

    private void startNetworkTimer() {
        if (networkStartTimer != null) {
            networkStartTimer.cancel();;
        }
        networkStartTimer = new CountDownTimer(20000, 20000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (lastGPSLocation == null) {
                    if (!networkRunning) {
                        if (networkEnabled) {
                            startNetwork();
                        }
                    }
                }
            }
        };
        networkStartTimer.start();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TheApplication.TAG, "FCMJob : onStopJob " + jobCounter);
        jobRunning = false;
        mParams = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        stopGPS();
        stopNetwork();
        if (mLocationTimer != null) {
            mLocationTimer.cancel();
        }
        if (networkStartTimer != null) {
            networkStartTimer.cancel();;
        }
        return false;
    }


}

