package algo.transit.utils;

import algo.transit.enums.TType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class CLArgs {
    private String startStop;
    private String endStop;
    private LocalTime startTime;
    private double walkingSpeed = 80.0;
    private double maxWalkTime = 10.0;
    private List<TType> forbiddenModes = new ArrayList<>();
    private Map<TType, Double> modeWeights = new HashMap<>();
    private boolean arriveBy = false;
    private String optimizationGoal = "time";
    private String outputFormat = "detailed";
    private boolean showStats = false;
    private boolean visualize = false;
}