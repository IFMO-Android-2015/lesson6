package ru.ifmo.android_2015.db.util;

import ru.ifmo.android_2015.db.CityContract;

/**
 * Created by Asus on 13.12.2015.
 */
public class Requests {
    public static String insertRequest = "INSERT INTO " + CityContract.Cities.TABLE + " ("
            + CityContract.Cities.CITY_ID + ","
            + CityContract.Cities.NAME + ","
            + CityContract.Cities.COUNTRY + ","
            + CityContract.Cities.LATITUDE + ","
            + CityContract.Cities.LONGITUDE
            + ") VALUES (?, ?, ?, ?, ?)";
}
