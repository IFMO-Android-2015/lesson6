package ru.ifmo.android_2015.model;

/**
 * @author creed
 * @date 21.12.15
 */
public class Point {
    private double lat = Double.NaN;
    private double lon = Double.NaN;

    public Point() {
    }

    public Point(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
