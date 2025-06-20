import javax.swing.*;
import java.awt.*;

public class StartScreen extends JPanel {
    private final int windowWidth = GameConstants.WINDOW_WIDTH;
    private final int windowHeight = GameConstants.WINDOW_HEIGHT;

    public StartScreen(Runnable pitchingModeCallback, Runnable hittingModeCallback) {
        setLayout(null);
        setBackground(new Color(135, 206, 235));

        // Title
        JLabel titleLabel = new JLabel("Baseball Simulator");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(200, 100, 600, 50);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel);

        // Instructions
        JLabel instructionsLabel = new JLabel("<html><center>Welcome to the Baseball Simulator!<br><br>" +
                "Pitching Mode:<br>" +
                "1 - Fastball<br>2 - Slider<br>3 - Curveball<br>4 - Changeup<br>" +
                "Q/W - Adjust Speed<br>Space - Reset<br>ESC - Pause<br><br>" +
                "Hitting Mode:<br>" +
                "Space - Swing<br>ESC - Pause<br>Any key to continue after result<br><br>" +
                "Choose a mode to start!</center></html>");
        instructionsLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        instructionsLabel.setForeground(Color.WHITE);
        instructionsLabel.setBounds(300, 200, 400, 400);
        instructionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(instructionsLabel);

        // Pitching button
        JButton pitchingButton = new JButton("Pitching");
        pitchingButton.setFont(new Font("Arial", Font.BOLD, 24));
        pitchingButton.setBounds(300, 500, 200, 50);
        pitchingButton.setBackground(Color.GREEN);
        pitchingButton.setForeground(Color.WHITE);
        pitchingButton.addActionListener(e -> pitchingModeCallback.run());
        add(pitchingButton);

        // Hitting button
        JButton hittingButton = new JButton("Hitting");
        hittingButton.setFont(new Font("Arial", Font.BOLD, 24));
        hittingButton.setBounds(500, 500, 200, 50);
        hittingButton.setBackground(Color.BLUE);
        hittingButton.setForeground(Color.WHITE);
        hittingButton.addActionListener(e -> hittingModeCallback.run());
        add(hittingButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, windowHeight, new Color(176, 224, 230));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, windowWidth, windowHeight);
    }
}