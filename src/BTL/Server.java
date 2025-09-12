package BTL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 30000;
    private static ConcurrentHashMap<Integer, PlayerHandler> waitingPlayer = new ConcurrentHashMap<>();
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
        private Socket socket;
        private int playerId;
        private BufferedReader BuffIn;
        private BufferedWriter BuffOut;
        private String move = null;
        private int score = 0; // điểm số người chơi

        public PlayerHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                BuffIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BuffOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                sendMessage("Chào mừng Người chơi " + playerId);
                sendMessage("Đang chờ người chơi khác...");

                PlayerHandler opponent = null;
                synchronized (waitingPlayer) {
                    if (waitingPlayer.isEmpty()) {
                        waitingPlayer.put(playerId, this);
                    } else {
                        int opponentId = waitingPlayer.keys().nextElement();
                        opponent = waitingPlayer.remove(opponentId);

                        if (opponent != null) {
                            sendMessage("Được ghép nối với Người chơi " + opponentId);
                            opponent.sendMessage("Được ghép nối với Người chơi " + playerId);

                            // bắt đầu game
                            new GameSession(this, opponent).start();
                        } else {
                            waitingPlayer.put(playerId, this);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Người chơi " + playerId + " bị ngắt kết nối.");
            }
        }

        public String getMove() {
            return move;
        }

        public void setMove(String move) {
            this.move = move;
        }

        public int getScore() {
            return score;
        }

        public void addScore() {
            this.score++;
        }

        public void sendMessage(String msg) {
            try {
                if (BuffOut != null) {
                    BuffOut.write(msg);
                    BuffOut.newLine();
                    BuffOut.flush();
                }
            } catch (IOException e) {
                System.out.println("Người chơi " + playerId + " đã ngắt kết nối, không thể gửi tin nhắn.");
                try {
                    socket.close();
                } catch (IOException ex) {
                }
            }
        }

        public String receiveMessage() throws IOException {
            return BuffIn.readLine();
        }
    }

    static class GameSession extends Thread {
        private PlayerHandler p1, p2;

        public GameSession(PlayerHandler p1, PlayerHandler p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    p1.sendMessage("TRÒ CHƠI BẮT ĐẦU! Chọn: KÉO / BÚA / BAO (hoặc QUIT để thoát)");
                    p2.sendMessage("TRÒ CHƠI BẮT ĐẦU! Chọn: KÉO / BÚA / BAO (hoặc QUIT để thoát)");

                    String move1 = p1.receiveMessage();
                    String move2 = p2.receiveMessage();

                    if (move1 == null || move1.equalsIgnoreCase("QUIT")) {
                        p2.sendMessage("Đối thủ đã thoát. Bạn thắng!");
                        break;
                    }
                    if (move2 == null || move2.equalsIgnoreCase("QUIT")) {
                        p1.sendMessage("Đối thủ đã thoát. Bạn thắng!");
                        break;
                    }

                    p1.setMove(move1);
                    p2.setMove(move2);

                    String result = getResult(move1, move2);

                    p1.sendMessage("Bạn chọn: " + move1 + " | Đối thủ chọn: " + move2);
                    p2.sendMessage("Bạn chọn: " + move2 + " | Đối thủ chọn: " + move1);

                    if (result.equals("Hòa")) {
                        p1.sendMessage("Kết quả: Hòa");
                        p2.sendMessage("Kết quả: Hòa");
                    } else if (result.equals("P1")) {
                        p1.addScore();
                        p1.sendMessage("Kết quả: Bạn đã thắng!");
                        p2.sendMessage("Kết quả: Bạn đã thua!");
                    } else {
                        p2.addScore();
                        p1.sendMessage("Kết quả: Bạn đã thua!");
                        p2.sendMessage("Kết quả: Bạn đã thắng!");
                    }

                    // Gửi bảng điểm
                    p1.sendMessage("Điểm số hiện tại: Bạn = " + p1.getScore() + " | Đối thủ = " + p2.getScore());
                    p2.sendMessage("Điểm số hiện tại: Bạn = " + p2.getScore() + " | Đối thủ = " + p1.getScore());
                }

            } catch (IOException e) {
                System.out.println("Một người chơi đã rời trận.");
            } finally {
                try { p1.socket.close(); } catch (Exception ignored) {}
                try { p2.socket.close(); } catch (Exception ignored) {}
            }
        }

        private String getResult(String move1, String move2) {
            if (move1.equalsIgnoreCase(move2)) return "Hòa";
            if ((move1.equalsIgnoreCase("BÚA") && move2.equalsIgnoreCase("KÉO")) ||
                (move1.equalsIgnoreCase("BAO") && move2.equalsIgnoreCase("BÚA")) ||
                (move1.equalsIgnoreCase("KÉO") && move2.equalsIgnoreCase("BAO"))) {
                return "P1";
            } else {
                return "P2";
            }
        }
    }
}
