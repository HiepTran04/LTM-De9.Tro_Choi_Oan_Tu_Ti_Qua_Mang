package BTL;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DanhSach extends JFrame {
    private JPanel playerPanel;

    public DanhSach(List<String> players, java.util.function.Consumer<String> onChallenge) {
        setTitle("👥 Người chơi online");
        setSize(320, 420);
        setLocationRelativeTo(null);

        // 🔹 Nền gradient xanh – hồng
        JPanel background = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(255, 182, 193),
                        0, getHeight(), new Color(135, 206, 250)
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        background.setLayout(new BorderLayout(10, 10));
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("👥 Người chơi online", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.DARK_GRAY);

        // Panel chứa danh sách người chơi
        playerPanel = new JPanel();
        playerPanel.setOpaque(false);
        playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(playerPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true));

        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(new Color(220, 20, 60));
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.setOpaque(false);
        southPanel.add(closeButton);

        background.add(title, BorderLayout.NORTH);
        background.add(scrollPane, BorderLayout.CENTER);
        background.add(southPanel, BorderLayout.SOUTH);

        add(background);

        updatePlayers(players, onChallenge);
    }

    public void updatePlayers(List<String> players, java.util.function.Consumer<String> onChallenge) {
        playerPanel.removeAll();
        for (String player : players) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setOpaque(false);

            // Tên người chơi (giới hạn width + cắt ...)
            JLabel nameLabel = new JLabel(player);
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            nameLabel.setForeground(Color.BLACK);
            nameLabel.setPreferredSize(new Dimension(160, 25)); // Giữ cố định 160px
            nameLabel.setMaximumSize(new Dimension(160, 25));
            nameLabel.setMinimumSize(new Dimension(160, 25));

            // Nếu tên quá dài thì hiển thị dấu "..."
            nameLabel.setToolTipText(player); // Hover để xem full tên

            // Nút Thách đấu
            JButton challengeButton = new JButton("⚔ Thách đấu");
            challengeButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
            challengeButton.setForeground(Color.WHITE);
            challengeButton.setBackground(new Color(100, 149, 237));
            challengeButton.setFocusPainted(false);
            challengeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            challengeButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            challengeButton.addActionListener(e -> onChallenge.accept(player));

            row.add(nameLabel);
            row.add(Box.createHorizontalGlue()); // đẩy nút sang phải
            row.add(challengeButton);

            row.setBorder(BorderFactory.createMatteBorder(
                    0, 0, 1, 0, new Color(255, 255, 255, 100)
            ));

            playerPanel.add(row);
        }
        playerPanel.revalidate();
        playerPanel.repaint();
    }

}
