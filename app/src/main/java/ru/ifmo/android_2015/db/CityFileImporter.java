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
    private SQLiteStatement precompiledInsert;

    public CityFileImporter(SQLiteDatabase db) {
        this.db = db;
        precompiledInsert = db.compileStatement("INSERT INTO cities ("
                + CityContract.CityColumns.CITY_ID + ","
                + CityContract.CityColumns.NAME + ","
                + CityContract.CityColumns.COUNTRY + ","
                + CityContract.CityColumns.LATITUDE + ","
                + CityContract.CityColumns.LONGITUDE
                + ") VALUES (?, ?, ?, ?, ?)");
    }

    public final synchronized void importCities(File srcFile, ProgressCallback progressCallback) throws IOException {
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
            try {
                precompiledInsert.close();
            } catch(Exception e) {
            }
        }
    }

    protected abstract CityJsonParser createParser();

    private void importCities(InputStream in) {
        CityJsonParser parser = createParser();
        try {
            if(!db.inTransaction()){
                db.beginTransaction();
            }

            parser.parseCities(in, this);

            if (db.inTransaction()) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Parse failed: " + e, e);
        } finally {
            db.endTransaction();
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
        precompiledInsert.bindLong(1, id);
        precompiledInsert.bindString(2, name);
        precompiledInsert.bindString(3, country);
        precompiledInsert.bindDouble(4, latitude);
        precompiledInsert.bindDouble(5, longitude);

        long rowId = precompiledInsert.executeInsert();
        if (rowId < 0) {
            Log.w(LOG_TAG, "Failed to insert: " + name);
            return false;
        }
        return true;
    }

    private static final String LOG_TAG = "CityReader";
}
