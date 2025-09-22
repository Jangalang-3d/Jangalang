package jangalang.util.types;

public class Pair<K, V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public Pair(Pair<K, V> p) {
        this.key = p.key;
        this.value = p.value;
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    public boolean equals(Pair<K, V> pair) {
        return this.key.equals(pair.key) && this.value.equals(pair.value);
    }

    public String toString() {
        return "Pair(" + this.key.toString() + ", " + this.value.toString() + ")";
    }
}
