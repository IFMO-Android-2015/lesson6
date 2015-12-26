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
    SQLiteStatement insert = null;

    protected abstract CityJsonParser createParser();

    private void importCities(InputStream in) {
        CityJsonParser parser = createParser();
        try {
            insert = db.compileStatement(String.format(
                    "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?)",
                    CityContract.Cities.TABLE,
                    CityContract.Cities.CITY_ID,
                    CityContract.Cities.NAME,
                    CityContract.Cities.COUNTRY,
                    CityContract.Cities.LATITUDE,
                    CityContract.Cities.LONGITUDE));
            if(!db.inTransaction()) {
                db.beginTransaction();
            }
            parser.parseCities(in, this);
            if (db.inTransaction()) {
                db.setTransactionSuccessful();
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        } finally {
            db.endTransaction();
            if (insert != null) {
                try { insert.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to close SQLiteStatement: " + e, e);
                }
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

        insert.bindLong(1, id);
        insert.bindString(2, name);
        insert.bindString(3, country);
        insert.bindDouble(4, latitude);
        insert.bindDouble(5, longitude);

        //long rowId = db.insert(CityContract.Cities.TABLE, null /*nullColumnHack not needed*/, values);
        if (insert.executeInsert() < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
            return false;
        }
        return true;
    }

    private static final String LOG_TAG = "CityReader";

}
