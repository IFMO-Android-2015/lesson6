package ru.ifmo.android_2015.db.util;

/**
 * Created by Ilnar Sabirzyanov on 12.12.2015.
 */
public class City {
    protected Long cityID;
    protected String name;
    protected String country;
    protected Double latitude;
    protected Double longitude;

    public City(Long primKey, String name, String country, Double latitude, Double longitude) {
        this.cityID = primKey;
        this.name = name;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getCityID() {
        return cityID;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
