package jangalang.common.maps;

import java.io.Serializable;
import java.util.ArrayList;
import jangalang.common.types.Pair;

public class MapData implements Serializable {
    private static final long serialVersionUID = 1L;

    private ArrayList<Pair<Double, Double>> spawns;
    private ArrayList<Wall> walls;

    public MapData() {
        this.spawns = new ArrayList<Pair<Double, Double>>();
        this.walls = new ArrayList<Wall>();
    }

    public void addSpawn(double x, double y) {
        this.spawns.add(new Pair<>(x, y));
    }

    public void addSpawn(Pair<Double, Double> p) {
        this.spawns.add(p);
    }

    public void addWall(Wall wall) {
        this.walls.add(wall);
    }

    public ArrayList<Pair<Double, Double>> getSpawns() {
        return this.spawns;
    }

    public ArrayList<Wall> getWalls() {
        return this.walls;
    }
}
