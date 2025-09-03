# Sierpinski Triangle Fractal Generator

This repository provides a Java implementation of the Sierpinski triangle fractal using the **Chaos Game algorithm**. The Sierpinski triangle is a self-similar, recursive geometric structure widely used as an example in mathematics, computer graphics, and fractal geometry.

## Features

- **Real-time fractal generation** using the Chaos Game algorithm
- **Interactive JavaFX interface** with configurable parameters
- **Configurable number of attractors** (3-7 vertices)
- **Distance restrictions** between consecutive attractor selections
- **Optional center attractor** for modified fractal patterns
- **Smooth 60 FPS animation** for real-time visualization
- **Color-coded visualization** with different colors for each attractor
- **Restart functionality** to regenerate the fractal
- **Command-line size configuration** for custom window dimensions

## What is the Sierpinski Triangle?

The Sierpinski triangle is a fractal formed by recursively subdividing an equilateral triangle into smaller equilateral triangles. This implementation uses the **Chaos Game algorithm**, which generates the fractal by:

1. Starting with attractor points (vertices of a polygon)
2. Beginning with a random point inside the polygon
3. Repeatedly selecting a random attractor and moving partway toward it
4. Plotting each new position to reveal the fractal pattern

The algorithm can generate not only the classic Sierpinski triangle (3 attractors) but also higher-order fractals with 4, 5, 6, or 7 attractors, each creating unique and beautiful patterns.

Learn more: [Wikipedia - Sierpinski Triangle](https://en.wikipedia.org/wiki/Sierpinski_triangle)

## Technical Implementation

### Chaos Game Algorithm
The implementation uses an optimized ratio calculation for different numbers of attractors:
- **3 attractors**: ratio = 0.5 (classic Sierpinski triangle)
- **4+ attractors**: calculated using the formula `r = 1 - 1/c(n)` where `c(n)` ensures optimal fractal density

### Distance Restrictions
The application supports various distance restrictions between consecutive attractor selections:
- **No restriction**: Any attractor can follow any other
- **Distance 1-4 restrictions**: Prevents selection of attractors at specific distances from the previous one

### Performance Optimizations
- **Efficient pixel rendering** using JavaFX WritableImage and PixelWriter
- **Batch point generation** (200 points per frame) for smooth animation
- **Color palette system** for fast color lookup and rendering

## Getting Started

### Prerequisites
- Java 11 or higher with JavaFX support
- JavaFX runtime libraries

### Building and Running

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Luicuelo/sierpinski.git
   cd sierpinski
   ```

2. **Compile the project:**
   ```bash
   javac --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml src/main/*.java -d bin
   ```

3. **Run the application:**
   ```bash
   java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -cp bin main.SierpinskiTriangle
   ```

4. **Custom window size (optional):**
   ```bash
   java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -cp bin main.SierpinskiTriangle --width=800 --height=600
   ```

### Using an IDE
1. Open the project in your preferred Java IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Ensure JavaFX libraries are properly configured
3. Run the `SierpinskiTriangle.main()` method

## User Interface Controls

- **Restart Button**: Clears the current fractal and starts generating a new one
- **Number of Attractors Dropdown**: Select 3-7 attractors for different fractal patterns
- **"Do not allow repeat point" Checkbox**: Prevents consecutive selection of the same attractor
- **"Add center as attractor" Checkbox**: Adds the center point as an additional attractor
- **Distance Restriction Dropdown**: Configure distance restrictions between consecutive selections

## Project Structure

```
sierpinski/
├── src/
│   └── main/
│       ├── SierpinskiTriangle.java    # Main JavaFX application class
│       └── FractalRenderer.java       # Fractal rendering and Chaos Game logic
├── bin/                               # Compiled classes
├── README.md                          # This file
├── LICENSE                           # MIT License
└── .gitignore                        # Git ignore rules
```

## Mathematical Background

The Chaos Game algorithm generates fractals through iterated function systems (IFS). For the Sierpinski triangle:

- **Attractor points**: Vertices of a regular polygon
- **Transformation ratio**: Determines how far to move toward each attractor
- **Optimal ratio formula**: `r = 1 - 1/c(n)` where `c(n) = Σ(k=0 to ⌊n/4⌋) 2cos(2πk/n)`

This ensures the fractal fills the available space without gaps or overlaps.

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

### Areas for contribution:
- Additional fractal algorithms (Barnsley fern, Dragon curve, etc.)
- Export functionality (save fractal as image)
- Animation recording capabilities
- Performance optimizations
- Additional color schemes and palettes

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by the mathematical work of Wacław Sierpiński
- Chaos Game algorithm concept by Michael Barnsley
- JavaFX framework for the interactive visualization

---

*If you have suggestions, find bugs, or want to contribute new features, feel free to open an issue or submit a pull request!*