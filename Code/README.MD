# Java Simulation of a Christmassy Concurrency Conundrum

## Overview
This Java program simulates the operation of a Present sorting machine. It reads configuration data from an external file, creates and operates hoppers, belts, turntables, and sacks, and provides detailed reports on the machine's status at regular intervals.

## Key Components
1. **Configuration File:** The program reads configuration data from an external file specified by `FILE_NAME`. This file defines the machine's setup, including the number of belts, hoppers, sacks, turntables, and other details.

2. **Hoppers, Belts, Sacks, and Turntables:** The program creates and manages these key components:
   - **Hoppers:** Receive and store presents, each with specific capacity and speed.
   - **Belts:** Transport presents between components, with adjustable lengths and destinations.
   - **Sacks:** Collect presents based on age categories.
   - **Turntables:** Direct presents between components with various connections (e.g., belts and sacks).

3. **Filling Hoppers:** The hoppers are filled with presents according to the configuration data from the file. Each hopper is loaded with presents.

4. **Multi-Threading:** The program starts the hoppers and turntables as separate threads, allowing concurrent execution of these components.

5. **Status Reporting:** At regular 10-second intervals, the program reports on the machine's status. It tracks the number of presents in hoppers, sacks, and belts, providing an overview of the machine's progress.

6. **Timer Control:** The simulation runs for a specified duration, controlled by a timer length defined in the configuration file.

7. **Shutdown:** After the timer expires, the program stops the hoppers from adding more presents to the machine.

8. **Final Summary:** The program generates a final report that includes various statistics, such as presents deposited, presents in sacks, presents remaining in the machine, and any missing presents.

## Usage
To run the program, compile the Java source code and execute the `MyClass` class. Ensure that the configuration file (specified in `FILE_NAME`) is available and correctly formatted to define the machine's setup.

## Example
For an example usage of the program and a complete code walkthrough, refer to the [`MyClass.java`](MyClass.java) source code.

## License
This software is provided under an open-source license. Please review the [LICENSE](LICENSE) file for more details.

If you have specific questions or need further assistance with any part of the program, please feel free to ask.
