package CO3401.AdvancedProducerConsumer;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Conveyor belts are acting as passive buffers (shared
 * memory space) between the various hoppers, turntables and
 * sacks.
 * The data members {@link #mNumFreeSlots} and {@link #mNumAvailSlots}
 * are used to coordinate actions between turntables that insert presents
 * to another Conveyor (act as Producers) belt and turntables that
 * take presents from Conveyor belts (act as Consumers).
 * <p>
 * Thread-safety is managed via the following methods for:
 *
 * <p> Present extraction from belt:
 * <p> - step 1: {@link Conveyor#acquireResourcesForPresentExtraction}
 * <p> - step 2: Interrogate present and place it to it next destination.
 * <p> - step 3: {@link Conveyor#releaseResourcesForPresentExtraction}
 *
 * <p> Present insertion to belt:
 * <p> - step 1: {@link Conveyor#acquireResourcesForPresentInsertion}
 * <p> - step 2: Add present to the Conveyor belt.
 * <p> - step 3: {@link Conveyor#releaseResourcesForPresentInsertion}
 *
 * @author anonymous
 */
public class Conveyor {

    private static final int FIRST_PRESENT = 0;
    // Use the semaphore to record number of free buffer slots.
    // In this case, there can be only N elements on belt where N corresponds to the belts's size.
    public final Semaphore mNumFreeSlots;
    // The belts the present will pass from until it reaches its corresponding sack.
    private final HashSet<Integer> mDestinations = new HashSet<>();
    final private int mId;
    private final ArrayQueue<Present> mPresents; // The requirements say this must be a fixed size array
    // One thread access the resource at a time
    private final Semaphore mMutex = new Semaphore(1);
    // One thread wait for another thread to finish.
    // In this case, a Turntable cannot access the Conveyor till an element gets inserted.
    private final Semaphore mNumAvailSlots = new Semaphore(0);
    // Check whether the the preceding turntable can deliver more presents.
    // By declaring it volatile, all writes to the mWaitingForPresents variable,
    // will get immediately updated to all threads.
    private volatile boolean mWaitingForPresents;
    // Flag that describes whether the Conveyor belt is connected with a Hopper
    private boolean mIsConnectedWithAHopper;

    /**
     * Public Constructor. Initialize the conveyor's id, size, container,
     * and "future present incoming" flag (mWaitingForPresents).
     * Initialize the nNumFreeSlots Semaphore with the same size as its container.
     *
     * @param mId      The id of the Conveyor instance.
     * @param capacity The number of presents that the Conveyor can store at the same time.
     */
    public Conveyor(int mId, int capacity) {
        this.mId = mId;
        mPresents = new ArrayQueue<>(capacity, () -> new Present[capacity]);
        mWaitingForPresents = true;
        mNumFreeSlots = new Semaphore(capacity);
        mIsConnectedWithAHopper = false;
    }

    /**
     * Add the specified hopper id the the instance destinations map.
     *
     * @param hopperID The id of the hopper.
     */
    public void addDestination(int hopperID) {
        mDestinations.add(hopperID);
    }

    /**
     * Check if the Conveyor belt is empty.
     *
     * @return True if the container is empty. Otherwise, false.
     */
    public boolean isEmpty() {
        return mPresents.isEmpty();
    }

    /**
     * Add the front element of the hopper to this conveyor's container - {@link #mPresents}.
     * It is suggested that the following methods are invoked
     * <p>
     * - before {@link Conveyor#acquireResourcesForPresentInsertion}
     * <p>
     * - after {@link Conveyor#releaseResourcesForPresentInsertion}
     * <p>
     * this method's invocation to ensure thread-safety.
     *
     * @param src The hopper's container.
     */
    public void movePresentFromHopperToConveyor(@NotNull ArrayQueue<Present> src, @NotNull AtomicBoolean timerState) {
        if (timerState != null && timerState.get() == Hopper.TIMER_RUN_OUT) {
            return;
        }
        // Remove a present from the source (hopper/another input belt)
        mPresents.add(src.get());  // Add it to the Conveyor
    }

    /**
     * Add the front element of the Conveyor object to the container.
     * It is suggested that the following methods are invoked
     * <p>
     * - before {@link Conveyor#acquireResourcesForPresentInsertion}
     * <p>
     * - after {@link Conveyor#releaseResourcesForPresentInsertion}
     * <p>
     * this method's invocation to ensure thread-safety.
     *
     * @param src The container of another conveyor object.
     */
    public void movePresentFromOneConveyorToAnother(@NotNull ArrayQueue<Present> src) {
        movePresentFromSrcToDest(src);
    }

    /**
     * Transfers the front present of the src's container
     * to this conveyor object's container.
     *
     * @param src The source of the inserted present.
     */
    private void movePresentFromSrcToDest(@NotNull ArrayQueue<Present> src) {
        // Remove a present from the source (hopper/another input belt)
        mPresents.add(src.get());  // Add it to this conveyor
    }

    /**
     * Access the conveyor's front element.
     * It is suggested that the following methods are invoked
     * <p>
     * - before {@link Conveyor#acquireResourcesForPresentInsertion}
     * <p>
     * - after {@link Conveyor#releaseResourcesForPresentInsertion}
     * <p>
     * this method invocation to ensure that no other thread
     * has access to the Conveyor object simultaneously (thread-safety).
     * {@link Conveyor#releaseResourcesForPresentInsertion} can be invoked once
     * the specific element is no longer needed. Usually combined with
     * {@link Conveyor#movePresentFromHopperToConveyor} in the following scenario:
     * <p> 1. {@link Conveyor#acquireResourcesForPresentInsertion} gets invoked to secure
     * the needed resources.
     * <p> 2. Conveyor#peekClosestPresent is called to access the present and
     * interrogate it.
     * <p> 3. The present gets passed on the its next destination.
     * <p> 4. {@link Conveyor#releaseResourcesForPresentInsertion} gets called to release
     * the acquired resources.
     *
     * @return A reference to the belt's first element.
     */
    public Present peekClosestPresent() throws IndexOutOfBoundsException {
        return mPresents.peek(FIRST_PRESENT); // Interrogate the present
    }

    /**
     * Requests for the resources needed to remove an element (present) from the
     * conveyor's container. If the resources are already taken by another thread,
     * then this thread will go to waiting state till the resource they get released.
     */
    public void acquireResourcesForPresentExtraction() {
        try {
            mNumAvailSlots.acquire();
            mMutex.acquire();
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Requests for the resources needed to add an element (present) to the
     * conveyor's container. If the resources are already taken by another thread,
     * then this thread will go to waiting state till the resource they get released.
     */
    public void acquireResourcesForPresentInsertion() {
        try {
            mNumFreeSlots.acquire();
            mMutex.acquire();
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Releases the resources needed to add an element (present) to the
     * conveyor's container. Threads that got blocked trying to acquire them
     * get notified.
     */
    public void releaseResourcesForPresentInsertion() {
        mMutex.release();
        mNumAvailSlots.release();
    }

    /**
     * Releases the resources needed to remove an element (present) from the
     * conveyor's container. Threads that got blocked trying to acquire them
     * get notified.
     */
    public void releaseResourcesForPresentExtraction() {
        mMutex.release();
        mNumFreeSlots.release();
    }

    /**
     * Access the present count of the Conveyor.
     *
     * @return The number of presents that the Conveyor currently holds.
     */
    public int getNumberOfPresents() {
        return mPresents.getSize();
    }

    /**
     * Access the destination set that holds info about the presents corresponding belt and sack.
     *
     * @return A HashSet that has data about the present's next destinations.
     */
    public HashSet<Integer> getDestinations() {
        return mDestinations;
    }

    /**
     * Access the id of the Conveyor instance.
     *
     * @return The id of the Conveyor object.
     */
    public int getId() {
        return mId;
    }

    /**
     * Access the data structure that stores the Conveyor's presents.
     *
     * @return A reference to the Conveyor's present container.
     */
    public ArrayQueue<Present> getPresents() {
        return mPresents;
    }

    /**
     * Check if the Conveyor is waiting for presents.
     *
     * @return True, if preceding Conveyor belts are not empty
     * or are also waiting for presents (is calculated recursively inside
     * the Turntable instances). Otherwise, false.
     */
    public boolean isWaitingForPresents() {
        return mWaitingForPresents;
    }

    /**
     * A setter that allows manipulation of the mWaitingForPresents data member.
     *
     * @param waitingForPresents A boolean that signals whether this object is waiting
     *                           for future presents.
     */
    public void setWaitingForPresents(boolean waitingForPresents) {
        this.mWaitingForPresents = waitingForPresents;
    }

    /**
     * Returns the value of {@link #mIsConnectedWithAHopper}.
     *
     * @return True, if the current conveyor object is associated
     * with a hopper object. Otherwise, false.
     */
    public boolean isConnectedWithAHopper() {
        return mIsConnectedWithAHopper;
    }

    /**
     * Setter for {@link #mIsConnectedWithAHopper}.
     *
     * @param connectedWithAHopper A boolean indicating whether the
     *                             current conveyor belt is associated with
     *                             a hopper.
     */
    public void setConnectedWithAHopper(boolean connectedWithAHopper) {
        mIsConnectedWithAHopper = connectedWithAHopper;
    }

    /**
     * Generates the "taken to available slots" ratio.
     * formula: number of presents currently on the conveyor belt
     * divided by the maximum number of presents the conveyor belt can
     * hold at the same time.
     * Note: doubles are used instead of floats for more precision.
     *
     * @return A double value between 0.0 - 1.0
     */
    public double getTakenToAvailableSlotsRatio() {
        return ((double) this.mPresents.getSize())
                / ((double) this.mPresents.getCapacity());
    }
}
