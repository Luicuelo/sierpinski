package main;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Main JavaFX application for the Sierpinski triangle fractal generator.
 * <p>
 * This application creates a window with a canvas where the Sierpinski triangle
 * fractal is generated using the Chaos Game algorithm. Users can configure
 * various parameters such as the number of attractors, distance restrictions,
 * and other fractal generation options.
 * </p>
 * <p>
 * Features:
 * <ul>
 * <li>Real-time Sierpinski triangle generation using Chaos Game algorithm</li>
 * <li>Configurable number of attractors (3-7)</li>
 * <li>Distance restrictions between consecutive points</li>
 * <li>Option to prevent repeating the same attractor</li>
 * <li>Option to add center point as an additional attractor</li>
 * <li>60 FPS animation loop for smooth visualization</li>
 * <li>Restart functionality</li>
 * <li>Configurable window size via command-line parameters</li>
 * </ul>
 * </p>
 * 
 * @see FractalRenderer
 */
public class SierpinskiTriangle extends Application {
	private static final int PIXEL_SIZE = 1;
	private static final int DEFAULT_WIDTH = 1000;
	private static final int DEFAULT_HEIGHT = 1000;
	private static final int BUTTON_HEIGHT = 25;

	private int canvasWidth;
	private int canvasHeight;
	private Canvas canvas;
	private FractalRenderer renderer;
	private AnimationTimer animationTimer;
	private boolean finished = false;

	private enum MaximalDistance {
		NO_RESTRICTION,
		NO_ALLOW_DISTANCE_1,
		NO_ALLOW_DISTANCE_2,
		NO_ALLOW_DISTANCE_3,
		NO_ALLOW_DISTANCE_4
	}

	/**
	 * Main entry point for the application.
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Starts the JavaFX application and initializes the user interface.
	 * 
	 * @param primaryStage the primary stage for this application
	 */
	@Override
	public void start(Stage primaryStage) {
		// Parse command line arguments for size
		Parameters params = getParameters();
		parseSize(params);

		primaryStage.setTitle("Sierpinski Triangle Fractal Generator");

		// Create and initialize
		initialize();

		// Setup mouse listeners
		setupMouseListeners();

		// Create the restart button
		Button restartButton = new Button("Restart");
		restartButton.setOnAction(e -> restart());
		restartButton.setPrefHeight(BUTTON_HEIGHT);

		// Create the number of attractors selector dropdown
		ComboBox<Integer> numberOfAttractorsCombo = new ComboBox<>();
		numberOfAttractorsCombo.getItems().addAll(3, 4, 5, 6, 7);
		numberOfAttractorsCombo.setPrefHeight(BUTTON_HEIGHT);
		numberOfAttractorsCombo.setValue(3);
		numberOfAttractorsCombo.setOnAction(e -> {
			renderer.attractorNumberOfPoints = numberOfAttractorsCombo.getValue();
			restart();
		});

		// Create the maximal distance selector dropdown
		ComboBox<MaximalDistance> maximalDistancePreviousPoint = new ComboBox<>();
		maximalDistancePreviousPoint.getItems().addAll(MaximalDistance.values());
		maximalDistancePreviousPoint.setPrefHeight(BUTTON_HEIGHT);
		maximalDistancePreviousPoint.setValue(MaximalDistance.NO_RESTRICTION);
		maximalDistancePreviousPoint.setOnAction(e -> {
			renderer.maximalDistanceAllowed = maximalDistancePreviousPoint.getValue().ordinal();
			restart();
		});

		CheckBox doNotAllowRepeatBox = new CheckBox();
		doNotAllowRepeatBox.setText("Do not allow repeat point");
		doNotAllowRepeatBox.setOnAction(e -> {
			renderer.doNotAllowRepeatPoint = doNotAllowRepeatBox.isSelected();
			restart();
		});

		CheckBox addCenterAsAttractorBox = new CheckBox();
		addCenterAsAttractorBox.setText("Add center as attractor");
		addCenterAsAttractorBox.setOnAction(e -> {
			renderer.addCenterAsAttractor = addCenterAsAttractorBox.isSelected();
			restart();
		});

		// Layout
		BorderPane root = new BorderPane();
		root.setCenter(canvas);

		HBox buttons = new HBox();
		root.setBottom(buttons);
		buttons.setPadding(new Insets(0, 10, 0, 0));
		buttons.setSpacing(10);
		buttons.getChildren().addAll(restartButton, numberOfAttractorsCombo, doNotAllowRepeatBox,
				addCenterAsAttractorBox, maximalDistancePreviousPoint);

		root.setBottom(buttons);

		// Create scene and show
		Scene scene = new Scene(root, canvasWidth, canvasHeight + BUTTON_HEIGHT);
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.show();
	}

	/**
	 * Parses command line arguments for canvas size.
	 * 
	 * @param params the application parameters containing named arguments
	 */
	private void parseSize(Parameters params) {
		try {
			if (params.getNamed().containsKey("width")) {
				canvasWidth = Integer.parseInt(params.getNamed().get("width"));
			} else {
				canvasWidth = DEFAULT_WIDTH;
			}

			if (params.getNamed().containsKey("height")) {
				canvasHeight = Integer.parseInt(params.getNamed().get("height"));
			} else {
				canvasHeight = DEFAULT_HEIGHT;
			}
		} catch (NumberFormatException e) {
			System.err.println("Invalid size parameters, using defaults");
			canvasWidth = DEFAULT_WIDTH;
			canvasHeight = DEFAULT_HEIGHT;
		}
	}

	/**
	 * Initializes the canvas and starts the animation loop. Sets up the graphics
	 * context and begins 60 FPS rendering.
	 */
	private void initialize() {
		canvas = new Canvas(canvasWidth, canvasHeight);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		finished = false;

		renderer = new FractalRenderer(gc, canvasWidth, canvasHeight, PIXEL_SIZE);

		animationTimer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (!finished && renderer != null) {
					try {
						renderer.updateRegion();
					} catch (Exception e) {
						System.err.println("Error updating fractal: " + e.getMessage());
						stopAnimation();
					}
				}
			}
		};
		animationTimer.start();
	}

	/**
	 * Configures mouse event handlers for interactive effects. Tracks mouse
	 * movement, entry, and exit events on the canvas.
	 */
	private void setupMouseListeners() {
		canvas.setOnMouseMoved(e -> {
			if (renderer != null) {
				renderer.setMousePosition((int) e.getX(), (int) e.getY(), true);
			}
		});

		canvas.setOnMouseExited(e -> {
			if (renderer != null) {
				renderer.setMousePosition(0, 0, false);
			}
		});

		canvas.setOnMouseEntered(e -> {
			if (renderer != null) {
				renderer.setMousePosition((int) e.getX(), (int) e.getY(), true);
			}
		});
	}

	/**
	 * Restarts the fractal generation by reinitializing the renderer.
	 */
	private void restart() {
		System.out.println("Restarting fractal generation...");
		if (renderer != null) {
			renderer.restart();
		}
	}

	/**
	 * Stops the animation timer and cleans up resources.
	 */
	private void stopAnimation() {
		finished = true;
		if (animationTimer != null) {
			animationTimer.stop();
		}
	}

	/**
	 * Called when the application is about to stop. Cleans up resources and exits.
	 */
	@Override
	public void stop() {
		stopAnimation();
		renderer = null;
		System.out.println("Sierpinski Triangle application destroyed");
		Platform.exit();
	}
}