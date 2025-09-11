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
    private JButton rockButton, paperButton, scissorsButton;

    public Client() {
        setTitle("Rock-Paper-Scissors Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        rockButton = new JButton("ROCK");
        paperButton = new JButton("PAPER");
        scissorsButton = new JButton("SCISSORS");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(rockButton);
        buttonPanel.add(paperButton);
        buttonPanel.add(scissorsButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Sự kiện click nút
        rockButton.addActionListener(e -> sendMove("ROCK"));
        paperButton.addActionListener(e -> sendMove("PAPER"));
        scissorsButton.addActionListener(e -> sendMove("SCISSORS"));

        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            textArea.append("Connected to server...\n");

            // Thread nhận tin nhắn từ server
            Thread listener = new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        textArea.append("[SERVER] " + msg + "\n");
                    }
                } catch (IOException e) {
                    textArea.append("Connection closed.\n");
                }
            });
            listener.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect to server!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void sendMove(String move) {
        if (out != null) {
            try {
                out.write(move);
                out.newLine();   // giống println()
                out.flush();     // bắt buộc flush để gửi đi ngay
                textArea.append("You chose: " + move + "\n");
            } catch (IOException e) {
                textArea.append("Error sending message!\n");
                e.printStackTrace();
            }
        } else {
            textArea.append("Not connected to server!\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}
