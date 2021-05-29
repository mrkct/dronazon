package it.cutecchia.sdp.common;

public class CityPoint {
    public static final int CITY_WIDTH = 10;
    public static final int CITY_HEIGHT = 10;

    public final int x, y;

    public CityPoint(int x, int y) {
        if (x < 0 || y < 0 || x >= CITY_WIDTH || y >= CITY_HEIGHT) {
            throw new IllegalArgumentException("The coordinates are out of bounds");
        }
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CityPoint)) {
            return false;
        }
        var other = (CityPoint) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }
}
