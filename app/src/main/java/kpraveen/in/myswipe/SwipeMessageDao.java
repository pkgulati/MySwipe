package kpraveen.in.myswipe;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface SwipeMessageDao {

    @Insert
    void insert(SwipeMessage message);

    @Query("select count(*) FROM swipe_message")
    long count();

    @Query("DELETE FROM swipe_message")
    void deleteAll();

    @Query("Delete from swipe_message where id = :id")
    void removeById(String id);

    @Query("SELECT * from swipe_message")
    List<SwipeMessage> fetchAllRecords();

}
