package kpraveen.in.myswipe;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import org.json.JSONObject;

@Entity (tableName = "json_data")
public class JsonData {

    @PrimaryKey
    @NonNull
    public String id;

    public String type;
    public String reference;
    public String data;
    public long   time;
    public String appInstanceId;

}
