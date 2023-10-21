package CO3401.AdvancedProducerConsumer;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A trivial Data structure template to serve as a container for the assignment's classes.
 * It is used by:
 * <p> - The Turntable class to store different kinds of connections.
 * <p> - The Conveyor class to store the presents currently on it.
 * <p> - The Hopper class to store collections of presents.
 * <p> - The Sack class to store all deposited presents.
 * It is a FIFO (First In, First Out) structure with the ability to remove elements
 * from either the front or any other valid index of the array.
 * <p>
 * Time Complexity:
 * <p> -> Access ({@link #peek(int)}) - O(1)
 * <p> -> Insertion ({@link #add(T)}) - O(1)
 * <p> -> Deletion ({@link #remove(int)} and {@link #get()}) - O(n)
 * <p> -> Search (not implemented; however, would need to iterate the array via a loop) - O(n)
 *
 * @param <T> Any data type
 * @author anonymous
 */
public class ArrayQueue<T> {

    private static final byte HEAD = 0;
    private final T[] mItems;
    private final int mCapacity;
    // By declaring it volatile, all writes to the mIndex variable,
    // will get immediately updated to all threads.
    private volatile int mIndex;

    /**
     * Public Constructor. Initialize the array container and
     * the pointer to it.
     *
     * @param mCapacity The maximum count of items that will
     *                  be stored in the container.
     * @param supplier  A function which does not take in any argument
     *                  but produces a value of type T[].
     */
    public ArrayQueue(int mCapacity, @NotNull Supplier<? extends T[]> supplier) {
        this.mCapacity = mCapacity;
        mItems = supplier.get(); // Or use this "hack": (T[]) new Object[mCapacity];
        // but it will cause an "Unchecked cast" warning
        mIndex = -1;
    }

    /**
     * Add the specified element to the back of the array.
     *
     * @param element The element to be stored.
     */
    public synchronized void add(T element) {
        if (!isFull()) {
            mItems[++mIndex] = element; // 1. mIndex = mIndex + 1, 2. [mIndex]
        }
    }

    /**
     * Remove and access the longest "waiting" element.
     *
     * @return The front element.
     */
    public synchronized T get() {
        if (!isEmpty()) {
            // Save a reference to the head
            T head = mItems[HEAD];
            // Remove the first node from the array
            remove(HEAD);
            return head;
        }
        return null;
    }

    /**
     * Removes the element on the specified index.
     *
     * @param index The position of the element inside the array.
     * @throws IndexOutOfBoundsException if the specified index is
     *                                   not valid.
     */
    public synchronized void remove(int index) {
        if (!isValid(index)) throw new IndexOutOfBoundsException();
        if (mIndex == 0) {
            mIndex--;
            return;
        }
        // Move all elements one position forward
        if (mIndex - index >= 0) System.arraycopy(mItems, index + 1, mItems, index, mIndex - index);
        mIndex--; // Decrement the size by one
    }

    /**
     * Check if the array has reached its maximum capacity.
     *
     * @return True if full. Otherwise, false.
     */
    public synchronized boolean isFull() {
        return mIndex == mCapacity - 1;
    }

    /**
     * Check if the array has no elements.
     *
     * @return True if empty. Otherwise, false.
     */
    public synchronized boolean isEmpty() {
        return mIndex == -1;
    }

    /**
     * Get the current size of the structure.
     *
     * @return The number of elements stored in the array.
     */
    public synchronized int getSize() {
        return mIndex + 1;
    }


    /**
     * Access the element in the specified index.
     *
     * @param index The position of an element.
     * @return The element in that position of the array,.
     * @throws IndexOutOfBoundsException if the specified index is
     *                                   not valid.
     */
    public synchronized T peek(int index) throws IndexOutOfBoundsException {
        if (index > mIndex || !isValid(index)) throw new IndexOutOfBoundsException();
        return mItems[index];
    }

    /**
     * Access the maximum number of elements the ArrayQueue can hold simultaneously.
     *
     * @return The capacity of the current ArrayQueue object.
     */
    public int getCapacity() {
        return this.mCapacity;
    }

    /**
     * Validate the given index.
     * If the array is empty, then any action that requires an index
     * is not feasible (see {@link ArrayQueue#peek} & {@link ArrayQueue#remove}).
     * If the index is not in the range of 0 (inclusive)
     * and the array's capacity.
     *
     * @param index A position in the array.
     * @return True, the index meets the above conditions. Otherwise, false.
     */
    private boolean isValid(int index) {
        return !isEmpty() && (index >= 0 && index < mCapacity);
    }
}
