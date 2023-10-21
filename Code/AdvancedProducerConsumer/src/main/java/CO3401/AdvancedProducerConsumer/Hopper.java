package CO3401.AdvancedProducerConsumer;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Brief description:</p>
 * <p>Hoppers have a collection of presents.
 * Hoppers are associated with a conveyor belt, and have a speed of
 * working. According to its pre-set speed of working, at appropriate intervals until it
 * is empty, a hopper will attempt to place presents onto the conveyor belt
 * – as long as there is space on the belt.
 * </p>
 * <p>Implementation details:</p>
 * <p>
 * At intervals, the hoppers place a single present onto their output conveyor belt.
 * The following steps are taken in every interval (see line 121). Initially, the hopper checks its
 * current size in terms of presents and the timer’s state (see line 111). If the hopper has no more
 * presents to distribute or the timer has run out, then it moves to its clean-up phase
 * (see lines 128-131).
 * Otherwise, it would acquire the resources concerning the buffer’s free slots and the
 * mutual exclusion lock (see line 114). Both locks are located inside the hopper’s output belt. By
 * acquiring the former lock, we ensure that the maximum number of presents that can be
 * placed onto the belt simultaneously is the same as its actual size. In contrast, by
 * acquiring the latter lock, we ensure that only a single thread can access the same
 * resource at a time. Once the appropriate resources have been obtained, the hopper
 * places a single present onto the output belt (see line 121).
 * It then proceeds to unlock the resources mentioned above (see line 124).
 * Finally, the hopper waits until the next interval passes so that it
 * can continue iterating through the process discussed above (see line 121).
 * Its clean-up phase consists of setting the flag data member of the
 * Conveyor (mWaitingForPresents) to false. This implies that once that Conveyor
 * belt is out of presents, it’ll no longer be supplied with presents.
 * Further, all locks related to that belt are released before the hopper finally shuts down.
 * </p>
 *
 * @author anonymous
 */
public class Hopper extends Thread {

    // Data members
    public static final boolean TIMER_RUN_OUT = true;
    public static final boolean TIMER_RUNNING = false;
    private static final byte NONE = 0;
    private static final long DEFAULT_INTERVAL_TIME = 1000L;
    private final ArrayQueue<Present> mCollection;
    private final int mIdentifier;
    private final Conveyor mBelt;
    private final int mSpeed;
    /**
     * All writes to the mTimerState variable,
     * will get immediately updated to all threads
     * {@link AtomicBoolean}.
     */
    private final AtomicBoolean mTimerState;
    private long mTotalWaitingTime;
    private int mInitialNumberOfPresents;

    /**
     * Public Constructor. Initialize the hopper's Identifier,
     * Conveyor object, and speed with the specified
     * arguments.
     * Initialize the hopper's container with the specified capacity.
     * Initialize both mTotalWaitingTime and mNumberOfDepositedPresents to zero.
     *
     * @param mIdentifier The id of the hopper.
     * @param con         The conveyor the hopper is attached to.
     * @param capacity    The amount of presents the hopper can deposit.
     * @param mSpeed      The speed that the hopper puts presents onto the attached belt.
     */
    public Hopper(int mIdentifier, @NotNull Conveyor con, int capacity, int mSpeed) {
        mCollection = new ArrayQueue<>(capacity, () -> new Present[capacity]);
        this.mIdentifier = mIdentifier;
        mBelt = con;
        this.mSpeed = mSpeed;
        mTotalWaitingTime = NONE;
        mInitialNumberOfPresents = NONE;
        mTimerState = new AtomicBoolean(TIMER_RUNNING);
        con.setConnectedWithAHopper(true);
    }

    /**
     * Add the specified present to the storage container.
     *
     * @param present The {@link Present} object to be added to the hopper's container.
     */
    public void fill(Present present) {
        mCollection.add(present);
    }

    /**
     * Invoked after {@link Thread#start} gets called.
     * Does all the logic of the hopper.
     */
    @Override
    public void run() {
        distributePresents();
    }

    /**
     * Add presents to the belt at every interval. Pause if the belt is full and wait till is has space.
     * Stop completely once the hopper has no more presents to put or when the timer
     * has run out.
     */
    private void distributePresents() {

        // Update the value of mInitialNumberOfPresents to the number of presents in the hopper.
        mInitialNumberOfPresents = this.mCollection.getSize(); // Before the machine starts working.

        // Keep on iterating till the hopper has no presents or till the timer has run out.
        while (!mCollection.isEmpty() && mTimerState.get() == TIMER_RUNNING) {
            long startTime = System.currentTimeMillis();
            /* Acquire resources from output belt */
            mBelt.acquireResourcesForPresentInsertion();

            long endTime = System.currentTimeMillis();
            long totalWaitingTime = endTime - startTime;

            mTotalWaitingTime += totalWaitingTime;

            mBelt.movePresentFromHopperToConveyor(mCollection, mTimerState);
            /* Release resources from output belt */
            mBelt.releaseResourcesForPresentInsertion();
            nextInterval();
        }
        // Clean-up phase
        // This triggers the beginning of the end of the system.
        mBelt.setWaitingForPresents(false);
        // Release all resources so that proceeding turntables can continue operating (to avoid deadlocks!).
        mBelt.releaseResourcesForPresentExtraction();
        mBelt.releaseResourcesForPresentInsertion();
    }

    /**
     * Sleeps the current thread to simulate the intervals.
     * By default, the hopper places one present every second.
     * For instance, if its speed is two then, it should place two presents
     * every second (1 present every 0.5 seconds).
     */
    private void nextInterval() {
        try {
            sleep(DEFAULT_INTERVAL_TIME / mSpeed);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Access the the total waiting time (in seconds) of the hopper.
     *
     * @return The amount of time the hopper was waiting to put a present onto
     * a Conveyor belt.
     */
    public long getTotalWaitingTimeInSeconds() {
        return (mTotalWaitingTime / 1000L);
    }

    /**
     * Access the total number of presents that were deposited
     * by this hopper.
     *
     * @return The count of all presents that were initially stored in this hopper.
     */
    public int getNumberOfPresentsDeposited() {
        return this.mInitialNumberOfPresents - this.mCollection.getSize();
    }

    /**
     * Access the hopper's id.
     *
     * @return The object's unique identifier.
     */
    public int getIdentifier() {
        return mIdentifier;
    }

    /**
     * Get the number of presents currently inside the hopper.
     *
     * @return The number of presents the hopper can distribute.
     */
    public int getNumberOfPresents() {
        return mCollection.getSize();
    }

    /**
     * Setter for {@link #mTimerState}.
     *
     * @param timerState A boolean indicating the state of the timer.
     */
    public void setTimerState(boolean timerState) {
        mTimerState.set(timerState);
    }
}
