package edu.curtin.saed.assignment1;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class PlaneMovement implements Runnable
{
    private final GridAreaIcon plane;
    private final GridArea area;
    private final GridAreaIcon destination;
    private final double speed;
    private final TextArea textArea;
    private volatile boolean isRunning;

    public PlaneMovement(GridAreaIcon plane, GridAreaIcon destination, GridArea area, double speed, TextArea textArea)
    {
        this.plane = plane;
        this.area = area;
        this.destination = destination;
        this.speed = speed;
        this.textArea = textArea;
        this.isRunning = true;  // Initialize isRunning as true
    }

    @Override
    public void run()
    {
        while (!Thread.currentThread().isInterrupted() && isRunning)
        {
            synchronized (area)
            {

                double deltaX = calculateDelta(plane.getX(), destination.getX());
                double deltaY = calculateDelta(plane.getY(), destination.getY());
                double distance = computeDistance(deltaX, deltaY);

                // If the plane is close enough to the destination, snap to position and stop
                if (distance < 0.5)
                {
                    Platform.runLater(() -> {
                        plane.setPosition(destination.getX(), destination.getY());
                        area.requestLayout();
                        textArea.appendText(plane.getCaption() + " arrived at " + destination.getCaption() + ".\n");
                    });
                    break;
                }

                // Calculate movement step based on speed and normalize the vector
                double[] step = computeStep(deltaX, deltaY, distance);

                // Update plane's position
                double newX = plane.getX() + step[0];
                double newY = plane.getY() + step[1];

                Platform.runLater(() -> {
                    plane.setPosition(newX, newY);
                    area.requestLayout();
                });

                try
                {
                    Thread.sleep(25); // Adjust sleep time for smoother movement
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private double calculateDelta(double start, double end)
    {
        return end - start; // Generic method to calculate the delta between two points
    }

    private double computeDistance(double deltaX, double deltaY)
    {
        return Math.hypot(deltaX, deltaY); // Use hypot for cleaner distance calculation
    }

    private double[] computeStep(double deltaX, double deltaY, double distance)
    {
        // Normalize the vector to get the direction, then scale by speed
        double factor = speed * 0.05 / distance;
        return new double[]{deltaX * factor, deltaY * factor}; // Return step as an array
    }

    public void stop()
    {
        isRunning = false;  // Method to stop the plane's movement
    }
}
