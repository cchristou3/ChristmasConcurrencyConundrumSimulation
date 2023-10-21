package CO3401.AdvancedProducerConsumer;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;


/**
 * <p>Brief description:</p>
 * The turntable has 2 alignments – North-South, and East-West. Presents
 * can be moved in either direction by the turntable, so it should only ever
 * need to move through 90 degrees from one alignment to the other. E.g.
 * If a present was moving from West to East, the turntable would not need
 * to rotate with the Present on it. Each turntable has a set of
 * destination age ranges associated with each of its output
 * ports (as well as knowing the identities of any attached conveyor belts /
 * sacks).
 *
 * <p>Implementation details:</p>
 * <p>Initially, the turntable checks
 * the number of recorded input conveyor belts. If the turntable
 * has no more input belts, then it transitions to its clean-up stage (see line 141).
 * Otherwise, it proceeds on picking the input belt with the highest priority (see line 176).
 * This procedure considers the number of presents each input belt has and whether
 * or not it is connected with a hopper. Of course, if the turntable has only one
 * input belt, then it is chosen by default. Next, it would acquire the resources
 * concerning the buffer’s available slots and the mutual exclusion lock (see line 250). Both
 * locks are located inside the hopper’s output belt. By acquiring the former lock,
 * we ensure that the current turntable cannot extract the present from the conveyor
 * belt till a present gets placed on it. Thus, a “Happens-Before” relationship is
 * established. In contrast, by acquiring the latter lock, we ensure that only a
 * single thread can access the same resource (conveyor) at a time. Once the resources
 * have been obtained, the turntable interrogates the closest present from the input belt
 * (see lines 262-279).
 * The present is then placed to its next destination and the turntable’s belts get
 * validated and updated ({@link #updateInputBeltOfProceedingTurntables},
 * {@link #isCurrentInputBeltValid(Conveyor, int)}).
 * For instance, input belts that have their flag (mWaitingForPresents)
 * set to false and are empty, are no more usable thus, they get removed from the turntable.
 * And based on the turntable’s input belts’ validity, its output belts’ flag (mWaitingForPresents)
 * is set accordingly. To illustrate, if its input belts are no more working, then it
 * implies that the output belt will no longer receive any more presents. As a result,
 * once it is out of presents, it will be out of order as well. After the validation
 * process, the appropriate resources are released. In case the present’s destination
 * involves an output conveyor belt, then an additional step is required. In this step,
 * the turntable acts as a hopper, it acquires the same resources a hopper needs, adds
 * the present to the belt, and then releases them ({@link #addPresentToBeltTransaction(Conveyor, Conveyor, int)}).
 * Everything discussed above constitutes one iteration. And as mentioned before,
 * the turntable keeps on iterating till it has no more input belts.
 * Its clean-up stage consists of updating its output belts’ flag (mWaitingForPresents)
 * to false while also releasing appropriate resources to allow proceeding turntables to
 * continue working. (otherwise, they’ll still be waiting to acquire resources)</p>
 *
 * @author anonymous
 */
public class Turntable extends Thread {

    // Data members
    public static final byte N = 0;
    public static final byte E = 1;
    public static final byte S = 2;
    public static final byte W = 3;
    private static final long ROTATE_90_DEGREES = 500L; // It should take 0.5 seconds to rotate the table 90 degrees.
    private static final long ROTATE_180_DEGREES = 0L;
    private static final long PRESENT_MOVEMENT = 750L; // It should take 0.75 seconds to move a present either on or off a turntable.
    private static final byte MAX_NUM_OF_SAME_CON_TYPE = 3; // Max number of the same Conveyor belt type.

    // global lookup: age-range -> SackID
    static HashMap<String, Integer> destinations = new HashMap<>();
    // Equivalent to HashMap<Connection, Integer>. Identifies the port number that a connection is associated with.
    // E.g. Connection instance -> compass direction
    private final PortHashMap ports;
    private final ArrayQueue<Connection> inputBeltConnections;
    private final ArrayQueue<Connection> outputBeltConnections;
    private final String id;
    private final Connection[] connections = new Connection[4];
    // this individual table's lookup: SackID -> output port
    private final HashMap<Integer, Integer> outputMap = new HashMap<>();
    // This keeps track of the turntable's facing direction (e.g. N, E, S, W)
    private int currentCompassDirection;

    /**
     * Initialize the Turntable's id with the specified id and
     * allocates memory for all data structures of the class instance.
     * Sets the value of {@link #currentCompassDirection} to its default value.
     *
     * @param ID The id of the turntable instance.
     */
    public Turntable(String ID) {
        id = ID;
        currentCompassDirection = N; // By default, the table's initial direction is North
        ports = new PortHashMap();
        final byte capacity = MAX_NUM_OF_SAME_CON_TYPE;
        inputBeltConnections = new ArrayQueue<>(capacity, () -> new Connection[capacity]);
        outputBeltConnections = new ArrayQueue<>(capacity, () -> new Connection[capacity]);
    }

    /**
     * Attach the specified connection to the turntable's
     * specified port number.
     *
     * @param port The number of the turntable's port.
     * @param conn The connection instance.
     */
    public void addConnection(int port, Connection conn) {
        connections[port] = conn;
        if (conn != null) {
            ports.put(conn, port);
            if (conn.getConnType() == ConnectionType.OutputBelt) {
                outputBeltConnections.add(conn);
                for (Integer integer : conn.getBelt().getDestinations()) {
                    outputMap.put(integer, port);
                }
            } else if (conn.getConnType() == ConnectionType.OutputSack) {
                outputMap.put(conn.getSack().getId(), port);
            } else if (conn.getConnType() == ConnectionType.InputBelt) {
                inputBeltConnections.add(conn);
            }
        }
    }

    /**
     * Starting point of the thread. Gets internally invoked after
     * {@link Thread#start()} gets called.
     * Handles all the logic related to the Turntable.
     */
    @Override
    public void run() {

        // In a real case scenario, after each session, the turntable would have been placed
        // in a such way that it would have the same compass direction as of one of its input
        // conveyor belts. Thus, the machine would immediately start working, without wasting
        // any time rotating.
        // Set the compass direction with compass direction of the first input conveyor belt.
        currentCompassDirection = ports.get(inputBeltConnections.peek(0));

        // Life-long operation of the turntable instance
        operate();

        // Clean-up phase
        for (int i = 0; i < outputBeltConnections.getSize(); i++) {
            /*
            The current turntable would need to insert a present onto the belt first,
            so that proceeding turntables can extract it (Happens - Before relationship).
            Thus, in case the current turntable has no more presents to offer, a deadlock might occur
            as the preceding turntable would be waiting for a present to be inserted.
            As a result, once a turntable has reached this point (stop inserting presents)
            it de-allocates those resources so that preceding turntables can continue working.
             */
            outputBeltConnections.peek(i).getBelt()
                    .releaseResourcesForPresentInsertion();
            outputBeltConnections.peek(i).getBelt()
                    .setWaitingForPresents(false);
        }
    }

    /**
     * A loop that ends when the {@link Turntable#continueOperating} method returns false
     */
    public void operate() {
        // Iterate till the the turntable has no more input belts.
        while (continueOperating()) { /* ignore */ }
    }

    /**
     * Select the input belt with the highest priority. If there is only one input belt,
     * then choose that one. Afterwards, perform a single "present transportation" iteration.
     * E.g. Take one present from the input belt and place it into the Sack/Output belt.
     *
     * @return True if the turntable has any viable input belts. Otherwise, false.
     */
    private boolean continueOperating() {
        // Proceed if there are any input belts available
        if (!inputBeltConnections.isEmpty()) {
            // Get the index of the input belt with the highest priority
            int indexOfPriorityInputBelt = getHighPriorityInputBeltIndex();

            // Get a reference to the input belt
            Connection inputBeltConnection = inputBeltConnections
                    .peek(indexOfPriorityInputBelt);

            // Transfer a present from its input belt to its specified Sack/Output belt.
            iteratePresentTransportation(inputBeltConnection,
                    indexOfPriorityInputBelt);

            // If the data structure that holds all our input belts is empty,
            // then stop iterating (working /  your work is done...)
            return !inputBeltConnections.isEmpty();
        }
        return false;
    }

    /**
     * Get the index of the connection of type InputBelt that has the highest priority.
     * Usually it is the one with the most presents currently on.
     *
     * @return The index of the input belt that has the highest priority
     */
    private int getHighPriorityInputBeltIndex() {
        int indexOfInputBeltWithHighestPriority = 0; // By default, the first one is chosen.
        // If there are more than one input belts
        if (inputBeltConnections.getSize() > 1) {
            int indexOfInputBeltConnectedToAHopper = 0;
            double max = inputBeltConnections.peek(0).getBelt()
                    .getTakenToAvailableSlotsRatio();
            // Find the index of the belt that has the highest taken to free
            // slots
            double takenToAvailableSlotsRatio; // number of taken slots / number of total slots
            for (int i = 1; i < inputBeltConnections.getSize(); i++) {
                takenToAvailableSlotsRatio = inputBeltConnections.peek(i)
                        .getBelt().getTakenToAvailableSlotsRatio();
                if (takenToAvailableSlotsRatio > max) {
                    max = takenToAvailableSlotsRatio;
                    indexOfInputBeltWithHighestPriority = i;
                }
                if (inputBeltConnections.peek(i).getBelt().isConnectedWithAHopper()) {
                    indexOfInputBeltConnectedToAHopper = i;
                }
            }
            // Always prioritise belts connected to a hopper if other belts are empty
            if (inputBeltConnections.peek(indexOfInputBeltWithHighestPriority)
                    .getBelt().isEmpty()) {
                indexOfInputBeltWithHighestPriority = indexOfInputBeltConnectedToAHopper;
            }
        }
        return indexOfInputBeltWithHighestPriority;
    }

    /**
     * Flow of the method:
     * <p> - Access the resources needed to extract a present from the input belt.
     * <p> - Retrieve a reference of the input belt's front present.
     * <p> - Interrogate it to find out its destination (Sack or another output belt).
     * <p> - Place it to its next destination.
     * <p> - Update the "waiting" status of proceeding out belts based on the input belt's
     * status and number of presents that are currently being contained.
     * <p> - Release the resources needed to extract a present from the input belt.
     * <p> Note: Whenever the turntable tries to access the resources that are already acquired
     * by another turntable, it goes to waiting status till the resources get released.
     *
     * @param inputBeltConnection      The chosen connection of type InputBelt.
     * @param indexOfPriorityInputBelt The index of the chosen connection of type InputBelt.
     */
    private void iteratePresentTransportation(@NotNull Connection inputBeltConnection,
                                              int indexOfPriorityInputBelt) {
        // Turn the table to receive the present
        rotate(ports.get(inputBeltConnection)); // corresponds to the port's direction

        /* Acquire the resource from the input belt for extracting a present from it / lock access to current input belt */
        inputBeltConnection.getBelt().acquireResourcesForPresentExtraction();

        ////////////////////// Start of Critical Region for Extraction //////////////////////

        // Validate the input after the thread acquires it. (Used for long waiting threads)
        boolean validInput = isCurrentInputBeltValid(inputBeltConnection.getBelt(), indexOfPriorityInputBelt);
        if (!validInput) return;

        // Get the closest present between the current input conveyor belt
        // and the turntable
        Present closestPresent;
        try {
            closestPresent = inputBeltConnection.getBelt().peekClosestPresent();
        } catch (IndexOutOfBoundsException e) {
            // In case, we try to reach out to an element that does not exist.
            // release, the appropriate resources and terminate the method.
            /* Release resources from input belt / notify other threads*/
            inputBeltConnection.getBelt().releaseResourcesForPresentExtraction();
            return;
        }

        movePresent(); // It takes 0.75 seconds to move a present either on or off a turntable

        // Find its destination
        String destination = closestPresent.readDestination();
        // Get the id of the sack that matches its destination with the
        // destination of the present
        Integer sackId = destinations.get(destination);
        // Get the corresponding port of the table
        Integer port = outputMap.get(sackId);

        Conveyor inputBelt = inputBeltConnection.getBelt();
        // If a port was found then add the present to the turntable's corresponding sack.
        if (port != null && connections[port].getSack() != null) {
            // Get a reference to the connection's sack
            Sack sack = connections[port].getSack();
            rotate(ports.get(connections[port])); // Rotate towards the sack
            // Add present to sack
            sack.addPresentToSack(closestPresent);

            inputBelt.getPresents().get(); // Remove element from belt

            // Update the belts' status
            updateInputBeltOfProceedingTurntables();
            // Once the input is empty & not working it should remove it -> table shuts down
            isCurrentInputBeltValid(inputBeltConnection.getBelt(), indexOfPriorityInputBelt);
        } else {
            // If port is null, then it does not have a sack
            // Pass the present to the output conveyor belt
            Conveyor outputBelt = outputBeltConnections.peek(0).getBelt();
            if (outputBeltConnections.getSize() > 1) {
                // If there are many output belts.
                // Iterate through the output belts till you find one whose destination
                // matches with the present's destination.
                for (int i = 0; i < outputBeltConnections.getSize(); i++) {
                    if (outputBeltConnections.peek(i).getBelt().getDestinations().contains(sackId)) {
                        outputBelt = outputBeltConnections.peek(i).getBelt();
                        break;
                    }
                }
            }
            // Add present to belt if its corresponding Conveyor belt was found.
            addPresentToBeltTransaction(outputBelt, inputBelt, indexOfPriorityInputBelt);
        }

        ////////////////////// End of Critical Region for Extraction //////////////////////

        /* Release resources from input belt / notify the threads*/
        inputBeltConnection.getBelt().releaseResourcesForPresentExtraction();

        movePresent();// It takes 0.75 seconds to move a present either on or off a turntable
    }

    /**
     * Validate the input belts viability.
     * If the belt is waiting for presents (flag set to true) or it still
     * has presents on it then it is a viable input belt.
     * Otherwise, remove it from the turntable's input belt container and release
     * all resources for both the current input belt and the turntable's out belts.
     *
     * @param inputBelt        The input belt to be validated.
     * @param indexOfInputBelt The index of the input belt to be validated.
     * @return True if input belt is valid. Otherwise, false.
     */
    private boolean isCurrentInputBeltValid(@NotNull Conveyor inputBelt, int indexOfInputBelt) {
        // If the current input belt is not supported by further present delivering and is empty,
        // then remove it from the queue
        if (inputBelt.isEmpty() && !inputBelt.isWaitingForPresents()) {
            // Release all the acquired resource of the input belt, then remove it.
            inputBelt.releaseResourcesForPresentExtraction();
            inputBeltConnections.remove(indexOfInputBelt);
            return false;
        }
        return true;
    }

    /**
     * Flow of method:
     * <p> - Access the resources needed to add a present to an output belt.
     * <p> - Transfer the present from the input belt to the specified output belt.
     * <p> - Update the viability of both chosen input belt and the turntable's output belts.
     * <p> - Release the resources needed to add a present to an output belt.
     *
     * @param outputBelt               The output belt that the present will be added on.
     * @param inputBelt                The input belt that the present will be removed on.
     * @param indexOfPriorityInputBelt The index of the input belt that the present will be removed on.
     */
    private void addPresentToBeltTransaction(@NotNull Conveyor outputBelt, @NotNull Conveyor inputBelt, int indexOfPriorityInputBelt) {
        /* Acquire the resources of the output belt */
        outputBelt.acquireResourcesForPresentInsertion();

        ////////////////////// Start of Critical Region for Insertion //////////////////////
        // Add to output belt
        outputBelt.movePresentFromOneConveyorToAnother(inputBelt.getPresents());
        // Update the belts' status
        updateInputBeltOfProceedingTurntables();
        isCurrentInputBeltValid(inputBelt, indexOfPriorityInputBelt);
        ////////////////////// End of Critical Region Insertion ///////////////////////////

        /* Release the resources of the output belt / notify other threads */
        outputBelt.releaseResourcesForPresentInsertion();
    }

    /**
     * Updates the the status of the turntable's output belts.
     * If the turntable has no valid input belts, then sets the status
     * of all its output belts to NOT waiting (false).
     * {@link Conveyor#setWaitingForPresents}
     */
    private void updateInputBeltOfProceedingTurntables() {
        if (!anyViableInputBelts()) {
            // The previous turntable has handed over all presents that it should have
            // and is no longer working
            for (int i = 0; i < outputBeltConnections.getSize(); i++) {
                if (outputBeltConnections.peek(i).getBelt() != null) {
                    outputBeltConnections.peek(i).getBelt().setWaitingForPresents(false);
                }
            }
        }
    }

    /**
     * Check whether there is at least one input belt that is "waiting" for presents or that still
     * has presents on it.
     *
     * @return True if any input belt complies with the above condition. Otherwise, false.
     */
    private boolean anyViableInputBelts() {
        // Traverse the input belt connections till you find one whose belt is not empty or
        // that will receive presents in the near future
        for (int j = 0; j < inputBeltConnections.getSize(); j++) {
            if (!inputBeltConnections.peek(j).getBelt().isEmpty() ||
                    inputBeltConnections.peek(j).getBelt().isWaitingForPresents()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The thread sleep for a certain amount of time to simulate
     * the time taken in turning. The current and the specified new direction
     * are used to calculate the rotation time.
     *
     * @param newFacingDirection The number of the port that the turntable will turn to.
     */
    public void rotate(int newFacingDirection) {

        long rotationTime = ROTATE_180_DEGREES;
        // If both directions, are  either both odd or both even then no rotation is needed
        // e.g. from 1 to 3 -> No rotation  -> They are aligned
        //      from 2 to 4 -> No rotation
        // Otherwise, rotation is simulated
        if (!((currentCompassDirection % 2 == 0 && newFacingDirection % 2 == 0) // Either both are even
                || (currentCompassDirection % 2 != 0 && newFacingDirection % 2 != 0))) { // or odd
            rotationTime = ROTATE_90_DEGREES;
        }

        try {
            currentCompassDirection = newFacingDirection;
            sleep(rotationTime);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Sleep the thread to simulate a present moving inwards and outwards
     * of the turntable.
     */
    private void movePresent() {
        try {
            sleep(PRESENT_MOVEMENT);
        } catch (InterruptedException ignore) {
        }
    }
}
