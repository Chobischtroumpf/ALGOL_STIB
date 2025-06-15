package algo.transit.models.pathfinder;

import algo.transit.enums.TType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TPreference {
    private double walkingSpeed;
    private double maxWalkingTime;
    private Map<TType, Double> modeWeights;
    private List<TType> forbiddenModes;
    private String optimizationGoal;
}