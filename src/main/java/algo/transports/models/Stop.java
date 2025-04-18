package algo.transports.models;

public class Stop {
    String stop_id;
    String stop_name;
    double stop_lat;
    double stop_lon;

    public Stop(String stop_id, String stop_name, double stop_lat, double stop_lon) {
        this.stop_id = stop_id;
        this.stop_name = stop_name;
        this.stop_lat = stop_lat;
        this.stop_lon = stop_lon;
    }

    public String getId()
    {
        return stop_id;
    }

    public String getName()
    {
        return stop_name;
    }

    public double getLat()
    {
        return stop_lat;
    }

    public double getLon()
    {
        return stop_lon;
    }
}
