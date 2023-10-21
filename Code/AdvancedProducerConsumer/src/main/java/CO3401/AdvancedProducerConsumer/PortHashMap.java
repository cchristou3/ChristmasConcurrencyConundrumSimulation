package CO3401.AdvancedProducerConsumer;

import org.jetbrains.annotations.NotNull;

/**
 * Trivial hash map implementation using a single array.
 * Used in Turntable class to map the port of each connection.
 *
 * <p>
 * Time Complexity:
 * <p> -> Access ({@link #get(Connection)}) - O(1)
 * <p> -> Insertion ({@link #put(Connection, Integer)}) - O(1)
 * <p> -> Deletion (not implemented; however, would have similar logic as {@link #put(Connection, Integer)}) - O(1)
 * <p> -> Search (not implemented; as a HashMap relies on key-value pairs to access its elements) - N/A
 *
 * @author anonymous
 */
public class PortHashMap {

    static final int DEFAULT_MAXIMUM_CAPACITY = 20;// The more spaces, the less likely a collision will occur

    private final Integer[] ports;

    /**
     * Initialize an array of size DEFAULT_MAXIMUM_CAPACITY.
     */
    public PortHashMap() {
        ports = new Integer[DEFAULT_MAXIMUM_CAPACITY];
    }

    /**
     * Hashes the key to produce a unique index inside our array.
     * In the produced index the value is stored.
     *
     * @param key   A Connection instance.
     * @param value An integer value.
     */
    public void put(@NotNull final Connection key, @NotNull final Integer value) {
        int index = hash(key);
        ports[index] = value;
    }

    /**
     * Access the Map's value based ont the specified key.
     *
     * @param key A Connection instance.
     * @return The corresponding value.
     */
    public Integer get(final Connection key) {
        if (key == null) return null;
        return ports[hash(key)];
    }

    /**
     * Produces a unique integer number based on a given key.
     * Generates numbers from 0 up till the size of the array.
     *
     * @param key A Connection instance.
     * @return A unique int.
     */
    private int hash(@NotNull final Connection key) {
        return Math.abs(key.hashCode()) % ports.length;
    }
}
