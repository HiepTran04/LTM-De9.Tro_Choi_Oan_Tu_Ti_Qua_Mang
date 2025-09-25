package BTL;

import java.io.*;
import java.net.*;
import java.text.Normalizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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
        private String status = "ONLINE";
        private GameSession currentSession;

        public PlayerHandler(Socket socket) {
            this.socket = socket;
        }

        public void setSession(GameSession session) {
            this.currentSession = session;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Nhận username
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) {
                    username = "NguoiChoi" + socket.getPort();
                }

                // Thêm vào danh sách online
                onlinePlayers.put(username, this);
                System.out.println(username + " đã vào server.");
                sendMessage("Xin chào " + username + "! Bạn đã kết nối thành công.\n");

                // Lắng nghe lệnh từ client
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (status.equals("BUSY")) {
                        if (currentSession != null) {
                            currentSession.submitMove(this, line);
                        }
                        continue;
                    }

                    if (line.equalsIgnoreCase("LIST")) {
                        handleList();
                    } else if (line.startsWith("CHALLENGE")) {
                        handleChallenge(line);
                    } else if (line.equalsIgnoreCase("BXH")) {
                        handleRanking();
                    } else {
                        sendMessage("Lệnh không hợp lệ.");
                    }
                }

            } catch (IOException e) {
                System.out.println(username + " bị ngắt kết nối.");
            } finally {
                onlinePlayers.remove(username);
                closeQuietly();
            }
        }

        private void handleList() {
            StringBuilder listMsg = new StringBuilder("ONLINE:");
            for (String name : onlinePlayers.keySet()) {
                if (!name.equals(username)) {
                    listMsg.append(name).append(",");
                }
            }
            if (listMsg.length() > 7 && listMsg.charAt(listMsg.length() - 1) == ',') {
                listMsg.deleteCharAt(listMsg.length() - 1);
            }
            sendMessage(listMsg.toString());
        }

        private void handleChallenge(String line) {
            String[] parts = line.split(" ", 2);
            if (parts.length < 2) {
                sendMessage("Sai cú pháp! Dùng: CHALLENGE <username>");
                return;
            }
            String target = parts[1].trim();
            PlayerHandler opponent = onlinePlayers.get(target);

            if (opponent == null) {
                sendMessage("Người chơi không tồn tại hoặc đã thoát.");
            } else if (opponent.status.equals("BUSY")) {
                sendMessage("Người chơi " + target + " đang bận.");
            } else if (this.status.equals("BUSY")) {
                sendMessage("Bạn đang bận trong trận, không thể thách đấu.");
            } else {
                this.status = "BUSY";
                opponent.status = "BUSY";

                sendMessage("Bạn đã thách đấu " + target + " thành công!");
                opponent.sendMessage("Bạn đã được " + username + " thách đấu!");

                startGame(this, opponent);
            }
        }

        private void handleRanking() {
            StringBuilder sb = new StringBuilder("BXH:");
            BangXepHangData.getRankingList().forEach(entry -> 
                sb.append(entry.getKey())
                  .append("[")
                  .append(entry.getValue())
                  .append("],")
            );
            if (sb.length() > 4 && sb.charAt(sb.length() - 1) == ',') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sendMessage(sb.toString());
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
        System.out.println("Trận mới: " + a.username + " vs " + b.username);
        GameSession session = new GameSession(a, b);
        a.setSession(session);
        b.setSession(session);
        session.start();
    }

    static class GameSession extends Thread {
        private final PlayerHandler p1, p2;
        private int score1 = 0, score2 = 0;
        private String move1 = null, move2 = null;

        public GameSession(PlayerHandler p1, PlayerHandler p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public void run() {
            try {
                p1.sendMessage("Trận mới với " + p2.username + " bắt đầu!");
                p2.sendMessage("Trận mới với " + p1.username + " bắt đầu!");
                p1.sendMessage("Chọn: KÉO / BÚA / BAO");
                p2.sendMessage("Chọn: KÉO / BÚA / BAO");
            } catch (Exception e) {
                System.out.println("Lỗi GameSession: " + e.getMessage());
            }
        }

        public synchronized void submitMove(PlayerHandler player, String move) {
            move = normalize(move);

            if (!(move.equals("BUA") || move.equals("BAO") || move.equals("KEO") || move.equals("QUIT"))) {
                player.sendMessage("Nước đi không hợp lệ!");
                return;
            }

            if ("QUIT".equals(move)) {
                handleQuit(player, (player == p1 ? p2 : p1));
                return;
            }

            if (player == p1 && move1 == null) move1 = move;
            if (player == p2 && move2 == null) move2 = move;

            if (move1 != null && move2 != null) {
                resolveRound();
            }
        }

        private synchronized void resolveRound() {
            String m1 = move1, m2 = move2;
            move1 = move2 = null;

            String result = getResult(m1, m2);
            if (result.equals("P1")) {
                score1++;
                BangXepHangData.updateWinLose(p1.username, p2.username);
                p1.sendMessage("Bạn thắng (" + m1 + " vs " + m2 + ")");
                p2.sendMessage("Bạn thua (" + m2 + " vs " + m1 + ")");
            } else if (result.equals("P2")) {
                score2++;
                BangXepHangData.updateWinLose(p2.username, p1.username);
                p1.sendMessage("Bạn thua (" + m1 + " vs " + m2 + ")");
                p2.sendMessage("Bạn thắng (" + m2 + " vs " + m1 + ")");
            } else {
                p1.sendMessage("Hòa (" + m1 + " vs " + m2 + ")");
                p2.sendMessage("Hòa (" + m2 + " vs " + m1 + ")");
            }

            p1.sendMessage("Điểm: " + p1.username + " [" + score1 + "] - " + p2.username + " [" + score2 + "]");
            p2.sendMessage("Điểm: " + p2.username + " [" + score2 + "] - " + p1.username + " [" + score1 + "]");

            p1.sendMessage("Chọn: KÉO / BÚA / BAO");
            p2.sendMessage("Chọn: KÉO / BÚA / BAO");
        }

        private void handleQuit(PlayerHandler quitter, PlayerHandler other) {
            System.out.println(quitter.username + " đã thoát trận.");
            quitter.status = "ONLINE";
            quitter.setSession(null);
            if (other != null && other.isConnected()) {
                other.sendMessage("Đối thủ đã thoát. Trận dừng lại.");
                other.status = "ONLINE";
                other.setSession(null);
            }
        }

        private String normalize(String input) {
            if (input == null) return "";
            String s = Normalizer.normalize(input, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            return s.toUpperCase();
        }

        private String getResult(String m1, String m2) {
            if (m1.equals(m2)) return "DRAW";
            if ((m1.equals("BUA") && m2.equals("KEO")) ||
                (m1.equals("BAO") && m2.equals("BUA")) ||
                (m1.equals("KEO") && m2.equals("BAO"))) {
                return "P1";
            }
            return "P2";
        }
    }
}
