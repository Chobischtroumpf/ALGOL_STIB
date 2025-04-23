package algo.transit.models;

import algo.transit.enums.TransportType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Route {
    private final String        routeId;
    private final String        shortName;
    private final String        longName;
    private final TransportType type;
    private final Set<Stop>     possibleStops = new HashSet<>();

    public Route(String routeID, String shortName, String longName, String typeStr) {
        this.routeId   = routeID;
        this.shortName = shortName;
        this.longName  = longName;
        this.type      = TransportType.fromString(typeStr);
    }

    @Override
    public String toString() {
        return "Route{" +
                "routeID='" + routeId + '\'' +
                ", routeShortName='" + shortName + '\'' +
                ", routeLongName='" + longName + '\'' +
                ", routeType=" + type +
                ", possibleStopsCount=" + possibleStops.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(routeId, route.routeId);
    }

    @Override
    public int hashCode() { return routeId != null ? routeId.hashCode() : 0; }

    public String getRouteId() { return routeId; }

    public String getShortName() { return shortName; }

    public String getLongName() { return longName; }

    public TransportType getType() { return type; }

    public Set<Stop> getPossibleStops() { return possibleStops; }

    public void addPossibleStop(Stop stop) { possibleStops.add(stop); }
}