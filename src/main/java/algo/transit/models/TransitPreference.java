package algo.transit.models;

import algo.transit.enums.TransportType;

import java.util.List;
import java.util.Map;

public class TransitPreference {
    public double walkingSpeed;
    public double maxWalkingTime;
    public Map<TransportType, Double> modeWeights;
    public List<TransportType> forbiddenModes;

    public TransitPreference(double walkingSpeed, double maxWalkingTime, Map<TransportType, Double> modeWeights, List<TransportType> forbiddenModes) {
        this.walkingSpeed = walkingSpeed;
        this.maxWalkingTime = maxWalkingTime;
        this.modeWeights = modeWeights;
        this.forbiddenModes = forbiddenModes;
    }

    public TransitPreference() {
        this.walkingSpeed = 80.0;
        this.maxWalkingTime = 5.0;
        this.modeWeights = Map.of(
                TransportType.FOOT, 1.0,
                TransportType.BUS, 1.0,
                TransportType.TRAIN, 1.0,
                TransportType.METRO, 1.0,
                TransportType.TRAM, 1.0
        );
        this.forbiddenModes = List.of();
    }

    public double getWalkDistance() {
        return walkingSpeed * maxWalkingTime;
    }
}