package principal;

import javafx.animation.Transition;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import java.nio.ByteBuffer;

/**
 * Renderer for the falling sand particle simulation.
 * <p>
 * This class handles the visualization of the particle simulation, including
 * rendering particles to the screen, managing the color palette, and processing
 * user interactions. It uses a block-based rendering approach where each cell
 * in the simulation is rendered as a 4x4 pixel block for better performance.
 * </p>
 * <p>
 * The renderer maintains two buffers for double buffering - one for the current
 * state and one for the next state. It also manages the palette of colors used
 * for rendering different particle types.
 * </p>
 * 
 * @see Sierpinski
 * @see Transition
 */
class Renderer {
	private static final int PALETTE_SIZE = 256;

	private int pixelSize;
	private final GraphicsContext gc;
	private final int width;
	private final int height;
	private final int cellWidth;
	private final int cellHeight;
	// private final Color[] palette = new Color[PALETTE_SIZE];
	private final WritableImage estadosImage;
	private final PixelWriter pixelWriter;
	private int[][] cellData; // generation buffer
	private int mouseX = 0;
	private int mouseY = 0;
	private boolean mouseInside = false;
	private int frameCounter = 0;
	private final Palette palette = new Palette();
	private byte[] blockBuffer; // BGRA format for 4x4 block

	private Point2D[] atracPoints;
	private Point2D actualPoint;
	public int actractorNumberOfPoints = 3;

	/**
	 * Internal palette class for managing the colors used in the simulation.
	 * <p>
	 * The palette contains 256 colors where:
	 * <ul>
	 * <li>Index 0: White (background)</li>
	 * <li>Indices 1-255: Random grayscale values</li>
	 * </ul>
	 * </p>
	 */
	private static class Palette {
		final byte[][] colors = new byte[PALETTE_SIZE][4];

		/**
		 * Constructs a new palette with background color and random
		 */
		public Palette() {

			colors[0][0] = (byte) 150;
			colors[0][1] = (byte) 150;
			colors[0][2] = (byte) 150;
			colors[0][3] = (byte) 255;

			for (int a = 1; a < PALETTE_SIZE; a++) {
				colors[a][0] = (byte) (80 + (Math.random() * 150));
				colors[a][1] = (byte) (80 + (Math.random() * 150));
				colors[a][2] = (byte) (80 + (Math.random() * 150));
				colors[a][3] = (byte) 255;
			}

		}

	}

	/**
	 * Creates a new plasma renderer and generates the initial fractal pattern.
	 *
	 * @param gc the graphics context to draw on
	 * @param w  canvas width in pixels
	 * @param h  canvas height in pixels
	 * @throws IllegalArgumentException if gc is null or dimensions are invalid
	 */
	public Renderer(GraphicsContext gc, int w, int h, int pixelSize) {
		if (gc == null) {
			throw new IllegalArgumentException("GraphicsContext cannot be null");
		}
		if (w <= 0 || h <= 0) {
			throw new IllegalArgumentException("Width and height must be positive");
		}

		this.gc = gc;
		this.width = w;
		this.height = h;
		this.pixelSize = pixelSize;
		this.blockBuffer = new byte[pixelSize * pixelSize * 4];
		this.cellWidth = width / pixelSize;
		this.cellHeight = height / pixelSize;

		// Create writable image and pixel writer for efficient drawing
		estadosImage = new WritableImage(width, height);
		pixelWriter = estadosImage.getPixelWriter();
		cellData = new int[cellHeight][cellWidth];

		initialize();
	}

	/**
	 * Performs initial drawing operations for the simulation.
	 * <p>
	 * This method is called during initialization to set up the initial state
	 * of the simulation. Currently it just prints a message to the console.
	 * </p>
	 */
	public void initialDrawing() {

		// initialColors();
		System.out.println("Initial Drawing");
		Thread.yield();
	}

	/**
	 * Initializes the renderer by clearing the data arrays and drawing the initial
	 * image.
	 */
	private void initialize() {
		// Clear matrix (all cells to 0)
		cellData = new int[cellHeight][cellWidth];

		/*
		 * atracPoints[0] = new Point2D(cellWidth / 2, 0);
		 * atracPoints[1] = new Point2D(0, cellHeight);
		 * atracPoints[2] = new Point2D(cellWidth, cellHeight);
		 */
		atracPoints = new Point2D[actractorNumberOfPoints];

		for (int i = 0; i < atracPoints.length; i++) {
			double angle = (2 * Math.PI * i / atracPoints.length) - Math.PI / 2;
			double x = cellWidth / 2 + cellWidth / 2.2 * Math.cos(angle);
			double y = cellHeight / 2 + cellHeight / 2.2 * Math.sin(angle);
			atracPoints[i] = new Point2D(x, y);
		}

		actualPoint = new Point2D(cellWidth / 2, cellHeight / 2);

		initialDrawing();
		updateImageFromPalette();
		gc.drawImage(estadosImage, 0, 0);
		System.out.println("Image Drawn");
	}

	/**
	 * Updates for one frame (called at 60 FPS). Processes pixel and redraws the
	 * canvas.
	 */
	public void updateRegion() {
		// Create point from mouse only every two frames
		/*
		 * if (mouseInside && frameCounter % 2 == 0) {
		 * int cellX = mouseX / PIXEL_SIZE;
		 * int cellY = mouseY / PIXEL_SIZE;
		 * // Check boundaries before assignment
		 * if (cellX >= 0 && cellX < cellWidth && cellY >= 0 && cellY < cellHeight &&
		 * cellData[cellY][cellX] == 0) {
		 * int actualColor = (frameCounter / 500) % 254 + 1;
		 * cellData[cellY][cellX] = actualColor;
		 * }
		 * }
		 */
		frameCounter++;

		iteration();
		gc.drawImage(estadosImage, 0, 0);
	}

	/**
	 * Performs one iteration of the simulation.
	 * <p>
	 * This method updates the state of all particles in the simulation based on
	 * the physics rules defined in the Transition class. It handles both regular
	 * particle movement and hit forces applied by the user.
	 * </p>
	 */
	public void iteration() {

		int nPoints = atracPoints.length;
		int[] colors = new int[nPoints];
		colors[0] = (frameCounter / 500) % 254 + 1;
		for (int i = 1; i < nPoints; i++) {
			colors[i] = (colors[i - 1] + 1) % 254 + 1;
		}

		double ratio = switch (nPoints) {
			case 3 -> 0.5d;
			default -> .5d+(nPoints/60d);
		};

		for (int a = 0; a < 50; a++) {
			// select point form atractor
			int random = (int) (Math.random() * nPoints);
			Point2D actractPoint = atracPoints[random];
			// calculate the middle point

			//actualPoint = actualPoint.midpoint(actractPoint);		
			double newDeltaX=((actractPoint.getX() - actualPoint.getX()) * ratio);
			double newDeltaY=((actractPoint.getY() - actualPoint.getY()) * ratio);
			actualPoint=actualPoint.add(newDeltaX,newDeltaY);
			cellData[(int) actualPoint.getY()][(int) actualPoint.getX()] = colors[random];
		}

		updateImageFromPalette();
	}

	/**
	 * Updates the image from the palette data.
	 * <p>
	 * This method converts the cell data into pixel data for display. Each cell
	 * is rendered as a 4x4 pixel block using the color from the palette. The
	 * method uses block-based rendering for better performance.
	 * </p>
	 */
	private void updateImageFromPalette() {
		WritablePixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraInstance();

		for (int cy = 0; cy < cellHeight; cy++) {
			for (int cx = 0; cx < cellWidth; cx++) {

				// Fill the entire 4x4 block (16 pixels) with the same color
				int color = cellData[cy][cx];
				byte b = palette.colors[color][0]; // B
				byte g = palette.colors[color][1]; // G
				byte r = palette.colors[color][2]; // R
				byte a = palette.colors[color][3]; // A

				for (int i = 0; i < pixelSize * pixelSize; i++) {
					int pos = i * 4;
					blockBuffer[pos] = b; // B
					blockBuffer[pos + 1] = g; // G
					blockBuffer[pos + 2] = r; // R
					blockBuffer[pos + 3] = a; // A
				}

				// Write 4x4 block at once
				int screenX = cx * pixelSize;
				int screenY = cy * pixelSize;
				if (screenX + pixelSize <= width && screenY + pixelSize <= height) {
					pixelWriter.setPixels(screenX, screenY, pixelSize, pixelSize, pixelFormat, blockBuffer, 0,
							pixelSize * 4);
				}
			}
		}
	}

	/**
	 * Updates mouse position for interactive plasma effects.
	 *
	 * @param x      mouse X coordinate
	 * @param y      mouse Y coordinate
	 * @param inside true if mouse is inside the canvas
	 */
	public void setMousePosition(int x, int y, boolean inside) {
		this.mouseX = x;
		this.mouseY = y;
		this.mouseInside = inside;
	}

	/**
	 * Regenerates the plasma fractal pattern without recreating the renderer.
	 * Reuses existing WritableImage and draws a new fractal pattern.
	 */
	public void restart() {
		initialize();
	}
}
