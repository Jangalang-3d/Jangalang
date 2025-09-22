package jangalang.engine.maps;

import java.util.ArrayList;
import jangalang.util.types.Pair;

public class Map {
    private ArrayList<Pair<Double, Double>> spawns;
    private ArrayList<Wall> walls;

    public Map() {
        this.spawns = new ArrayList<Pair<Double, Double>>();
        this.walls = new ArrayList<Wall>();
    }

    public void addSpawn(double x, double y) {
        this.spawns.add(new Pair<>(x, y));
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
