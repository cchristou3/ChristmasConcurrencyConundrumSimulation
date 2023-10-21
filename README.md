# CO3401 Advanced Software Engineering Techniques – Coursework (Part 1)

## Title: Java Simulation of a Christmassy Concurrency Conundrum

This repository contains the written answers to the questions in the Coursework (Part 1) of the CO3401 Advanced Software Engineering Techniques module. The questions and detailed answers are provided below.

## Main Program Structure

The main program outlined structure is as follows:

1. **Read from a Configuration File:**
   - Read from a configuration file to create the configuration of Hoppers, Belts, Turntables, and Sacks.

2. **Initialization:**
   - Initialize the system by filling the hoppers with Presents according to the configuration file.
   - Call the run methods on the threaded objects to start the simulation.

3. **Simulation:**
   - Run the simulation for the specified duration.
   - Output reports every 10 seconds to monitor the system's progress.
   - At the appropriate time, instigate the shutdown of the machine.

4. **Output Final Report:**
   - After the simulation completes, output the final report summarizing the system's performance.

## Author
This coursework is prepared by an anonymous author.

---

## Question 1

### Hopper
The hopper is responsible for distributing presents in the system. Here's how it operates:

- The hopper checks its current size in terms of presents and the timer's state.
- If the hopper has no more presents to distribute or the timer has run out, it moves to its clean-up phase.
- Otherwise, it acquires the appropriate resources and places a single present onto the output belt.
- Once the present is placed, the hopper unlocks the resources.
- The hopper then waits for the next interval to continue the process.
During its clean-up phase, it sets the mWaitingForPresents flag of the Conveyor to false, signaling that it will no longer receive presents. It releases all locks related to that belt before shutting down.

### Turntable
The turntable's operation is as follows:

- It checks the number of recorded input conveyor belts.
- If there are no more input belts, it transitions to its clean-up stage.
- Otherwise, it selects the input belt with the highest priority.
- It acquires the appropriate resources.
- Once the resources are obtained, the turntable interrogates the closest present from the input belt.
- The present is placed at its next destination, and the turntable's belts are validated and updated.
- Input belts with their mWaitingForPresents flag set to false and empty are removed from the turntable.
- The turntable updates the mWaitingForPresents flag of its output belts based on the validity of its input belts.
- If the turntable's input belts are no longer working, it implies that the output belt will no longer receive any more presents.
- After validation, the appropriate resources are released.
- If the present's destination involves an output conveyor belt, the turntable acts as a hopper, acquires the necessary resources, adds the present to the belt, and releases them.
- The turntable keeps iterating until it has no more input belts.

During its clean-up stage, the turntable updates its output belts' mWaitingForPresents flag to false and releases appropriate resources to allow proceeding turntables to continue working.

### Stopping all the threads and finishing gracefully at the end of the simulation
When the timer runs out, all hoppers are set to the corresponding state (TIMER_RUN_OUT). This triggers the hoppers to finish placing the current present and transition to their clean-up phases. As a result, all turntables will continue directing presents on the machine until they steadily shut down one by one, starting from the ones nearest to the hoppers.

Based on the program's logic, the system will continue working until no more presents are left for all conveyor belts.


## Potential Issues

### Deadlock
A deadlock occurs when two or more threads are trying to access resources that have already been acquired by other threads. In this situation, each thread is waiting for a resource that is held by another thread, leading to a standstill where no progress is made. However, in the current system and with sample scenarios, the occurrence of deadlocks is zero. This is because the code has been designed to handle the de-allocation of allocated resources independently. For example, the turntable's isCurrentInputBeltValid method releases previously acquired resources of the current input belt before removing it from the turntable. Additionally, advanced scenarios like try-catch blocks are handled carefully to ensure appropriate resource de-allocation. The hopper's clean-up phase also releases the right resources to avoid leaving proceeding turntables stuck in a waiting state.

### Livelock
Livelock is another concurrency issue similar to deadlock. In a livelock, threads are actively trying to resolve the situation, but their states keep changing without making any progress. This can happen when two or more threads require multiple resources and keep releasing one resource to obtain the other, resulting in an endless loop. In the current implementation, the occurrence of livelock is zero. Livelock may only occur if the code includes countermeasures to release locks in case of an event, which leads to the repeated acquisition and release of resources. This has not been implemented in the current code.

## Console Output and Scenarios
The repository also includes console output from different scenarios (scenario1.txt, scenario2.txt, scenario3.txt, scenario4.txt, and scenario5.txt) demonstrating the behavior of the system in various situations. These scenarios provide insights into how the system handles different input conditions and the resulting behavior.

Please refer to the specific scenario files for the detailed console output and analysis of each scenario.

You can watch a demonstration of the coursework [here](https://youtu.be/3TzfokdHSVc).

---

**Note:** This README file provides an overview of the written answers and links to the scenario outputs. Detailed code or source files are not included in this repository, as it primarily focuses on the written responses to the questions and scenario results.
