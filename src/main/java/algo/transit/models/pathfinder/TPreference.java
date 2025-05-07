package algo.transit.models.pathfinder;

import algo.transit.enums.TType;

import java.util.List;
import java.util.Map;

public class TPreference {
    public double walkingSpeed;
    public double maxWalkingTime;
    public Map<TType, Double> modeWeights;
    public List<TType> forbiddenModes;
    public String optimizationGoal;

    public TPreference(
            double walkingSpeed,
            double maxWalkingTime,
            Map<TType, Double> modeWeights,
            List<TType> forbiddenModes,
            String optimizationGoal
    ) {
        this.walkingSpeed = walkingSpeed;
        this.maxWalkingTime = maxWalkingTime;
        this.modeWeights = modeWeights;
        this.forbiddenModes = forbiddenModes;
        this.optimizationGoal = optimizationGoal;
    }
}