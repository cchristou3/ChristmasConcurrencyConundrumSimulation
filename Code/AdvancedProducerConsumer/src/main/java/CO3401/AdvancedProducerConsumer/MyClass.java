package CO3401.AdvancedProducerConsumer;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static java.lang.Thread.sleep;

/**
 * <h1>CO3401 Advanced Software Engineering Techniques – Coursework (Part 1)</h1>
 * <h2>Title: Java Simulation of a Christmassy Concurrency Conundrum</h2>
 * <p>
 * <h3>Main program outlined structure:</h3>
 * <ul>
 * <li>
 * Read from a configuration file, and create the configuration of Hoppers, Belts, Turntables
 * and Sacks.
 * <ul><li>
 * Fill the hoppers with Presents according to the configuration file. Call the run
 * methods on the threaded objects.
 * </li><li>
 * For the specified duration of the simulation, output reports every 10 seconds. At
 * the appropriate time, instigate the shutdown of the machine.
 * </li></ul></li><li>
 * Output the final report.
 * </li>
 * </ul>
 *
 * @author anonymous
 */
public class MyClass {

    private static final String FILE_NAME
            = "C:\\Users\\cchar\\Documents\\Computing Year 4\\CO3401 Advanced Software Engineering Techniques\\" +
            "Assignment Part 1 - Final\\Code\\AdvancedProducerConsumer\\src\\main\\resources\\Scenarios\\scenario5.txt";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // These variables will store the configuration
        // of the Present sorting machine

        int numBelts;
        Conveyor[] belts;

        int numHoppers;
        Hopper[] hoppers;

        int numSacks;
        Sack[] sacks;

        int numTurntables;
        Turntable[] tables;

        int timerLength;

        ////////////////////////////////////////////////////////////////////////

        // READ FILE
        // =========
        Scanner inputStream = null;
        try {
            inputStream = new Scanner(new File(FILE_NAME));
        } catch (FileNotFoundException ex) {
            System.out.println("Error opening file");
            System.exit(0);
        }

        String line = "";

        // READ BELTS
        // ----------
        // Skip though any blank lines to start
        while (!line.startsWith("BELTS") && inputStream.hasNextLine()) {
            line = inputStream.nextLine();
        }

        numBelts = inputStream.nextInt();
        inputStream.nextLine();

        belts = new Conveyor[numBelts];

        for (int b = 0; b < numBelts; b++) {
            line = inputStream.nextLine(); // e.g. 1 length 5 destinations 1 2

            System.out.println(line);

            Scanner beltStream = new Scanner(line);
            int id = beltStream.nextInt();
            beltStream.next(); // skip "length"

            int length = beltStream.nextInt();
            belts[b] = new Conveyor(id, length);
            beltStream.next(); // skip "destinations"

            while (beltStream.hasNextInt()) {
                int dest = beltStream.nextInt();
                belts[b].addDestination(dest);
            }
        } // end of reading belt lines

        // READ HOPPERS
        // ------------
        // Skip though any blank lines
        while (!line.startsWith("HOPPERS") && inputStream.hasNextLine()) {
            line = inputStream.nextLine();
        }

        numHoppers = inputStream.nextInt();
        inputStream.nextLine();

        hoppers = new Hopper[numHoppers];

        for (int h = 0; h < numHoppers; h++) {
            // Each hopper line will look like this:
            // e.g. 1 belt 1 capacity 10 speed 1

            int id = inputStream.nextInt();
            inputStream.next(); // skip "belt"

            int belt = inputStream.nextInt();
            inputStream.next(); // skip "capacity"

            int capacity = inputStream.nextInt();
            inputStream.next(); // skip "speed"

            int speed = inputStream.nextInt();
            line = inputStream.nextLine(); // skip rest of line

            hoppers[h] = new Hopper(id, belts[belt - 1], capacity, speed);

        } // end of reading hopper lines

        // READ SACKS
        // ------------
        // Skip though any blank lines
        while (!line.startsWith("SACKS") && inputStream.hasNextLine()) {
            line = inputStream.nextLine();
        }

        numSacks = inputStream.nextInt();
        inputStream.nextLine();

        sacks = new Sack[numSacks];

        for (int s = 0; s < numSacks; s++) {
            // Each sack line will look like this:
            // e.g. 1 capacity 20 age 0-3

            int id = inputStream.nextInt();
            inputStream.next(); // skip "capacity"

            int capacity = inputStream.nextInt();
            inputStream.next(); // skip "age"

            String age = inputStream.next();
            line = inputStream.nextLine(); // skip rest of line

            sacks[s] = new Sack(id, capacity);
            Turntable.destinations.put(age, id);

        } // end of reading sack lines

        // READ TURNTABLES
        // ---------------
        // Skip though any blank lines
        while (!line.startsWith("TURNTABLES") && inputStream.hasNextLine()) {
            line = inputStream.nextLine();
        }

        numTurntables = inputStream.nextInt();
        inputStream.nextLine();

        tables = new Turntable[numTurntables];

        for (int t = 0; t < numTurntables; t++) {
            // Each turntable line will look like this:
            // A N ib 1 E null S os 1 W null

            String tableId = inputStream.next();
            tables[t] = new Turntable(tableId);

            int connId;

            inputStream.next(); // skip "N"
            Connection north = null;
            String Ntype = inputStream.next();
            if (!"null".equals(Ntype)) {
                connId = inputStream.nextInt();
                if (null != Ntype) {
                    switch (Ntype) {
                        case "os":
                            north = new Connection(ConnectionType.OutputSack, null, sacks[connId - 1]);
                            break;
                        case "ib":
                            north = new Connection(ConnectionType.InputBelt, belts[connId - 1], null);
                            break;
                        case "ob":
                            north = new Connection(ConnectionType.OutputBelt, belts[connId - 1], null);
                            break;
                    }
                    tables[t].addConnection(Turntable.N, north);
                }
            }

            inputStream.next(); // skip "E"
            Connection east;
            String Etype = inputStream.next();
            if (!"null".equals(Etype)) {
                connId = inputStream.nextInt();
                if (null != Etype) {
                    switch (Etype) {
                        case "os":
                            east = new Connection(ConnectionType.OutputSack, null, sacks[connId - 1]);
                            break;
                        case "ib":
                            east = new Connection(ConnectionType.InputBelt, belts[connId - 1], null);
                            break;
                        default:
                            east = new Connection(ConnectionType.OutputBelt, belts[connId - 1], null);
                            break;
                    }
                    tables[t].addConnection(Turntable.E, east);
                }
            }

            inputStream.next(); // skip "S"
            Connection south;
            String Stype = inputStream.next();
            if (!"null".equals(Stype)) {
                connId = inputStream.nextInt();
                if (null != Stype) {
                    switch (Stype) {
                        case "os":
                            south = new Connection(ConnectionType.OutputSack, null, sacks[connId - 1]);
                            break;
                        case "ib":
                            south = new Connection(ConnectionType.InputBelt, belts[connId - 1], null);
                            break;
                        default:
                            south = new Connection(ConnectionType.OutputBelt, belts[connId - 1], null);
                            break;
                    }
                    tables[t].addConnection(Turntable.S, south);
                }
            }

            inputStream.next(); // skip "W"
            Connection west;
            String Wtype = inputStream.next();
            if (!"null".equals(Wtype)) {
                connId = inputStream.nextInt();
                if (null != Wtype) {
                    switch (Wtype) {
                        case "os":
                            west = new Connection(ConnectionType.OutputSack, null, sacks[connId - 1]);
                            break;
                        case "ib":
                            west = new Connection(ConnectionType.InputBelt, belts[connId - 1], null);
                            break;
                        default:
                            west = new Connection(ConnectionType.OutputBelt, belts[connId - 1], null);
                            break;
                    }
                    tables[t].addConnection(Turntable.W, west);
                }
            }

            line = inputStream.nextLine(); // skip rest of line
        } // end of reading turntable lines

        // FILL THE HOPPERS
        // ----------------
        for (int i = 0; i < numHoppers; i++) {
            // Skip though any blank lines
            while (!line.startsWith("PRESENTS") && inputStream.hasNextLine()) {
                line = inputStream.nextLine();
            }
            int numPresents = inputStream.nextInt();
            inputStream.nextLine();
            for (int p = 0; p < numPresents; p++) {
                hoppers[i].fill(new Present(inputStream.next()));
                line = inputStream.nextLine();
            }

            System.out.println("Filled Hopper " + hoppers[i].getIdentifier());
        }

        // READ TIMER LENGTH
        // -----------------
        // Skip though any blank lines
        while (!line.startsWith("TIMER") && inputStream.hasNextLine()) {
            line = inputStream.nextLine();
        }
        Scanner timerStream = new Scanner(line);
        timerStream.next(); // skip "TIMER"
        timerLength = timerStream.nextInt();

        System.out.println("Machine will run for " + timerLength + "s.\n");

        ///////////////////////////////////////////////////////////////////////
        // END OF SETUP ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////


        // START the hoppers!
        for (int h = 0; h < numHoppers; h++) {
            hoppers[h].start();
        }

        // START the turntables!
        for (int t = 0; t < numTurntables; t++) {
            tables[t].start();
        }

        long time = 0;
        long currentTime;
        long startTime = System.currentTimeMillis();
        System.out.println("*** Machine Started ***");
        while (time < timerLength) {
            // sleep in 10 second bursts
            try {
                sleep(10000); // 10000
            } catch (InterruptedException ignored) {
            }
            currentTime = System.currentTimeMillis();
            time = (currentTime - startTime) / 1000;
            System.out.println("\nInterim Report @ " + time + "s:");

            int giftsInSacks = 0;
            for (Sack sack :
                    sacks) {
                giftsInSacks += sack.getTotalNumberOfPresents();
            }

            int giftsInHoppers = 0;
            for (Hopper hopper :
                    hoppers) {
                giftsInHoppers += hopper.getNumberOfPresents();
            }

            int giftsInMachine = 0;
            for (Conveyor conveyor :
                    belts) {
                giftsInMachine += conveyor.getNumberOfPresents();
            }

            System.out.println(giftsInHoppers + " presents remaining in hoppers;\n" + giftsInSacks + " presents sorted into sacks;\n" +
                    giftsInMachine + " presents in the machine.");
            System.out.println();

        }

        // Time is up
        // Immediately cease all hoppers from adding presents to the input belts
        for (Hopper hopper :
                hoppers) {
            hopper.setTimerState(Hopper.TIMER_RUN_OUT);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("*** Input Stopped after " + (endTime - startTime) / 1000 + "s. ***");

        // Block the calling (main) thread until both the Hopper
        // and the Turntable threads have terminated.
        blockMainUntillFinished(hoppers);
        blockMainUntillFinished(tables);

        endTime = System.currentTimeMillis();
        System.out.println("*** Machine completed shutdown after " + (endTime - startTime) / 1000 + "s. ***");


        // FINAL REPORTING
        ////////////////////////////////////////////////////////////////////////

        System.out.println();
        System.out.println("\nFINAL REPORT\n");
        System.out.println("Configuration: " + FILE_NAME);
        System.out.println("Total Run Time " + (endTime - startTime) / 1000 + "s.");

        int giftsDeposited = 0;
        for (Hopper hopper :
                hoppers) {
            giftsDeposited += hopper.getNumberOfPresentsDeposited();
        }

        for (int h = 0; h < numHoppers; h++) {
            System.out.println("Hopper " + hoppers[h].getIdentifier() + " deposited " + hoppers[h].getNumberOfPresentsDeposited() +
                    " presents and waited " + hoppers[h].getTotalWaitingTimeInSeconds() + "s.");
        }
        System.out.println();

        int giftsOnMachine = 0;
        int giftsInSacks = 0;
        // Add all the presents inside the sacks
        for (Sack sack :
                sacks) {
            giftsInSacks += sack.getTotalNumberOfPresents();
        }
        // Add the presents that are on the belts
        for (Conveyor belt :
                belts) {
            giftsOnMachine += belt.getNumberOfPresents();
        }

        System.out.print("\nOut of " + giftsDeposited + " gifts deposited, ");
        System.out.print(giftsOnMachine + " are still on the machine, and ");
        System.out.println(giftsInSacks + " made it into the sacks");

        int missing = giftsDeposited - giftsInSacks - giftsOnMachine;
        System.out.println(missing + " gifts went missing.");
    }

    /**
     * Traverse the threads of the given array and call for each its
     * {@link Thread#join()} method.
     * Thus, the calling thread will get blocked till all threads terminate.
     *
     * @param threads An array of threads and its subclasses.
     */
    private static void blockMainUntillFinished(@NotNull Thread[] threads) {
        for (Thread thread :
                threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}