package BTL;

import java.io.*;
import java.util.*;

public class BangXepHangData {
    private static final String FILE_NAME = "bxh.dat";
    private static Map<String, Integer> ranking = new HashMap<>();

    static {
        loadFromFile(); // Tự động load khi class được nạp
    }

    // Cập nhật khi có kết quả trận đấu
    public static synchronized void updateWinLose(String winner, String loser) {
        ranking.put(winner, ranking.getOrDefault(winner, 0) + 3);
        ranking.put(loser, ranking.getOrDefault(loser, 0));
        saveToFile();
    }

    // Lấy BXH đã sắp xếp
    public static synchronized List<Map.Entry<String, Integer>> getRankingList() {
        return ranking.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();
    }

    // Lấy Map cho client
    public static synchronized Map<String, Integer> getRankingMap() {
        return new HashMap<>(ranking);
    }

    // Lưu BXH xuống file
    private static synchronized void saveToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(ranking);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load BXH từ file
    @SuppressWarnings("unchecked")
    private static synchronized void loadFromFile() {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            ranking = (Map<String, Integer>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
