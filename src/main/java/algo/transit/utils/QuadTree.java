package algo.transit.utils;

import algo.transit.models.common.Stop;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuadTree {
    public static final int MAX_POINTS = 4;
    public static final int MAX_DEPTH = 10;

    public final double minX, minY, maxX, maxY;
    public final int depth;
    public final List<Stop> points;
    public QuadTree[] children;
    private static final Map<String, Double> distanceCache = new ConcurrentHashMap<>(10000);

    public QuadTree(
            double minX,
            double minY,
            double maxX,
            double maxY,
            int depth
    ) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.depth = depth;
        this.points = new ArrayList<>();
        this.children = null;
    }

    public boolean insert(Stop stop) {
        if (!inBounds(stop)) return false;

        if (children == null && points.size() < MAX_POINTS || depth >= MAX_DEPTH) {
            points.add(stop);
            return true;
        }

        if (children == null) {
            split();
            for (Stop existingStop : points) insertIntoChildren(existingStop);
            points.clear();
        }

        return insertIntoChildren(stop);
    }

    public List<Stop> findNearby(double lat, double lon, double radius) {
        List<Stop> result = new ArrayList<>();
        if (!intersectsRadius(lat, lon, radius)) return result;

        for (Stop stop : points) {
            if (distance(lat, lon, stop.latitude, stop.longitude) <= radius) result.add(stop);
        }

        if (children != null) {
            for (QuadTree child : children) result.addAll(child.findNearby(lat, lon, radius));
        }

        return result;
    }

    @Contract(pure = true)
    private boolean inBounds(@NotNull Stop stop) {
        return stop.longitude >= minX && stop.longitude <= maxX && stop.latitude >= minY && stop.latitude <= maxY;
    }

    private void split() {
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;
        int nextDepth = depth + 1;

        children = new QuadTree[4];
        children[0] = new QuadTree(minX, minY, midX, midY, nextDepth);
        children[1] = new QuadTree(midX, minY, maxX, midY, nextDepth);
        children[2] = new QuadTree(minX, midY, midX, maxY, nextDepth);
        children[3] = new QuadTree(midX, midY, maxX, maxY, nextDepth);
    }

    private boolean insertIntoChildren(Stop stop) {
        for (QuadTree child : children) if (child.insert(stop)) return true;
        return false;
    }

    private boolean intersectsRadius(double lat, double lon, double radius) {
        double closestX = Math.max(minX, Math.min(lon, maxX));
        double closestY = Math.max(minY, Math.min(lat, maxY));
        return distance(lat, lon, closestY, closestX) <= radius;
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        // Create cache key
        String key = String.valueOf(lat1) + ',' + lon1 + '-' + lat2 + ',' + lon2;

        // Check cache first
        Double cachedDistance = distanceCache.get(key);
        if (cachedDistance != null) return cachedDistance;
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        if (distanceCache.size() < 10000) distanceCache.put(key, distance);
        return distance;
    }

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}