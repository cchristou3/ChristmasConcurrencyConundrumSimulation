package CO3401.AdvancedProducerConsumer;

/**
 * Used to achieve communication between Turntable, Conveyor,
 * and Sack class instances that are related to each other.
 *
 * @author anonymous
 */
public class Connection {

    private final ConnectionType mConnType;
    private final Conveyor mBelt;
    private final Sack mSack;

    /**
     * Public Constructor. Initialize the class' data members.
     *
     * @param ct The type of connection.
     * @param c  The related Conveyor instance.
     * @param s  The related Sack instance.
     */
    public Connection(ConnectionType ct, Conveyor c, Sack s) {
        mConnType = ct;
        mBelt = c;
        mSack = s;
    }

    private boolean isNull(Object o) {
        return o == null;
    }

    /**
     * Access the object's ConnectionType.
     *
     * @return The object's ConnectionType.
     */
    public ConnectionType getConnType() {
        return mConnType;
    }

    /**
     * Access the object's Conveyor belt instance.
     *
     * @return A reference to the object's Conveyor belt.
     */
    public Conveyor getBelt() {
        return mBelt;
    }

    /**
     * Access the object's Sack instance.
     *
     * @return A reference to the object's Sack.
     */
    public Sack getSack() {
        return mSack;
    }

    /**
     * Generate a unique int value for the object based on the values
     * of its data members.
     * To be used in custom Hash Map implementations.
     *
     * @return An integer that uniquely identifies this object.
     * @see PortHashMap
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (mConnType == null ? 0 : mConnType.hashCode());
        hash = 31 * hash + (mSack == null ? 0 : mSack.hashCode());
        hash = 31 * hash + (mBelt == null ? 0 : mBelt.hashCode());
        return hash;
    }
}
