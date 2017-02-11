package ru.ifmo.android_2015.db.util;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import ru.ifmo.android_2015.db.CityContract;
import ru.ifmo.android_2015.model.City;

/**
 * @author creed
 * @date 21.12.15
 */
public class DbExecutor  {

    private static final String LOG_TAG = "CityReader";

    private SQLiteDatabase db;
    private SQLiteStatement statement;
    private BlockingQueue<Runnable> queue;
    private ThreadPoolExecutor executor;

    private static DbExecutor instance = null;

    private AtomicLong processed = new AtomicLong(0);
    private AtomicLong total = new AtomicLong(0);
    private AtomicBoolean parsingFinished = new AtomicBoolean(false);


    public DbExecutor() {
        queue = new LinkedBlockingQueue<Runnable>();
        executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, queue, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                Log.e(LOG_TAG, "rejectedExecution");
            }
        });
    }

    public static DbExecutor getInstance() {
        if (instance == null) {
            instance = new DbExecutor();
        }
        return instance;
    }

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    public void start() {
        if (db == null) return;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                db.execSQL("DROP TABLE IF EXISTS`" + CityContract.Cities.TABLE + "`");
                db.execSQL(CityContract.Cities.CREATE_TABLE);
                statement = db.compileStatement(CityContract.Cities.INSERT_INTO_TABLE);
                db.beginTransaction();
            }
        });
    }

    public void insertCity(final City city) {
        if (db == null) return;
        total.incrementAndGet();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                statement.bindLong(1, city.getId());
                statement.bindString(2, city.getName());
                statement.bindString(3, city.getCountry());
                statement.bindDouble(4, city.getCoord().getLat());
                statement.bindDouble(5, city.getCoord().getLon());
                statement.executeInsert();

                long v = processed.incrementAndGet();
                if (v % 10000 == 0) {
                    Log.d(LOG_TAG, "Processed " + processed.get() + " cities");
                }
                if (parsingFinished.get() && total.get() == v) {
                    finish();
                }
            }
        });

    }

    public void setParsingFinished(boolean value) {
        parsingFinished.set(value);
    }

    private void finish() {
        Log.d(LOG_TAG, "Inserting finished");
        db.setTransactionSuccessful();
        db.endTransaction();

        if (statement != null) {
            try {
                statement.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while close SQLiteStatement: " + e.getMessage());
            }
        }
    }
}
