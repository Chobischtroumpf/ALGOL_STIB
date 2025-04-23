package algo.transit.models.graph;

import algo.transit.models.Stop;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SpatialIndex {
    private final Map<GridKey, List<Stop>> grid = new HashMap<>();
    private static final double GRID_SIZE = 0.01; // approx 1km grid cells

    // Insert a stop into the spatial index
    public void insert(@NotNull Stop stop) {
        GridKey key = new GridKey(stop.getLat(), stop.getLon());
        grid.computeIfAbsent(key, k -> new ArrayList<>()).add(stop);
    }

    // Find stops near the given stop within maxDistance
    public List<Stop> findNearbyStops(@NotNull Stop center, double maxDistance) {
        List<Stop> result = new ArrayList<>();

        // Calculate grid range to search
        int radius = (int) Math.ceil(maxDistance / (GRID_SIZE * 111000)) + 1;
        int baseLatIdx = (int) (center.getLat() / GRID_SIZE);
        int baseLonIdx = (int) (center.getLon() / GRID_SIZE);

        // Search surrounding grid cells
        for (int latDiff = -radius; latDiff <= radius; latDiff++) {
            for (int lonDiff = -radius; lonDiff <= radius; lonDiff++) {
                GridKey key = new GridKey(
                        baseLatIdx + latDiff,
                        baseLonIdx + lonDiff
                );

                List<Stop> cellStops = grid.get(key);
                if (cellStops != null) {
                    result.addAll(cellStops);
                }
            }
        }

        return result;
    }

    // Grid cell key
    private static class GridKey {
        final int latIdx, lonIdx;

        public GridKey(double lat, double lon) {
            this.latIdx = (int) (lat / GRID_SIZE);
            this.lonIdx = (int) (lon / GRID_SIZE);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GridKey gridKey = (GridKey) o;
            return latIdx == gridKey.latIdx && lonIdx == gridKey.lonIdx;
        }

        @Override
        public int hashCode() { return Objects.hash(latIdx, lonIdx); }
    }
}