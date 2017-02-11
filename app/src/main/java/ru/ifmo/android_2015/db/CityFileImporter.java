package ru.ifmo.android_2015.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.database.sqlite.SQLiteStatement;
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

    public static final int INSERTS_IN_TRANSACTION = 10250;
    private SQLiteDatabase db;
    private SQLiteStatement insert;
    private int importedCount;
    private int inTransactionCount;

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
        try {
            insert = db.compileStatement(CityContract.Cities.INSERT);

            try {
                CityJsonParser parser = createParser();
                try {
                    parser.parseCities(in, this);

                    if (db.inTransaction()) {
                        db.setTransactionSuccessful();
                    }

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
                }
            } finally {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
            }
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
            inTransactionCount = 0;
        }

        insert.bindLong(1, id);
        insert.bindString(2, name);
        insert.bindString(3, country);
        insert.bindDouble(4, lat);
        insert.bindDouble(5, lon);
        long rowId = insert.executeInsert();
        if (rowId < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
        } else {
            inTransactionCount++;
        }

        if (inTransactionCount == INSERTS_IN_TRANSACTION) {
            db.setTransactionSuccessful();
            db.endTransaction();
            importedCount += INSERTS_IN_TRANSACTION;
            Log.d(LOG_TAG, "Processed " + importedCount + " cities");
        }
    }

    private static final String LOG_TAG = "CityReader";

}
