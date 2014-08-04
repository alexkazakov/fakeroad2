package mobi.droid.fakeroad.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by max on 21.01.14.
 */
public class LocationDbHelper extends SQLiteOpenHelper{

    public static final int DB_VERSION = 1;

    public LocationDbHelper(final Context context){
        super(context, "location.db", null, DB_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db){
        db.execSQL("create TABLE IF NOT EXISTS routes " +
                           "(id integer primary key," +
                           "route_id INTEGER," +
                           "lat REAL," +
                           "lng REAL," +
                           "ordinal INTEGER" +
                           ")");
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion){
        db.execSQL("drop table trips");
        onCreate(db);
    }

    public int queryNextRouteID(){
        SQLiteDatabase readableDatabase = getReadableDatabase();
        if(readableDatabase == null){
            return -1;
        }
        int routeID = 0;
        Cursor cursor;
        cursor = readableDatabase.rawQuery("SELECT MAX(route_id) FROM routes", null);
        if(cursor.moveToFirst()){
            routeID = cursor.getInt(0);
        }
        cursor.close();
        return routeID + 1;
    }

    public List<LatLng> queryPoints(final int aRouteID){
        SQLiteDatabase readableDatabase = getReadableDatabase();
        if(readableDatabase == null){
            return null;
        }
        List<LatLng> list = new ArrayList<LatLng>();
        double lat = 0;
        double lng = 0;
        Cursor cursor;
        cursor = readableDatabase.query("routes", null, "route_id = ?",
                                        new String[]{String.valueOf(aRouteID)}, null, null,
                                        "ordinal");
        while(cursor.moveToNext()){
            lat = cursor.getDouble(cursor.getColumnIndex("lat"));
            lng = cursor.getDouble(cursor.getColumnIndex("lng"));
            list.add(new LatLng(lat, lng));
        }
        cursor.close();
        return list;
    }

    public Cursor routeCursor(final int aRouteID){
        SQLiteDatabase readableDatabase = getReadableDatabase();
        if(readableDatabase == null){
            return null;
        }
        return readableDatabase.query("routes", null,
                                      "route_id = " + aRouteID,
                                      null, null, null,
                                      "ordinal");
    }

    public LatLng readLatLng(Cursor aCursor){
        double lat = aCursor.getDouble(aCursor.getColumnIndex("lat"));
        double lng = aCursor.getDouble(aCursor.getColumnIndex("lng"));
        return new LatLng(lat, lng);
    }

    public void writeLatLng(final int aRouteID, final List<LatLng> aLoc){
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if(writableDatabase == null){
            return;
        }
        writableDatabase.beginTransaction();
        try{
            for(int i = 0; i < aLoc.size(); i++){
                LatLng latLng = aLoc.get(i);
                ContentValues contentValues = new ContentValues();
                contentValues.put("route_id", aRouteID);
                contentValues.put("lat", latLng.latitude);
                contentValues.put("lng", latLng.longitude);
                contentValues.put("ordinal", i);
                long rowID = writableDatabase.insert("routes", null, contentValues);
            }
            writableDatabase.setTransactionSuccessful();
        } finally{
            writableDatabase.endTransaction();
        }

    }

    public long writeLatLng(final int aRouteID, final LatLng aLoc, final int aOrder){
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if(writableDatabase == null){
            return -1;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("route_id", aRouteID);
        contentValues.put("lat", aLoc.latitude);
        contentValues.put("lng", aLoc.longitude);
        contentValues.put("ordinal", aOrder);
        long rowID;
        rowID = writableDatabase.insert("routes", null, contentValues);
        return rowID;
    }

}
