package BTL;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class DanhSach extends JFrame {
    private JPanel playerPanel;

    public DanhSach(List<String> players, Consumer<String> onChallenge, Runnable onRefresh) {
        setTitle("👥 Người chơi online");
        setSize(360, 460);
        setLocationRelativeTo(null);

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

        JLabel title = new JLabel("Người chơi online", SwingConstants.CENTER);
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

        // Nút đóng
        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(new Color(220, 20, 60));
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());

        // Nút làm mới
        JButton refreshButton = new JButton("🔄 Làm mới");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(new Color(60, 179, 113));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> {
            // Reset lại danh sách trên UI
            playerPanel.removeAll();
            playerPanel.revalidate();
            playerPanel.repaint();

            // Gọi callback refresh để client gửi lệnh LIST về server
            if (onRefresh != null) {
                onRefresh.run();
            }
        });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        southPanel.setOpaque(false);
        southPanel.add(refreshButton);
        southPanel.add(closeButton);

        background.add(title, BorderLayout.NORTH);
        background.add(scrollPane, BorderLayout.CENTER);
        background.add(southPanel, BorderLayout.SOUTH);

        add(background);

        updatePlayers(players, onChallenge);
    }

    public void updatePlayers(List<String> players, Consumer<String> onChallenge) {
        playerPanel.removeAll();
        for (String player : players) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setOpaque(false);

            JLabel nameLabel = new JLabel(player);
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            nameLabel.setForeground(Color.BLACK);
            nameLabel.setPreferredSize(new Dimension(160, 25));
            nameLabel.setMaximumSize(new Dimension(160, 25));
            nameLabel.setMinimumSize(new Dimension(160, 25));
            nameLabel.setToolTipText(player);

            JButton challengeButton = new JButton("Thách đấu");
            challengeButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
            challengeButton.setForeground(Color.WHITE);
            challengeButton.setBackground(new Color(100, 149, 237));
            challengeButton.setFocusPainted(false);
            challengeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            challengeButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            if (player.toUpperCase().contains("BUSY")) {
                challengeButton.setEnabled(false);
                challengeButton.setBackground(new Color(169, 169, 169));
                challengeButton.setToolTipText("Người chơi đang bận");
            } else {
                challengeButton.addActionListener(e -> onChallenge.accept(player));
            }

            row.add(nameLabel);
            row.add(Box.createHorizontalGlue());
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
