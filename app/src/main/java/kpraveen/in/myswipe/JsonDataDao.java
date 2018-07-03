package kpraveen.in.myswipe;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface JsonDataDao {

    @Insert
    void insert(JsonData activity);

    @Query("select count(*) FROM json_data")
    long count();

    @Query("DELETE FROM json_data")
    void deleteAll();

    @Query("Delete from json_data where id = :id")
    void removeById(String id);

    @Query("SELECT * from json_data order by time ASC")
    List<JsonData> fetchAllRecords();

}
