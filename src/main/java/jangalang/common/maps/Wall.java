package jangalang.common.maps;

import java.io.Serializable;

import jangalang.common.types.Pair;

public class Wall implements Serializable {
    private static final long serialVersionUID = 1L;

    public Pair<Double, Double> start;
    public Pair<Double, Double> end;

    public Wall(Pair<Double, Double> start, Pair<Double, Double> end) {
        this.start = start;
        this.end = end;
    }

    public Wall(Double x1, Double y1, Double x2, Double y2) {
        this.start = new Pair<Double, Double>(x1, y1);
        this.end = new Pair<Double, Double>(x2, y2);
    }

    public Wall(Wall w) {
        this.start = w.start;
        this.end = w.end;
    }

    public boolean playerIntersect(double cx, double cy, double radius) {
        double x1 = start.getKey();
        double y1 = start.getValue();
        double x2 = end.getKey();
        double y2 = end.getValue();

        // Vector math: distance from circle center to line segment
        double dx = x2 - x1;
        double dy = y2 - y1;
        double t = ((cx - x1) * dx + (cy - y1) * dy) / (dx * dx + dy * dy);

        t = Math.clamp(t, 0, 1);
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;

        double distSq = (cx - closestX) * (cx - closestX) + (cy - closestY) * (cy - closestY);
        return distSq <= radius*radius;
    }

    public Double rayIntersect(double rx, double ry, double rdx, double rdy) {
        double x1 = start.getKey();
        double y1 = start.getValue();
        double x2 = end.getKey();
        double y2 = end.getValue();

        double sx = x2 - x1;
        double sy = y2 - y1;

        double denom = rdx * sy - rdy * sx;
        if (Math.abs(denom) < 1e-9)
            return null; // parallel

        double qx = rx - x1;
        double qy = ry - y1;

        double t = (rdx * qy - rdy * qx) / denom; // segment param (0..1)
        double u = (sx * qy - sy * qx) / denom; // ray param (>=0)

        return t >= 0.0 && t <= 1.0 && u >= 0.0
            ? u
            : null;
    }

    public double[] getNormal() {
        double x1 = start.getKey();
        double y1 = start.getValue();
        double x2 = end.getKey();
        double y2 = end.getValue();

        // assuming wall has (x1,y1) to (x2,y2)
        double dx = x2 - x1;
        double dy = y2 - y1;
        // perpendicular vector (-dy, dx) or (dy, -dx)
        double nx = -dy;
        double ny = dx;
        double len = Math.sqrt(nx*nx + ny*ny);
        return new double[]{ nx/len, ny/len };
    }

    public boolean equals(Object o) {
        if (!(o instanceof Wall)) return false;
        Wall w = (Wall) o;
        return this.start.equals(w.start) && this.end.equals(w.end);
    }
}
