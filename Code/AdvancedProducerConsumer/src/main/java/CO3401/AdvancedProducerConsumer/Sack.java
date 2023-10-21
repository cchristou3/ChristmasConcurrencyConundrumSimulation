package CO3401.AdvancedProducerConsumer;

import static java.lang.Thread.sleep;

/**
 * A buffer for depositing Presents.
 *
 * @author anonymous
 */
public class Sack {

    /*
     * When the sacks are full, elves can deliver them in
     * a trolley to the appropriate grotto. Assuming the elves
     * first move aside the full sack and replace it with a new one
     * and then deliver it to the grotto, then the actual time needed
     * to replace every single sack would be constant.
     */
    private static final long REPLACING_SACK = 100L;

    private final int mId;
    private final int mCapacity;
    private ArrayQueue<Present> mAccumulation;
    private int mTotalNumberOfPresents;

    /**
     * Public Constructor. Initialize the Sack's data members
     * and create a new container that will hold all the presents
     * with maximum size the specified capacity argument.
     *
     * @param mId      The unique identifier of the Sack instance.
     * @param capacity The maximum number of presents it can hold.
     */
    public Sack(int mId, int capacity) {
        mCapacity = capacity;
        mAccumulation = new ArrayQueue<>(mCapacity, () -> new Present[mCapacity]);
        this.mId = mId;
        mTotalNumberOfPresents = 0;
    }

    /**
     * Check if the container has reached its maximum capacity.
     *
     * @return True if full. Otherwise, false.
     */
    public boolean isFull() {
        return mAccumulation.isFull();
    }

    /**
     * Creates a new present container and sleeps the current
     * thread for a specific amount of time to simulate the sack replacement.
     */
    public void elfReplacingSackWithNewOne() {
        mAccumulation = new ArrayQueue<>(mCapacity, () -> new Present[mCapacity]);
        try {
            sleep(REPLACING_SACK);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Add the present to the sack's container. Also, increments
     * the number of total presents stored on a sack of this age range.
     * <p>
     * It is suggested that the following methods are invoked
     * <p>
     * - before Conveyor#acquireResourcesForPresentInsertion
     * <p>
     * - after Conveyor#releaseResourcesForPresentInsertion
     * <p>
     * this method invocation to ensure thread-safety.
     *
     * @param present The present to be inserted to the sack.
     */
    public void addPresentToSack(Present present) {
        if (isFull()) {
            elfReplacingSackWithNewOne();
        }
        this.mAccumulation.add(present);
        mTotalNumberOfPresents++;
    }

    /**
     * Access the sack's id.
     *
     * @return The unique identifier of the sack.
     */
    public int getId() {
        return mId;
    }

    /**
     * Access the total number stored in this kind sack.
     *
     * @return The count of all presents that were stored in
     * a sack of this age range.
     */
    public int getTotalNumberOfPresents() {
        return mTotalNumberOfPresents;
    }
}
