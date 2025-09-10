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
        System.out.println("=== Máy chủ Rock-Paper-Scissors đã được khởi động ===");
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

        public PlayerHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                BuffIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BuffOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                sendMessage("Chào mừng người chơi " + playerId);
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

                            // Bắt đầu game
                            new GameSession(this, opponent).start();
                        } else {
                            // Nếu vì lý do nào đó opponent null thì quay lại hàng chờ
                            waitingPlayer.put(playerId, this);
                            sendMessage("Đang chờ người chơi khác...");
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

        public void sendMessage(String msg) {
            try {
                BuffOut.write(msg);
                BuffOut.newLine();
                BuffOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
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
                p1.sendMessage("TRÒ CHƠI BẮT ĐẦU! Chọn: ROCK / PAPER / SCISSORS");
                p2.sendMessage("TRÒ CHƠI BẮT ĐẦU! Chọn: ROCK / PAPER / SCISSORS");

                p1.setMove(p1.receiveMessage());
                p2.setMove(p2.receiveMessage());

                String result = getResult(p1.getMove(), p2.getMove());
                p1.sendMessage("Bạn chọn: " + p1.getMove() + " | Đối thủ chọn: " + p2.getMove());
                p2.sendMessage("Bạn chọn: " + p2.getMove() + " | Đối thủ chọn: " + p1.getMove());

                if (result.equals("Hòa")) {
                    p1.sendMessage("Kết quả: Hòa");
                    p2.sendMessage("Kết quả: Hòa");
                } else if (result.equals("P1")) {
                    p1.sendMessage("Kết quả: Bạn đã thắng!");
                    p2.sendMessage("Kết quả: Bạn đã thua!");
                } else {
                    p1.sendMessage("Kết quả: Bạn đã thua!");
                    p2.sendMessage("Kết quả: Bạn đã thắng!");
                }

                p1.socket.close();
                p2.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String getResult(String move1, String move2) {
            if (move1.equals(move2)) return "Hòa";
            if ((move1.equals("ROCK") && move2.equals("SCISSORS")) ||
                (move1.equals("PAPER") && move2.equals("ROCK")) ||
                (move1.equals("SCISSORS") && move2.equals("PAPER"))) {
                return "P1";
            } else {
                return "P2";
            }
        }
    }
}
