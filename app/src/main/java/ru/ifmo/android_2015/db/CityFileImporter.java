package ru.ifmo.android_2015.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.GZIPInputStream;

import ru.ifmo.android_2015.db.util.City;
import ru.ifmo.android_2015.json.CityJsonParser;
import ru.ifmo.android_2015.json.CityParserCallback;
import ru.ifmo.android_2015.util.ObservableInputStream;
import ru.ifmo.android_2015.util.ProgressCallback;

public abstract class CityFileImporter implements CityParserCallback {

    private SQLiteDatabase db;
    private SQLiteStatement insert;
    private int importedCount;

    public CityFileImporter(SQLiteDatabase db) {
        this.db = db;
    }

    public final synchronized void importCities(File srcFile,
                                                ProgressCallback progressCallback)
            throws IOException {

        InputStream in = null;

        try {
            long fileSize = srcFile.length();
            in = new FileInputStream(srcFile);
            in = new BufferedInputStream(in);
            in = new ObservableInputStream(in, fileSize, progressCallback);
            in = new GZIPInputStream(in);
            importCities(in);

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to close file: " + e, e);
                }
            }
        }
    }

    protected abstract CityJsonParser createParser();
/*
//  @@@@Transac All@@@@
    private void importCities(InputStream in) {

        CityJsonParser parser = createParser();
        try {
            insert = db.compileStatement(CityContract.Cities.INSERT);
            db.beginTransaction();
            parser.parseCities(in, this);
            insertFromQueue(db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        } finally {
            if (insert != null) {
                insert.close();
            }
            db.endTransaction();
        }
    }

    @Override
    public void onCityParsed(long id, String name, String country, double lat, double lon) {
        //insertCity(db, id, name, country, lat, lon);
//        addToQueue(id, name, country, lat, lon);
        insertFromQueueNoTransac(db, new City(id, name, country, lat, lon));
//        importedCount++;
//        if (importedCount % 10000 == 0) {
//            insertFromQueue(db);
//            Log.d(LOG_TAG, "Processed " + importedCount + " cities");
//        }
    }

    Queue<City> queue = new ArrayDeque<>();
    private void addToQueue(long id,
                            @NonNull String name,
                            @NonNull String country,
                            double latitude,
                            double longitude) {
        final ContentValues values = new ContentValues();
        values.put(CityContract.CityColumns.CITY_ID, id);
        values.put(CityContract.CityColumns.NAME, name);
        values.put(CityContract.CityColumns.COUNTRY, country);
        values.put(CityContract.CityColumns.LATITUDE, latitude);
        values.put(CityContract.CityColumns.LONGITUDE, longitude);
        queue.add(new City(id, name, country, latitude, longitude));
    }


    private void insertFromQueue(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            while (!queue.isEmpty()) {
                final City values = queue.remove();
                insert.bindLong(1, values.getCityID());
                insert.bindString(2, values.getName());
                insert.bindString(3, values.getCountry());
                insert.bindDouble(4, values.getLatitude());
                insert.bindDouble(5, values.getLongitude());
                insert.execute();
                insert.clearBindings();
                //db.insert(CityContract.Cities.TABLE, null, queue.remove());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertFromQueueNoTransac (SQLiteDatabase db, City values) {
        insert.bindLong(1, values.getCityID());
        insert.bindString(2, values.getName());
        insert.bindString(3, values.getCountry());
        insert.bindDouble(4, values.getLatitude());
        insert.bindDouble(5, values.getLongitude());
        insert.execute();
        insert.clearBindings();
        //db.insert(CityContract.Cities.TABLE, null, queue.remove());
    }
//@@@@all transac@@@@
*/

    //@@@@10000 transaction@@@@
    private void importCities(InputStream in) {
        CityJsonParser parser = createParser();
        try {
            insert = db.compileStatement(CityContract.Cities.INSERT);
            parser.parseCities(in, this);
            insertFromQueue(db);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        } finally {
            if (insert != null) {
                insert.close();
            }
        }
    }

    @Override
    public void onCityParsed(long id, String name, String country, double lat, double lon) {
        addToQueue(id, name, country, lat, lon);
        importedCount++;
        if (importedCount % 10000 == 0) {
            insertFromQueue(db);
            Log.d(LOG_TAG, "Processed  " + importedCount + " cities");
        }
    }

    Queue<City> queue = new ArrayDeque<>();
    private void addToQueue(long id,
                            @NonNull String name,
                            @NonNull String country,
                            double latitude,
                            double longitude) {
        queue.add(new City(id, name, country, latitude, longitude));
    }


    private void insertFromQueue(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            while (!queue.isEmpty()) {
                final City values = queue.remove();
                insert.bindLong(1, values.getCityID());
                insert.bindString(2, values.getName());
                insert.bindString(3, values.getCountry());
                insert.bindDouble(4, values.getLatitude());
                insert.bindDouble(5, values.getLongitude());
                insert.execute();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
//@@@@end 10000 transac@@@@
    private static final String LOG_TAG = "CityReader";

}
