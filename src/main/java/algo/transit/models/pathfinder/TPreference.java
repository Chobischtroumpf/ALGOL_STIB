package algo.transit.models.pathfinder;

import algo.transit.enums.TType;

import java.util.List;
import java.util.Map;

public class TPreference {
    public double walkingSpeed;
    public double maxWalkingTime;
    public Map<TType, Double> modeWeights;
    public List<TType> forbiddenModes;
    public double modeSwitchPenalty;
    public int maxTransfers;
    public double maxWaitTime;
    public double minTransferTime;
    public String optimizationGoal;

    public TPreference(
            double walkingSpeed,
            double maxWalkingTime,
            Map<TType, Double> modeWeights,
            List<TType> forbiddenModes,
            double modeSwitchPenalty,
            int maxTransfers,
            double maxWaitTime,
            double minTransferTime,
            String optimizationGoal
    ) {
        this.walkingSpeed = walkingSpeed;
        this.maxWalkingTime = maxWalkingTime;
        this.modeWeights = modeWeights;
        this.forbiddenModes = forbiddenModes;
        this.modeSwitchPenalty = modeSwitchPenalty;
        this.maxTransfers = maxTransfers;
        this.maxWaitTime = maxWaitTime;
        this.minTransferTime = minTransferTime;
        this.optimizationGoal = optimizationGoal;
    }
}