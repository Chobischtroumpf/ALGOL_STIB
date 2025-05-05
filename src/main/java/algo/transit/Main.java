package algo.transit;

import algo.transit.controllers.MetaController;

public class Main {
    public static void main(String[] args) {
        double walkingSpeed = 80.0;   // default meters per minute
        double maxWalkingTime = 5.0;  // default walking time

        if (args.length >= 2) {
            try {
                walkingSpeed = Double.parseDouble(args[0]);
                maxWalkingTime = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid arguments. Using defaults.");
            }
        }

        MetaController metaController = new MetaController(walkingSpeed, maxWalkingTime);
        System.out.println("Transit system initialized successfully.");
    }
}
