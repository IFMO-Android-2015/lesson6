package ru.ifmo.android_2015.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author creed
 * @date 21.12.15
 */
public class City {

    @SerializedName("_id")
    private long id;

    private String name = "";
    private String country = "";

    private Point coord = new Point();

    public City() {
    }

    public Point getCoord() {
        return coord;
    }

    public void setCoord(Point coord) {
        this.coord = coord;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
