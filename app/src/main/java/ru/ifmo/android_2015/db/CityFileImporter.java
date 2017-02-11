package ru.ifmo.android_2015.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
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

    public static final int INSERTS_IN_TRANSACTION = 10000;

    private SQLiteDatabase db;
    private SQLiteStatement statement;
    private boolean inTransaction;
    private int insertsMadeInCurrentTx;

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

    private void importCities(final InputStream in) {
        statement = db.compileStatement(CityContract.Cities.INSERT);
        try {
            try {
                CityJsonParser parser = createParser();
                parser.parseCities(in, CityFileImporter.this);

                if (db.inTransaction()) {
                    db.setTransactionSuccessful();
                }
            } finally {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        } finally {
            statement.close();
        }
    }

    @Override
    public void onCityParsed(long id, @NonNull String name, @NonNull String country, double lat, double lon) {
        if(!inTransaction) {
            inTransaction = true;
            db.beginTransaction();
            insertsMadeInCurrentTx = 0;
        }

        statement.bindLong(1, id);
        statement.bindString(2, name);
        statement.bindString(3, country);
        statement.bindDouble(4, lat);
        statement.bindDouble(5, lon);
        statement.executeInsert();
        insertsMadeInCurrentTx++;

        if (insertsMadeInCurrentTx == INSERTS_IN_TRANSACTION) {
            inTransaction = false;
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    private static final String LOG_TAG = "CityReader";
}
