package BTL;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 50000;

    // Danh sách toàn bộ người chơi online
    private static final ConcurrentHashMap<String, PlayerHandler> onlinePlayers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== Máy chủ Oẳn Tù Tì đã được khởi động ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new PlayerHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class PlayerHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private String username;
        private String status = "IDLE"; // IDLE = rảnh, BUSY = đang chơi

        public PlayerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Nhận username
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) {
                    username = "Người chơi " + socket.getPort();
                }

                // Thêm vào danh sách online
                onlinePlayers.put(username, this);
                System.out.println("👤 " + username + " đã vào server.");
                sendMessage("Xin chào " + username + "! Bạn đã kết nối thành công.\n" +
                        "👉 Gõ LIST để xem người chơi online.\n👉 Gõ CHALLENGE <tên> để thách đấu.");

                // Lắng nghe lệnh từ client
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equalsIgnoreCase("LIST")) {
                        handleList();
                    } else if (line.startsWith("CHALLENGE")) {
                        handleChallenge(line);
                    } else {
                        sendMessage("⚠ Lệnh không hợp lệ. Dùng: LIST hoặc CHALLENGE <username>");
                    }
                }

            } catch (IOException e) {
                System.out.println("⚠ " + username + " bị ngắt kết nối.");
            } finally {
                // Xóa khỏi danh sách online khi thoát
                onlinePlayers.remove(username);
                closeQuietly();
            }
        }

        private void handleList() {
            StringBuilder sb = new StringBuilder("👥 Người chơi online:\n");
            for (String name : onlinePlayers.keySet()) {
                if (!name.equals(username)) {
                    PlayerHandler ph = onlinePlayers.get(name);
                    sb.append("- ").append(name)
                      .append(" (").append(ph.status).append(")\n");
                }
            }
            sendMessage(sb.toString());
            StringBuilder listMsg = new StringBuilder("ONLINE:");
            for (String name : onlinePlayers.keySet()) {
                if (!name.equals(username)) {
                    listMsg.append(name).append(",");
                }
            }
            // Bỏ dấu phẩy cuối
            if (listMsg.charAt(listMsg.length() - 1) == ',') {
                listMsg.deleteCharAt(listMsg.length() - 1);
            }
            sendMessage(listMsg.toString());
        }


        private void handleChallenge(String line) {
            String[] parts = line.split(" ", 2);
            if (parts.length < 2) {
                sendMessage("⚠ Sai cú pháp! Dùng: CHALLENGE <username>");
                return;
            }
            String target = parts[1].trim();
            PlayerHandler opponent = onlinePlayers.get(target);

            if (opponent == null) {
                sendMessage("⚠ Người chơi không tồn tại hoặc đã thoát.");
            } else if (opponent.status.equals("BUSY")) {
                sendMessage("⚠ Người chơi " + target + " đang bận.");
            } else if (this.status.equals("BUSY")) {
                sendMessage("⚠ Bạn đang bận trong trận, không thể thách đấu.");
            } else {
                this.status = "BUSY";
                opponent.status = "BUSY";

                sendMessage("Bạn đã thách đấu " + target + " thành công!");
                opponent.sendMessage("⚔ Bạn đã được " + username + " thách đấu!");

                startGame(this, opponent);
            }
        }

        public synchronized void sendMessage(String msg) {
            try {
                out.write(msg);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                closeQuietly();
            }
        }

        public String receiveMessage() throws IOException {
            return in.readLine();
        }

        public boolean isConnected() {
            return socket != null && !socket.isClosed();
        }

        public void closeQuietly() {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void startGame(PlayerHandler a, PlayerHandler b) {
        System.out.println("🎮 Trận mới: " + a.username + " vs " + b.username);
        new GameSession(a, b).start();
    }

    static class GameSession extends Thread {
        private final PlayerHandler p1, p2;
        private int score1 = 0, score2 = 0;

        public GameSession(PlayerHandler p1, PlayerHandler p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public void run() {
            try {
                p1.sendMessage("🎮 Trận mới với " + p2.username + " bắt đầu!");
                p2.sendMessage("🎮 Trận mới với " + p1.username + " bắt đầu!");

                while (true) {
                    p1.sendMessage("👉 Chọn: KÉO / BÚA / BAO (QUIT để thoát)");
                    p2.sendMessage("👉 Chọn: KÉO / BÚA / BAO (QUIT để thoát)");

                    String m1 = p1.receiveMessage();
                    String m2 = p2.receiveMessage();

                    if (m1 == null || m1.equalsIgnoreCase("QUIT")) {
                        handleQuit(p1, p2);
                        return;
                    }
                    if (m2 == null || m2.equalsIgnoreCase("QUIT")) {
                        handleQuit(p2, p1);
                        return;
                    }

                    String result = getResult(m1.trim(), m2.trim());
                    if (result.equals("P1")) {
                        score1++;
                        p1.sendMessage("✅ Bạn thắng (" + m1 + " vs " + m2 + ")");
                        p2.sendMessage("❌ Bạn thua (" + m2 + " vs " + m1 + ")");
                    } else if (result.equals("P2")) {
                        score2++;
                        p1.sendMessage("❌ Bạn thua (" + m1 + " vs " + m2 + ")");
                        p2.sendMessage("✅ Bạn thắng (" + m2 + " vs " + m1 + ")");
                    } else {
                        p1.sendMessage("🤝 Hòa (" + m1 + " vs " + m2 + ")");
                        p2.sendMessage("🤝 Hòa (" + m2 + " vs " + m1 + ")");
                    }

                    p1.sendMessage("Điểm số: " + p1.username + " [" + score1 + "] - " + p2.username + " [" + score2 + "]");
                    p2.sendMessage("Điểm số: " + p2.username + " [" + score2 + "] - " + p1.username + " [" + score1 + "]");
                }
            } catch (Exception e) {
                System.out.println("⚠ Lỗi GameSession: " + e.getMessage());
            }
        }

        private void handleQuit(PlayerHandler quitter, PlayerHandler other) {
            System.out.println("ℹ " + quitter.username + " đã thoát trận.");
            quitter.closeQuietly();

            quitter.status = "IDLE"; // Người thoát quay về trạng thái rảnh
            if (other != null && other.isConnected()) {
                other.sendMessage("⚠ Đối thủ đã thoát. Trận dừng lại.");
                other.status = "IDLE";
            }
        }

        private String getResult(String m1, String m2) {
            if (m1.equalsIgnoreCase(m2)) return "DRAW";
            if ((m1.equalsIgnoreCase("BÚA") && m2.equalsIgnoreCase("KÉO")) ||
                (m1.equalsIgnoreCase("BAO") && m2.equalsIgnoreCase("BÚA")) ||
                (m1.equalsIgnoreCase("KÉO") && m2.equalsIgnoreCase("BAO"))) {
                return "P1";
            }
            return "P2";
        }
    }
}
