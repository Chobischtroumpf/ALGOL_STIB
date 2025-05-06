package algo.transit.pathfinding;

import algo.transit.models.Transition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public record State(String stopId, LocalTime time, double cost, List<Transition> path, double heuristic,
                    String lastMode) implements Comparable<State> {

    @Contract("_, _, _ -> new")
    public static @NotNull State createInitialState(String stopId, LocalTime time, double heuristic) {
        return new State(stopId, time, 0, new ArrayList<>(), heuristic, "NONE");
    }

    @Contract("_, _, _, _, _ -> new")
    public @NotNull State createSuccessor(String newStopId, LocalTime newTime, double transitionCost,
                                          Transition transition, double heuristic) {
        List<Transition> newPath = new ArrayList<>(path);
        newPath.add(transition);
        return new State(newStopId, newTime, cost + transitionCost, newPath, heuristic,
                transition.mode());
    }

    public double getTotalCost() {
        return cost + heuristic;
    }

    @Override
    public int compareTo(@NotNull State other) {
        int costComparison = Double.compare(this.getTotalCost(), other.getTotalCost());
        if (costComparison != 0) return costComparison;

        // If costs are equal, prefer states with fewer transitions
        return Integer.compare(this.path.size(), other.path.size());
    }

    @Override
    public @NotNull String toString() {
        return "State{stopId='" + stopId + "', time=" + time + ", cost=" + cost +
                ", pathLen=" + path.size() + ", h=" + heuristic + ", totalCost=" + getTotalCost() + '}';
    }
}