[![BETransitPathfinder Release](https://github.com/Chobischtroumpf/BETransitPathfinder/actions/workflows/release.yml/badge.svg)](https://github.com/Chobischtroumpf/BETransitPathfinder/actions/workflows/release.yml)

## Requirements

- Java 17 or higher (project uses Java 23 features if available)
- Maven 3.6 or higher

## Compilation

To compile the project, run the following command in the root directory:

```bash
mvn clean compile
```

## Execution

### Running the Application

To run the application with Maven:

```bash
mvn exec:java -Dexec.mainClass="algo.transit.BETransitPathfinder" -Dexec.args="[arguments]"
```

Replace `[arguments]` with the appropriate command-line arguments (see below).

Alternatively, after compiling, you can run the application directly with Java:

```bash
java -cp target/classes algo.transit.BETransitPathfindernsitPathfinder [arguments]
```

### Command-Line Arguments

The application requires at least three arguments:

```
START_STOP END_STOP START_TIME [OPTIONS]
```

- `START_STOP`: The ID of the starting stop (e.g., "SNCB-S8891660")
- `END_STOP`: The ID of the destination stop (e.g., "TEC-X615aya")
- `START_TIME`: The departure time in HH:MM format (e.g., "10:30")

#### Optional Arguments

- `--walking-speed <speed>`: Set walking speed in meters per minute (default: 80.0)
- `--max-walk-time <time>`: Set maximum walking time in minutes (default: 10.0)
- `--forbidden-modes <modes>`: Set forbidden transport modes (e.g., BUS, TRAIN)
- `--mode-weights <mode:weight>`: Set custom weights for transport modes (e.g., BUS:1.5 TRAIN:0.8)
- `--arrive-by`: Find path arriving at specified time, not departing
- `--optimization-goal <goal>`: Set optimization goal: time|transfers|walking (default: time)
- `--output-format <format>`: Set output format: detailed|summary (default: detailed)
- `--show-stats`: Show detailed statistics about the found path
- `--visualize`: Enable visualization of the pathfinding algorithm
- `--help`: Display help message
