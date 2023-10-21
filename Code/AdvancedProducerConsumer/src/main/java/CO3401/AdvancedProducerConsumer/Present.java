package CO3401.AdvancedProducerConsumer;

/**
 * A POJO used to represent the actual presents of the simulation.
 * It has attributes which store the age-group of child it
 * is suitable for.
 *
 * @author anonymous
 */
public class Present {
    final private String mAgeRange;

    /**
     * Initialize the present with its destination.
     *
     * @param destination Corresponds to the age range of the present.
     */
    public Present(String destination) {
        mAgeRange = destination;
    }

    /**
     * Access the age range of the present.
     *
     * @return The final destination (sack) of this Present object.
     */
    public String readDestination() {
        return mAgeRange;
    }

    /**
     * The representation of the Present object as a String.
     *
     * @return The destination of the present.
     */
    @Override
    public String toString() {
        return mAgeRange;
    }
}
