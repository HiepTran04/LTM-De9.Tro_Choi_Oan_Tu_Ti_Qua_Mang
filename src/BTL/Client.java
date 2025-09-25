package BTL;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class Client extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 50000;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private JTextArea textArea;
    private JButton rockButton, paperButton, scissorsButton, quitButton;
    private JButton listButton;
    private JButton rankButton;
    private JLabel scoreLabel;

    private int myScore = 0;
    private int opponentScore = 0;

    private String username;
    private String opponentName = "Đối thủ";

    // 🔹 Cửa sổ danh sách
    private DanhSach danhSachFrame;

    public Client(String username) {
        this.username = username;

        setTitle("Oẳn Tù Tì - " + username);
        setSize(650, 520);
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

        rockButton = createStyledButton("BÚA", new Color(70, 130, 180));
        paperButton = createStyledButton("BAO", new Color(60, 179, 113));
        scissorsButton = createStyledButton("KÉO", new Color(218, 112, 214));
        quitButton = createStyledButton("THOÁT", new Color(220, 20, 60));
        listButton = createStyledButton("Danh sách", new Color(100, 149, 237));
        rankButton = createStyledButton("BXH", new Color(255, 165, 0));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        topRow.setOpaque(false);
        topRow.add(rockButton);
        topRow.add(paperButton);
        topRow.add(scissorsButton);

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.add(listButton, BorderLayout.WEST);
        bottomRow.add(rankButton, BorderLayout.CENTER);
        bottomRow.add(quitButton, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        southPanel.setOpaque(false);
        southPanel.add(topRow);
        southPanel.add(bottomRow);

        background.add(scoreLabel, BorderLayout.NORTH);
        background.add(scrollPane, BorderLayout.CENTER);
        background.add(southPanel, BorderLayout.SOUTH);

        add(background);

        rockButton.addActionListener(e -> sendMove("BUA"));
        paperButton.addActionListener(e -> sendMove("BAO"));
        scissorsButton.addActionListener(e -> sendMove("KEO"));
        quitButton.addActionListener(e -> quitGame());
        listButton.addActionListener(e -> sendCommand("LIST"));
        rankButton.addActionListener(e -> showRank());

        connectToServer();
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(140, 40));
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

            appendLog("Kết nối thành công tới máy chủ...");
            appendLog("Tên của bạn: " + username);

            Thread listener = new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        if (msg.trim().isEmpty()) continue;
                        appendLog("[MÁY CHỦ] " + msg);

                        String lower = msg.toLowerCase();

                        // 🔹 Nhận danh sách người chơi
                        if (msg.startsWith("ONLINE:")) {
                            handleOnlineList(msg);
                            continue;
                        }

                        // 🔹 Khi server thông báo bắt đầu trận mới
                        if (msg.startsWith("Trận mới với")) {
                            opponentName = msg.replace("Trận mới với", "")
                                              .replace("bắt đầu!", "").trim();
                            myScore = 0;
                            opponentScore = 0;
                            updateScoreboard("RESET");
                            continue;
                        }

                        if (lower.contains("ghép") || lower.contains("thách đấu")) {
                            resetOpponentFromMessage(msg, lower);
                            continue;
                        }

                        if (lower.contains("trận mới") || lower.contains("điểm đã được reset")) {
                            myScore = 0;
                            opponentScore = 0;
                            updateScoreboard("RESET");
                            continue;
                        }

                        if (msg.startsWith("Điểm:") || msg.startsWith("Điểm")) {
                            parseAndUpdateScore(msg);
                            continue;
                        }

                        if (lower.contains("thắng")) updateScoreboard("WIN");
                        else if (lower.contains("thua")) updateScoreboard("LOSE");
                        else if (lower.contains("hòa")) updateScoreboard("DRAW");
                    }
                } catch (IOException e) {
                    appendLog("Kết nối đã đóng.");
                } finally {
                    try { if (socket != null) socket.close(); } catch (IOException ignored) {}
                }
            });

            listener.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối tới máy chủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleOnlineList(String msg) {
        String data = msg.substring(7).trim();
        List<String> players = new ArrayList<>();
        if (!data.isEmpty()) {
            for (String p : data.split(",")) {
                players.add(p.trim());
            }
        }

        SwingUtilities.invokeLater(() -> {
            if (danhSachFrame == null || !danhSachFrame.isDisplayable()) {
                danhSachFrame = new DanhSach(
                    players,
                    target -> {
                        sendCommand("CHALLENGE " + target);
                        appendLog("Bạn đã gửi lời thách đấu tới " + target);
                    },
                    () -> {
                        sendCommand("LIST");
                        appendLog("Đang làm mới danh sách người chơi...");
                    }
                );
                danhSachFrame.setVisible(true);
            } else {
                danhSachFrame.updatePlayers(
                    players,
                    target -> {
                        sendCommand("CHALLENGE " + target);
                        appendLog("Bạn đã gửi lời thách đấu tới " + target);
                    }
                );
            }
        });
    }

    private void resetOpponentFromMessage(String msg, String lower) {
        int idx = lower.lastIndexOf("với");
        if (idx != -1) {
            String opp = msg.substring(idx + 3).trim();
            opp = opp.replaceAll("[.?!]$", "").trim();
            opponentName = opp;
            myScore = 0;
            opponentScore = 0;
            updateScoreboard("RESET");
        }
    }

    private void sendMove(String move) {
        sendCommand(move);
        appendLog("Bạn đã chọn: " + move);
    }

    private void sendCommand(String cmd) {
        if (out != null) {
            try {
                out.write(cmd);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                appendLog("Lỗi khi gửi lệnh.");
            }
        }
    }

    private void quitGame() {
        sendCommand("QUIT");
        appendLog("Bạn đã rời trận đấu, quay lại danh sách.");
        myScore = 0;
        opponentScore = 0;
        opponentName = "Đối thủ";
        updateScoreboard("RESET");
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

    private void parseAndUpdateScore(String msg) {
        try {
            Pattern p = Pattern.compile("(Điểm|Điểm số):\\s*(\\S+)\\s*\\[(\\d+)]\\s*-\\s*(\\S+)\\s*\\[(\\d+)]",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher m = p.matcher(msg);
            if (m.find()) {
                String name1 = m.group(2).trim();
                int s1 = Integer.parseInt(m.group(3));
                String name2 = m.group(4).trim();
                int s2 = Integer.parseInt(m.group(5));

                if (username.equalsIgnoreCase(name1)) {
                    myScore = s1;
                    opponentScore = s2;
                    opponentName = name2;
                } else if (username.equalsIgnoreCase(name2)) {
                    myScore = s2;
                    opponentScore = s1;
                    opponentName = name1;
                }
                updateScoreboard("UPDATE");
            }
        } catch (Exception e) {
            System.out.println("Lỗi parse điểm: " + e.getMessage());
        }
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(line + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    // 🔹 Mở cửa sổ BXH
    private void showRank() {
        Map<String, Integer> ranking = BangXepHangData.getRankingMap();
        if (ranking == null || ranking.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có dữ liệu BXH!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Map.Entry<String, Integer>> list = ranking.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();

        BangXepHang bxhFrame = new BangXepHang(list);
        bxhFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame loginFrame = new JFrame("Nhập tên người chơi Oẳn Tù Tì");
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

            JLabel title = new JLabel("Nhập tên người chơi Oẳn Tù Tì", SwingConstants.CENTER);
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
