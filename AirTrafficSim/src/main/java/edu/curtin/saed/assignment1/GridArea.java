package edu.curtin.saed.assignment1;

import javafx.geometry.VPos;
import javafx.scene.canvas.*;
import javafx.scene.transform.Affine;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import java.util.*;

public class GridArea extends Pane
{
    private double gridWidth;
    private double gridHeight;
    private double gridSquareSize = 1.0;
    private boolean gridLines = true;
    private Color captionColour = Color.WHITE;
    private List<GridAreaIcon> icons = new ArrayList<>();
    private Canvas canvas = null;

    public GridArea(double gridWidth, double gridHeight)
    {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    public void setGridLines(boolean gridLines)
    {
        this.gridLines = gridLines;
    }

    public List<GridAreaIcon> getIcons()
    {
        return icons;
    }

    public void setCaptionColour(Color captionColour)
    {
        this.captionColour = captionColour;
    }

    @Override
    public void layoutChildren()
    {
        super.layoutChildren();
        if(canvas == null)
        {
            canvas = new Canvas();
            canvas.widthProperty().bind(widthProperty());
            canvas.heightProperty().bind(heightProperty());
            getChildren().add(canvas);
        }

        GraphicsContext gfx = canvas.getGraphicsContext2D();
        gfx.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());

        gridSquareSize = Math.min(getWidth() / gridWidth, getHeight() / gridHeight);

        if(gridLines)
        {
            gfx.setStroke(Color.DARKGREY);
            for(double gridX = 0.0; gridX < gridWidth; gridX++)
            {
                double x = (gridX + 0.5) * gridSquareSize;
                gfx.strokeLine(x, gridSquareSize / 2.0, x, (gridHeight - 0.5) * gridSquareSize);
            }
            for(double gridY = 0.0; gridY < gridHeight; gridY++)
            {
                double y = (gridY + 0.5) * gridSquareSize;
                gfx.strokeLine(gridSquareSize / 2.0, y, (gridWidth - 0.5) * gridSquareSize, y);
            }
        }

        for(var icon : icons)
        {
            if(icon.isShown())
            {
                drawIcon(gfx, icon);
            }
        }
    }

    private void drawIcon(GraphicsContext gfx, GridAreaIcon icon)
    {
        double x = (icon.getX() + 0.5) * gridSquareSize;
        double y = (icon.getY() + 0.5) * gridSquareSize;

        var image = icon.getImage();
        double fullSizePixelWidth = image.getWidth();
        double fullSizePixelHeight = image.getHeight();

        double displayedPixelWidth, displayedPixelHeight;
        if(fullSizePixelWidth > fullSizePixelHeight)
        {
            displayedPixelWidth = gridSquareSize;
            displayedPixelHeight = gridSquareSize * fullSizePixelHeight / fullSizePixelWidth;
        }
        else
        {
            displayedPixelHeight = gridSquareSize;
            displayedPixelWidth = gridSquareSize * fullSizePixelWidth / fullSizePixelHeight;
        }

        gfx.save();
        gfx.translate(x, y);
        gfx.rotate(icon.getRotation());
        gfx.drawImage(image, -displayedPixelWidth / 2.0, -displayedPixelHeight / 2.0, displayedPixelWidth, displayedPixelHeight);
        gfx.restore();

        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setTextBaseline(VPos.TOP);
        gfx.setStroke(captionColour);
        gfx.strokeText(icon.getCaption(), x, y + gridSquareSize * 0.35);
    }
}
