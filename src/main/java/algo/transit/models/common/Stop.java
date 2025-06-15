package algo.transit.models.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(of = "stopId")
public class Stop {
    String stopId;
    String name;
    double latitude;
    double longitude;

    Map<String, Route> routes = new HashMap<>();
    Map<String, Trip> trips = new HashMap<>();

    public Stop(String stopId, String name, double latitude, double longitude) {
        this.stopId = stopId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}