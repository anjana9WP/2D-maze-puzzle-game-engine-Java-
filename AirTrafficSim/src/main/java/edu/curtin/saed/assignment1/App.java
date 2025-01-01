package edu.curtin.saed.assignment1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application
{
    private static final Logger LOGGER = Logger.getLogger(App.class.getName()); // Use Logger in accordance to PMD ruleset
    private static final double SPEED = 10.0; // Speed of planes
    private static final int GRID_SIZE = 10; // Grid size
    private GridArea area;
    private ExecutorService threadPool;
    private volatile boolean isRunning;
    private TextArea textArea;
    private Label planesServicingLabel;
    private Label planesServicedLabel;
    private Label completedTripsLabel;
    private Label planesInFlightLabel;
    private int planesServicing = 0;
    private int planesServiced = 0;
    private int totalCompletedTrips = 0;
    private int planesInFlight = 0;
    private final ConcurrentMap<GridAreaIcon, BlockingQueue<GridAreaIcon>> planeRequests = new ConcurrentHashMap<>();

    public static void main(String[] args)
    {
        launch();
    }

    @Override
    public void start(Stage stage)
    {
        area = new GridArea(GRID_SIZE, GRID_SIZE);
        area.setStyle("-fx-background-color: #006000;");
        setupAirportsAndPlanes();

        Button startBtn = new Button("Start");
        Button endBtn = new Button("End");
        Label statusText = new Label("Air Traffic Simulator");

        planesServicingLabel = new Label("Planes Undergoing Servicing: 0");
        planesServicedLabel = new Label("Planes Finished Servicing: 0");
        completedTripsLabel = new Label("Total Completed Plane Trips: 0"); // New label
        planesInFlightLabel = new Label("Planes In-Flight: 0"); // New label for in-flight planes

        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.appendText("Simulation Initialized.\n");

        startBtn.setOnAction(event -> startSimulation());
        endBtn.setOnAction(event -> endSimulation());

        var toolbar = new ToolBar(startBtn, endBtn, new Separator(), statusText, planesServicingLabel, planesServicedLabel, completedTripsLabel, planesInFlightLabel);
        var splitPane = new SplitPane(area, textArea);
        splitPane.setDividerPositions(0.75);

        stage.setTitle("Air Traffic Simulator");
        var contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);

        var scene = new Scene(contentPane, 1200, 800);
        stage.setScene(scene);
        stage.show();
    }

    private void setupAirportsAndPlanes() {
        Random rand = new Random();

        // Define the number of airports and planes per airport
        int totalAirports = 10;
        int planesPerAirport = 10;

        // Create airports and planes, placing each airport at a unique random position
        for (int airportIndex = 0; airportIndex < totalAirports; airportIndex++) {
            // Generate unique coordinates for the airport
            int airportX = rand.nextInt(GRID_SIZE);
            int airportY = rand.nextInt(GRID_SIZE);

            // Create the airport icon
            GridAreaIcon airportIcon = new GridAreaIcon(airportX, airportY, 0.0, 1.0,
                    App.class.getClassLoader().getResourceAsStream("airport.png"),
                    String.format("Airport %d", airportIndex));
            area.getIcons().add(airportIcon);

            // Generate planes for the current airport and add them to the grid
            createPlanesForAirport(airportIndex, airportX, airportY, planesPerAirport);
        }
    }

    private void createPlanesForAirport(int airportIndex, int airportX, int airportY, int planesPerAirport) {
        for (int planeIndex = 0; planeIndex < planesPerAirport; planeIndex++) {
            // Construct unique ID for each plane based on airport and plane index
            String planeId = String.format("Plane %d", (airportIndex * planesPerAirport) + planeIndex);

            // Create the plane icon at the airport's position
            GridAreaIcon planeIcon = new GridAreaIcon(airportX, airportY, 0.0, 1.0,
                    App.class.getClassLoader().getResourceAsStream("plane.png"),
                    planeId);

            // Add the plane to the grid and set up its request queue
            area.getIcons().add(planeIcon);
            planeRequests.put(planeIcon, new LinkedBlockingQueue<>()); // Initialize with a BlockingQueue
        }
    }

    private void startSimulation()
    {
        if (isRunning) {
            textArea.appendText("Simulation is already running.\n");
            return;
        }

        threadPool = Executors.newCachedThreadPool();
        isRunning = true; // Indicate that the simulation is running
        textArea.appendText("Simulation Started.\n");

        // Launch a flight request process for each airport
        for (int originAirport = 0; originAirport < 10; originAirport++) {
            final int finalOriginAirport = originAirport; // Create a final variable

            try {
                int nAirports = 10; // Number of airports
                Process flightRequestProcess = new ProcessBuilder("saed_flight_requests.bat", String.valueOf(nAirports), String.valueOf(finalOriginAirport))
                        .redirectErrorStream(true)
                        .start();

                // Start a thread to handle flight requests for this airport
                threadPool.submit(() -> {
                    // Use try-with-resources inside the thread to ensure BufferedReader is closed properly
                    try (BufferedReader flightRequestReader = new BufferedReader(new InputStreamReader(flightRequestProcess.getInputStream()))) {
                        handleFlightRequests(flightRequestReader, flightRequestProcess, finalOriginAirport);
                    } catch (IOException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(Level.SEVERE, "Error closing flight request reader for Airport " + finalOriginAirport, e);
                        }
                    }
                });

            } catch (IOException e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Error starting flight request process for Airport " + finalOriginAirport, e);
                }
                textArea.appendText("Error starting flight request process for Airport " + finalOriginAirport + ".\n");
            }
        }

        // Start servicing planes
        for (GridAreaIcon plane : planeRequests.keySet()) {
            threadPool.submit(() -> servicePlane(plane));
        }
    }

    private void endSimulation()
    {
        if (!isRunning) {
            textArea.appendText("Simulation is not running.\n");
            return;
        }

        isRunning = false; // Indicate that the simulation is stopping

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow(); // Force shutdown if not done within 5 seconds
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        textArea.appendText("Simulation Ended.\n");
    }

    private void handleFlightRequests(BufferedReader flightRequestReader, Process flightRequestProcess, int originAirport)
    {
        try {
            String line;
            while (isRunning && (line = flightRequestReader.readLine()) != null) {
                final String lineCopy = line.trim();
                if (lineCopy.isEmpty()) {
                    continue; // Skip empty lines
                }

                Platform.runLater(() -> {
                    try {
                        int destinationAirport = Integer.parseInt(lineCopy);

                        // Validate airport ID
                        if (destinationAirport < 0 || destinationAirport >= 10) {
                            textArea.appendText("Error: Invalid destination airport " + destinationAirport + " from Airport " + originAirport + ".\n");
                            return;
                        }

                        GridAreaIcon destination = getAirportById(destinationAirport);
                        if (destination != null) {
                            GridAreaIcon availablePlane = getAvailablePlane();
                            if (availablePlane != null) {
                                textArea.appendText("Flight request: Plane " + availablePlane.getCaption().split(" ")[1] +
                                        " to Airport " + destinationAirport + ".\n");

                                planesInFlight++; // Increment in-flight planes
                                planesInFlightLabel.setText("Planes In-Flight: " + planesInFlight); // Update in-flight label

                                try {
                                    planeRequests.get(availablePlane).put(destination); // Use put to add to the BlockingQueue
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            } else {
                                textArea.appendText("No available planes to handle request from Airport " + originAirport + " to Airport " + destinationAirport + ".\n");
                            }
                        } else {
                            textArea.appendText("Error: Destination airport " + destinationAirport + " not found.\n");
                        }
                    } catch (NumberFormatException e) {
                        textArea.appendText("Error: Invalid flight request '" + lineCopy + "' from Airport " + originAirport + ".\n");
                    }
                });
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error reading flight requests from Airport " + originAirport, e);
            }
            Platform.runLater(() -> textArea.appendText("Error reading flight requests from Airport " + originAirport + ".\n"));
        } finally {
            // Ensure the process is terminated when the simulation ends
            if (flightRequestProcess.isAlive()) {
                flightRequestProcess.destroy();
            }
        }
    }

    private GridAreaIcon getAvailablePlane()
    {
        synchronized (planeRequests) { // Avoid method-level synchronized
            for (GridAreaIcon plane : planeRequests.keySet()) {
                if (planeRequests.get(plane).isEmpty()) {
                    return plane;
                }
            }
        }
        return null;
    }

    private GridAreaIcon getAirportById(int id)
    {
        synchronized (area.getIcons()) { // Avoid method-level synchronized
            for (GridAreaIcon icon : area.getIcons()) {
                if (icon.getCaption().equals("Airport " + id)) {
                    return icon;
                }
            }
        }
        return null;
    }

    private void servicePlane(GridAreaIcon plane)
    {
        while (isRunning && !Thread.currentThread().isInterrupted())
        {
            GridAreaIcon destination;

            try {
                destination = planeRequests.get(plane).take(); // Use take to retrieve from the BlockingQueue
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (destination != null) {
                final GridAreaIcon finalDestination = destination; // Make the variable effectively final
                PlaneMovement movement = new PlaneMovement(plane, finalDestination, area, SPEED, textArea);
                Thread planeThread = new Thread(movement);
                planeThread.start();

                try {
                    planeThread.join(); // Wait until the plane reaches its destination
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (isRunning) { // Check if the simulation is still running before servicing
                    Platform.runLater(() -> {
                        // After landing, ensure the plane is set to the correct position
                        plane.setPosition(finalDestination.getX(), finalDestination.getY());
                        area.requestLayout();

                        // Re-add the plane to the grid area if it was somehow removed
                        if (!area.getIcons().contains(plane)) {
                            area.getIcons().add(plane);
                        }

                        // Decrement in-flight planes count when landing
                        planesInFlight--;
                        planesInFlightLabel.setText("Planes In-Flight: " + planesInFlight); // Update in-flight label

                        // Start the plane servicing process
                        startPlaneServicing(finalDestination, plane);
                    });
                }
            }

            try {
                Thread.sleep(500); // Small delay to avoid overloading the system
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void startPlaneServicing(GridAreaIcon airport, GridAreaIcon plane) {
        // Check if the thread pool is not shut down before submitting a task
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.submit(() -> {
                Process serviceProcess = null;
                try {
                    serviceProcess = new ProcessBuilder(
                            "saed_plane_service.bat",
                            airport.getCaption().split(" ")[1],
                            plane.getCaption().split(" ")[1])
                            .redirectErrorStream(true)
                            .start();

                    // Update the servicing count
                    Platform.runLater(() -> {
                        planesServicing++;
                        planesServicingLabel.setText("Planes Undergoing Servicing: " + planesServicing);
                    });

                    try (BufferedReader serviceReader = new BufferedReader(new InputStreamReader(serviceProcess.getInputStream()))) {
                        String output;
                        while ((output = serviceReader.readLine()) != null) {
                            final String finalOutput = output;
                            Platform.runLater(() -> textArea.appendText(finalOutput + "\n"));
                        }
                    }

                    serviceProcess.waitFor(); // Wait for the process to finish

                    if (isRunning) { // Check if the simulation is still running before updating the UI
                        // Update the counts after servicing
                        Platform.runLater(() -> {
                            planesServicing--;
                            planesServiced++;
                            planesServicingLabel.setText("Planes Undergoing Servicing: " + planesServicing);
                            planesServicedLabel.setText("Planes Finished Servicing: " + planesServiced);

                            totalCompletedTrips++; // Increment total completed trips
                            completedTripsLabel.setText("Total Completed Plane Trips: " + totalCompletedTrips); // Update completed trips label

                            textArea.appendText("Plane " + plane.getCaption().split(" ")[1] + " has completed servicing at " + airport.getCaption() + ".\n");
                        });
                    }

                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during plane servicing", e); // Replacing printStackTrace
                } catch (InterruptedException e) {
                    if (serviceProcess != null) {
                        serviceProcess.destroy();
                    }
                    Thread.currentThread().interrupt();  // Restore the interrupted status
                    Platform.runLater(() -> textArea.appendText("Servicing interrupted for Plane " + plane.getCaption().split(" ")[1] + ".\n"));
                } finally {
                    if (serviceProcess != null && serviceProcess.isAlive()) {
                        serviceProcess.destroy();  // Ensure the process is destroyed
                    }
                }
            });
        }
    }
}
