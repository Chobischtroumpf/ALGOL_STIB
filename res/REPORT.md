# Optimizing Transit Navigation: An Algorithmic Approach to Pathfinding Across Belgium's National Transport Networks

## 1. Introduction and Project Overview

This report presents an in-depth analysis of applying pathfinding algorithms to optimize travel across Belgium's four
main national transport agencies. The project explores algorithmic solutions for minimizing travel time and distance
while navigating the country's four main transit systems.

## 2. Problem Statement and Objectives

Our primary objective was to implement an algorithm that can determine the most efficient route between any two transit
stops in Belgium, based on a given departure time. The algorithm needed to process data from multiple transport agencies
to create an unified navigation system that could effectively guide travelers across the entire national network.

### 2.1 Core Objectives

* Design and implement an algorithm to compute the shortest path between any two stops in Belgium's national transit
  network
* Incorporate temporal constraints based on specified departure times

### 2.2 Extended Functionality

In addition to the core pathfinding requirement, the project specifications included several supplemental features:

* Support for alternative optimization strategies (e.g., minimizing transfers or walking distance)
* Ability to exclude specific modes of transport from routing
* Customizable weighting of transport types based on user preferences
* Configurable walking parameters (e.g., speed, maximum acceptable walking distance)

## 3. Data Sources and Processing

The project uses General Transit Feed Specification (GTFS) data, streamlined to include only components essential for
pathfinding. Detailed GTFS standards are available at gtfs.org.

### 3.1 GTFS Data Selection

From the comprehensive GTFS standard format (which typically includes over 25 distinct data files), our implementation
uses the four components given with the project:

1. Stops - Geographic coordinates of pick-up/drop-off points
2. Trips - Sequence of two or more stops that occurs at a specific time (within a route)
3. Stop Times - Specific arrival/departure times for each stop in a trip
4. Routes - Logical groupings of trips shown as a single service to passengers

### 3.2 Data Integration

The first challenge we encountered was managing over 17 million lines of data, split across four transport agencies and
four CSV files per agency. Our initial strategy was to leverage an existing Java library capable of handling this task
efficiently. We selected OpenCSV, a library that appeared intuitive and easy to integrate. The approach was simple:
iterate through each file, instantiate a reader, and parse all rows sequentially.

Each row was converted into a domain-specific object corresponding to the file type, with attributes mapped to
fields—parsing numeric values as int or double as needed. These objects were stored in memory for later use in the
pathfinding logic.

`Data loading time: 45.386 seconds`

While functionally sound, this method presented significant performance limitations. Notably, it required upwards of
8–10 GB of RAM to complete execution—rendering it unusable on machines with lower memory specifications.

Given that our development machines each had 32 GB of RAM, this limitation initially went unnoticed. However, to ensure
broader accessibility and robustness, we recognized the need for substantial optimization.

### 3.3 Heap Memory Analysis

We conducted a focused analysis to understand the root cause of the high memory consumption. The conclusion was
straightforward: Java's object-oriented paradigm introduces overhead by wrapping even simple data into full-fledged
objects. Consequently, we were attempting to handle approximately 17 million object instances—clearly inefficient.

The primary culprit was the StopTime object, which accounted for a significant portion of memory usage. We revised the
data model to eliminate it as a persistent in-memory entity. Instead, its role was maintained implicitly by embedding
stop-time information as lightweight links—such as timestamped associations—between Trip and Stop objects, without
representing it as a standalone object.

`Data loading time: 40.164 seconds`

While this did not yield a dramatic improvement in parsing speed, it significantly reduced heap usage, enabling the
application to run comfortably on less powerful hardware.

### 3.4 Parser Redesign: Iterator-Based Optimization

After several days of testing minor improvements without meaningful gains in performance or memory usage, we
reconsidered our overall parsing approach. It became evident that incremental tweaks were insufficient. Our solution was
to rearchitect the parser itself, adopting more advanced—but less conventional—Java patterns.

We transformed our static CSV reader into an Iterator/Iterable abstraction, parameterized by a file path and a converter
function. This converter would handle the transformation of raw CSV lines into domain objects on the fly, as the file
was iterated. This shift effectively fused reading and parsing into a single pass, eliminating the overhead of holding
intermediate data in memory.

The results were substantial: parsing time was halved, and heap usage dropped to under 2 GB, aligning with the bounds we
set for ourselves.

`Data loading time: 20.411 seconds`

## 4. Pathfinding & Algorithmical Approach

With the data pipeline stabilized and memory concerns resolved, we shifted focus to the heart of the project: the
pathfinding algorithm.

Our initial approach involved constructing a dedicated Graph class to represent the entire transit network. While
conceptually sound, this quickly became problematic—embedding over 17 million entities into memory again would
effectively double our memory usage, pushing us close to our upper performance bounds.

More critically, this design added an unnecessary layer of abstraction and verbosity, which complicated debugging and
slowed iteration.

Moreover, introducing distinct classes for edges, such as walking and transit edges, along with various segment types,
made the structure overly rigid and complex. After spending about a week attempting to make this approach functional, we
ultimately discarded it.

### 4.1 Final Model Design

Following the dead end with the explicit graph structure, we opted to temporarily move our pathfinding logic to Python—a
language we were more familiar with and better suited for rapid prototyping of algorithms.

Returning to Java with a more grounded plan, we abandoned the idea of a static Graph class. Instead, we leaned into the
object relationships already present in memory, treating the dataset itself as a virtual graph. The concept of edges was
retained, but encoded through the relationships between Trip, Stop, and their associated attributes.

Key abstractions such as Connections and Transitions were introduced to represent movement between stops—either via
transit or walking.

A Connection encapsulates the idea of moving from one stop to another with associated metadata like trip ID, route,
times, and transport mode. A Transition is derived from these and includes additional information like cost and
user-readable formatting.

To further enhance routing efficiency, especially for walking transfers, we implemented a QuadTree structure to index
stop locations geographically. This spatial partitioning enabled fast lookups of nearby stops within a given radius,
drastically improving the performance of walking connection generation.

We used the Haversine formula to compute geographic distances between two coordinates:

$$d = 2R \cdot \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)}\right)$$

Where:

* $d$ is the distance between two points,
* $R$ is the Earth's radius (typically 6,371,000 meters),
* $\phi_1$, $\phi_2$ are latitudes in radians,
* $\Delta\phi$ and $\Delta\lambda$ are the differences in latitude and longitude, respectively.

This QuadTree, built at load time, allows efficient lookup of nearby stops for walking transitions during pathfinding.
Only feasible links—based on user walking speed and maximum distance—are generated, ensuring performance without
sacrificing routing flexibility.

Finally, user preferences were integrated via a flexible TransitPreferences configuration, allowing adjustments to
walking speed, transfer penalties, forbidden modes, and more. This made it possible to adapt the algorithm to various
user scenarios without modifying the core logic.

With all these components in place—virtualized graph representation, optimized transition handling, spatial indexing,
and dynamic user preferences—we were able to begin reliable and efficient pathfinding across the full dataset.

## 5. Dijkstra-Based Time-Aware Pathfinder

### 5.1 Why This Algorithm Works

Our problem is a time-dependent shortest path search over a multimodal, geo-distributed network. The algorithm must
consider:

* Temporal constraints (departure/waiting times)
* Multiple transport modes (train, bus, tram, metro)
* Transfers and walking segments
* User preferences (e.g., avoid transfers or walking)

We selected a Dijkstra-based algorithm with a dynamic cost function, as it satisfies all problem constraints:

* Guarantees optimal paths on graphs with non-negative weights
* Supports dynamic edge generation at runtime
* Scales well through pruning and heuristics

### 5.2 Virtual Graph Design

Instead of precomputing a massive in-memory graph, we treat the GTFS dataset as a virtual graph:

* Nodes: stop-time instances
* Edges: dynamically generated connections (transit or walking)
* Graph traversal: driven by temporal queries and proximity lookups

This avoids memory overhead and utilizes natural GTFS relationships (trips, stops, times) as edge logic.

### 5.3 Key Data Structures

* Priority queue (min-heap): orders states by accumulated cost
* Visited map: stores best known cost per stop ID
* QuadTree: indexes stop coordinates for fast walking link generation

Each state is a DijkstraState, tracking:

* Current stop ID and time
* Accumulated cost
* Sequence of Transition objects
* Last transport mode used
* Number of transfers

### 5.4 Cost Function

The cost function is flexible and reflects real-world travel friction:

$$\text{cost} = t_\text{transit} \cdot w_\text{mode} + t_\text{wait} \cdot w_\text{wait} + p_\text{transfer}$$

Where:

* $w_mode$: user-defined weight per transport mode (train, tram, etc.)
* $w_wait$: wait penalty
* $p_transfer$: cost for changing lines/modes

This enables optimization for:

* Speed
* Fewer transfers
* Avoiding certain modes or walking

Transfer penalties and minimum transfer times are enforced; if unmet, the transition is discarded.

### 5.5 Pseudocode

```shell
function find_path(source, target, departure_time, prefs):
    queue = priority_queue()
    visited = {}

    initial = DijkstraState(source, departure_time, 0, [], None, 0)
    queue.push(initial)

    while not queue.is_empty():
        state = queue.pop()

        if state.stop_id == target:
            return state.path

        if visited.get(state.stop_id, ∞) <= state.cost:
            continue
        visited[state.stop_id] = state.cost

        for conn in get_transit_connections(state, prefs):
            if is_valid(conn, state, prefs):
                queue.push(apply_transition(state, conn, prefs))

        for near_stop in quad_tree.nearby_stops(state.stop_id, prefs.max_walk_dist):
            walk_conn = create_walking_connection(state.stop_id, near_stop, state.time)
            if is_valid(walk_conn, state, prefs):
                queue.push(apply_transition(state, walk_conn, prefs))

    return None
```

### 5.6 Why It Finds the Best Path

Dijkstra's algorithm guarantees the optimal path when:

* All edge weights (costs) are non-negative (true here)
* The state expansion ensures that the first time we reach the target stop is the minimum cost path

We ensure this by:

* Using a min-priority queue ordered by total cost,
* Preventing revisiting worse paths to the same stop,
* Dynamically generating only valid transitions at each step

### 5.7 Pruning and Heuristics

To scale efficiently, multiple pruning strategies are used:

* Temporal pruning skips trips that depart too early or too far in the future.
* Spatial pruning ignores transitions that don't bring us closer to the target:

  $$\text{distance}(\text{next_stop}, \text{target}) < \delta * \text{distance}(\text{current_stop}, \text{target})$$

  where $\delta$ (the deviation tolerance) defaults to 1.25 but is relaxed the closer we get to local stops.
* Transfer limiting adds an optional max-transfer threshold

## 6. Bonus Feature: Graphical Visualization

As an enhancement to the core functionality, we developed a graphical visualization component using OpenGL. This feature
provides users with an intuitive visual representation of calculated routes, displaying the optimal path overlaid on a
map of Belgium's transit network.