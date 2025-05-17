Firefighting Drones Simulation

Overview

    This project simulates firefighting drones responding to fire incidents using multithreading. The simulation involves three main subsystems:

    DroneSubsystem: Simulates individual drones that navigate to fire zones, drop water to combat fires, and then return to base to refill.

    FireIncidentSubSystem: Reads and processes fire incident events from a CSV file and loads zone information from another CSV file. It sends fire event notifications to the Scheduler based on event timestamps.

    Scheduler: Acts as the mediator between the drones and fire incidents. It maintains queues of fire events and available drones, assigns tasks to drones, and manages drone statuses.


File Descriptions

    Main.java
    The entry point of the application. It initializes and starts the threads for the Scheduler, FireIncidentSubSystem, and one DroneSubsystem instance.

    DroneSubsystem.java
    Implements a simulation of a single firefighting drone. The drone goes through different states—Idle, En Route, Dropping Agent, Refilling—and uses delays (scaled by TIME_SCALE) to simulate travel, water dropping, and refilling. Once a task is completed, the drone notifies the Scheduler.

    FireIncidentSubSystem.java
    Reads fire incident events and zone definitions from CSV files. It processes fire events sequentially, delaying execution to simulate real-time event occurrences based on timestamps, and then sends these events to the Scheduler.

    Scheduler.java
    Manages the queues of incoming fire events and available drones. It assigns a drone to a fire event when one is available. After a drone completes its task, the Scheduler resets the drone’s status and makes it available for future tasks.

    Zone.java
    Represents a geographical zone with a start and end coordinate. It calculates the center of the zone, which is used to determine travel times for drones.

    FireIncidentEvent.java
    Represents a fire incident event. It includes details such as the event time, zone ID, event type, severity, water needed for the incident, and the corresponding zone object.

    DroneStateMachine.java
    A set of classes that manages and handles the DroneSubsystem states, events, activities, conditions and state change sequences

    SchedulerState.java
    A set of classes that manages and handles the Scheduler states, events, activities, conditions and state change sequences

    Sample_zone_file.csv
    This file contains zone information.
    Expected Format:
    ZoneID,StartCoordinate,EndCoordinate
    1,(x1;y1),(x2;y2)

    Sample_event_file.csv
    This file contains fire incident events.
    Expected Format:
    Time,ZoneID,EventType,Severity
    HH:mm:ss,1,Type,Severity

    DroneSubsystemTest.java
    A set of unit tests for DroneSubsystem.java

    DroneStateMachineTest.java
    A set of unit tests for DroneStateMachineTest.java

    FireIncidentEventTest.java
    A set of unit tests for FireIncidentEvent.java

    FireIncidentSubsystemTest.java
    A set of unit tests for FireIncidentSubsystem.java

    SchedulerTest.java
    A set of unit tests for Scheduler.java

    ZoneTest.java
    A set of unit tests for Zone.java


Setup Instructions

    Ensure you have Java JDK installed and an IDE such as IntelliJ IDEA.
    Ensure all java files are located in src\main\java directory.
    Ensure the event and zone CSV files are located in src\main\resources
    Ensure CSV files are in format outlined in File Descriptions.
    Edit DroneSubsystem.java run configuration to allow for multiple instances

    1. Open project in an IDE such as IntelliJ IDEA
    2. Ensure src\main\java directory is marked as Source Root directory
    3. Run the Scheduler.java class
    4. Run as many instances of DroneSubsystem.java class as you need. (Ex. 5 instances for 5 drones)
    5. Run the FireIncidentSubSystem.java class


Test Instructions

    Follow Setup Instructions
    Additionally,
    Ensure all test files are located in src\test\java

    1. Open project in an IDE such as IntelliJ IDEA
    2. Ensure src\tests\java directory is marked as Test Resources Root directory
    3. Right-click src\tests\java directory and select "Run all tests"


Breakdown of Responsibilities
    List of team members and their contributions to Iteration 1:
    Akul Ghai
        - Wrote Scheduler class code
        - Synchronized Scheduler and DroneSubsystem class
        - Debugged final draft of code
    Ben Shvetz
        - Wrote DroneSubsystem class code
        - Wrote Zone class code
        - Wrote README.txt
    Jawad Mohammed
        - Wrote Tests related to FireIncident
        - Assisted with general system design
    Daniel Kuchanski
        - Class Diagram and Sequence Diagram UML
        - Initial FireIncidentEvent class code
        - Helped write Scheduler code
    Jonas Hallgrimsson
        - Wrote FireIncidentSubSystem Class
        - Completed FireIncidentEvent Class
        - Wrote FireIncidentEvent Test Class
    Shahrik Amin
        - Wrote DroneSubsystem class code
        - Helped write Zone class code
        - Wrote DroneSubsystem Test class code

    List of team members and their contributions to Iteration 2:
    Akul Ghai
        - Worked on new Scheduler logic, Scheduler state machine
        - Debugging Scheduler and Drone connections
        - Updated on Scheduler state machine diagram
    Ben Shvetz
        - Worked on Drone state machine diagram
        - Worked on DroneStateMachine.java and updating DroneSubsystem
        - Updated readme and javadocs
        - Worked on SchedulerStateTest and updated SchedulerTest
    Jawad Mohammed
        - Sequence Diagrams
        - Small edits in readme
    Daniel Kuchanski
        - Worked on Scheduler state machine diagram
        - Worked on Scheduler state machine
    Jonas Hallgrimsson
        - Worked on Drone state machine diagram
        - Worked on DroneStateMachine.java and updating DroneSubsystem
        - Updated DroneSubSystemTest
        - Created DroneStateMachineTest
    Shahrik Amin
        - Worked on Drone state machine diagram
        - Worked on DroneStateMachine.java and updating DroneSubsystem
        - Worked on Class Diagram
        - Updated readme and javadocs

List of team members and their contributions to Iteration 3:
    Akul Ghai
        - 
    Ben Shvetz
        - Worked on UDP implementation for DroneSubsystem and DroneStateMachine classes
        - Updated drones test files
        - Updated readme
    Jawad Mohammed
        - 
    Daniel Kuchanski
         - Worked on Scheduler state machine diagram
         - Worked on Scheduler state machine
         - Created initial Scheduler version and helper methods
         - Created SchedulerTest.java
    Jonas Hallgrimsson
        - Updated FireIncidentSubSystem to UDP/RPC calls.
        - Updated FireIncidnetSubSystemTest
        - Updated FireIncidentEvent
    Shahrik Amin
        - 

List of team members and their contributions to Iteration 4:
    Akul Ghai
        - Worked on Scheduler to receive and simulate faults in coordination with drone.
        - Worked on scheduler to implement 2 threads, one for receiving fire events and one for drone scheduling.
    Ben Shvetz
        - Worked on handling of faults in DroneSubsystem and DroneStateMachine
        - Worked on new state machine diagram for drone
    Jawad Mohammed
        - FireIncident Planning
        - Fire Incident Tests
        - Timing Diagram
    Daniel Kuchanski
         - Worked on Scheduler to receive and simulate faults in coordination with drone.
         - Worked on updated scheduler state machine diagram
    Jonas Hallgrimsson
        - Updated FireIncidentSubSystem to send new faults.
        - Updated FireIncidnetEvent to send new faults.
        - Updated event_log to add new faults.
    Shahrik Amin
        - Worked on handling of faults in DroneSubsystem and DroneStateMachine
        - Worked on new state machine diagram for drone

List of team members and their contributions to Iteration 5:
    Akul Ghai
        - Scheduler fixes
    Ben Shvetz
        - Metrics and drone fixes
    Jawad Mohammed
        - GUI
    Daniel Kuchanski
         - Scheduler fixes
    Jonas Hallgrimsson
        - GUI
    Shahrik Amin
        - Metrics and drone fixes
