package principal;

import javafx.animation.AnimationTimer;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Main JavaFX application for the falling sand particle simulation.
 * <p>
 * This application creates a window with a canvas where users can interact with
 * a falling sand physics simulation. Users can add particles by moving the
 * mouse
 * over the canvas and apply forces by clicking. The simulation runs at 60 FPS.
 * </p>
 * <p>
 * Features:
 * <ul>
 * <li>Real-time particle physics simulation</li>
 * <li>Interactive mouse controls for adding particles and applying forces</li>
 * <li>60 FPS animation loop</li>
 * <li>Restart functionality</li>
 * <li>Configurable window size via command-line parameters</li>
 * </ul>
 * </p>
 * 
 * @see Renderer
 * @see Transition
 */
public class Sierpinski extends Application {
	private static final int PIXEL_SIZE = 4; // 4x4 pixels per cell
	private static final int DEFAULT_WIDTH = 1000;
	private static final int DEFAULT_HEIGHT = 1000;
	private static final int BUTTON_HEIGHT = 25;

	private int canvasWidth;
	private int canvasHeight;
	private Canvas canvas;
	private Renderer renderer;
	private AnimationTimer animationTimer;
	private boolean finished = false;

	private enum numberOfPoints {
		n3,
		n4,
		n5,
		n6
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

		primaryStage.setTitle("SandFX");

		// Create and initialize
		initialize();

		// Setup mouse listeners
		setupMouseListeners();

		// Create the restart button
		Button restartButton = new Button("Restart");
		restartButton.setOnAction(e -> restart());
		restartButton.setPrefHeight(BUTTON_HEIGHT);

		// Create the algorithm selector dropdown
		ComboBox<numberOfPoints> numberOfPointsCombo = new ComboBox<>();
		numberOfPointsCombo.getItems().addAll(numberOfPoints.values());
		numberOfPointsCombo.setPrefHeight(BUTTON_HEIGHT);
		numberOfPointsCombo.setValue(numberOfPoints.n3);
		// Add a listener to update the actualSortAlgorithm field when selection changes
		numberOfPointsCombo.setOnAction(e -> {
			renderer.actractorNumberOfPoints = ((numberOfPoints) numberOfPointsCombo.getValue()).ordinal()+3;
			restart();
		});

		// Layout
		BorderPane root = new BorderPane();
		root.setCenter(canvas);

		HBox buttons = new HBox();
		root.setBottom(buttons);
		buttons.setPadding(new Insets(0, 10, 0, 0));
		buttons.setSpacing(10);
		buttons.getChildren().addAll(restartButton, numberOfPointsCombo);

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

		renderer = new Renderer(gc, canvasWidth, canvasHeight,PIXEL_SIZE);

		animationTimer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (!finished && renderer != null) {
					try {
						renderer.updateRegion();
					} catch (Exception e) {
						System.err.println("Error updating : " + e.getMessage());
						stopAnimation();
					}
				}
			}
		};
		animationTimer.start();
	}

	/**
	 * Configures mouse event handlers for interactive effects. Tracks mouse
	 * movement, entry, and exit events on the canvas. Also handles mouse clicks
	 * for applying forces to particles in the simulation.
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
	 * Restarts the simulation by reinitializing the renderer.
	 */
	private void restart() {
		System.out.println("Restarting ...");
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
		System.out.println(" destroyed");
		Platform.exit();
	}
}