package algo.transit.pathfinding;

import algo.transit.models.Transition;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class State implements Comparable<State> {
    public final String stopId;
    public final LocalTime time;
    public final double cost;
    public final List<Transition> path;
    public final double heuristic;

    public State(String stopId, LocalTime time, double cost, List<Transition> path, double heuristic) {
        this.stopId = stopId;
        this.time = time;
        this.cost = cost;
        this.path = path;
        this.heuristic = heuristic;
    }

    public static State createInitialState(String stopId, LocalTime time, double heuristic) {
        return new State(stopId, time, 0, new ArrayList<>(), heuristic);
    }

    public State createSuccessor(String newStopId, LocalTime newTime, double transitionCost,
                                 Transition transition, double heuristic) {
        List<Transition> newPath = new ArrayList<>(path);
        newPath.add(transition);
        return new State(newStopId, newTime, cost + transitionCost, newPath, heuristic);
    }

    public double getTotalCost() {
        return cost + heuristic;
    }

    @Override
    public int compareTo(State other) {
        return Double.compare(this.getTotalCost(), other.getTotalCost());
    }

    @Override
    public String toString() {
        return "State{stopId='" + stopId + "', time=" + time + ", cost=" + cost +
                ", pathLen=" + path.size() + ", h=" + heuristic + ", totalCost=" + getTotalCost() + '}';
    }
}