package kpraveen.in.myswipe;

import android.Manifest;
import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SwipeMessageFragment.OnListFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_READ_SMS = 23001;
    private static final int PERMISSION_LOCATION_RESULT = 23872;

    private ArrayList<SwipeMessage> messages;

    private Calendar calendar;

    private TextView selectedDateText;
    private TextView reachedOfficeText;
    private TextView swipeInText;
    private TextView swipeOutText;
    private TextView durationText;
    private TextView swipeOutLabelText;
    private TextView statusText;

    private long swipeInTime = 0;
    private long swipeOutTime = 0;
    private CountDownTimer timer;
    private long reachedOfficeTime = 0;
    private long leavingTime = 0;
    private long inTime = 0;
    private String statusRemarks = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TheApplication.TAG, "onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        selectedDateText = (TextView) findViewById(R.id.selected_date);
        reachedOfficeText = (TextView) findViewById(R.id.reached_office);
        swipeInText = (TextView) findViewById(R.id.swipe_in_time);
        swipeOutLabelText = (TextView) findViewById(R.id.swipe_out_label);
        swipeOutText = (TextView) findViewById(R.id.swipe_out_time);
        durationText = (TextView) findViewById(R.id.duration);
        statusText = (TextView) findViewById(R.id.status_text);

        //LocationJob.scheduleNow(this);

        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);
        calendar.set(Calendar.MILLISECOND, 0);
        Log.d(TheApplication.TAG, "Calendar ts "+ calendar.getTimeInMillis());

        ImageButton prevButton = (ImageButton) findViewById(R.id.previous_month_arrow);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long endTime = calendar.getTimeInMillis();
                calendar.setTimeInMillis(calendar.getTimeInMillis()-AlarmManager.INTERVAL_DAY);
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                String preferenceName = "swipeData" + df.format(calendar.getTimeInMillis());
                SharedPreferences pref = MainActivity.this.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
                boolean messagesRead = pref.getBoolean("messagesRead", false);
                if (!messagesRead) {
                    Log.d(TheApplication.TAG, "read messages for " + df.format(calendar.getTimeInMillis()));
                    MessageManager.readMessages(MainActivity.this, calendar.getTimeInMillis(), endTime);
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putBoolean("messagesRead", true);
                    edit.commit();
                }
                readSwipeData();
                refreshDisplay();
            }
        });

        ImageButton nextButton = (ImageButton) findViewById(R.id.next_month_arrow);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 1);
                today.set(Calendar.MILLISECOND, 0);
                Log.d(TheApplication.TAG, "Calendar ts "+ calendar.getTimeInMillis());
                Log.d(TheApplication.TAG, "Today ts "+ today.getTimeInMillis());
                if (calendar.getTimeInMillis() >= today.getTimeInMillis()) {
                    Toast.makeText(MainActivity.this, "Try next day, tomorrow", Toast.LENGTH_SHORT).show();
                    return;
                }
                calendar.setTimeInMillis(calendar.getTimeInMillis()+AlarmManager.INTERVAL_DAY);
                readSwipeData();
                refreshDisplay();
            }
        });

        Context context = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TheApplication.TAG, "Request ignore battery optimization");
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                context.startActivity(intent);
            }
        }

        SharedPreferences pref = this.getSharedPreferences("application", Context.MODE_PRIVATE);
        int lastUsedVersion = pref.getInt("lastUsedVersion", 0);
        if (lastUsedVersion < BuildConfig.VERSION_CODE) {
            Log.d(TheApplication.TAG, "Version Upgrade " + lastUsedVersion);
            JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancelAll();
            }
            scheduleJobs();
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt("lastUsedVersion", BuildConfig.VERSION_CODE);
            editor.commit();
        }

        FCMJob.scheduleJob(this, FCMJob.FCM_JOB_ID, 1);

        boolean permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECEIVE_SMS}, PERMISSION_READ_SMS);
        } else {
            boolean locationPermissionGranted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!locationPermissionGranted) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_RESULT);
            }
            readMessages();
        }

        scheduleJobs();

        timer = new CountDownTimer(9 * AlarmManager.INTERVAL_HOUR, 30000) {
            @Override
            public void onTick(long millisUntilFinished) {
                readSwipeData();
                refreshDisplay();
            }
            @Override
            public void onFinish() {

            }
        };

    }

    private void readMessages() {
        SharedPreferences pref = this.getSharedPreferences("application", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long smsReadTime = pref.getLong("smsReadTime", 0);
        if (smsReadTime == 0) {
            smsReadTime = now - AlarmManager.INTERVAL_DAY;
        }
        MessageManager.readMessages(this, smsReadTime, now);
        SharedPreferences.Editor edit = pref.edit();
        edit.putLong("smsReadTime", now);
        edit.commit();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_RESULT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    String message = "Thank you for permission";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    String message = "Without Location permission this application will not function properly";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }

            case PERMISSION_READ_SMS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    refreshDisplay();
                } else {
                    String message = "For this application SMS Read Permission is must";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }

                boolean locationPermissionGranted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (!locationPermissionGranted) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_RESULT);
                }

                return;
            }
        }
    }

    public void scheduleJobs() {
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
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
                LocationJob.schedulePeriodic(this);
            }
            if (!locationJobScheduled) {
                LocationJob.scheduleJob(this, LocationJob.LOCATION_JOB_ONE, 1);
            }
        }
    }

    public void readSwipeData() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        String preferenceName = "swipeData" + df.format(calendar.getTimeInMillis());
        Log.d(TheApplication.TAG, "Fetching data for " + preferenceName);
        SharedPreferences spref = this.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        swipeInTime = spref.getLong("swipeInTime", 0);
        swipeOutTime = spref.getLong("swipeOutTime", 0);
        reachedOfficeTime = spref.getLong("reachedOfficeTime", 0);
        inTime = spref.getLong("inTime", 0);
        leavingTime = spref.getLong("leavingTime", 0);
        statusRemarks = spref.getString("statusRemarks", "");
    }

    public void refreshDisplay() {

        SimpleDateFormat df = new SimpleDateFormat("d MMMM");
        selectedDateText.setText(df.format(calendar.getTimeInMillis()));

        SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm");
        if (inTime > 0) {
            reachedOfficeText.setText(timeformat.format(inTime));
        } else if (reachedOfficeTime > 0) {
            reachedOfficeText.setText(timeformat.format(reachedOfficeTime));
        } else {
            reachedOfficeText.setText("--");
        }

        if (swipeInTime > 0) {
            swipeInText.setText(timeformat.format(swipeInTime));
        } else {
            swipeInText.setText("--");
        }

        long startTime = 0;
        long endTime = 0;
        if (inTime > 0) {
            startTime = inTime;
        } else if (swipeInTime > 0) {
            startTime = swipeInTime + UserConfiguration.instance.swipeAdjustment;
        } else if (reachedOfficeTime > 0) {
            startTime = reachedOfficeTime + UserConfiguration.instance.locationTimeAdjustment;
        }

        statusText.setText(statusRemarks);
        long duration = 0;
        if (startTime > 0) {
            Calendar now = Calendar.getInstance();

            if (swipeOutTime > 0) {
                swipeOutLabelText.setText("Swipe Out");
                swipeOutText.setText(timeformat.format(swipeOutTime));
                endTime = swipeOutTime + UserConfiguration.instance.swipeAdjustment;
                duration = endTime - startTime;
            } else if (now.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) &&
                    now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
                duration = System.currentTimeMillis() - startTime;
                if (leavingTime > 0) {
                    endTime = leavingTime;
                } else {
                    endTime = startTime + 9 * AlarmManager.INTERVAL_HOUR;
                }
                swipeOutLabelText.setText("Leave At");
                swipeOutText.setText(timeformat.format(endTime));
            } else {
                duration = 0;
                swipeOutText.setText("--");
            }
        } else {
            swipeOutLabelText.setText("Swipe Out");
            if (swipeOutTime > 0) {
                swipeOutText.setText(timeformat.format(swipeOutTime));
            } else {
                swipeOutText.setText("--");
            }
            duration = 0;
        }

        if (duration > 0) {
            long hrs = duration / 3600000;
            long mins = (duration / 60000) % 60;
            String str = "";
            if (hrs <= 9) {
                str = " " + hrs;
            } else {
                str += hrs;
            }
            if (mins <= 9) {
                str += ":0" + mins;
            } else {
                str += ":" + mins;
            }
            durationText.setText(str);
        } else {
            durationText.setText("--");
        }

    }

    private void logout() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("application", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("loggedIn", false);
        editor.commit();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void clearData() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        String preferenceName = "swipeData" + df.format(calendar.getTimeInMillis());
        getSharedPreferences(preferenceName, MODE_PRIVATE).edit().clear().commit();
        inTime = 0;
        swipeInTime = 0;
        swipeOutTime = 0;
        reachedOfficeTime = 0;
    }

    public void fetchData() {
        UserConfiguration.fetch(this, new DoneCallback() {
            @Override
            public void onDone(boolean flag) {
            }
        });
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        String yyymmdd = df.format(calendar.getTimeInMillis());
        TheApplication.fetchdata(this, yyymmdd, new DoneCallback() {
            @Override
            public void onDone(boolean flag) {
                readSwipeData();
                refreshDisplay();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_refresh) {
            fetchData();
        } else if (id == R.id.nav_reset) {
            clearData();
            JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancelAll();
            }

            UserConfiguration.fetch(this, new DoneCallback() {
                @Override
                public void onDone(boolean flag) {
                }
            });

            SharedPreferences pref = MainActivity.this.getSharedPreferences("application", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.remove("smsReadTime");
            editor.commit();
            readMessages();

            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
            String yyymmdd = df.format(calendar.getTimeInMillis());

            TheApplication.fetchdata(MainActivity.this,yyymmdd, new DoneCallback() {
                @Override
                public void onDone(boolean flag) {
                    scheduleJobs();
                    LocationJob.scheduleNow(MainActivity.this);
                }
            });

        } else if (id == R.id.download_apk) {
            Intent updateIntent = new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://iot.kpraveen.in/swipeapk"));
            startActivity(updateIntent);
        } else if (id == R.id.logout) {
            clearData();
            logout();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onListFragmentInteraction(SwipeMessage item) {
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchData();
        sendActivity();
        Log.d(TheApplication.TAG, "onStart");
    }

    private void sendActivity() {
        JSONObject data = new JSONObject();
        try {
            data.put("type", "MySwipe");
            data.put("deviceManufacturer", Build.MANUFACTURER);
            data.put("deviceModel", Build.MODEL);
            data.put("deviceProduct", Build.PRODUCT);
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
    }

    @Override
    protected void onResume() {
        Log.d(TheApplication.TAG, "onResume");
        super.onResume();
        readMessages();
        readSwipeData();
        refreshDisplay();
        if (System.currentTimeMillis() - LocationHelper.lastLocationTime(this) > 2 *TheApplication.MINUTE)  {
            LocationJob.scheduleNow(this);
        }
        timer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
    }

}
