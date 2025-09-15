package BTL;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 50000;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private JTextArea textArea;
    private JButton rockButton, paperButton, scissorsButton, quitButton;
    private JLabel scoreLabel;

    private int myScore = 0;
    private int opponentScore = 0;

    private String username;
    private String opponentName = "Đối thủ";

    public Client(String username) {
        this.username = username;

        setTitle("🎮 Oẳn Tù Tì - " + username);
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel background = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(135, 206, 250),
                        0, getHeight(), new Color(255, 182, 193));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        background.setLayout(new BorderLayout(15, 15));
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scoreLabel = new JLabel(username + " vs " + opponentName + " | Điểm: 0 - 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        scoreLabel.setForeground(Color.DARK_GRAY);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textArea);

        rockButton = createStyledButton("✊ BÚA", new Color(70, 130, 180));
        paperButton = createStyledButton("✋ BAO", new Color(60, 179, 113));
        scissorsButton = createStyledButton("✌ KÉO", new Color(218, 112, 214));
        quitButton = createStyledButton("❌ THOÁT", new Color(220, 20, 60));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(rockButton);
        buttonPanel.add(paperButton);
        buttonPanel.add(scissorsButton);
        buttonPanel.add(quitButton);

        background.add(scoreLabel, BorderLayout.NORTH);
        background.add(scrollPane, BorderLayout.CENTER);
        background.add(buttonPanel, BorderLayout.SOUTH);

        add(background);

        rockButton.addActionListener(e -> sendMove("BÚA"));
        paperButton.addActionListener(e -> sendMove("BAO"));
        scissorsButton.addActionListener(e -> sendMove("KÉO"));
        quitButton.addActionListener(e -> quitGame());

        connectToServer();
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(120, 45));
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(baseColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addChangeListener(e -> button.setBackground(button.getModel().isRollover() ? baseColor.darker() : baseColor));
        return button;
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            out.write(username);
            out.newLine();
            out.flush();

            textArea.append("✅ Kết nối thành công tới máy chủ...\n");
            textArea.append("👤 Tên của bạn: " + username + "\n");

            Thread listener = new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        if (msg.startsWith("Bạn đã được ghép với")) {
                            opponentName = msg.replace("Bạn đã được ghép với", "").trim();
                            myScore = 0;
                            opponentScore = 0;
                            updateScoreboard("RESET");
                        }

                        if (msg.contains("Điểm đã được reset")) {
                            myScore = 0;
                            opponentScore = 0;
                            updateScoreboard("RESET");
                        }

                        // ✅ Bắt điểm số từ server
                        if (msg.startsWith("Điểm:")) {
                            parseAndUpdateScore(msg);
                        }
                    }
                } catch (IOException e) {
                    textArea.append("⚠️ Kết nối đã đóng.\n");
                }
            });
            listener.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối tới máy chủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void sendMove(String move) {
        if (out != null) {
            try {
                out.write(move);
                out.newLine();
                out.flush();
                textArea.append("👉 Bạn đã chọn: " + move + "\n");
            } catch (IOException e) {
                textArea.append("❌ Lỗi khi gửi nước đi.\n");
            }
        } else {
            textArea.append("⚠️ Chưa kết nối tới máy chủ!\n");
        }
    }

    private void quitGame() {
        try {
            if (out != null) {
                out.write("QUIT");
                out.newLine();
                out.flush();
            }
            textArea.append("👋 Bạn đã thoát khỏi trò chơi.\n");
            if (socket != null) socket.close();
            dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateScoreboard(String result) {
        SwingUtilities.invokeLater(() -> {
            scoreLabel.setText(username + " vs " + opponentName + " | Điểm: " + myScore + " - " + opponentScore);
            switch (result) {
                case "WIN" -> scoreLabel.setForeground(new Color(0, 180, 0));
                case "LOSE" -> scoreLabel.setForeground(Color.RED);
                case "DRAW" -> scoreLabel.setForeground(new Color(255, 140, 0));
                case "RESET" -> scoreLabel.setForeground(Color.DARK_GRAY);
                default -> scoreLabel.setForeground(Color.DARK_GRAY);
            }
        });
    }

    // ✅ Hàm parse điểm số từ Server
    private void parseAndUpdateScore(String msg) {
        try {
            String[] parts = msg.split(" ");
            String name1 = parts[1];
            int score1 = Integer.parseInt(parts[2].replace("[", "").replace("]", ""));
            String name2 = parts[4];
            int score2 = Integer.parseInt(parts[5].replace("[", "").replace("]", ""));

            if (username.equals(name1)) {
                myScore = score1;
                opponentName = name2;
                opponentScore = score2;
            } else {
                myScore = score2;
                opponentName = name1;
                opponentScore = score1;
            }
            updateScoreboard("UPDATE");
        } catch (Exception e) {
            System.out.println("⚠ Lỗi parse điểm: " + msg);
        }
    }

    // 🔹 Form Login
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame loginFrame = new JFrame("Đăng nhập Oẳn Tù Tì");
            loginFrame.setSize(400, 220);
            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loginFrame.setLocationRelativeTo(null);

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    GradientPaint gp = new GradientPaint(0, 0, new Color(255, 182, 193),
                            0, getHeight(), new Color(135, 206, 250));
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            panel.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("🎮 Đăng nhập Oẳn Tù Tì", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 20));
            title.setForeground(Color.DARK_GRAY);

            JTextField usernameField = new JTextField();
            usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));

            JButton loginButton = new JButton("Vào game");
            loginButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            loginButton.setBackground(new Color(70, 130, 180));
            loginButton.setForeground(Color.WHITE);
            loginButton.setFocusPainted(false);
            loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(title, gbc);

            gbc.gridy = 1;
            gbc.gridwidth = 2;
            panel.add(usernameField, gbc);

            gbc.gridy = 2;
            gbc.gridwidth = 2;
            panel.add(loginButton, gbc);

            loginFrame.add(panel);
            loginFrame.setVisible(true);

            Action loginAction = new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    String username = usernameField.getText().trim();
                    if (!username.isEmpty()) {
                        new Client(username).setVisible(true);
                        usernameField.setText("");
                    } else {
                        JOptionPane.showMessageDialog(loginFrame, "Vui lòng nhập tên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            loginButton.addActionListener(loginAction);
            usernameField.addActionListener(loginAction);
        });
    }
}
