import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class MLBsim extends JPanel {
    private Timer timer;
    private JFrame mainFrame;

    // 物理常數
    private static final double PITCHER_MOUND_DISTANCE_FT = 60.5;
    private static final double HOME_PLATE_FRONT_FT = 1.417;

    // 球的狀態變數
    private double x_ft, y_ft, z_ft;
    private double vx, vy, vz;
    private double ax, ay;

    // 時間管理變數
    private long lastFrameTime;

    // 遊戲狀態變數
    private String pitchType = "none";
    private boolean isPitching = false;
    private boolean ballReachedCatcher = false;
    private boolean isPaused = false;
    private boolean isHittingMode = false;
    private int countdown = 0;
    private String hitResult = null;
    private boolean swingAttempted = false;
    
    // 手動瞄準相關變數
    private double aimX_ft = 0;
    private double aimY_ft = 2.5;
    private Point mousePos;

    // 軌跡起點
    private double startX_ft, startY_ft, startZ_ft;
    private double endZ_ft = HOME_PLATE_FRONT_FT;

    // 顯示與攝影機參數
    private final int windowWidth = 1000;
    private final int windowHeight = 700;
    private final int vanishingPointX = windowWidth / 2;
    private final int vanishingPointY = windowHeight / 2;
    private final double cameraY_ft = 2.5;
    private final double cameraZ_ft = -4.0;
    private final double focalLength = 700;

    // 其他工具
    private final Random random = new Random();
    private BufferedImage ballImage;

    // 好球帶定義
    private final double strikeZoneLeft_ft = -0.83;
    private final double strikeZoneRight_ft = 0.83;
    private final double strikeZoneTop_ft = 3.5;
    private final double strikeZoneBottom_ft = 1.5;

    // 球種數據庫
    private static class PitchData {
        final double releaseSpeed_mph, horizontalMovement_in, verticalMovement_in, release_pos_x_ft, release_pos_z_ft;
        PitchData(double speed, double h_mov, double v_mov, double rel_x, double rel_z) {
            this.releaseSpeed_mph = speed;
            this.horizontalMovement_in = h_mov;
            this.verticalMovement_in = v_mov;
            this.release_pos_x_ft = rel_x;
            this.release_pos_z_ft = rel_z;
        }
    }
    private final Map<String, PitchData> pitchDatabase = new HashMap<>();

    public MLBsim(boolean hittingMode, JFrame frame) {
        this.isHittingMode = hittingMode;
        this.mainFrame = frame;
        
        initializePitchDatabase();
        setupKeyBindings();
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!isHittingMode && !isPitching) {
                    mousePos = e.getPoint();
                    updateAimPosition();
                    repaint();
                }
            }
        });

        setFocusable(true);
        timer = new Timer(16, this::actionPerformed);
        timer.start();

        try {
            ballImage = ImageIO.read(new File("src/ball.png"));
        } catch (IOException e) {
            System.err.println("Failed to load ball image: " + e.getMessage());
        }
        
        if (isHittingMode) countdown = 180;
        resetPitch();
    }
    
    private void initializePitchDatabase() {
        initialize_AllPitches(this.pitchDatabase);
    }
    
    private void initialize_AllPitches(Map<String, PitchData> db) {
        db.clear();
        db.put("FF", new PitchData(94.0, -7.0, -15.0, -2.0, 6.0));
        db.put("SI", new PitchData(93.0, -11.0, -23.0, -2.2, 5.8));
        db.put("FC", new PitchData(89.0, 2.0, -25.0, -1.5, 6.1));
        db.put("SL", new PitchData(85.0, 5.5, -36.0, -1.8, 5.9));
        db.put("SW", new PitchData(83.0, 16.0, -40.0, -2.0, 5.9));
        db.put("CU", new PitchData(79.0, 9.5, -55.0, -1.5, 6.2));
        db.put("KC", new PitchData(81.0, 8.0, -50.0, -1.6, 6.1));
        db.put("CH", new PitchData(84.5, -9.0, -28.0, -2.2, 5.8));
        db.put("FS", new PitchData(86.0, -6.0, -34.0, -2.0, 6.0));
    }
    
    private void initialize_ZackWheeler(Map<String, PitchData> db) {
        db.clear();
        db.put("FF", new PitchData(95.2, -8.2, -15.5, -1.8, 6.1));
        db.put("SI", new PitchData(94.5, -10.0, -20.3, -1.9, 5.2));
        db.put("SL", new PitchData(90.8, 3.0, -24.0, -1.7, 6.1));
        db.put("CU", new PitchData(81.0, 9.0, -48.0, -1.5, 6.2));
    }

    private void initialize_MaxFried(Map<String, PitchData> db) {
        db.clear();
        db.put("CU", new PitchData(74.5, -15.0, -58.0, 2.0, 6.0));
        db.put("SI", new PitchData(93.8, 11.5, -23.5, 1.9, 5.9));
        db.put("FF", new PitchData(93.9, 9.0, -16.0, 1.8, 6.0));
        db.put("SL", new PitchData(84.7, -7.8, -35.0, 1.9, 6.0));
        db.put("CH", new PitchData(87.0, 11.0, -30.0, 2.1, 5.8));
    }

    private void initialize_ShoheiOhtani(Map<String, PitchData> db) {
        db.clear();
        db.put("SW", new PitchData(85.0, 17.0, -38.5, -1.9, 6.0));
        db.put("FS", new PitchData(87.8, -8.5, -35.0, -2.0, 5.9));
        db.put("FF", new PitchData(96.8, -7.5, -15.0, -1.8, 6.1));
        db.put("CU", new PitchData(77.5, 10.5, -54.0, -1.7, 6.2));
        db.put("FC", new PitchData(91.0, 2.5, -28.0, -1.8, 6.1));
    }

    private void initialize_PaulSkenes(Map<String, PitchData> db) {
        db.clear();
        db.put("FF", new PitchData(99.8, -8.6, -25.66, -2.4, 5.6));
        db.put("FS", new PitchData(95.1, -13.0, -30.0, -2.0, 5.8));
        db.put("SL", new PitchData(86.8, 7.0, -34.0, -1.8, 6.1));
        db.put("CU", new PitchData(83.5, 7.5, -46.0, -1.7, 6.2));
    }

    private void initialize_TylerRogers(Map<String, PitchData> db) {
        db.clear();
        db.put("SI", new PitchData(83.16, -0.99, -47.89, -3.82, 1.26));
        db.put("SL", new PitchData(73.94, 5.92, -32.58, -3.82, 1.30));
    }

    private Point project3D(double objX_ft, double objY_ft, double objZ_ft) {
        double deltaX = objX_ft - 0;
        double deltaY = objY_ft - cameraY_ft;
        double deltaZ = objZ_ft - cameraZ_ft;
        if (deltaZ <= 0.1) return null;
        double projectedX = (deltaX * focalLength) / deltaZ;
        double projectedY = (deltaY * focalLength) / deltaZ;
        int screenX = vanishingPointX + (int) projectedX;
        int screenY = vanishingPointY - (int) projectedY;
        return new Point(screenX, screenY);
    }
    
    private int calculateBallSize(double objZ_ft) {
        double deltaZ = objZ_ft - cameraZ_ft;
        if (deltaZ <= 0) return 0;
        double visualSize = (0.24 * focalLength) / deltaZ;
        return Math.max(2, (int)visualSize);
    }

    private void calculateRealisticTrajectory(String type) {
        PitchData data = pitchDatabase.get(type);
        if (data == null) data = pitchDatabase.values().iterator().next();
        this.pitchType = type;
        startX_ft = data.release_pos_x_ft;
        startY_ft = data.release_pos_z_ft;
        startZ_ft = PITCHER_MOUND_DISTANCE_FT;
        x_ft = startX_ft; y_ft = startY_ft; z_ft = startZ_ft;
        double releaseSpeed_fts = data.releaseSpeed_mph * 1.467;
        double pfx_x_ft = data.horizontalMovement_in / 12.0;
        double pfx_z_ft = data.verticalMovement_in / 12.0;
        double flightDistance = startZ_ft - endZ_ft;
        double flightTime = flightDistance / releaseSpeed_fts;
        this.ax = (2 * pfx_x_ft) / (flightTime * flightTime);
        this.ay = (2 * pfx_z_ft) / (flightTime * flightTime);
        double targetX, targetY;
        if (isHittingMode) {
            if (random.nextDouble() < 0.6) {
                targetX = random.nextDouble() * (strikeZoneRight_ft - strikeZoneLeft_ft) + strikeZoneLeft_ft;
                targetY = random.nextDouble() * (strikeZoneTop_ft - strikeZoneBottom_ft) + strikeZoneBottom_ft;
            } else {
                double margin = 0.5;
                targetX = (random.nextBoolean() ? 1 : -1) * (strikeZoneRight_ft + margin);
                targetY = random.nextDouble() * (strikeZoneTop_ft + margin - (strikeZoneBottom_ft - margin)) + (strikeZoneBottom_ft - margin);
            }
        } else {
            targetX = this.aimX_ft;
            targetY = this.aimY_ft;
        }
        this.vx = ((targetX - startX_ft) / flightTime) - (0.5 * ax * flightTime);
        this.vy = ((targetY - startY_ft) / flightTime) - (0.5 * ay * flightTime);
        this.vz = releaseSpeed_fts;
    }
    
    private void actionPerformed(ActionEvent e) {
        if (isPaused) { lastFrameTime = System.nanoTime(); return; }
        long currentTime = System.nanoTime();
        double frameTime = (currentTime - lastFrameTime) / 1_000_000_000.0;
        lastFrameTime = currentTime;
        frameTime = Math.min(frameTime, 0.05);

        if (isHittingMode && !isPitching && hitResult == null) {
            if (countdown > 0) { countdown--; } else { startPitch(randomPitchType()); }
        }
        
        if (isPitching && !ballReachedCatcher) {
            double prev_x = x_ft; double prev_y = y_ft; double prev_z = z_ft;
            vx += ax * frameTime;
            vy += ay * frameTime;
            x_ft += vx * frameTime;
            y_ft += vy * frameTime;
            z_ft -= vz * frameTime;
            
            if (z_ft <= endZ_ft) {
                double z_travel_in_frame = prev_z - z_ft;
                if (z_travel_in_frame > 0) {
                    double fraction = (prev_z - endZ_ft) / z_travel_in_frame;
                    x_ft = prev_x + (x_ft - prev_x) * fraction;
                    y_ft = prev_y + (y_ft - prev_y) * fraction;
                }
                z_ft = endZ_ft;
                isPitching = false;
                ballReachedCatcher = true;
                boolean isStrike = x_ft >= strikeZoneLeft_ft && x_ft <= strikeZoneRight_ft && y_ft >= strikeZoneBottom_ft && y_ft <= strikeZoneTop_ft;
                if (isHittingMode && !swingAttempted) {
                    hitResult = isStrike ? "Strike" : "Ball";
                }
            }
        }
        repaint();
    }

    private void resetPitch() {
        vx=vy=vz=ax=ay=0;
        isPitching = ballReachedCatcher = swingAttempted = false;
        pitchType = "none";
        hitResult = null;
        if (isHittingMode) { countdown = 180; }
        PitchData data = pitchDatabase.values().iterator().next();
        x_ft = data.release_pos_x_ft;
        y_ft = data.release_pos_z_ft;
        z_ft = PITCHER_MOUND_DISTANCE_FT;
        lastFrameTime = System.nanoTime();
        repaint();
    }
    
    private String randomPitchType() {
        Object[] types = pitchDatabase.keySet().toArray();
        return (String) types[random.nextInt(types.length)];
    }

    private void startPitch(String type) {
        if (!pitchDatabase.containsKey(type)) {
            System.err.println("Pitch type " + type + " not in current pitcher's arsenal.");
            return;
        }
        if (!isPitching && !isPaused) {
            this.isPitching = true;
            this.ballReachedCatcher = false;
            this.swingAttempted = false;
            this.hitResult = null;
            calculateRealisticTrajectory(type);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(135, 206, 235));
        g2d.fillRect(0, 0, windowWidth, windowHeight);
        drawField(g2d);
        drawStrikeZone(g2d);
        drawPitcherMound(g2d);
        if (!isHittingMode && !isPitching) {
            drawAimingReticle(g2d);
        }
        drawUI(g2d);
        if (isPitching || ballReachedCatcher) {
            drawBall(g2d);
        }
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, windowWidth, windowHeight);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("Paused", (windowWidth - g2d.getFontMetrics().stringWidth("Paused")) / 2, windowHeight / 2);
        }
    }

    private void drawField(Graphics2D g2d) {
        Point groundStart = project3D(0, 0, PITCHER_MOUND_DISTANCE_FT);
        Point groundEnd = project3D(0, 0, 0);
        if(groundStart != null && groundEnd != null) {
            g2d.setColor(new Color(188, 143, 143));
            Polygon dirtArea = new Polygon();
            dirtArea.addPoint(groundStart.x - 200, groundStart.y);
            dirtArea.addPoint(groundStart.x + 200, groundStart.y);
            dirtArea.addPoint(windowWidth, groundEnd.y);
            dirtArea.addPoint(0, groundEnd.y);
            g2d.fillPolygon(dirtArea);
        }
    }

    private void drawStrikeZone(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(new Color(255, 255, 255, 100));
        Point tl = project3D(strikeZoneLeft_ft, strikeZoneTop_ft, endZ_ft);
        Point tr = project3D(strikeZoneRight_ft, strikeZoneTop_ft, endZ_ft);
        Point bl = project3D(strikeZoneLeft_ft, strikeZoneBottom_ft, endZ_ft);
        Point br = project3D(strikeZoneRight_ft, strikeZoneBottom_ft, endZ_ft);
        if (tl != null && tr != null && bl != null && br != null) {
            g2d.drawLine(tl.x, tl.y, tr.x, tr.y);
            g2d.drawLine(tr.x, tr.y, br.x, br.y);
            g2d.drawLine(br.x, br.y, bl.x, bl.y);
            g2d.drawLine(bl.x, bl.y, tl.x, tl.y);
        }
    }

    private void drawPitcherMound(Graphics2D g2d) {
        Point moundCenter = project3D(0, 0, PITCHER_MOUND_DISTANCE_FT);
        if (moundCenter != null) {
            int moundSize = calculateBallSize(PITCHER_MOUND_DISTANCE_FT) * 10;
            g2d.setColor(new Color(160, 82, 45));
            g2d.fillOval(moundCenter.x - moundSize/2, moundCenter.y - moundSize/4, moundSize, moundSize/2);
        }
    }

    private Color getHitResultColor(String result) {
        if (result == null) return Color.RED;
        switch (result) {
            case "Perfect": return Color.CYAN;
            case "A bit early": case "A bit late": return Color.GREEN;
            case "Early": case "Late": return Color.ORANGE;
            default: return Color.RED;
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 500, 160, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString(isHittingMode ? "Hitting Mode" : "Pitching Mode (Manual Aim)", 20, 30);
        if (isHittingMode) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Space=Swing | M=Menu | ESC=Pause", 20, 50);
            if (countdown > 0) {
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.setColor(Color.YELLOW);
                g2d.drawString("Pitch in: " + ((countdown / 60) + 1), windowWidth / 2 - 60, 60);
            } else if (hitResult != null) {
                g2d.setFont(new Font("Arial", Font.BOLD, 36));
                g2d.setColor(getHitResultColor(hitResult));
                g2d.drawString(hitResult, windowWidth / 2 - g2d.getFontMetrics().stringWidth(hitResult)/2, 80);
            }
        } else {
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            String pitchList = "Pitches: ";
            Object[] availablePitches = pitchDatabase.keySet().toArray();
            for(int i = 0; i < availablePitches.length; i++) {
                pitchList += (i + 1) + "=" + availablePitches[i] + " ";
            }
            g2d.drawString(pitchList, 20, 50);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("F1=All | F2=Wheeler | F3=Fried | F4=Ohtani | F5=Skenes | F6=Rogers", 20, 70);
            g2d.drawString("M=Menu | Space=Reset | ESC=Pause", 20, 90);
            
            PitchData data = pitchDatabase.get(pitchType.equals("none") ? pitchDatabase.keySet().iterator().next() : pitchType);
            if (data != null) {
                g2d.drawString("Speed: " + String.format("%.1f mph", data.releaseSpeed_mph), 20, 110);
            }
            if(isPitching) g2d.drawString("Pitching: " + pitchType, 20, 130);
            else if(ballReachedCatcher) g2d.drawString("Ball reached catcher.", 20, 130);
            else g2d.drawString("Select Pitcher (F1-F6), Aim, Select Pitch.", 20, 130);
        }
    }

    private void drawBall(Graphics2D g2d) {
        Point ballPos = project3D(x_ft, y_ft, z_ft);
        if (ballPos == null) return;
        int ballSize = calculateBallSize(z_ft);
        if (ballImage != null) {
            g2d.drawImage(ballImage, ballPos.x - ballSize/2, ballPos.y - ballSize/2, ballSize, ballSize, null);
        } else {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(ballPos.x - ballSize/2, ballPos.y - ballSize/2, ballSize, ballSize);
        }
    }
    
    private void togglePause() { isPaused = !isPaused; repaint(); }
    
    private void updateAimPosition() {
        if (mousePos == null) return;
        double screenAimMinX = 200, screenAimMaxX = windowWidth - 200;
        double screenAimMinY = 150, screenAimMaxY = windowHeight - 150;
        double worldTargetMinX = -2.0, worldTargetMaxX = 2.0;
        double worldTargetMinY = 0.5, worldTargetMaxY = 4.5;
        double percentX = (mousePos.x - screenAimMinX) / (screenAimMaxX - screenAimMinX);
        double percentY = (mousePos.y - screenAimMinY) / (screenAimMaxY - screenAimMinY);
        percentX = Math.max(0, Math.min(1, percentX));
        percentY = Math.max(0, Math.min(1, percentY));
        aimX_ft = worldTargetMinX + percentX * (worldTargetMaxX - worldTargetMinX);
        aimY_ft = worldTargetMinY + (1 - percentY) * (worldTargetMaxY - worldTargetMinY);
    }
    
    private void drawAimingReticle(Graphics2D g2d) {
        Point screenPos = project3D(aimX_ft, aimY_ft, endZ_ft);
        if (screenPos == null) return;
        g2d.setColor(new Color(255, 0, 0, 150));
        g2d.setStroke(new BasicStroke(2));
        int size = 15;
        g2d.drawLine(screenPos.x - size, screenPos.y, screenPos.x + size, screenPos.y);
        g2d.drawLine(screenPos.x, screenPos.y - size, screenPos.x, screenPos.y + size);
    }
    
    private void setupKeyBindings() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "swingAction");
        actionMap.put("swingAction", new SwingAction());
        for (int i = 0; i <= 9; i++) {
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, 0), "pitchAction" + i);
            actionMap.put("pitchAction" + i, new PitchSelectAction(i));
        }
        for (int i = 0; i < 6; i++) {
             inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1 + i, 0), "pitcherAction" + (i + 1));
             actionMap.put("pitcherAction" + (i + 1), new PitcherSelectAction(i + 1));
        }
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "resetAction");
        actionMap.put("resetAction", new ResetAction());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "menuAction");
        actionMap.put("menuAction", new MenuAction());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "pauseAction");
        actionMap.put("pauseAction", new PauseAction());
    }

    private abstract class GameAction extends AbstractAction { }
    private class SwingAction extends GameAction { @Override public void actionPerformed(ActionEvent e) { if (isHittingMode) { if (hitResult != null) { resetPitch(); } else if (isPitching && !swingAttempted) { swingAttempted = true; double swingTimeRatio = (startZ_ft - z_ft) / (startZ_ft - endZ_ft); final double PERFECT_START = 0.91, PERFECT_END = 0.97; final double GOOD_START = 0.86, GOOD_END = 1.0; final double OK_START = 0.82, OK_END = 1.04; if (swingTimeRatio >= PERFECT_START && swingTimeRatio <= PERFECT_END) { hitResult = "Perfect"; } else if (swingTimeRatio > PERFECT_END && swingTimeRatio <= GOOD_END) { hitResult = "A bit late"; } else if (swingTimeRatio < PERFECT_START && swingTimeRatio >= GOOD_START) { hitResult = "A bit early"; } else if (swingTimeRatio > GOOD_END && swingTimeRatio <= OK_END) { hitResult = "Late"; } else if (swingTimeRatio < GOOD_START && swingTimeRatio >= OK_START) { hitResult = "Early"; } else { hitResult = "Miss"; } } } } }
    private class PitchSelectAction extends GameAction { private int pitchIndex; public PitchSelectAction(int index) { this.pitchIndex = index == 0 ? 9 : index - 1; } @Override public void actionPerformed(ActionEvent e) { if (!isHittingMode) { String[] availablePitches = pitchDatabase.keySet().toArray(new String[0]); if (pitchIndex < availablePitches.length) { startPitch(availablePitches[pitchIndex]); } } } }
    private class PitcherSelectAction extends GameAction {
        private int pitcherNum;
        public PitcherSelectAction(int num) { this.pitcherNum = num; }
        @Override public void actionPerformed(ActionEvent e) {
            if (!isHittingMode) {
                switch (pitcherNum) {
                    case 1: initialize_AllPitches(pitchDatabase); break;
                    case 2: initialize_ZackWheeler(pitchDatabase); break;
                    case 3: initialize_MaxFried(pitchDatabase); break;
                    case 4: initialize_ShoheiOhtani(pitchDatabase); break;
                    case 5: initialize_PaulSkenes(pitchDatabase); break;
                    case 6: initialize_TylerRogers(pitchDatabase); break;
                }
                resetPitch();
            }
        }
    }
    private class ResetAction extends GameAction { @Override public void actionPerformed(ActionEvent e) { if (!isHittingMode) resetPitch(); } }
    private class MenuAction extends GameAction { @Override public void actionPerformed(ActionEvent e) { showStartScreen(mainFrame); } }
    private class PauseAction extends GameAction { @Override public void actionPerformed(ActionEvent e) { togglePause(); } }
    
    static class StartScreen extends JPanel {
        public StartScreen(JFrame frame) {
            setLayout(null); setBackground(new Color(135, 206, 235));
            JLabel title = new JLabel("Baseball Simulator"); title.setFont(new Font("Arial", Font.BOLD, 36)); title.setForeground(Color.WHITE); title.setBounds(300, 50, 400, 50); title.setHorizontalAlignment(SwingConstants.CENTER); add(title);
            JButton pitchingButton = new JButton("Pitching"); pitchingButton.setBounds(300, 500, 200, 50); pitchingButton.addActionListener(e -> showGamePanel(frame, false)); add(pitchingButton);
            JButton hittingButton = new JButton("Hitting"); hittingButton.setBounds(500, 500, 200, 50); hittingButton.addActionListener(e -> showGamePanel(frame, true)); add(hittingButton);
        }
    }

    public static void showStartScreen(JFrame frame) {
        frame.getContentPane().removeAll();
        StartScreen startScreen = new StartScreen(frame);
        frame.add(startScreen);
        frame.revalidate();
        frame.repaint();
    }

    public static void showGamePanel(JFrame frame, boolean isHittingMode) {
        frame.getContentPane().removeAll();
        MLBsim panel = new MLBsim(isHittingMode, frame);
        frame.add(panel);
        frame.revalidate();
        frame.repaint();
        panel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Baseball Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            
            showStartScreen(frame);
            
            frame.setVisible(true);
        });
    }
}