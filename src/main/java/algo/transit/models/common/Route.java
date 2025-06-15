package algo.transit.models.common;

import algo.transit.enums.TType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.HashSet;
import java.util.Set;

@Value
public class Route {
    String routeId;
    String shortName;
    String longName;
    TType type;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    Set<Stop> possibleStops = new HashSet<>();

    public Route(String routeID, String shortName, String longName, String typeStr) {
        this.routeId = routeID;
        this.shortName = shortName;
        this.longName = longName;
        this.type = TType.fromString(typeStr);
    }
}