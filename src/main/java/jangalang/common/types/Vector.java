package jangalang.common.types;

import java.io.Serializable;

public class Vector implements Serializable {
    private static final long serialVersionUID = 1L;
    public double x;
    public double y;

    public Vector() {
        this.x = 0;
        this.y = 0;
    }

    public Vector(double viewAngle) {
        this.x = Math.cos(viewAngle);
        this.y = Math.sin(viewAngle);
    }

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector(Vector v) {
        this.x = v.x;
        this.y = v.y;
    }

    public double dotProduct(Vector a, Vector b) {
        return (a.x * b.x) + (a.y * b.y);
    }

    public void multiply(double d) {
        this.x *= d;
        this.y *= d;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Vector)) return false;
        Vector v = (Vector) o;
        return this.x == v.x && this.y == v.y;
    }
}
