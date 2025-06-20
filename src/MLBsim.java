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

    // 物理常數等...
    private static final double GRAVITY_FT_S2 = 32.17;
    private static final double PITCHER_MOUND_DISTANCE_FT = 60.5;
    private static final double HOME_PLATE_FRONT_FT = 1.417;
    private double x_ft, y_ft, z_ft;
    private double vx, vy, vz;
    private double ax, ay;
    private long lastFrameTime;
    private String pitchType = "none";
    private boolean isPitching = false;
    private boolean ballReachedCatcher = false;
    private boolean isPaused = false;
    private boolean isHittingMode = false;
    private int countdown = 0;
    private String hitResult = null;
    private boolean swingAttempted = false;
    private double aimX_ft = 0;
    private double aimY_ft = 2.5;
    private Point mousePos;
    private double startX_ft, startY_ft, startZ_ft;
    private double endZ_ft = HOME_PLATE_FRONT_FT;
    private final int windowWidth = 1000;
    private final int windowHeight = 700;
    private final int vanishingPointX = windowWidth / 2;
    private final int vanishingPointY = windowHeight / 2;
    private final double cameraY_ft = 2.5;
    private final double cameraZ_ft = -4.0;
    private final double focalLength = 700;
    private final Random random = new Random();
    private BufferedImage ballImage;
    private final double strikeZoneLeft_ft = -0.83;
    private final double strikeZoneRight_ft = 0.83;
    private final double strikeZoneTop_ft = 3.5;
    private final double strikeZoneBottom_ft = 1.5;

    private static class PitchData {
        final double releaseSpeed_mph, pfx_x_in, pfx_z_in, release_pos_x_ft, release_pos_z_ft;
        PitchData(double s, double px, double pz, double rx, double rz) {
            releaseSpeed_mph = s; pfx_x_in = px; pfx_z_in = pz; release_pos_x_ft = rx; release_pos_z_ft = rz;
        }
    }
    private final Map<String, PitchData> pitchDatabase = new HashMap<>();

    public MLBsim(boolean hittingMode, JFrame frame) {
        this.isHittingMode = hittingMode;
        this.mainFrame = frame;
        
        initializePitchDatabase();
        
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
        // =================================================================
        //  *** 修改點：恢復使用 KeyListener ***
        // =================================================================
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
        
        timer = new Timer(16, this::actionPerformed);
        timer.start();

        try {
            ballImage = ImageIO.read(new File("ball.png"));
        } catch (IOException e) {
            System.err.println("Failed to load ball image: " + e.getMessage());
        }
        
        if (isHittingMode) countdown = 180;
        resetPitch();
    }
    
    // ... (所有 initialize... 和計算方法維持不變) ...
    private void initializePitchDatabase() { initialize_AllPitches(this.pitchDatabase); }
    private void initialize_AllPitches(Map<String, PitchData> db) { db.clear(); db.put("FF", new PitchData(94.0, -6.5, 16.5, -2.0, 6.0)); db.put("SI", new PitchData(93.0, -9.5, 7.5, -2.2, 5.8)); db.put("FC", new PitchData(89.0, 1.5, 6.0, -1.5, 6.1)); db.put("SL", new PitchData(85.0, 5.0, 1.5, -1.8, 5.9)); db.put("SW", new PitchData(83.0, 15.0, -2.0, -2.0, 5.9)); db.put("CU", new PitchData(79.0, 9.0, -10.0, -1.5, 6.2)); db.put("KC", new PitchData(81.0, 7.5, -8.0, -1.6, 6.1)); db.put("CH", new PitchData(84.5, -8.0, 5.5, -2.2, 5.8)); db.put("FS", new PitchData(86.0, -5.0, 2.5, -2.0, 6.0)); }
    private void initialize_ZackWheeler(Map<String, PitchData> db) { db.clear(); db.put("FF", new PitchData(95.5, -7.8, 15.5, -1.8, 6.1)); db.put("SI", new PitchData(94.8, -11.5, 9.2, -1.9, 6.0)); db.put("SL", new PitchData(91.2, 2.5, 5.1, -1.7, 6.1)); db.put("CU", new PitchData(81.5, 8.5, -7.0, -1.5, 6.2)); }
    private void initialize_MaxFried(Map<String, PitchData> db) { db.clear(); db.put("CU", new PitchData(75.0, -14.5, -9.8, 2.0, 6.0)); db.put("SI", new PitchData(94.0, 11.0, 7.0, 1.9, 5.9)); db.put("FF", new PitchData(94.2, 8.5, 15.0, 1.8, 6.0)); db.put("SL", new PitchData(85.0, -7.2, 1.0, 1.9, 6.0)); db.put("CH", new PitchData(87.5, 10.5, 4.5, 2.1, 5.8)); }
    private void initialize_ShoheiOhtani(Map<String, PitchData> db) { db.clear(); db.put("SW", new PitchData(85.5, 16.5, -1.5, -1.9, 6.0)); db.put("FS", new PitchData(88.0, -8.0, 1.8, -2.0, 5.9)); db.put("FF", new PitchData(97.2, -7.0, 16.0, -1.8, 6.1)); db.put("CU", new PitchData(78.0, 10.0, -9.5, -1.7, 6.2)); db.put("FC", new PitchData(91.5, 2.0, 4.8, -1.8, 6.1)); }
    private void initialize_PaulSkenes(Map<String, PitchData> db) { db.clear(); db.put("FF", new PitchData(100.1, -8.0, 16.8, -1.9, 6.0)); db.put("FS", new PitchData(94.5, -12.5, 4.0, -2.0, 5.8)); db.put("SL", new PitchData(86.5, 6.5, 2.0, -1.8, 6.1)); db.put("CU", new PitchData(84.0, 7.0, -6.5, -1.7, 6.2)); }
    private Point project3D(double objX_ft, double objY_ft, double objZ_ft) { double deltaX = objX_ft - 0; double deltaY = objY_ft - cameraY_ft; double deltaZ = objZ_ft - cameraZ_ft; if (deltaZ <= 0.1) return null; double projectedX = (deltaX * focalLength) / deltaZ; double projectedY = (deltaY * focalLength) / deltaZ; int screenX = vanishingPointX + (int) projectedX; int screenY = vanishingPointY - (int) projectedY; return new Point(screenX, screenY); }
    private int calculateBallSize(double objZ_ft) { double deltaZ = objZ_ft - cameraZ_ft; if (deltaZ <= 0) return 0; double visualSize = (0.24 * focalLength) / deltaZ; return Math.max(2, (int)visualSize); }
    private void calculateRealisticTrajectory(String type) { PitchData data = pitchDatabase.get(type); if (data == null) data = pitchDatabase.values().iterator().next(); this.pitchType = type; startX_ft = data.release_pos_x_ft; startY_ft = data.release_pos_z_ft; startZ_ft = PITCHER_MOUND_DISTANCE_FT; x_ft = startX_ft; y_ft = startY_ft; z_ft = startZ_ft; double releaseSpeed_fts = data.releaseSpeed_mph * 1.467; double pfx_x_ft = data.pfx_x_in / 12.0; double pfx_z_ft = data.pfx_z_in / 12.0; double flightDistance = startZ_ft - endZ_ft; double flightTime = flightDistance / releaseSpeed_fts; this.ax = (2 * pfx_x_ft) / (flightTime * flightTime); this.ay = (2 * pfx_z_ft) / (flightTime * flightTime); double targetX, targetY; if (isHittingMode) { if (random.nextDouble() < 0.6) { targetX = random.nextDouble() * (strikeZoneRight_ft - strikeZoneLeft_ft) + strikeZoneLeft_ft; targetY = random.nextDouble() * (strikeZoneTop_ft - strikeZoneBottom_ft) + strikeZoneBottom_ft; } else { double margin = 0.5; targetX = (random.nextBoolean() ? 1 : -1) * (strikeZoneRight_ft + margin); targetY = random.nextDouble() * (strikeZoneTop_ft + margin - (strikeZoneBottom_ft - margin)) + (strikeZoneBottom_ft - margin); } } else { targetX = this.aimX_ft; targetY = this.aimY_ft; } double totalVerticalAcceleration = ay - GRAVITY_FT_S2; this.vx = ((targetX - startX_ft) / flightTime) - (0.5 * ax * flightTime); this.vy = ((targetY - startY_ft) / flightTime) - (0.5 * totalVerticalAcceleration * flightTime); this.vz = releaseSpeed_fts; }
    private void actionPerformed(ActionEvent e) { if (isPaused) { lastFrameTime = System.nanoTime(); return; } long currentTime = System.nanoTime(); double frameTime = (currentTime - lastFrameTime) / 1_000_000_000.0; lastFrameTime = currentTime; frameTime = Math.min(frameTime, 0.05); if (isHittingMode && !isPitching && hitResult == null) { if (countdown > 0) { countdown--; } else { startPitch(randomPitchType()); } } if (isPitching && !ballReachedCatcher) { double prev_x = x_ft; double prev_y = y_ft; double prev_z = z_ft; vx += ax * frameTime; vy += (ay - GRAVITY_FT_S2) * frameTime; x_ft += vx * frameTime; y_ft += vy * frameTime; z_ft -= vz * frameTime; if (z_ft <= endZ_ft) { double z_travel_in_frame = prev_z - z_ft; if (z_travel_in_frame > 0) { double fraction = (prev_z - endZ_ft) / z_travel_in_frame; x_ft = prev_x + (x_ft - prev_x) * fraction; y_ft = prev_y + (y_ft - prev_y) * fraction; } z_ft = endZ_ft; isPitching = false; ballReachedCatcher = true; boolean isStrike = x_ft >= strikeZoneLeft_ft && x_ft <= strikeZoneRight_ft && y_ft >= strikeZoneBottom_ft && y_ft <= strikeZoneTop_ft; if (isHittingMode && !swingAttempted) { hitResult = isStrike ? "Strike" : "Ball"; } } } repaint(); }
    private void resetPitch() { vx=vy=vz=ax=ay=0; isPitching = ballReachedCatcher = swingAttempted = false; pitchType = "none"; hitResult = null; if (isHittingMode) { countdown = 180; } else { PitchData data = pitchDatabase.values().iterator().next(); x_ft = data.release_pos_x_ft; y_ft = data.release_pos_z_ft; z_ft = PITCHER_MOUND_DISTANCE_FT; } lastFrameTime = System.nanoTime(); repaint(); }
    private String randomPitchType() { Object[] types = pitchDatabase.keySet().toArray(); return (String) types[random.nextInt(types.length)]; }
    private void startPitch(String type) { if (!pitchDatabase.containsKey(type)) { System.err.println("Pitch type " + type + " not in current pitcher's arsenal."); return; } if (!isPitching && !isPaused) { this.isPitching = true; this.ballReachedCatcher = false; this.swingAttempted = false; this.hitResult = null; calculateRealisticTrajectory(type); } }
    @Override protected void paintComponent(Graphics g) { super.paintComponent(g); Graphics2D g2d = (Graphics2D) g; g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2d.setColor(new Color(135, 206, 235)); g2d.fillRect(0, 0, windowWidth, windowHeight); drawField(g2d); drawStrikeZone(g2d); drawPitcherMound(g2d); if (!isHittingMode && !isPitching) { drawAimingReticle(g2d); } drawUI(g2d); if (isPitching || ballReachedCatcher) { drawBall(g2d); } if (isPaused) { g2d.setColor(new Color(0, 0, 0, 150)); g2d.fillRect(0, 0, windowWidth, windowHeight); g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 48)); g2d.drawString("Paused", (windowWidth - g2d.getFontMetrics().stringWidth("Paused")) / 2, windowHeight / 2); } }
    private void drawField(Graphics2D g2d) { Point groundStart = project3D(0, 0, PITCHER_MOUND_DISTANCE_FT); Point groundEnd = project3D(0, 0, 0); if(groundStart != null && groundEnd != null) { g2d.setColor(new Color(188, 143, 143)); Polygon dirtArea = new Polygon(); dirtArea.addPoint(groundStart.x - 200, groundStart.y); dirtArea.addPoint(groundStart.x + 200, groundStart.y); dirtArea.addPoint(windowWidth, groundEnd.y); dirtArea.addPoint(0, groundEnd.y); g2d.fillPolygon(dirtArea); } }
    private void drawStrikeZone(Graphics2D g2d) { g2d.setStroke(new BasicStroke(3)); g2d.setColor(new Color(255, 255, 255, 100)); Point tl = project3D(strikeZoneLeft_ft, strikeZoneTop_ft, endZ_ft); Point tr = project3D(strikeZoneRight_ft, strikeZoneTop_ft, endZ_ft); Point bl = project3D(strikeZoneLeft_ft, strikeZoneBottom_ft, endZ_ft); Point br = project3D(strikeZoneRight_ft, strikeZoneBottom_ft, endZ_ft); if (tl != null && tr != null && bl != null && br != null) { g2d.drawLine(tl.x, tl.y, tr.x, tr.y); g2d.drawLine(tr.x, tr.y, br.x, br.y); g2d.drawLine(br.x, br.y, bl.x, bl.y); g2d.drawLine(bl.x, bl.y, tl.x, tl.y); } }
    private void drawPitcherMound(Graphics2D g2d) { Point moundCenter = project3D(0, 0, PITCHER_MOUND_DISTANCE_FT); if (moundCenter != null) { int moundSize = calculateBallSize(PITCHER_MOUND_DISTANCE_FT) * 10; g2d.setColor(new Color(160, 82, 45)); g2d.fillOval(moundCenter.x - moundSize/2, moundCenter.y - moundSize/4, moundSize, moundSize/2); } }
    private Color getHitResultColor(String result) { if (result == null) return Color.RED; switch (result) { case "Perfect": return Color.CYAN; case "A bit early": case "A bit late": return Color.GREEN; case "Early": case "Late": return Color.ORANGE; default: return Color.RED; } }
    private void drawUI(Graphics2D g2d) { g2d.setColor(new Color(0, 0, 0, 150)); g2d.fillRoundRect(10, 10, 420, 160, 10, 10); g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 16)); g2d.drawString(isHittingMode ? "Hitting Mode" : "Pitching Mode (Manual Aim)", 20, 30); if (isHittingMode) { g2d.setFont(new Font("Arial", Font.PLAIN, 12)); g2d.drawString("Space=Swing | M=Menu | ESC=Pause", 20, 50); if (countdown > 0) { g2d.setFont(new Font("Arial", Font.BOLD, 24)); g2d.setColor(Color.YELLOW); g2d.drawString("Pitch in: " + ((countdown / 60) + 1), windowWidth / 2 - 60, 60); } else if (hitResult != null) { g2d.setFont(new Font("Arial", Font.BOLD, 36)); g2d.setColor(getHitResultColor(hitResult)); g2d.drawString(hitResult, windowWidth / 2 - g2d.getFontMetrics().stringWidth(hitResult)/2, 80); } } else { g2d.setFont(new Font("Arial", Font.PLAIN, 11)); String pitchList = "Pitches: "; Object[] availablePitches = pitchDatabase.keySet().toArray(); for(int i = 0; i < availablePitches.length; i++) { pitchList += (i + 1) + "=" + availablePitches[i] + " "; } g2d.drawString(pitchList, 20, 50); g2d.setFont(new Font("Arial", Font.PLAIN, 12)); g2d.drawString("F1=All | F2=Wheeler | F3=Fried | F4=Ohtani | F5=Skenes", 20, 70); g2d.drawString("M=Menu | Space=Reset | ESC=Pause", 20, 90); PitchData data = pitchDatabase.get(pitchType.equals("none") ? pitchDatabase.keySet().iterator().next() : pitchType); if (data != null) { g2d.drawString("Speed: " + String.format("%.1f mph", data.releaseSpeed_mph), 20, 110); } if(isPitching) g2d.drawString("Pitching: " + pitchType, 20, 130); else if(ballReachedCatcher) g2d.drawString("Ball reached catcher.", 20, 130); else g2d.drawString("Select Pitcher (F1-F5), Aim, Select Pitch.", 20, 130); } }
    private void drawBall(Graphics2D g2d) { Point ballPos = project3D(x_ft, y_ft, z_ft); if (ballPos == null) return; int ballSize = calculateBallSize(z_ft); if (ballImage != null) { g2d.drawImage(ballImage, ballPos.x - ballSize/2, ballPos.y - ballSize/2, ballSize, ballSize, null); } else { g2d.setColor(Color.WHITE); g2d.fillOval(ballPos.x - ballSize/2, ballPos.y - ballSize/2, ballSize, ballSize); } }
    private void togglePause() { isPaused = !isPaused; repaint(); }
    private void updateAimPosition() { if (mousePos == null) return; double screenAimMinX = 200, screenAimMaxX = windowWidth - 200; double screenAimMinY = 150, screenAimMaxY = windowHeight - 150; double worldTargetMinX = -2.0, worldTargetMaxX = 2.0; double worldTargetMinY = 0.5, worldTargetMaxY = 4.5; double percentX = (mousePos.x - screenAimMinX) / (screenAimMaxX - screenAimMinX); double percentY = (mousePos.y - screenAimMinY) / (screenAimMaxY - screenAimMinY); percentX = Math.max(0, Math.min(1, percentX)); percentY = Math.max(0, Math.min(1, percentY)); aimX_ft = worldTargetMinX + percentX * (worldTargetMaxX - worldTargetMinX); aimY_ft = worldTargetMinY + (1 - percentY) * (worldTargetMaxY - worldTargetMinY); }
    private void drawAimingReticle(Graphics2D g2d) { Point screenPos = project3D(aimX_ft, aimY_ft, endZ_ft); if (screenPos == null) return; g2d.setColor(new Color(255, 0, 0, 150)); g2d.setStroke(new BasicStroke(2)); int size = 15; g2d.drawLine(screenPos.x - size, screenPos.y, screenPos.x + size, screenPos.y); g2d.drawLine(screenPos.x, screenPos.y - size, screenPos.x, screenPos.y + size); }
    
    // =================================================================
    //  *** 新增：handleKeyPress 方法，整合所有按鍵邏輯 ***
    // =================================================================
    private void handleKeyPress(KeyEvent e) {
        // 通用按鍵
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            togglePause();
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_M) {
            showStartScreen(mainFrame);
            return;
        }
        if (isPaused) return;

        // 分模式處理
        if (isHittingMode) {
            handleHittingModeKeys(e);
        } else {
            handlePitchingModeKeys(e);
        }
        repaint();
    }

    private void handleHittingModeKeys(KeyEvent e) {
        if (hitResult != null) {
            resetPitch();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE && isPitching && !swingAttempted) {
            swingAttempted = true;
            double swingTimeRatio = (startZ_ft - z_ft) / (startZ_ft - endZ_ft);
            
            final double PERFECT_START = 0.91, PERFECT_END = 0.97;
            final double GOOD_START = 0.86, GOOD_END = 1.0;
            final double OK_START = 0.82, OK_END = 1.04;

            if (swingTimeRatio >= PERFECT_START && swingTimeRatio <= PERFECT_END) {
                hitResult = "Perfect";
            } else if (swingTimeRatio > PERFECT_END && swingTimeRatio <= GOOD_END) {
                hitResult = "A bit late";
            } else if (swingTimeRatio < PERFECT_START && swingTimeRatio >= GOOD_START) {
                hitResult = "A bit early";
            } else if (swingTimeRatio > GOOD_END && swingTimeRatio <= OK_END) {
                hitResult = "Late";
            } else if (swingTimeRatio < GOOD_START && swingTimeRatio >= OK_START) {
                hitResult = "Early";
            } else {
                hitResult = "Miss";
            }
        }
    }

    private void handlePitchingModeKeys(KeyEvent e) {
        // 處理投手選擇
        switch (e.getKeyCode()) {
            case KeyEvent.VK_F1: initialize_AllPitches(pitchDatabase); resetPitch(); return;
            case KeyEvent.VK_F2: initialize_ZackWheeler(pitchDatabase); resetPitch(); return;
            case KeyEvent.VK_F3: initialize_MaxFried(pitchDatabase); resetPitch(); return;
            case KeyEvent.VK_F4: initialize_ShoheiOhtani(pitchDatabase); resetPitch(); return;
            case KeyEvent.VK_F5: initialize_PaulSkenes(pitchDatabase); resetPitch(); return;
            case KeyEvent.VK_SPACE: resetPitch(); return;
        }

        // 處理球種選擇
        String[] availablePitches = pitchDatabase.keySet().toArray(new String[0]);
        int pitchIndex = -1;
        int keyCode = e.getKeyCode();

        if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {
            pitchIndex = keyCode - KeyEvent.VK_1;
        }
        
        if (pitchIndex != -1 && pitchIndex < availablePitches.length) {
            startPitch(availablePitches[pitchIndex]);
        }
    }
    
    // =================================================================
    //  *** Main and StartScreen (維持不變) ***
    // =================================================================
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