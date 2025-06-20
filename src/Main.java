import javax.swing.*;
import data.Pitcher; // 導入 Pitcher 類別

public class Main { // 或者 BaseballGame
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Baseball Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            // 顯示起始畫面
            showStartScreen(frame);

            frame.setVisible(true);
        });
    }

    // 這個方法會從 StartScreenPanel 和未來的其他面板中調用
    public static void showStartScreen(JFrame frame) {
        frame.getContentPane().removeAll();
        StartScreenPanel startScreen = new StartScreenPanel(frame);
        frame.add(startScreen);
        frame.revalidate();
        frame.repaint();
    }

    // 顯示投手選擇畫面
    public static void showPitchSelectionScreen(JFrame frame) {
        frame.getContentPane().removeAll();
        PitchSelectionPanel selectionPanel = new PitchSelectionPanel(frame);
        frame.add(selectionPanel);
        frame.revalidate();
        frame.repaint();
        selectionPanel.requestFocusInWindow(); // 確保面板獲得焦點
    }

    // 修改 showGamePanel 以接收 Pitcher 物件
    public static void showGamePanel(JFrame frame, boolean isHittingMode, Pitcher selectedPitcher) {
        frame.getContentPane().removeAll();
        // 這裡的 GamePanel 將需要修改其構造函數以接收 selectedPitcher
        GamePanel gamePanel = new GamePanel(isHittingMode, frame, selectedPitcher);
        frame.add(gamePanel);
        frame.revalidate();
        frame.repaint();
        gamePanel.requestFocusInWindow();
    }

    // 重載一個沒有 Pitcher 參數的方法，用於 Hitting Mode
    public static void showGamePanel(JFrame frame, boolean isHittingMode) {
        showGamePanel(frame, isHittingMode, null); // Hitting mode 不需要選擇投手
    }
}