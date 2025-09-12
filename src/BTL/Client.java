package BTL;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 30000;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private JTextArea textArea;
    private JButton rockButton, paperButton, scissorsButton, quitButton;
    private JLabel scoreLabel;

    // Điểm số
    private int myScore = 0;
    private int opponentScore = 0;

    public Client() {
        setTitle("Trò chơi Oẳn Tù Tì - Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Scoreboard
        scoreLabel = new JLabel("Điểm của bạn: 0 | Điểm đối thủ: 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        rockButton = new JButton("BÚA");
        paperButton = new JButton("BAO");
        scissorsButton = new JButton("KÉO");
        quitButton = new JButton("THOÁT");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(rockButton);
        buttonPanel.add(paperButton);
        buttonPanel.add(scissorsButton);
        buttonPanel.add(quitButton);

        add(scoreLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        rockButton.addActionListener(e -> sendMove("BÚA"));
        paperButton.addActionListener(e -> sendMove("BAO"));
        scissorsButton.addActionListener(e -> sendMove("KÉO"));
        quitButton.addActionListener(e -> quitGame());

        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            textArea.append("Kết nối thành công tới máy chủ...\n");

            Thread listener = new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        textArea.append("[MÁY CHỦ] " + msg + "\n");

                        if (msg.toLowerCase().contains("thắng")) {
                            myScore++;
                            updateScoreboard();
                        } else if (msg.toLowerCase().contains("thua")) {
                            opponentScore++;
                            updateScoreboard();
                        }
                    }
                } catch (IOException e) {
                    textArea.append("Kết nối đã đóng.\n");
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
                textArea.append("Bạn đã chọn: " + move + "\n");
            } catch (IOException e) {
                textArea.append("Lỗi khi gửi nước đi.\n");
            }
        } else {
            textArea.append("Chưa kết nối tới máy chủ!\n");
        }
    }

    private void quitGame() {
        try {
            if (out != null) {
                out.write("QUIT");
                out.newLine();
                out.flush();
            }
            textArea.append("Bạn đã thoát khỏi trò chơi.\n");
            if (socket != null) socket.close();
            dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateScoreboard() {
        SwingUtilities.invokeLater(() -> {
            scoreLabel.setText("Điểm của bạn: " + myScore + " | Điểm đối thủ: " + opponentScore);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}
