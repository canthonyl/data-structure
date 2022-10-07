package canthonyl.fixture;

import java.util.Objects;

public class Point {

    public static Point of(Integer x, Integer y){
        return new Point(x, y);
    }

    public final Integer x;
    public final Integer y;

    public Point(Integer x, Integer y){
        this.x = x;
        this.y = y;
    }

    public Point add(Integer changeX, Integer changeY){
        return new Point(x + changeX, y + changeY);
    }

    public Point addDx(Integer changeX){
        return new Point(x + changeX, y);
    }

    public Point addDy(Integer changeY){
        return new Point(x , y+ changeY);
    }

    public boolean betweenX(Point x1, Point x2) {
        return x >= x1.x && x <= x2.x;
    }

    public boolean betweenY(Point y1, Point y2) {
        return y >= y1.y && y <= y2.y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Objects.equals(x, point.x) && Objects.equals(y, point.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Coord(" + x +
                ", " + y +
                ')';
    }
}