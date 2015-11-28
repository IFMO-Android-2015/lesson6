package ru.ifmo.android_2015.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.GZIPInputStream;

import ru.ifmo.android_2015.json.CityJsonParser;
import ru.ifmo.android_2015.json.CityParserCallback;
import ru.ifmo.android_2015.util.ObservableInputStream;
import ru.ifmo.android_2015.util.ProgressCallback;

public abstract class CityFileImporter implements CityParserCallback {

    private SQLiteDatabase db;
    private int importedCount;
    private List<City> city = new ArrayList<>();
    private SQLiteStatement insert = null;

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
        CityJsonParser parser = createParser();
        try {
            parser.parseCities(in, this);
            trans();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        }
    }

    public void trans() {
        db.beginTransaction();
        try {
            insert = db.compileStatement("INSERT INTO cities" + " VALUES(?, ?, ?, ?, ?)");
            for (City anyCity : city) {
                insertCity(db,anyCity.id,anyCity.name,anyCity.country,anyCity.latitude,anyCity.longitude);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            if (insert != null) {
                insert.close();
            }
        }
    }

    @Override
    public void onCityParsed(long id, String name, String country, double lat, double lon) {

        city.add(new City(id,name,country,lat,lon));
        importedCount++;
        
    }

    private boolean insertCity(SQLiteDatabase db,
                               long id,
                               @NonNull String name,
                               @NonNull String country,
                               double latitude,
                               double longitude) {
        insert.bindLong(1, id);
        insert.bindString(2, name);
        insert.bindString(3,country);
        insert.bindDouble(4,latitude);
        insert.bindDouble(5,longitude);
        long rowId = insert.executeInsert();

        if (rowId < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
            return false;
        }
        return true;
    }

    public class City {
        long id;
        String name;
        String country;
        double latitude;
        double longitude;
        public City(long id, String name, String country, double latitude, double longitude) {
            this.id = id;
            this.country = country;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final String LOG_TAG = "CityReader";

}
