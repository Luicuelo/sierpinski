package main;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import java.nio.ByteBuffer;

/**
 * Renderer for the Sierpinski triangle fractal visualization.
 * <p>
 * This class handles the visualization of the Sierpinski triangle fractal using
 * the Chaos Game algorithm. It manages the rendering of points to the screen,
 * color palette management, and processing of user interactions. The renderer
 * uses a pixel-based approach where each point in the fractal is rendered as
 * a colored pixel.
 * </p>
 * <p>
 * The Chaos Game algorithm works by:
 * <ul>
 * <li>Starting with a set of attractor points (vertices of a polygon)</li>
 * <li>Beginning with a random point inside the polygon</li>
 * <li>Repeatedly selecting a random attractor and moving partway toward it</li>
 * <li>Plotting each new position to reveal the fractal pattern</li>
 * </ul>
 * </p>
 * 
 * @see SierpinskiTriangle
 */
class FractalRenderer {
	private static final int PALETTE_SIZE = 256;

	private int pixelSize;
	private final GraphicsContext gc;
	private final int width;
	private final int height;
	private final int cellWidth;
	private final int cellHeight;
	private final WritableImage fractalImage;
	private final PixelWriter pixelWriter;
	private int[][] cellData;
	private int mouseX = 0;
	private int mouseY = 0;
	private boolean mouseInside = false;
	private int frameCounter = 0;
	private final ColorPalette palette = new ColorPalette();
	private byte[] blockBuffer;

	private Point2D[] attractorPoints;
	private Point2D currentPoint;
	public int attractorNumberOfPoints = 3;
	public int maximalDistanceAllowed = 0;
	public boolean doNotAllowRepeatPoint = false;
	public boolean addCenterAsAttractor = false;
	private int lastPointIndex;

	/**
	 * Internal color palette class for managing the colors used in the fractal visualization.
	 * <p>
	 * The palette contains 256 colors where:
	 * <ul>
	 * <li>Index 0: Dark gray (background)</li>
	 * <li>Indices 1-255: Random bright colors for fractal points</li>
	 * </ul>
	 * </p>
	 */
	private static class ColorPalette {
		final byte[][] colors = new byte[PALETTE_SIZE][4];

		/**
		 * Constructs a new palette with background color and random colors for fractal points.
		 */
		public ColorPalette() {
			// Background color (dark gray)
			colors[0][0] = (byte) 80;  // B
			colors[0][1] = (byte) 80;  // G
			colors[0][2] = (byte) 80;  // R
			colors[0][3] = (byte) 255; // A

			// Generate random bright colors for fractal points
			for (int i = 1; i < PALETTE_SIZE; i++) {
				colors[i][0] = (byte) (80 + (Math.random() * 150));  // B
				colors[i][1] = (byte) (80 + (Math.random() * 150));  // G
				colors[i][2] = (byte) (80 + (Math.random() * 150));  // R
				colors[i][3] = (byte) 255;                           // A
			}
		}
	}

	/**
	 * Creates a new fractal renderer and initializes the fractal generation.
	 *
	 * @param gc        the graphics context to draw on
	 * @param w         canvas width in pixels
	 * @param h         canvas height in pixels
	 * @param pixelSize size of each rendered pixel (typically 1)
	 * @throws IllegalArgumentException if gc is null or dimensions are invalid
	 */
	public FractalRenderer(GraphicsContext gc, int w, int h, int pixelSize) {
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
		fractalImage = new WritableImage(width, height);
		pixelWriter = fractalImage.getPixelWriter();
		cellData = new int[cellHeight][cellWidth];

		initialize();
	}

	/**
	 * Performs initial setup for the fractal generation.
	 */
	public void initialDrawing() {
		System.out.println("Initializing Sierpinski Triangle fractal generation");
		Thread.yield();
	}

	/**
	 * Initializes the renderer by clearing the data arrays and setting up attractor points.
	 */
	private void initialize() {
		// Clear matrix (all cells to background color)
		cellData = new int[cellHeight][cellWidth];

		// Create attractor points in a regular polygon configuration
		attractorPoints = new Point2D[attractorNumberOfPoints + (addCenterAsAttractor ? 1 : 0)];

		int i;
		for (i = 0; i < attractorNumberOfPoints; i++) {
			double angle = (2 * Math.PI * i / attractorNumberOfPoints) - Math.PI / 2;
			double x = cellWidth / 2 + cellWidth / 2.2 * Math.cos(angle);
			double y = cellHeight / 2 + cellHeight / 2.2 * Math.sin(angle);
			attractorPoints[i] = new Point2D(x, y);
		}
		
		// Add center point as attractor if enabled
		if (addCenterAsAttractor) {
			attractorPoints[i] = new Point2D(cellWidth / 2, cellHeight / 2);
		}
		
		// Start at the center of the canvas
		currentPoint = new Point2D(cellWidth / 2, cellHeight / 2);

		initialDrawing();
		updateImageFromPalette();
		gc.drawImage(fractalImage, 0, 0);
		System.out.println("Fractal image initialized and drawn");
	}

	/**
	 * Updates the fractal for one frame (called at 60 FPS). Generates new points
	 * using the Chaos Game algorithm and redraws the canvas.
	 */
	public void updateRegion() {
		frameCounter++;
		chaosGameIteration();
		gc.drawImage(fractalImage, 0, 0);
	}

	/**
	 * Performs one iteration of the Chaos Game algorithm.
	 * <p>
	 * This method generates multiple new points in the fractal by:
	 * <ol>
	 * <li>Randomly selecting an attractor point</li>
	 * <li>Applying distance restrictions if configured</li>
	 * <li>Moving the current point partway toward the selected attractor</li>
	 * <li>Plotting the new point with a color based on the selected attractor</li>
	 * </ol>
	 * </p>
	 */
	public void chaosGameIteration() {
		int numPoints = attractorPoints.length;
		int[] colors = new int[numPoints];
		
		// Generate colors for each attractor
		colors[0] = (frameCounter / 500) % 254 + 1;
		for (int i = 1; i < numPoints; i++) {
			colors[i] = (colors[i - 1] + 1) % 254 + 1;
		}

		// Calculate optimal ratio for the number of attractors
		double ratio = switch (numPoints) {
			case 3 -> 0.5d;
			case 4 -> addCenterAsAttractor ? 2d / 3d : 0.5d;
			default -> calculateOptimalRatio(numPoints);
		};

		// Generate 200 new points per frame for smooth animation
		for (int iteration = 0; iteration < 200; iteration++) {
			// Select random attractor point
			int randomAttractorIndex = (int) (Math.random() * numPoints);

			// Apply distance restrictions if enabled
			int distanceFromLastPoint = Math.abs(randomAttractorIndex - lastPointIndex);
			distanceFromLastPoint = Math.min(distanceFromLastPoint, 
				(numPoints - randomAttractorIndex) + lastPointIndex);
			
			if (doNotAllowRepeatPoint && distanceFromLastPoint == 0) {
				continue;
			}
			if (maximalDistanceAllowed != 0 && distanceFromLastPoint == maximalDistanceAllowed) {
				continue;
			}
			
			lastPointIndex = randomAttractorIndex;
			Point2D attractorPoint = attractorPoints[randomAttractorIndex];
			
			// Calculate new position (move partway toward the attractor)
			double newDeltaX = (attractorPoint.getX() - currentPoint.getX()) * ratio;
			double newDeltaY = (attractorPoint.getY() - currentPoint.getY()) * ratio;
			currentPoint = currentPoint.add(newDeltaX, newDeltaY);
			
			// Plot the point with the attractor's color
			int x = (int) currentPoint.getX();
			int y = (int) currentPoint.getY();
			if (x >= 0 && x < cellWidth && y >= 0 && y < cellHeight) {
				cellData[y][x] = colors[randomAttractorIndex];
			}
		}

		updateImageFromPalette();
	}

	/**
	 * Calculates the optimal ratio for the Chaos Game with n attractors.
	 * <p>
	 * This formula ensures that the fractal fills the space optimally without
	 * creating gaps or overlaps. The ratio determines how far to move toward
	 * each attractor point.
	 * </p>
	 * 
	 * @param n The number of attractors (must be >= 3)
	 * @return The optimal ratio for moving toward attractors
	 * @throws IllegalArgumentException if n < 3
	 */
	public static double calculateOptimalRatio(int n) {
		if (n < 3) {
			throw new IllegalArgumentException("Number of attractors must be at least 3");
		}

		// Calculate c(n) = sum from k=0 to floor(n/4) of 2 * cos(2 * pi * k / n)
		double c_n = 0.0;
		int k_max = n / 4; // floor(n/4)
		for (int k = 0; k <= k_max; k++) {
			double angle = 2 * Math.PI * k / n;
			c_n += 2 * Math.cos(angle);
		}

		// Optimal r = 1 - 1/c(n)
		return 1.0 - 1.0 / c_n;
	}

	/**
	 * Updates the display image from the cell data using the color palette.
	 * <p>
	 * This method converts the cell data into pixel data for display. Each cell
	 * is rendered using the corresponding color from the palette. The method
	 * uses efficient pixel writing for better performance.
	 * </p>
	 */
	private void updateImageFromPalette() {
		WritablePixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraInstance();

		for (int cy = 0; cy < cellHeight; cy++) {
			for (int cx = 0; cx < cellWidth; cx++) {
				// Get color for this cell
				int color = cellData[cy][cx];
				byte b = palette.colors[color][0]; // B
				byte g = palette.colors[color][1]; // G
				byte r = palette.colors[color][2]; // R
				byte a = palette.colors[color][3]; // A

				// Fill the pixel block with the color
				for (int i = 0; i < pixelSize * pixelSize; i++) {
					int pos = i * 4;
					blockBuffer[pos] = b;     // B
					blockBuffer[pos + 1] = g; // G
					blockBuffer[pos + 2] = r; // R
					blockBuffer[pos + 3] = a; // A
				}

				// Write pixel block to image
				int screenX = cx * pixelSize;
				int screenY = cy * pixelSize;
				if (screenX + pixelSize <= width && screenY + pixelSize <= height) {
					pixelWriter.setPixels(screenX, screenY, pixelSize, pixelSize, 
						pixelFormat, blockBuffer, 0, pixelSize * 4);
				}
			}
		}
	}

	/**
	 * Updates mouse position for potential future interactive features.
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
	 * Restarts the fractal generation without recreating the renderer.
	 * Clears the current fractal and begins generating a new one.
	 */
	public void restart() {
		initialize();
	}
}