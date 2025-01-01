package edu.curtin.saed.assignment1;

import javafx.scene.image.Image;
import java.io.InputStream;

public class GridAreaIcon
{
    private double x;
    private double y;
    private double rotation;
    private double scale;
    private Image image;
    private String caption;
    private boolean shown = true;

    public GridAreaIcon(double x, double y, double rotation, double scale, InputStream imageStream, String caption)
    {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.scale = scale;
        this.image = new Image(imageStream);
        this.caption = caption;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRotation() { return rotation; }
    public double getScale() { return scale; }
    public Image getImage() { return image; }
    public String getCaption() { return caption; }
    public boolean isShown() { return shown; }

    public void setPosition(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public void setRotation(double rotation)
    {
        this.rotation = rotation;
    }

    public void setShown(boolean shown)
    {
        this.shown = shown;
    }
}
