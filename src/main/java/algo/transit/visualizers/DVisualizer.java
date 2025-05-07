package algo.transit.visualizers;

import algo.transit.models.visualizer.StateRecorder;
import algo.transit.models.common.Stop;
import algo.transit.models.pathfinder.Transition;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

public class DVisualizer extends JFrame implements GLEventListener {
    // Animation settings
    private static final int FPS = 60;
    private static final int TOTAL_DURATION_MS = 5000;
    private static final int TOTAL_FRAMES = FPS * (TOTAL_DURATION_MS / 1000);

    // OpenGL components
    private final GLCanvas canvas;
    private final FPSAnimator animator;
    private TextRenderer textRenderer;
    private GLUT glut;

    // Data
    private final Map<String, Stop> stops;
    private Stop startStop;
    private Stop endStop;
    private StateRecorder recorder;
    private List<Transition> finalPath = new ArrayList<>();
    private final Set<String> pathIntermediateStops = new HashSet<>();

    // Animation state
    private int currentFrame = 0;
    private int[] framesToStateIndex;
    private final Set<String> currentExploredStops = new HashSet<>();
    private boolean showPath = false;
    private int pathFoundFrame = -1;

    // Spatial data
    private double minLat, maxLat, minLon, maxLon;

    // Control
    private final java.util.concurrent.CountDownLatch completionLatch = new java.util.concurrent.CountDownLatch(1);

    public DVisualizer(Map<String, Stop> stops) {
        super("ALGOL Pathfinder Visualizer");
        this.stops = stops;

        // Create OpenGL capabilities profile
        GLProfile profile = GLProfile.getMaximum(true);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setDoubleBuffered(true);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(4); // Anti-aliasing

        // Create canvas
        canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this);

        // Setup animator
        animator = new FPSAnimator(canvas, FPS, true);

        // Create control panel
        JPanel controlPanel = createControlPanel();

        add(canvas, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                animator.stop();
                completionLatch.countDown();
            }
        });
    }

    private @NotNull JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        JButton playButton = new JButton("Play");
        JButton resetButton = new JButton("Reset");
        JButton skipButton = new JButton("Skip to End");

        // Configure buttons
        playButton.addActionListener(_ -> {
            if (animator.isAnimating()) {
                animator.stop();
                playButton.setText("Play");
            } else {
                animator.start();
                playButton.setText("Pause");
            }
        });

        resetButton.addActionListener(_ -> {
            animator.stop();
            currentFrame = 0;
            updateExploredStopsForCurrentFrame();
            canvas.repaint();
            playButton.setText("Play");
        });

        skipButton.addActionListener(_ -> {
            animator.stop();
            currentFrame = TOTAL_FRAMES - 1;
            updateExploredStopsForCurrentFrame();
            showPath = true;
            canvas.repaint();
            playButton.setText("Play");
        });

        // Add to layout
        controlPanel.add(playButton);
        controlPanel.add(resetButton);
        controlPanel.add(skipButton);
        return controlPanel;
    }

    public void setAlgorithmData(@NotNull StateRecorder recorder) {
        this.recorder = recorder;

        // Set start and end stops
        startStop = stops.get(recorder.startStopId);
        endStop = stops.get(recorder.endStopId);

        // Process path data
        finalPath = recorder.finalPath;
        pathIntermediateStops.clear();

        for (Transition transition : finalPath) {
            if (!transition.fromStop().equals(recorder.startStopId) &&
                    !transition.fromStop().equals(recorder.endStopId)) {
                pathIntermediateStops.add(transition.fromStop());
            }
            if (!transition.toStop().equals(recorder.startStopId) &&
                    !transition.toStop().equals(recorder.endStopId)) {
                pathIntermediateStops.add(transition.toStop());
            }
        }

        calculateBounds();
        calculateFrameMapping();
        findPathFoundFrame();
        resetAnimation();
    }

    private void findPathFoundFrame() {
        // Determine at which frame the path is found
        if (recorder == null || framesToStateIndex == null) return;

        // The path is found at the last step of the exploration
        int lastExploredStateIndex = recorder.exploredStates.size() - 1;

        // Find the frame that corresponds to this state
        for (int i = 0; i < framesToStateIndex.length; i++) {
            if (framesToStateIndex[i] >= lastExploredStateIndex) {
                pathFoundFrame = i;
                break;
            }
        }

        // If we couldn't find it, set it to the last frame
        if (pathFoundFrame == -1) {
            pathFoundFrame = TOTAL_FRAMES - 1;
        }
    }

    private void calculateBounds() {
        if (startStop == null || endStop == null) return;

        // Start with the start and end stops
        minLat = Math.min(startStop.latitude, endStop.latitude);
        maxLat = Math.max(startStop.latitude, endStop.latitude);
        minLon = Math.min(startStop.longitude, endStop.longitude);
        maxLon = Math.max(startStop.longitude, endStop.longitude);

        // Expand bounds to include all path stops
        for (Transition transition : finalPath) {
            Stop fromStop = stops.get(transition.fromStop());
            Stop toStop = stops.get(transition.toStop());

            if (fromStop != null) {
                minLat = Math.min(minLat, fromStop.latitude);
                maxLat = Math.max(maxLat, fromStop.latitude);
                minLon = Math.min(minLon, fromStop.longitude);
                maxLon = Math.max(maxLon, fromStop.longitude);
            }

            if (toStop != null) {
                minLat = Math.min(minLat, toStop.latitude);
                maxLat = Math.max(maxLat, toStop.latitude);
                minLon = Math.min(minLon, toStop.longitude);
                maxLon = Math.max(maxLon, toStop.longitude);
            }
        }

        // Add padding
        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;

        // Ensure minimum zoom level
        latRange = Math.max(latRange, 0.01);
        lonRange = Math.max(lonRange, 0.01);

        minLat -= latRange * 0.3;
        maxLat += latRange * 0.3;
        minLon -= lonRange * 0.3;
        maxLon += lonRange * 0.3;
    }

    private void calculateFrameMapping() {
        int totalStates = recorder.getTotalSteps();
        framesToStateIndex = new int[TOTAL_FRAMES];

        // Map each frame to a state index
        for (int i = 0; i < TOTAL_FRAMES; i++) {
            framesToStateIndex[i] = (int) (((double) i / (TOTAL_FRAMES - 1)) * (totalStates - 1));
        }
    }

    private void resetAnimation() {
        currentFrame = 0;
        showPath = false;
        updateExploredStopsForCurrentFrame();
    }

    private void updateExploredStopsForCurrentFrame() {
        if (recorder == null || framesToStateIndex == null || currentFrame >= framesToStateIndex.length) {
            return;
        }

        int stateIndex = framesToStateIndex[currentFrame];
        currentExploredStops.clear();

        // Add all states up to current index
        for (int i = 0; i <= stateIndex && i < recorder.exploredStates.size(); i++) {
            currentExploredStops.add(recorder.exploredStates.get(i));
        }

        // Show path only after the path is found
        showPath = pathFoundFrame > 0 && currentFrame >= pathFoundFrame;
    }

    // Convert geographic coordinates to screen coordinates
    @Contract(value = "_, _, _, _ -> new", pure = true)
    private float @NotNull [] geoToScreen(
            double lat,
            double lon,
            int width,
            int height
    ) {
        float x = (float) ((lon - minLon) / (maxLon - minLon) * width);
        float y = height - (float) ((lat - minLat) / (maxLat - minLat) * height);
        return new float[]{x, y};
    }

    @Override
    public void init(@NotNull GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Initialize OpenGL settings
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_LINE_SMOOTH);

        // Create text renderer
        textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 12));
        glut = new GLUT();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        if (textRenderer != null) textRenderer.dispose();
    }

    @Override
    public void display(@NotNull GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        int width = drawable.getSurfaceWidth();
        int height = drawable.getSurfaceHeight();

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, width, height, 0, -1, 1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        if (startStop == null || endStop == null || recorder == null) {
            // Draw loading text
            textRenderer.beginRendering(width, height);
            textRenderer.setColor(0, 0, 0, 1);
            textRenderer.draw("Load algorithm data to begin visualization", 10, 20);
            textRenderer.endRendering();
            return;
        }

        // Draw background stops
        gl.glPointSize(2.0f);
        gl.glBegin(GL2.GL_POINTS);
        gl.glColor4f(0.9f, 0.9f, 0.9f, 0.5f); // Light gray

        for (Stop stop : stops.values()) {
            // Skip important stops
            if (pathIntermediateStops.contains(stop.stopId) ||
                    stop.stopId.equals(recorder.startStopId) ||
                    stop.stopId.equals(recorder.endStopId) ||
                    currentExploredStops.contains(stop.stopId)) {
                continue;
            }

            float[] pos = geoToScreen(stop.latitude, stop.longitude, width, height);
            gl.glVertex2f(pos[0], pos[1]);
        }
        gl.glEnd();

        // Draw explored stops
        gl.glPointSize(5.0f);
        gl.glBegin(GL2.GL_POINTS);
        gl.glColor4f(0.8f, 0.8f, 1.0f, 0.8f); // Light blue

        for (String stopId : currentExploredStops) {
            Stop stop = stops.get(stopId);
            if (stop != null) {
                float[] pos = geoToScreen(stop.latitude, stop.longitude, width, height);
                gl.glVertex2f(pos[0], pos[1]);
            }
        }

        gl.glEnd();

        // Draw path if it's time to show it
        if (showPath) {
            // Draw path lines
            gl.glLineWidth(3.0f);
            gl.glColor4f(0.0f, 0.0f, 1.0f, 0.8f); // Blue
            gl.glBegin(GL2.GL_LINES);

            for (Transition transition : finalPath) {
                Stop fromStop = stops.get(transition.fromStop());
                Stop toStop = stops.get(transition.toStop());

                if (fromStop != null && toStop != null) {
                    float[] fromPos = geoToScreen(fromStop.latitude, fromStop.longitude, width, height);
                    float[] toPos = geoToScreen(toStop.latitude, toStop.longitude, width, height);

                    gl.glVertex2f(fromPos[0], fromPos[1]);
                    gl.glVertex2f(toPos[0], toPos[1]);
                }
            }
            gl.glEnd();

            // Draw path intermediate stops
            gl.glPointSize(8.0f);
            gl.glBegin(GL2.GL_POINTS);
            gl.glColor4f(0.4f, 0.4f, 1.0f, 0.9f); // Darker blue

            for (String stopId : pathIntermediateStops) {
                if (currentExploredStops.contains(stopId)) {
                    Stop stop = stops.get(stopId);
                    if (stop != null) {
                        float[] pos = geoToScreen(stop.latitude, stop.longitude, width, height);
                        gl.glVertex2f(pos[0], pos[1]);
                    }
                }
            }
            gl.glEnd();
        }

        // Draw start and end stops
        gl.glPointSize(12.0f);
        gl.glBegin(GL2.GL_POINTS);

        // Start stop
        gl.glColor4f(0.0f, 0.8f, 0.0f, 1.0f); // Green
        float[] startPos = geoToScreen(startStop.latitude, startStop.longitude, width, height);
        gl.glVertex2f(startPos[0], startPos[1]);

        // End stop
        gl.glColor4f(0.8f, 0.0f, 0.0f, 1.0f); // Red
        float[] endPos = geoToScreen(endStop.latitude, endStop.longitude, width, height);
        gl.glVertex2f(endPos[0], endPos[1]);

        gl.glEnd();

        // Draw progress bar
        int barWidth = width - 40;
        int barHeight = 20;
        int barX = 20;
        int barY = height - 40;

        // Progress bar border
        gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f); // Black
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2f(barX, barY);
        gl.glVertex2f(barX + barWidth, barY);
        gl.glVertex2f(barX + barWidth, barY + barHeight);
        gl.glVertex2f(barX, barY + barHeight);
        gl.glEnd();

        // Progress bar fill
        float progress = (float) currentFrame / (TOTAL_FRAMES - 1);
        int progressWidth = (int) (barWidth * progress);

        gl.glColor4f(0.0f, 0.6f, 0.0f, 0.8f); // Green
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2f(barX, barY);
        gl.glVertex2f(barX + progressWidth, barY);
        gl.glVertex2f(barX + progressWidth, barY + barHeight);
        gl.glVertex2f(barX, barY + barHeight);
        gl.glEnd();

        // Draw text
        textRenderer.beginRendering(width, height);
        textRenderer.setColor(0, 0, 0, 1);

        // Legend in top-left corner
        int legendX = 20;
        int legendY = 20;
        int lineHeight = 20;

        textRenderer.draw("Start Stop: " + startStop.name, legendX, legendY);
        textRenderer.draw("End Stop: " + endStop.name, legendX, legendY + lineHeight);
        textRenderer.draw("Explored Stops: " + currentExploredStops.size(), legendX, legendY + lineHeight * 2);

        // Only show path info if the path is visible
        if (showPath) {
            textRenderer.draw("Path Found: " + finalPath.size() + " segments", legendX, legendY + lineHeight * 3);
        } else if (currentFrame < pathFoundFrame && pathFoundFrame > 0) {
            textRenderer.draw("Searching for path...", legendX, legendY + lineHeight * 3);
        }

        // Progress text
        int stateIndex = framesToStateIndex[currentFrame];
        float timeSeconds = (float) currentFrame / FPS;
        String progressText = String.format("Step %d/%d (%.1f sec)",
                stateIndex, recorder.getTotalSteps() - 1, timeSeconds);

        int textWidth = (int) textRenderer.getBounds(progressText).getWidth();
        textRenderer.draw(progressText, barX + (barWidth - textWidth) / 2, barY + 5);

        textRenderer.endRendering();

        // Advance animation
        if (animator.isAnimating()) {
            currentFrame++;
            if (currentFrame >= TOTAL_FRAMES) {
                currentFrame = TOTAL_FRAMES - 1;
                animator.stop();
            }
            updateExploredStopsForCurrentFrame();
        }
    }

    @Override
    public void reshape(
            @NotNull GLAutoDrawable drawable,
            int x,
            int y,
            int width,
            int height
    ) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }

    public void waitForCompletion() {
        try {
            completionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}