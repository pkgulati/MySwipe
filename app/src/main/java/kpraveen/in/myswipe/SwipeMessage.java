package kpraveen.in.myswipe;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

@Entity (tableName = "swipe_message")
public class SwipeMessage {

    @PrimaryKey
    @NonNull
    public String   id;

    public String   text;
    public long     smsTime;
    public String   serviceCenter;
    public String   address;
    public long     smsSentTime;
    public String   source;

    public String toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("text", text);
            obj.put("smsTime", smsTime);
            obj.put("smsSentTime", smsSentTime);
            obj.put("serviceCenter", serviceCenter);
            obj.put("address", address);
            obj.put("source", source);
            obj.put("appInstanceId", TheApplication.appInstanceId);
            if (id != null && !id.isEmpty()) {
                obj.put("id", id);
            }
        } catch (JSONException e) {

        }
        return obj.toString();
    };
}
