package algo.transit.models;

import algo.transit.enums.TransportType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Route {
    public final String routeId;
    public final String shortName;
    public final String longName;
    public final TransportType type;
    public final Set<Stop> possibleStops = new HashSet<>();

    public Route(String routeID, String shortName, String longName, String typeStr) {
        this.routeId = routeID;
        this.shortName = shortName;
        this.longName = longName;
        this.type = TransportType.fromString(typeStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(routeId, ((Route) o).routeId);
    }

    @Override
    public int hashCode() {
        return routeId != null ? routeId.hashCode() : 0;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public TransportType getType() {
        return type;
    }

    public Set<Stop> getPossibleStops() {
        return possibleStops;
    }

    public void addPossibleStop(Stop stop) {
        possibleStops.add(stop);
    }
}