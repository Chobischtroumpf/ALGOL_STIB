package algo.transports.models;

import algo.transports.enums.TransportType;

import java.util.Objects;

public class Route {
    private String routeId;
    private String routeShortName;
    private String routeLongName;
    private TransportType routeType;

    public Route() {
        // Default
    }

    public Route(String routeID, String routeShortName, String routeLongName, TransportType routeType) {
        this.routeId = routeID;
        this.routeShortName = routeShortName;
        this.routeLongName = routeLongName;
        this.routeType = routeType;
    }

    public Route(String routeID, String routeShortName, String routeLongName, String routeTypeStr) {
        this.routeId = routeID;
        this.routeShortName = routeShortName;
        this.routeLongName = routeLongName;
        this.routeType = TransportType.fromString(routeTypeStr);
    }

    public String getRouteID() {
        return routeId;
    }

    public void setRouteID(String routeID) {
        this.routeId = routeID;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
    }

    public String getRouteLongName() {
        return routeLongName;
    }

    public void setRouteLongName(String routeLongName) {
        this.routeLongName = routeLongName;
    }

    public TransportType getRouteType() {
        return routeType;
    }

    public void setRouteType(TransportType routeType) {
        this.routeType = routeType;
    }

    public void setRouteType(String routeTypeStr) {
        this.routeType = TransportType.fromString(routeTypeStr);
    }

    @Override
    public String toString() {
        return "Route{" +
                "routeID='" + routeId + '\'' +
                ", routeShortName='" + routeShortName + '\'' +
                ", routeLongName='" + routeLongName + '\'' +
                ", routeType=" + routeType +
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
    public int hashCode() {
        return routeId != null ? routeId.hashCode() : 0;
    }
}