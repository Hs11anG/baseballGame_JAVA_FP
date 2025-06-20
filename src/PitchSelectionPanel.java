import data.Pitcher;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PitchSelectionPanel extends JPanel {
    private JFrame mainFrame;
    private DatabaseManager dbManager;
    private JList<Pitcher> pitcherList;
    private DefaultListModel<Pitcher> listModel;

    public PitchSelectionPanel(JFrame frame) {
        this.mainFrame = frame;
        this.dbManager = new DatabaseManager(); // 初始化資料庫管理器
        setLayout(new BorderLayout());
        setBackground(new Color(135, 206, 235));

        JLabel title = new JLabel("choosing");
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        pitcherList = new JList<>(listModel);
        pitcherList.setFont(new Font("Arial", Font.PLAIN, 20));
        pitcherList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 單選模式
        pitcherList.setCellRenderer(new DefaultListCellRenderer() { // 自定義渲染器，讓列表顯示更美觀
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Pitcher) {
                    Pitcher p = (Pitcher) value;
                    label.setText(p.getPname() + " (PID: " + p.getPid() + ")"); // 顯示投手姓名和PID
                }
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(pitcherList);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        add(scrollPane, BorderLayout.CENTER);

        // 選擇按鈕
        JButton selectButton = new JButton("choose this pitcher");
        selectButton.setFont(new Font("Arial", Font.BOLD, 20));
        selectButton.addActionListener(e -> {
            Pitcher selectedPitcher = pitcherList.getSelectedValue();
            if (selectedPitcher != null) {
                System.out.println("Selected Pitcher: " + selectedPitcher.getPname());
                // 進入遊戲面板，並傳遞選定的投手資訊
                Main.showGamePanel(mainFrame, false, selectedPitcher); // 將投手物件傳遞給遊戲面板
            } else {
                JOptionPane.showMessageDialog(this, "choose one pitch！", "no choose", JOptionPane.WARNING_MESSAGE);
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectButton);

        // 返回菜單按鈕
        JButton backButton = new JButton("back to menu (M)");
        backButton.setFont(new Font("Arial", Font.BOLD, 20));
        backButton.addActionListener(e -> Main.showStartScreen(mainFrame));
        buttonPanel.add(backButton);

        add(buttonPanel, BorderLayout.SOUTH);

        loadPitchers(); // 加載投手列表
    }

    private void loadPitchers() {
        listModel.clear(); // 清空舊數據
        List<Pitcher> pitchers = dbManager.getAllPitchers();
        for (Pitcher p : pitchers) {
            listModel.addElement(p);
        }
        if (!pitchers.isEmpty()) {
            pitcherList.setSelectedIndex(0); // 默認選中第一個
        }
    }
}