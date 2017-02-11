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
import java.util.zip.GZIPInputStream;

import ru.ifmo.android_2015.json.CityJsonParser;
import ru.ifmo.android_2015.json.CityParserCallback;
import ru.ifmo.android_2015.util.ObservableInputStream;
import ru.ifmo.android_2015.util.ProgressCallback;

public abstract class CityFileImporter implements CityParserCallback {

    private SQLiteDatabase db;
    private int importedCount;
    private SQLiteStatement insert;

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

    private void importCities(InputStream in) {
        db.beginTransaction();
        CityJsonParser parser = createParser();
        try {
            insert = db.compileStatement("INSERT INTO " + CityContract.Cities.TABLE + "(" + CityContract.CityColumns.CITY_ID + ", " + CityContract.CityColumns.NAME + ", " + CityContract.CityColumns.COUNTRY + ", " + CityContract.CityColumns.LATITUDE + ", " + CityContract.CityColumns.LONGITUDE +
                    ") VALUES (?, ?, ?, ?, ?)");

            parser.parseCities(in, this);
            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        } finally {
            db.endTransaction();
            if (insert != null) {
                insert.close();
                insert = null;
            }
        }
    }

    @Override
    public void onCityParsed(long id, String name, String country, double lat, double lon) {
        insertCity(db, id, name, country, lat, lon);
        importedCount++;
        if (importedCount % 1000 == 0) {
            Log.d(LOG_TAG, "Processed " + importedCount + " cities");
        }
    }

    private boolean insertCity(SQLiteDatabase db,
                               long id,
                               @NonNull String name,
                               @NonNull String country,
                               double latitude,
                               double longitude) {
        final ContentValues values = new ContentValues();
        insert.bindLong(1,id);
        //values.put(CityContract.CityColumns.CITY_ID, id);
        insert.bindString(2,name);
        //values.put(CityContract.CityColumns.NAME, name);
        insert.bindString(3,country);
        //values.put(CityContract.CityColumns.COUNTRY, country);
        insert.bindDouble(4,latitude);
        //values.put(CityContract.CityColumns.LATITUDE, latitude);
        insert.bindDouble(5,longitude);
        //values.put(CityContract.CityColumns.LONGITUDE, longitude);
        insert.executeInsert();
        //long rowId = db.insert(CityContract.Cities.TABLE, null /*nullColumnHack not needed*/, values);
        /*if (rowId < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
            return false;
        }*/
        return true;
    }

    private static final String LOG_TAG = "CityReader";

}
