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

    public static final int INSERTS_IN_TRANSACTION = 9001;
    private SQLiteDatabase db;
    private int importedCount;
    private SQLiteStatement insert;

    public CityFileImporter(SQLiteDatabase db) {
        this.db = db;
    }

    private int insertsCount;

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
        try {
            Log.i(LOG_TAG, "Start import");
            insert = db.compileStatement("INSERT INTO cities ("
                    + CityContract.CityColumns.CITY_ID + ","
                    + CityContract.CityColumns.NAME + ","
                    + CityContract.CityColumns.COUNTRY + ","
                    + CityContract.CityColumns.LATITUDE + ","
                    + CityContract.CityColumns.LONGITUDE
                    + ") VALUES (?,?,?,?,?)");
            CityJsonParser parser = createParser();
            parser.parseCities(in, this);
            if (db.inTransaction()) {
                db.setTransactionSuccessful();
            }
            if (db.inTransaction()) {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        } finally {
            if (insert != null) {
                try {
                    insert.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }
    }

    @Override
    public void onCityParsed(long id, String name, String country, double lat, double lon) {
        if (!db.inTransaction()) {
            db.beginTransaction();
            Log.i(LOG_TAG, "Begin transaction");
            insertsCount = 0;
        }
        insertCity(db, id, name, country, lat, lon);
        if (insertsCount == INSERTS_IN_TRANSACTION) {
            db.setTransactionSuccessful();
            db.endTransaction();
            importedCount += INSERTS_IN_TRANSACTION;
            Log.d(LOG_TAG, "Transaction ready " + importedCount + " cities");
        }
    }

    private boolean insertCity(SQLiteDatabase db,
                               long id,
                               @NonNull String name,
                               @NonNull String country,
                               double latitude,
                               double longitude) {
        final ContentValues values = new ContentValues();
        insert.bindLong(1, id);
        insert.bindString(2, name);
        insert.bindString(3, country);
        insert.bindDouble(4, latitude);
        insert.bindDouble(5, longitude);

        long rowId = insert.executeInsert();
        if (rowId < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
            return false;
        } else {
            insertsCount++;
        }
        return true;
    }

    private static final String LOG_TAG = "CityReader";

}
