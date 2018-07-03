package kpraveen.in.myswipe;;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class AlarmService extends IntentService {

    public AlarmService() {
        super("AlarmService");
    }

    public static void setAlarm(Context context, int requestCode, long time) {
        Intent intent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time,  AlarmManager.INTERVAL_DAY , pendingIntent);
    }

    public static void setDailyAlarms(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 6);
        calendar.set(Calendar.MINUTE, 30);
        long time = calendar.getTimeInMillis();
        if (System.currentTimeMillis() > time) {
            time = time + AlarmManager.INTERVAL_DAY;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Intent startIntent = new Intent(this, LocationService.class);
        //startIntent.putExtra("startedBy", "Alarm");
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //    this.startForegroundService(startIntent);
        //} else {
        //    this.startService(startIntent);
       // }
    }

}
