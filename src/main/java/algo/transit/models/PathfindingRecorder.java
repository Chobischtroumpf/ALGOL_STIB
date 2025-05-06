package algo.transit.models;

import java.util.ArrayList;
import java.util.List;

public class PathfindingRecorder {
    public final List<String> exploredStates = new ArrayList<>();
    public List<Transition> finalPath = new ArrayList<>();

    public String startStopId;
    public String endStopId;

    public void setStartAndEndStops(String startStopId, String endStopId) {
        this.startStopId = startStopId;
        this.endStopId = endStopId;
    }

    public void recordExploredState(String stopId) {
        exploredStates.add(stopId);
    }

    public void recordFinalPath(List<Transition> path) {
        this.finalPath = new ArrayList<>(path);
    }

    public int getTotalSteps() {
        return exploredStates.size();
    }
}