import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Baseball Simulator");
            frame.setSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            // Initialize the start screen
            createStartScreen(frame);

            frame.setVisible(true);

            System.out.println("=== Baseball Simulator ===");
            System.out.println("Pitching Mode Controls:");
            System.out.println("1 - Fastball");
            System.out.println("2 - Slider");
            System.out.println("3 - Curveball");
            System.out.println("4 - Changeup");
            System.out.println("Q/W - Adjust Speed");
            System.out.println("Space - Reset");
            System.out.println("ESC - Pause");
            System.out.println("Hitting Mode Controls:");
            System.out.println("Space - Swing");
            System.out.println("ESC - Pause");
            System.out.println("Any key to continue after result");
        });
    }

    private static void createStartScreen(JFrame frame) {
        StartScreen startScreen = new StartScreen(
            () -> {
                frame.getContentPane().removeAll();
                CatcherPerspectivePitch panel = new CatcherPerspectivePitch(false, () -> createStartScreen(frame));
                frame.add(panel);
                frame.revalidate();
                frame.repaint();
                panel.requestFocusInWindow();
            },
            () -> {
                frame.getContentPane().removeAll();
                CatcherPerspectivePitch panel = new CatcherPerspectivePitch(true, () -> createStartScreen(frame));
                frame.add(panel);
                frame.revalidate();
                frame.repaint();
                panel.requestFocusInWindow();
            }
        );
        frame.getContentPane().removeAll();
        frame.add(startScreen);
        frame.revalidate();
        frame.repaint();
        startScreen.requestFocusInWindow();
    }
}