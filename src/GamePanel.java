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
import java.util.Arrays; // 新增導入

import data.Pitcher; // 導入 Pitcher 類別
import data.TrajectoryData; // 導入 TrajectoryData 類別

public class GamePanel extends JPanel { // 將 MLBsim 改名為 GamePanel
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
    private final Map<String, TrajectoryData> pitchDatabase = new HashMap<>(); // 修改類型為 TrajectoryData
    private DatabaseManager dbManager; // 新增 DatabaseManager 實例
    private Pitcher currentPitcher; // 新增變數來儲存當前投手


    public GamePanel(boolean hittingMode, JFrame frame, Pitcher selectedPitcher) {
        this.isHittingMode = hittingMode;
        this.mainFrame = frame;
        this.currentPitcher = selectedPitcher; // 儲存選定的投手
        this.dbManager = new DatabaseManager(); // 初始化 DatabaseManager
        
        if (isHittingMode) {
            // 打擊模式可以預設載入所有球種或者某些通用球種
            initialize_AllPitchesDefault(); // 創建一個預設的通用球種初始化方法
        } else if (currentPitcher != null) {
            // 投球模式，載入選定投手的球種
            loadPitcherPitchData(currentPitcher.getPid());
        } else {
            // 處理沒有選擇投手但進入投球模式的情況 (例如直接啟動遊戲就進入投球模式)
            // 這裡可以選擇載入一個預設投手，或者回到選擇介面
            System.err.println("Warning: Pitching mode started without selected pitcher. Loading default pitches.");
            initialize_AllPitchesDefault();
        }

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
            ballImage = ImageIO.read(new File("src/ball.png")); // 確保 ball.png 在 src 資料夾下
        } catch (IOException e) {
            System.err.println("Failed to load ball image: " + e.getMessage());
        }
        
        if (isHittingMode) countdown = 180;
        resetPitch();
    }
    
    // 新增方法來從資料庫載入特定投手的球種數據
    private void loadPitcherPitchData(int pitcherId) {
        pitchDatabase.clear();
        pitchDatabase.putAll(dbManager.getPitchDataForPitcher(pitcherId));
        // 確保至少有一種球可以投，如果資料庫中沒有為該投手設定球種
        if (pitchDatabase.isEmpty()) {
            System.err.println("No pitch data found for pitcher PID: " + pitcherId + ". Loading default pitches.");
            initialize_AllPitchesDefault();
        }
    }
    
    // 創建一個通用球種的預設初始化方法 (可以用於打擊模式或沒有特定投手時)
    private void initialize_AllPitchesDefault() {
        // 這裡可以使用資料庫中某個預設投手的球種，或者硬編碼一些基礎球種
        // 為了方便，這裡暫時硬編碼一些數據，這些數據應與資料庫中的 Trajectory 表格相符
        // PID 和 BID 這裡暫時設為 0，實際應與通用投手的 PID/BID 對應
        pitchDatabase.put("4SEAMFAST", new TrajectoryData(0, 1, 33.0, -2.7, -13.5, -2.2, 5.8, 96.8));
        pitchDatabase.put("SLIDER", new TrajectoryData(0, 2, 35.0, 8.6, -32.4, -2.4, 5.7, 83.8));
        pitchDatabase.put("CURVE", new TrajectoryData(0, 3, 9.0, 6.9, -46.9, -2.1, 5.2, 81.0));
        pitchDatabase.put("CHANGE", new TrajectoryData(0, 4, 9.0, -10.1, -27.2, -2.6, 5.6, 88.4));
        pitchDatabase.put("SINKER", new TrajectoryData(0, 5, 6.0, -8.7, -22.8, -2.3, 5.7, 94.3));
        pitchDatabase.put("SPLIT", new TrajectoryData(0, 6, 6.0, -4.0, -28.0, -2.0, 6.1, 88.6));
        pitchDatabase.put("SWEEPER", new TrajectoryData(0, 7, 15.0, 2.0, -25.6, -2.4, 5.7, 88.6));
        pitchDatabase.put("CUTTER", new TrajectoryData(0, 8, 15.0, 2.0, -25.6, -2.4, 5.7, 88.6));
    }
    
    // 移除原有的初始化特定投手數據的方法
    // private void initialize_ZackWheeler(Map<String, PitchData> db) { ... }
    // private void initialize_MaxFried(Map<String, PitchData> db) { ... }
    // private void initialize_ShoheiOhtani(Map<String, PitchData> db) { ... }
    // private void initialize_PaulSkenes(Map<String, PitchData> db) { ... }
    // private void initialize_TylerRogers(Map<String, PitchData> db) { ... }

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
        TrajectoryData data = pitchDatabase.get(type); // 使用 TrajectoryData
        if (data == null) {
            System.err.println("Pitch type " + type + " data not found. Using a default pitch.");
            data = pitchDatabase.values().iterator().next(); // 使用任一可用球種作為備用
        }
        this.pitchType = type;
        
        // 使用 TrajectoryData 中的釋放點
        startX_ft = data.getRex(); 
        startY_ft = data.getRey(); 
        startZ_ft = PITCHER_MOUND_DISTANCE_FT; // 投手丘距離
        
        x_ft = startX_ft; y_ft = startY_ft; z_ft = startZ_ft;
        
        // 從 TrajectoryData 獲取速度和位移
        double releaseSpeed_mph = data.getSpeed();
        double horizontalMovement_in = data.getHmov();
        double verticalMovement_in = data.getVmov();

        double releaseSpeed_fts = releaseSpeed_mph * 1.467; // mph 轉 fts
        double pfx_x_ft = horizontalMovement_in / 12.0; // inches 轉 ft
        double pfx_z_ft = verticalMovement_in / 12.0; // inches 轉 ft

        double flightDistance = startZ_ft - endZ_ft;
        double flightTime = flightDistance / releaseSpeed_fts;

        // 計算加速度
        this.ax = (2 * pfx_x_ft) / (flightTime * flightTime);
        this.ay = (2 * pfx_z_ft) / (flightTime * flightTime);
        
        double targetX, targetY;
        if (isHittingMode) {
            // 打擊模式下，球路落點隨機化
            if (random.nextDouble() < 0.6) { // 60% 機率投進好球帶
                targetX = random.nextDouble() * (strikeZoneRight_ft - strikeZoneLeft_ft) + strikeZoneLeft_ft;
                targetY = random.nextDouble() * (strikeZoneTop_ft - strikeZoneBottom_ft) + strikeZoneBottom_ft;
            } else { // 40% 機率投在好球帶外
                double margin = 0.5;
                targetX = (random.nextBoolean() ? 1 : -1) * (strikeZoneRight_ft + margin);
                targetY = random.nextDouble() * (strikeZoneTop_ft + margin - (strikeZoneBottom_ft - margin)) + (strikeZoneBottom_ft - margin);
            }
        } else {
            // 投球模式下，球路落點根據玩家瞄準決定
            targetX = this.aimX_ft;
            targetY = this.aimY_ft;
        }

        // 計算初始速度
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
            z_ft -= vz * frameTime; // 球向攝影機 (Z軸負方向) 移動
            
            if (z_ft <= endZ_ft) {
                // 球到達本壘板前的精確位置
                double z_travel_in_frame = prev_z - z_ft;
                if (z_travel_in_frame > 0) {
                    double fraction = (prev_z - endZ_ft) / z_travel_in_frame;
                    x_ft = prev_x + (x_ft - prev_x) * fraction;
                    y_ft = prev_y + (y_ft - prev_y) * fraction;
                }
                z_ft = endZ_ft;
                isPitching = false;
                ballReachedCatcher = true;

                // 判斷是否為好球
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
        
        // 重置球的位置到投手釋放點
        // 確保 pitchDatabase 不為空，以防資料載入失敗
        TrajectoryData data = pitchDatabase.isEmpty() ? new TrajectoryData(0,0,0,0,0, -2.0, 6.0, 90.0) : pitchDatabase.values().iterator().next(); 
        x_ft = data.getRex();
        y_ft = data.getRey();
        z_ft = PITCHER_MOUND_DISTANCE_FT;
        lastFrameTime = System.nanoTime();
        repaint();
    }
    
    private String randomPitchType() {
        // 確保 pitchDatabase 不為空
        if (pitchDatabase.isEmpty()) {
            // 如果資料庫為空，可以考慮拋出錯誤或返回一個預設球種
            return "4SEAMFAST"; // 備用
        }
        Object[] types = pitchDatabase.keySet().toArray();
        return (String) types[random.nextInt(types.length)];
    }

    private void startPitch(String type) {
        if (!pitchDatabase.containsKey(type)) {
            System.err.println("Pitch type " + type + " not in current pitcher's arsenal.");
            // 可以選擇不投球，或投一個預設球種
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
            g2d.setColor(new Color(188, 143, 143)); // 土色
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
        g2d.setColor(new Color(255, 255, 255, 100)); // 半透明白色
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
            g2d.setColor(new Color(160, 82, 45)); // 深土色
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
        g2d.setColor(new Color(0, 0, 0, 150)); // 半透明黑色背景
        g2d.fillRoundRect(10, 10, 500, 160, 10, 10); // 調整高度以容納更多信息

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        // 顯示當前模式和投手名稱
        g2d.drawString(isHittingMode ? "Hitting Mode" : ("Pitching Mode - " + (currentPitcher != null ? currentPitcher.getPname() : "N/A")), 20, 30);
        
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
            g2d.drawString("M=Menu | C=Change Pitcher | Space=Reset | ESC=Pause", 20, 70); // 新增 C 鍵提示
            
            // 顯示當前球種的具體數據 (速度)
            TrajectoryData displayData = null;
            if (!pitchType.equals("none") && pitchDatabase.containsKey(pitchType)) {
                displayData = pitchDatabase.get(pitchType);
            } else if (!pitchDatabase.isEmpty()) {
                // 如果沒有明確選擇球種，顯示列表中的第一個球種數據
                displayData = pitchDatabase.values().iterator().next();
            }

            if (displayData != null) {
                g2d.drawString("Speed: " + String.format("%.1f mph", displayData.getSpeed()), 20, 90);
            }

            if(isPitching) g2d.drawString("Pitching: " + pitchType, 20, 110);
            else if(ballReachedCatcher) g2d.drawString("Ball reached catcher.", 20, 110);
            else g2d.drawString("Select Pitch (1-" + availablePitches.length + "), Aim.", 20, 110);
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
        double worldTargetMinX = -2.0, worldTargetMaxX = 2.0; // 調整 X 軸瞄準範圍
        double worldTargetMinY = 0.5, worldTargetMaxY = 4.5; // 調整 Y 軸瞄準範圍
        
        double percentX = (mousePos.x - screenAimMinX) / (screenAimMaxX - screenAimMinX);
        double percentY = (mousePos.y - screenAimMinY) / (screenAimMaxY - screenAimMinY);
        
        percentX = Math.max(0, Math.min(1, percentX));
        percentY = Math.max(0, Math.min(1, percentY)); // Y 軸倒置，因為螢幕 Y 軸向下增加

        aimX_ft = worldTargetMinX + percentX * (worldTargetMaxX - worldTargetMinX);
        aimY_ft = worldTargetMinY + (1 - percentY) * (worldTargetMaxY - worldTargetMinY); // (1 - percentY) 使滑鼠向上移動時 Y 值增大
    }
    
    private void drawAimingReticle(Graphics2D g2d) {
        Point screenPos = project3D(aimX_ft, aimY_ft, endZ_ft);
        if (screenPos == null) return;
        g2d.setColor(new Color(255, 0, 0, 150)); // 半透明紅色
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

        // 為 1-9 數字鍵綁定球種選擇
        // 假設球種數量不會超過 9 種，或者需要更複雜的選球邏輯
        for (int i = 1; i <= 9; i++) { 
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, 0), "pitchAction" + i);
            actionMap.put("pitchAction" + i, new PitchSelectAction(i));
        }

        // 移除原有的 F1-F6 投手選擇綁定，因為現在從 PitchSelectionPanel 選擇
        // for (int i = 0; i < 6; i++) {
        //      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1 + i, 0), "pitcherAction" + (i + 1));
        //      actionMap.put("pitcherAction" + (i + 1), new PitcherSelectAction(i + 1));
        // }
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "resetAction");
        actionMap.put("resetAction", new ResetAction());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "menuAction");
        actionMap.put("menuAction", new MenuAction());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "pauseAction");
        actionMap.put("pauseAction", new PauseAction());

        // 新增 'C' 鍵綁定，返回投手選擇介面
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "changePitcherAction");
        actionMap.put("changePitcherAction", new ChangePitcherAction());
    }

    // 抽象類別，方便統一管理遊戲動作
    private abstract class GameAction extends AbstractAction { }

    private class SwingAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isHittingMode) {
                if (hitResult != null) { // 如果有打擊結果，表示本球局結束，按空格重置
                    resetPitch();
                } else if (isPitching && !swingAttempted) {
                    swingAttempted = true;
                    // 計算揮棒時機
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
        }
    }

    private class PitchSelectAction extends GameAction {
        private int pitchNumber; // 修改為 pitchNumber，直接對應按下的數字鍵
        public PitchSelectAction(int number) { 
            this.pitchNumber = number; 
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isHittingMode) {
                // 從當前投手的 pitchDatabase 中獲取球種列表
                String[] availablePitches = pitchDatabase.keySet().toArray(new String[0]);
                // 檢查按下的數字是否在有效範圍內
                if (pitchNumber >= 1 && pitchNumber <= availablePitches.length) {
                    startPitch(availablePitches[pitchNumber - 1]); // 陣列索引從 0 開始
                } else {
                    System.out.println("Invalid pitch selection. Available pitches are 1 to " + availablePitches.length);
                }
            }
        }
    }

    // 移除 PitcherSelectAction，因為現在投手選擇在 PitchSelectionPanel 中進行
    // private class PitcherSelectAction extends GameAction { ... }

    private class ResetAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isHittingMode) { // 只在投球模式下按空格是重置投球
                resetPitch();
            }
        }
    }

    private class MenuAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Main.showStartScreen(mainFrame); // 返回主菜單
        }
    }

    private class PauseAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            togglePause(); // 暫停/恢復遊戲
        }
    }

    private class ChangePitcherAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isHittingMode) { // 只有在投球模式下才允許切換投手
                Main.showPitchSelectionScreen(mainFrame); // 返回投手選擇介面
            }
        }
    }
}