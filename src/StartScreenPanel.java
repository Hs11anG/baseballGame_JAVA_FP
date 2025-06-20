import javax.swing.*;
import java.awt.*;

public class StartScreenPanel extends JPanel {
    private JFrame mainFrame; // 用來儲存主框架的引用

    public StartScreenPanel(JFrame frame) {
        this.mainFrame = frame;
        setLayout(null);
        setBackground(new Color(135, 206, 235));

        JLabel title = new JLabel("Baseball Simulator");
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setBounds(300, 50, 400, 50);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title);

        JButton pitchingButton = new JButton("Pitching");
        pitchingButton.setBounds(300, 500, 200, 50);
        pitchingButton.addActionListener(e -> {
            // 調用 Main 類別中的方法來切換面板
            Main.showPitchSelectionScreen(mainFrame); // 顯示投手選擇畫面
        });
        add(pitchingButton);

        JButton hittingButton = new JButton("Hitting");
        hittingButton.setBounds(500, 500, 200, 50);
        hittingButton.addActionListener(e -> {
            // 調用 Main 類別中的方法來切換面板
            Main.showGamePanel(mainFrame, true); // 進入打擊模式
        });
        add(hittingButton);
    }
}