import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class CatcherPerspectivePitch extends JPanel {
    private Timer timer;
    
    // Ball position and velocity (catcher's perspective, Z-axis points toward pitcher)
    private double x = 0, y = 0, z = 400; // z=400 is pitcher position, z=0 is catcher position
    private double vx = 0, vy = 0, vz = 0;
    
    // Pitch parameters
    private double ballSpeed = 1000; // Ball speed (m/s)
    private String pitchType = "none";
    private boolean isPitching = false;
    private int animationStep = 0;
    private int totalSteps = 0;
    private boolean ballReachedCatcher = false;
    private boolean isPaused = false;
    private boolean isHittingMode = false;
    private int countdown = 0; // Countdown for hitting mode (frames)
    private String hitResult = null; // Stores hit result (Hit/Out/Miss/Strike/Ball)
    private boolean swingAttempted = false; // Tracks if swing was attempted
    private double targetX = 0, targetY = 0; // Stores intended pitch target for strike/ball check
    
    // Start and end points (catcher's perspective coordinate system)
    private double startX = -15, startY = -80, startZ = 400;    // Pitcher position
    private double endX = 0, endY = 20, endZ = 50;              // Home plate position
    
    // Ball image asset
    private BufferedImage ballImage;
    
    // Random number generator
    private final Random random = new Random();

    public CatcherPerspectivePitch(boolean hittingMode) {
        this.isHittingMode = hittingMode;
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
        timer = new Timer(16, this::actionPerformed); // ~60fps
        timer.start();

        // Load ball image
        try {
            ballImage = ImageIO.read(new File("src/ball.png"));
        } catch (IOException e) {
            System.err.println("Failed to load ball image, using default circle: " + e.getMessage());
        }
        
        if (isHittingMode) {
            countdown = 180; // Initialize countdown for hitting mode
        }
    }

    // Perspective projection
    private Point project3D(double x, double y, double z) {
        double minZ = GameConstants.VIEWER_DISTANCE + 10;
        if (z <= minZ) z = minZ;
        
        double denominator = (z - GameConstants.VIEWER_DISTANCE);
        double projectedX = (x * GameConstants.FOCAL_LENGTH) / denominator;
        double projectedY = (y * GameConstants.FOCAL_LENGTH) / denominator;
        
        projectedX = Math.max(-GameConstants.WINDOW_WIDTH, Math.min(GameConstants.WINDOW_WIDTH, projectedX));
        projectedY = Math.max(-GameConstants.WINDOW_HEIGHT, Math.min(GameConstants.WINDOW_HEIGHT, projectedY));
        
        int screenX = GameConstants.VANISHING_POINT_X + (int)projectedX;
        int screenY = GameConstants.VANISHING_POINT_Y + (int)projectedY;
        
        if (screenX < -100 || screenX > GameConstants.WINDOW_WIDTH + 100 || 
            screenY < -100 || screenY > GameConstants.WINDOW_HEIGHT + 100) {
            return null;
        }
        
        return new Point(screenX, screenY);
    }

    // Calculate ball's visual size
    private int calculateBallSize(double z) {
        if (z <= GameConstants.VIEWER_DISTANCE) z = GameConstants.VIEWER_DISTANCE + 1;
        
        double baseBallSize = 8.0;
        double visualSize = (baseBallSize * GameConstants.FOCAL_LENGTH) / (z - GameConstants.VIEWER_DISTANCE);
        return Math.max(4, Math.min(80, (int)visualSize));
    }

    // Calculate pitch trajectory
    private void calculateTrajectory(String type, double speed) {
        // Random endpoint: 60% chance inside strike zone, 40% chance outside in hitting mode
        if (isHittingMode) {
            if (random.nextDouble() < 0.6) {
                // Inside strike zone
                endX = random.nextDouble() * (GameConstants.STRIKE_ZONE_RIGHT - GameConstants.STRIKE_ZONE_LEFT) + GameConstants.STRIKE_ZONE_LEFT;
                endY = random.nextDouble() * (GameConstants.STRIKE_ZONE_BOTTOM - GameConstants.STRIKE_ZONE_TOP) + GameConstants.STRIKE_ZONE_TOP;
            } else {
                // Outside strike zone, within reasonable range
                double margin = 10; // Margin around strike zone
                endX = random.nextDouble() * (GameConstants.STRIKE_ZONE_RIGHT + margin - (GameConstants.STRIKE_ZONE_LEFT - margin)) + (GameConstants.STRIKE_ZONE_LEFT - margin);
                endY = random.nextDouble() * (GameConstants.STRIKE_ZONE_BOTTOM + margin - (GameConstants.STRIKE_ZONE_TOP - margin)) + (GameConstants.STRIKE_ZONE_TOP - margin);
                // Ensure outside strike zone
                if (endX >= GameConstants.STRIKE_ZONE_LEFT && endX <= GameConstants.STRIKE_ZONE_RIGHT && 
                    endY >= GameConstants.STRIKE_ZONE_TOP && endY <= GameConstants.STRIKE_ZONE_BOTTOM) {
                    // Nudge outside if it accidentally falls inside
                    endX = (endX < 0) ? GameConstants.STRIKE_ZONE_LEFT - margin : GameConstants.STRIKE_ZONE_RIGHT + margin;
                    endY = (endY < GameConstants.STRIKE_ZONE_BOTTOM / 2) ? GameConstants.STRIKE_ZONE_TOP - margin : GameConstants.STRIKE_ZONE_BOTTOM + margin;
                }
            }
        } else {
            // Pitching mode: always inside strike zone
            endX = random.nextDouble() * (GameConstants.STRIKE_ZONE_RIGHT - GameConstants.STRIKE_ZONE_LEFT) + GameConstants.STRIKE_ZONE_LEFT;
            endY = random.nextDouble() * (GameConstants.STRIKE_ZONE_BOTTOM - GameConstants.STRIKE_ZONE_TOP) + GameConstants.STRIKE_ZONE_TOP;
        }
        endZ = 50;
        
        // Store target position for strike/ball check
        targetX = endX;
        targetY = endY;
        
        // Adjust trajectory based on pitch type
        switch (type) {
            case "fastball":
                break;
            case "slider":
                endX -= 14;
                endY += 1;
                break;
            case "curve":
                endX -= 13;
                endY += 3;
                break;
            case "changeup":
                break;
        }
        
        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;
        double totalDistance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        double actualSpeed = speed;
        if (type.equals("changeup")) {
            actualSpeed = speed * 0.93;
        } else if (type.equals("curve")) {
            actualSpeed = speed * 0.8;
        } else if (type.equals("slider")) {
            actualSpeed = speed * 0.90;
        }
        
        double flightTime = totalDistance / actualSpeed;
        totalSteps = (int)(flightTime * 1000 / 16);
        if (totalSteps <= 0) totalSteps = 1;
        
        vx = dx / totalSteps;
        vy = dy / totalSteps;
        vz = dz / totalSteps;
    }

    // Apply pitch-specific effects
    private void applyPitchEffects() {
        double progress = (double)animationStep / totalSteps;
        
        switch (pitchType) {
            case "fastball":
                y += progress * 0.3;
                break;
            case "slider":
                if (progress > 0.7) {
                    double effect = (progress - 0.7) * 10;
                    x += effect;
                    y += effect * 0.3;
                } else {
                    x -= 0.001;
                }
                break;
            case "curve":
                if (progress < 0.6) {
                    y -= progress * 3;
                } else {
                    double effect = (progress - 0.6) * 3;
                    x += effect;
                    double dropProgress = (progress - 0.6) * 2.5;
                    y += dropProgress * dropProgress * 10;
                }
                break;
            case "changeup":
                y += progress * 0.5;
                if (progress > 0.7) {
                    double effect = (progress - 0.7) * 10;
                    y += effect * 0.3;
                }
                break;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Sky background
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235), 
                                                     0, GameConstants.WINDOW_HEIGHT, new Color(176, 224, 230));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        
        // Draw field
        drawField(g2d);
        
        // Draw strike zone
        drawStrikeZone(g2d);
        
        // Draw pitcher mound
        drawPitcherMound(g2d);
        
        // Draw UI
        drawUI(g2d);
        
        // Draw ball
        if (isPitching || animationStep == 0 || ballReachedCatcher) {
            drawBall(g2d);
        }

        // Draw pause overlay
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String pauseText = "Paused";
            int textWidth = g2d.getFontMetrics().stringWidth(pauseText);
            g2d.drawString(pauseText, (GameConstants.WINDOW_WIDTH - textWidth) / 2, GameConstants.WINDOW_HEIGHT / 2);
        }
    }

    private void drawField(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        for (int i = -5; i <= 5; i++) {
            Point start = project3D(i * 50, 100, 400);
            Point end = project3D(i * 10, 60, 50);
            if (start != null && end != null) {
                g2d.setColor(new Color(46, 125, 50, 100));
                g2d.drawLine(start.x, start.y, end.x, end.y);
            }
        }
        for (int z = 100; z <= 350; z += 50) {
            g2d.setColor(new Color(255, 255, 255, 50));
            Point left = project3D(-30, 60, z);
            Point right = project3D(30, 60, z);
            if (left != null && right != null) {
                g2d.drawLine(left.x, left.y, right.x, right.y);
            }
        }
    }

    private void drawStrikeZone(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(4));
        g2d.setColor(Color.BLACK);
        
        double strikeZoneZ = endZ;
        
        Point topLeft = project3D(GameConstants.STRIKE_ZONE_LEFT, GameConstants.STRIKE_ZONE_TOP, strikeZoneZ);
        Point topRight = project3D(GameConstants.STRIKE_ZONE_RIGHT, GameConstants.STRIKE_ZONE_TOP, strikeZoneZ);
        Point bottomLeft = project3D(GameConstants.STRIKE_ZONE_LEFT, GameConstants.STRIKE_ZONE_BOTTOM, strikeZoneZ);
        Point bottomRight = project3D(GameConstants.STRIKE_ZONE_RIGHT, GameConstants.STRIKE_ZONE_BOTTOM, strikeZoneZ);
        
        if (topLeft != null && topRight != null && bottomLeft != null && bottomRight != null) {
            g2d.drawLine(topLeft.x, topLeft.y, topRight.x, topRight.y);
            g2d.drawLine(topRight.x, topRight.y, bottomRight.x, bottomRight.y);
            g2d.drawLine(bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y);
            g2d.drawLine(bottomLeft.x, bottomLeft.y, topLeft.x, topLeft.y);
            
            Point centerH1 = project3D(GameConstants.STRIKE_ZONE_LEFT, (GameConstants.STRIKE_ZONE_TOP + GameConstants.STRIKE_ZONE_BOTTOM) / 2, strikeZoneZ);
            Point centerH2 = project3D(GameConstants.STRIKE_ZONE_RIGHT, (GameConstants.STRIKE_ZONE_TOP + GameConstants.STRIKE_ZONE_BOTTOM) / 2, strikeZoneZ);
            Point centerV1 = project3D((GameConstants.STRIKE_ZONE_LEFT + GameConstants.STRIKE_ZONE_RIGHT) / 2, GameConstants.STRIKE_ZONE_TOP, strikeZoneZ);
            Point centerV2 = project3D((GameConstants.STRIKE_ZONE_LEFT + GameConstants.STRIKE_ZONE_RIGHT) / 2, GameConstants.STRIKE_ZONE_BOTTOM, strikeZoneZ);
            
            if (centerH1 != null && centerH2 != null && centerV1 != null && centerV2 != null) {
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.drawLine(centerH1.x, centerH1.y, centerH2.x, centerH2.y);
                g2d.drawLine(centerV1.x, centerV1.y, centerV2.x, centerV2.y);
            }
        }
    }

    private void drawPitcherMound(Graphics2D g2d) {
        Point moundCenter = project3D(0, 0, 400);
        if (moundCenter != null) {
            int moundSize = calculateBallSize(400) * 3;
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillOval(moundCenter.x - moundSize/2, moundCenter.y - moundSize/2, 
                        moundSize, moundSize);
            g2d.setColor(new Color(160, 82, 45));
            g2d.drawOval(moundCenter.x - moundSize/2, moundCenter.y - moundSize/2, 
                        moundSize, moundSize);
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 300, 140, 10, 10);
        
        g2d.setColor(Color.WHITE);
        g2d.drawString(isHittingMode ? "Hitting Mode" : "Pitching Mode", 20, 30);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        if (isHittingMode) {
            g2d.drawString("Space=Swing  ESC=Pause", 20, 50);
            g2d.drawString("Swing when ball is close for a hit!", 20, 70);
            if (countdown > 0) {
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.setColor(Color.RED);
                g2d.drawString("Pitch in: " + ((countdown / 60) + 1), GameConstants.WINDOW_WIDTH / 2 - 50, GameConstants.WINDOW_HEIGHT / 2);
            } else if (hitResult != null) {
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.setColor(hitResult.equals("Hit") || hitResult.equals("Strike") ? Color.GREEN : Color.RED);
                g2d.drawString("Result: " + hitResult, GameConstants.WINDOW_WIDTH / 2 - 50, GameConstants.WINDOW_HEIGHT / 2);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.setColor(Color.WHITE);
                g2d.drawString("Press any key to continue", 20, 90);
            }
        } else {
            g2d.drawString("1=Fastball  2=Slider  3=Curve  4=Changeup", 20, 50);
            g2d.drawString("Q/W=Speed+/-  Space=Reset  ESC=Pause", 20, 70);
            g2d.drawString("Speed: " + String.format("%.1f", ballSpeed) + " m/s", 20, 90);
            if (isPitching) {
                g2d.drawString("Pitching: " + pitchType + " (" + 
                              String.format("%.1f", (double)animationStep/totalSteps*100) + "%)", 20, 110);
            } else if (ballReachedCatcher) {
                g2d.drawString("Ball reached catcher", 20, 110);
            } else {
                g2d.drawString("Press number key to pitch", 20, 110);
            }
        }
        
        if (isPitching && !isHittingMode) {
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.setColor(Color.YELLOW);
            double currentSpeed = ballSpeed * Math.abs(vz) / Math.abs(startZ - endZ) * 16;
            g2d.drawString(String.format("%.1f mph", currentSpeed * 2.237), 
                          GameConstants.WINDOW_WIDTH - 150, 40);
        }
    }

    private void drawBall(Graphics2D g2d) {
        Point ballPos = project3D(x, y, z);
        if (ballPos == null) return;
        
        int ballSize = calculateBallSize(z);
        
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(ballPos.x - ballSize/2 + 2, ballPos.y - ballSize/2 + 2, 
                    ballSize, ballSize);
        
        if (ballImage != null) {
            g2d.drawImage(ballImage, 
                         ballPos.x - ballSize/2, 
                         ballPos.y - ballSize/2, 
                         ballSize, ballSize, null);
        } else {
            RadialGradientPaint ballGradient = new RadialGradientPaint(
                ballPos.x - ballSize/4, ballPos.y - ballSize/4, ballSize/2,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{Color.WHITE, new Color(240, 240, 240), new Color(200, 200, 200)}
            );
            g2d.setPaint(ballGradient);
            g2d.fillOval(ballPos.x - ballSize/2, ballPos.y - ballSize/2, ballSize, ballSize);
            
            g2d.setColor(new Color(200, 50, 50));
            g2d.setStroke(new BasicStroke(Math.max(1, ballSize/10)));
            g2d.drawArc(ballPos.x - ballSize/3, ballPos.y - ballSize/3, 
                       ballSize*2/3, ballSize*2/3, 45, 90);
            g2d.drawArc(ballPos.x - ballSize/3, ballPos.y - ballSize/3, 
                       ballSize*2/3, ballSize*2/3, 225, 90);
        }
    }

    private void actionPerformed(ActionEvent e) {
        if (isPaused) return;

        if (isHittingMode) {
            if (countdown > 0) {
                countdown--;
                if (countdown == 0) {
                    startPitch(randomPitchType());
                }
            } else if (isPitching && !ballReachedCatcher) {
                x += vx;
                y += vy;
                z += vz;
                applyPitchEffects();
                animationStep++;
                
                if (animationStep >= totalSteps || z <= endZ) {
                    isPitching = false;
                    ballReachedCatcher = true;
                    if (!swingAttempted) {
                        hitResult = (targetX >= GameConstants.STRIKE_ZONE_LEFT && targetX <= GameConstants.STRIKE_ZONE_RIGHT && 
                                     targetY >= GameConstants.STRIKE_ZONE_TOP && targetY <= GameConstants.STRIKE_ZONE_BOTTOM) ? "Strike" : "Ball";
                    }
                }
            }
        } else {
            if (isPitching && !ballReachedCatcher) {
                x += vx;
                y += vy;
                z += vz;
                applyPitchEffects();
                animationStep++;
                
                if (animationStep >= totalSteps || z <= endZ) {
                    isPitching = false;
                    ballReachedCatcher = true;
                    System.out.println("Ball reached home plate! Position: (" + 
                                     String.format("%.1f", x) + ", " + 
                                     String.format("%.1f", y) + ")");
                    if (x >= GameConstants.STRIKE_ZONE_LEFT && x <= GameConstants.STRIKE_ZONE_RIGHT && 
                        y >= GameConstants.STRIKE_ZONE_TOP && y <= GameConstants.STRIKE_ZONE_BOTTOM) {
                        System.out.println("Strike!");
                    } else {
                        System.out.println("Ball!");
                    }
                }
            }
        }
        repaint();
    }

    private void resetPitch() {
        x = startX;
        y = startY;
        z = startZ;
        vx = vy = vz = 0;
        isPitching = false;
        ballReachedCatcher = false;
        pitchType = "none";
        animationStep = 0;
        totalSteps = 0;
        swingAttempted = false;
        hitResult = null;
        targetX = 0;
        targetY = 0;
        if (isHittingMode) {
            countdown = 180; // 3 seconds at 60fps
        }
    }

    private String randomPitchType() {
        String[] types = {"fastball", "slider", "curve", "changeup"};
        return types[random.nextInt(types.length)];
    }

    private void startPitch(String type) {
        if (!isPitching && !isPaused) {
            x = startX;
            y = startY;
            z = startZ;
            vx = vy = vz = 0;
            isPitching = true;
            ballReachedCatcher = false;
            pitchType = type;
            animationStep = 0;
            totalSteps = 0;
            swingAttempted = false;
            hitResult = null;
            targetX = 0;
            targetY = 0;
            calculateTrajectory(type, ballSpeed);
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        System.out.println(isPaused ? "Game paused" : "Game resumed");
        repaint();
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            togglePause();
            return;
        }

        if (isPaused) return;

        if (isHittingMode) {
            if (hitResult != null) {
                resetPitch();
                repaint();
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE && isPitching && !swingAttempted) {
                swingAttempted = true;
                double progress = (double)animationStep / totalSteps;
                boolean isStrike = targetX >= GameConstants.STRIKE_ZONE_LEFT && targetX <= GameConstants.STRIKE_ZONE_RIGHT && 
                                   targetY >= GameConstants.STRIKE_ZONE_TOP && targetY <= GameConstants.STRIKE_ZONE_BOTTOM;
                if (!isStrike) {
                    hitResult = "Out";
                } else if (progress >= 0.8 && progress <= 0.9) {
                    hitResult = random.nextDouble() < 0.7 ? "Hit" : "Out";
                } else {
                    hitResult = "Out";
                }
            }
        } else {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_1:
                    startPitch("fastball");
                    break;
                case KeyEvent.VK_2:
                    startPitch("slider");
                    break;
                case KeyEvent.VK_3:
                    startPitch("curve");
                    break;
                case KeyEvent.VK_4:
                    startPitch("changeup");
                    break;
                case KeyEvent.VK_Q:
                    ballSpeed = Math.max(15, ballSpeed - 2);
                    System.out.println("Speed adjusted to: " + ballSpeed + " m/s");
                    break;
                case KeyEvent.VK_W:
                    ballSpeed = Math.min(50, ballSpeed + 2);
                    System.out.println("Speed adjusted to: " + ballSpeed + " m/s");
                    break;
                case KeyEvent.VK_SPACE:
                    resetPitch();
                    break;
            }
        }
        repaint();
    }
}