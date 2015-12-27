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
    private SQLiteStatement statement;

    public CityFileImporter(SQLiteDatabase db) {
        this.db = db;
        this.statement = db.compileStatement("INSERT INTO cities (_id, name, country, latitude, longitude) " +
                "VALUES (?, ?, ?, ?, ?)");
    }

    public final synchronized void importCities(File srcFile,
                                                ProgressCallback progressCallback)
            throws IOException {


        try {
            long fileSize = srcFile.length();
            importCities(new GZIPInputStream(new ObservableInputStream(new BufferedInputStream(new FileInputStream(srcFile)), fileSize, progressCallback)));

        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to close file: " + e, e);
        }
    }

    protected abstract CityJsonParser createParser();

    private void importCities(InputStream in) {
        CityJsonParser parser = createParser();
        db.beginTransaction();
        try {
            parser.parseCities(in, this);

        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        } finally {
            db.endTransaction();
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to close SQLiteStatement: " + e, e);
                }
            }
        }
    }

    @Override
    public void onCityParsed(long id, String name, String country, double lat, double lon) {
        insertCity(id, name, country, lat, lon);
        importedCount++;
        if (importedCount % 1000 == 0) {
            Log.d(LOG_TAG, "Processed " + importedCount + " cities");
        }
    }

    private boolean insertCity(long id,
                               @NonNull String name,
                               @NonNull String country,
                               double latitude,
                               double longitude) {
        statement.bindLong(1, id);
        statement.bindString(2, name);
        statement.bindString(3, country);
        statement.bindDouble(4, latitude);
        statement.bindDouble(5, longitude);
        long rodId = statement.executeInsert();
        if (rodId < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
            return false;
        }
        return true;
    }

    private static final String LOG_TAG = "CityReader";

}
