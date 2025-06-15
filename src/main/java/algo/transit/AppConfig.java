package algo.transit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();

    // --- Pruning Settings ---
    public static final double DEVIATION_TOLERANCE;
    public static final int MAX_STOPS_TO_CONSIDER;

    // --- Cost Settings ---
    public static final double MODE_SWITCH_PENALTY;

    static {
        // This block is executed once when the class is loaded.
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Unable to find config.properties. Loading default values.");
                properties.put("pathfinder.pruning.deviation_tolerance", "1.25");
                properties.put("pathfinder.pruning.max_stops_to_consider", "5");
                properties.put("pathfinder.cost.mode_switch_penalty", "5.0");

            } else {
                properties.load(input);
                logger.info("Application configuration loaded successfully.");
            }
        } catch (Exception e) {
            logger.error("Error loading configuration, using default values.", e);
        } finally {
            DEVIATION_TOLERANCE = Double.parseDouble(properties.getProperty("pathfinder.pruning.deviation_tolerance"));
            MAX_STOPS_TO_CONSIDER = Integer.parseInt(properties.getProperty("pathfinder.pruning.max_stops_to_consider"));
            MODE_SWITCH_PENALTY = Double.parseDouble(properties.getProperty("pathfinder.cost.mode_switch_penalty"));
        }
    }

    // Private constructor to prevent instantiation
    private AppConfig() {
    }
}