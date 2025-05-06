package algo.transit.models;

import algo.transit.enums.TransportType;

import java.util.List;
import java.util.Map;

public class TransitPreference {
    public double walkingSpeed;
    public double maxWalkingTime;
    public Map<TransportType, Double> modeWeights;
    public List<TransportType> forbiddenModes;
    public double modeSwitchPenalty;

    public TransitPreference(double walkingSpeed, double maxWalkingTime,
                             Map<TransportType, Double> modeWeights,
                             List<TransportType> forbiddenModes,
                             double modeSwitchPenalty) {
        this.walkingSpeed = walkingSpeed;
        this.maxWalkingTime = maxWalkingTime;
        this.modeWeights = modeWeights;
        this.forbiddenModes = forbiddenModes;
        this.modeSwitchPenalty = modeSwitchPenalty;
    }
}