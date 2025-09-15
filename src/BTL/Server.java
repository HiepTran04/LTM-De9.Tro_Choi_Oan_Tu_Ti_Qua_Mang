package BTL;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

public class Server {
    private static final int PORT = 50000;

    // Hàng chờ cho 1 vs 1
    private static final Queue<PlayerHandler> waitingQueue = new LinkedList<>();
    private static final Object queueLock = new Object();

    private static int playerIdCounter = 1;

    public static void main(String[] args) {
        System.out.println("=== Máy chủ Oẳn Tù Tì đã được khởi động ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                PlayerHandler handler = new PlayerHandler(socket, playerIdCounter++);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class PlayerHandler extends Thread {
        private final Socket socket;
        private final int playerId;
        private BufferedReader in;
        private BufferedWriter out;
        private String username;
        private int score = 0;

        public PlayerHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Đọc username ngay khi kết nối
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) {
                    username = "Người chơi " + playerId;
                }

                sendMessage("Xin chào " + username + "! Bạn đã kết nối thành công.");

                // Ghép cặp
                synchronized (queueLock) {
                    if (!waitingQueue.isEmpty()) {
                        PlayerHandler other = waitingQueue.remove();
                        System.out.println("🔗 Ghép: " + username + " <-> " + other.username);

                        this.sendMessage("Bạn đã được ghép với " + other.username);
                        other.sendMessage("Bạn đã được ghép với " + this.username);

                        startGame(this, other);
                    } else {
                        waitingQueue.add(this);
                        System.out.println("[" + username + "] đang chờ đối thủ...");
                        sendMessage("Đang chờ người chơi khác...");
                    }
                }

            } catch (IOException e) {
                System.out.println("⚠ Người chơi " + playerId + " (" + username + ") bị ngắt kết nối khi đăng nhập.");
                synchronized (queueLock) {
                    waitingQueue.remove(this);
                }
                closeQuietly();
            }
        }

        public synchronized void sendMessage(String msg) {
            try {
                if (out != null) {
                    out.write(msg);
                    out.newLine();
                    out.flush();
                }
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

        public void resetScore() { score = 0; }
        public void addScore() { score++; }
        public int getScore() { return score; }
        public String getUsername() { return username; }

        public void closeQuietly() {
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        }

        public void requeueOrMatch() {
            synchronized (queueLock) {
                if (!waitingQueue.isEmpty()) {
                    PlayerHandler other = waitingQueue.remove();
                    System.out.println("🔗 (requeue) Ghép: " + username + " <-> " + other.username);
                    this.sendMessage("Bạn đã được ghép lại với " + other.username);
                    other.sendMessage("Bạn đã được ghép lại với " + this.username);
                    startGame(this, other);
                } else {
                    waitingQueue.add(this);
                    System.out.println("🔄 " + username + " quay lại hàng chờ.");
                    sendMessage("Bạn đã quay lại hàng chờ. Đang chờ đối thủ mới...");
                }
            }
        }
    }

    private static void startGame(PlayerHandler a, PlayerHandler b) {
        System.out.println("🎮 Trận mới: " + a.getUsername() + " vs " + b.getUsername());
        new GameSession(a, b).start();
    }

    static class GameSession extends Thread {
        private final PlayerHandler p1, p2;

        public GameSession(PlayerHandler p1, PlayerHandler p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public void run() {
            try {
                // Reset điểm khi bắt đầu
                p1.resetScore();
                p2.resetScore();
                p1.sendMessage("Trận mới bắt đầu! Điểm đã được reset.");
                p2.sendMessage("Trận mới bắt đầu! Điểm đã được reset.");

                while (true) {
                    p1.sendMessage("👉 Hãy chọn: KÉO / BÚA / BAO (hoặc QUIT để thoát)");
                    p2.sendMessage("👉 Hãy chọn: KÉO / BÚA / BAO (hoặc QUIT để thoát)");

                    String move1 = null, move2 = null;
                    try { move1 = p1.receiveMessage(); } catch (IOException ignored) {}
                    try { move2 = p2.receiveMessage(); } catch (IOException ignored) {}

                    if (move1 == null || move1.equalsIgnoreCase("QUIT")) {
                        handleQuit(p1, p2);
                        return;
                    }
                    if (move2 == null || move2.equalsIgnoreCase("QUIT")) {
                        handleQuit(p2, p1);
                        return;
                    }

                    String result = getResult(move1.trim(), move2.trim());

                    // Gửi kết quả rõ ràng
                    if (result.equals("P1")) {
                        p1.addScore();
                        p1.sendMessage("✅ Bạn (" + move1 + ") thắng " + p2.getUsername() + " (" + move2 + ")");
                        p2.sendMessage("❌ Bạn (" + move2 + ") thua " + p1.getUsername() + " (" + move1 + ")");
                    } else if (result.equals("P2")) {
                        p2.addScore();
                        p1.sendMessage("❌ Bạn (" + move1 + ") thua " + p2.getUsername() + " (" + move2 + ")");
                        p2.sendMessage("✅ Bạn (" + move2 + ") thắng " + p1.getUsername() + " (" + move1 + ")");
                    } else {
                        p1.sendMessage("🤝 Hòa! Bạn (" + move1 + ") vs " + p2.getUsername() + " (" + move2 + ")");
                        p2.sendMessage("🤝 Hòa! Bạn (" + move2 + ") vs " + p1.getUsername() + " (" + move1 + ")");
                    }

                    // Gửi điểm số cập nhật
                    p1.sendMessage("Điểm: " + p1.getUsername() + " [" + p1.getScore() + "] - " + p2.getUsername() + " [" + p2.getScore() + "]");
                    p2.sendMessage("Điểm: " + p2.getUsername() + " [" + p2.getScore() + "] - " + p1.getUsername() + " [" + p1.getScore() + "]");
                }
            } catch (Exception e) {
                System.out.println("⚠ Lỗi tại GameSession: " + e.getMessage());
            }
        }

        private void handleQuit(PlayerHandler quitter, PlayerHandler other) {
            System.out.println("ℹ " + quitter.getUsername() + " đã rời trận.");
            if (other.isConnected()) {
                other.sendMessage("Đối thủ (" + quitter.getUsername() + ") đã thoát. Bạn sẽ được ghép với người chơi mới!");
                other.requeueOrMatch();
            }
            quitter.closeQuietly();
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
