package jangalang.util.types;

public class Vector {
    public double x;
    public double y;

    public Vector() {
        this.x = 0;
        this.y = 0;
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

    public boolean equals(Vector v) {
        return this.x == v.x && this.y == v.y;
    }
}
