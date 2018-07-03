package kpraveen.in.myswipe;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class LocationHelper {

    public static boolean loaded = false;

    public static ArrayList<Location> list = new ArrayList<Location>();
    private static long lastSaveTime = 0;

    public synchronized static void load(Context context) {
        SharedPreferences pref = context.getSharedPreferences("location", Context.MODE_PRIVATE);
        String str = pref.getString("locations", "");
        if (!str.isEmpty()) {
            try {
                JSONArray array = new JSONArray(str);
                list.clear();
                Log.d(TheApplication.TAG, "Number of locations loaded " + array.length());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Location loc = new Location("dummy");
                    loc.setLatitude(obj.getDouble("latitude"));
                    loc.setLongitude(obj.getDouble("longitude"));
                    loc.setTime(obj.getLong("time"));
                    list.add(loc);
                }
            } catch (JSONException e) {

            }
        }
        loaded = true;
    }

    public synchronized static void locationReceived(Context context, Location location) {
        list.add(location);
        if (System.currentTimeMillis() - lastSaveTime > 120000) {
             LocationHelper.saveLocations(context);
             lastSaveTime = System.currentTimeMillis();
        }
        double distance = location.distanceTo(TheApplication.officeLocation);
        if (location.getAccuracy() < UserConfiguration.instance.maxLocationAccuracy &&
                distance < UserConfiguration.instance.distanceAccuracy) {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
            String preferenceName = "swipeData" + df.format(System.currentTimeMillis());
            SharedPreferences pref = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
            boolean reachedOffice = pref.getBoolean("reachedOffice", false);
            if (!reachedOffice) {
                Log.d(TheApplication.TAG, "-----------   REACHED OFFICE ---------------");
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("reachedOffice", true);
                editor.putLong("reachedOfficeTime", System.currentTimeMillis());
                editor.commit();
                postReachedActivity(context);
            }
        }
    }

    public static void postReachedActivity(Context context) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        JSONObject data = new JSONObject();
        try {
            data.put("type", "ReachedOffice");
            data.put("appName", "MySwipe");
            data.put("appInstanceId", TheApplication.appInstanceId);
            data.put("justtime", tf.format(System.currentTimeMillis()));
            data.put("yyyymmdd", df.format(System.currentTimeMillis()));
            data.put("time", System.currentTimeMillis());
            data.put("name", TheApplication.username);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MessageManager.postData(context, TheApplication.baseUrl + "/api/activities?access_token=" + TheApplication.accessToken, data.toString());
    }

    public synchronized static void saveLocations(Context context) {
        JSONArray array = new JSONArray();
        int i = 1;
        while (i < list.size() - 1) {
            Location previous = list.get(i - 1);
            Location location = list.get(i);
            long diff = location.getTime() - previous.getTime();
            if (diff < UserConfiguration.instance.locationStorageInterval) {
                list.remove(i);
            } else {
                i++;
            }
        }
        while (list.size() > UserConfiguration.instance.locationStorageCount) {
            list.remove(0);
        }
        for (i = 0; i < list.size(); i++) {
            Location location = list.get(i);
            JSONObject obj = new JSONObject();
            try {
                obj.put("latitude", location.getLatitude());
                obj.put("longitude", location.getLongitude());
                obj.put("accuracy", location.getAccuracy());
                obj.put("provider", location.getProvider());
                obj.put("time", location.getTime());
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        SharedPreferences pref = context.getSharedPreferences("location", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("locations", array.toString());
        editor.commit();
        lastSaveTime = System.currentTimeMillis();
    }

    public static boolean isStationary(Context context, int minutes) {
        if (!loaded) {
            load(context);
        }
        if (list.size() < 2) {
            return false;
        }
        Location lastLocation = list.get(list.size() - 1);
        for (int i = list.size() - 2; i >= 0; i--) {
            Location location = list.get(i);
            if (lastLocation.getTime() - location.getTime() > minutes * 60000) {
                double distance = lastLocation.distanceTo(location);
                Log.d(TheApplication.TAG, "Stationary check distance  " + distance);
                if (distance < UserConfiguration.instance.stationaryCheckDistance) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public static long lastLocationTime(Context context) {
        if (!loaded) {
            load(context);
        }
        if (list.size() > 0) {
            Location location = list.get(list.size() - 1);
            return location.getTime();
        }
        return 0;
    }

    public static double distanceFromOffice(Context context) {
        if (!loaded) {
            load(context);
        }

        if (list.size() > 0) {
            Location location = list.get(list.size() - 1);
            double distance = location.distanceTo(TheApplication.officeLocation);
            return distance;
        }
        return -1;
    }

}
