import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.Timer;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.mongodb.client.*;
import org.bson.Document;
import com.mongodb.client.model.Sorts;

public class Sudoku extends JFrame {
    private JTextField[][] cells = new JTextField[9][9];
    private int[][] solution = new int[9][9];
    private long startTime;
    private Timer timer;
    private JLabel timerLabel;

    public Sudoku() {
        setTitle("Sudoku Game");
        setSize(600, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new GridLayout(9, 9));
        Font font = new Font("Arial", Font.BOLD, 20);

        int[][] puzzle = generateRandomPuzzle();
        solution = copyGrid(puzzle);

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                JTextField tf = new JTextField();
                tf.setHorizontalAlignment(JTextField.CENTER);
                tf.setFont(font);
                tf.setBorder(getCellBorder(i, j));

                if (puzzle[i][j] != 0) {
                    tf.setText(String.valueOf(puzzle[i][j]));
                    tf.setEditable(false);
                    tf.setBackground(Color.LIGHT_GRAY);
                } else {
                    final int row = i;
                    final int col = j;
                    tf.getDocument().addDocumentListener(new DocumentListener() {
                        public void insertUpdate(DocumentEvent e) { updateColor(tf, row, col); }
                        public void removeUpdate(DocumentEvent e) { updateColor(tf, row, col); }
                        public void changedUpdate(DocumentEvent e) { updateColor(tf, row, col); }
                    });
                    tf.addKeyListener(new KeyAdapter() {
                        public void keyTyped(KeyEvent e) {
                            char c = e.getKeyChar();
                            if (!Character.isDigit(c) || c == '0' || tf.getText().length() >= 1) {
                                e.consume();
                            }
                        }
                    });
                }
                cells[i][j] = tf;
                gridPanel.add(tf);
            }
        }

        JButton checkButton = new JButton("Submit");
        checkButton.addActionListener(e -> checkSolution());

        JButton leaderboardButton = new JButton("Leaderboard");
        leaderboardButton.addActionListener(e -> showLeaderboard());

        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> {
            dispose();
            new Sudoku();
        });

        timerLabel = new JLabel("Time: 0s");
        timer = new Timer(1000, e -> updateTimer());
        timer.start();

        JPanel controlPanel = new JPanel();
        controlPanel.add(checkButton);
        controlPanel.add(leaderboardButton);
        controlPanel.add(newGameButton);
        controlPanel.add(timerLabel);

        add(gridPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        setVisible(true);
        startTime = System.currentTimeMillis();
    }

    private Border getCellBorder(int row, int col) {
        int top = (row % 3 == 0) ? 3 : 1;
        int left = (col % 3 == 0) ? 3 : 1;
        int bottom = (row == 8) ? 3 : 1;
        int right = (col == 8) ? 3 : 1;
        return BorderFactory.createMatteBorder(top, left, bottom, right, Color.BLACK);
    }

    private void updateTimer() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        timerLabel.setText("Time: " + elapsed + "s");
    }

    private void updateColor(JTextField tf, int row, int col) {
        String text = tf.getText();
        if (text.matches("[1-9]")) {
            int val = Integer.parseInt(text);
            boolean duplicate = false;

            // Check for duplicates in row
            for (int j = 0; j < 9; j++) {
                if (j != col && cells[row][j].getText().equals(text)) {
                    duplicate = true;
                    break;
                }
            }

            // Check for duplicates in column
            for (int i = 0; i < 9; i++) {
                if (i != row && cells[i][col].getText().equals(text)) {
                    duplicate = true;
                    break;
                }
            }

            // Check for duplicates in 3x3 box
            int startRow = (row / 3) * 3;
            int startCol = (col / 3) * 3;
            for (int i = startRow; i < startRow + 3; i++) {
                for (int j = startCol; j < startCol + 3; j++) {
                    if ((i != row || j != col) && cells[i][j].getText().equals(text)) {
                        duplicate = true;
                        break;
                    }
                }
                if (duplicate) break;
            }

            if (duplicate) {
                tf.setBackground(Color.RED);
            } else if (val == solution[row][col]) {
                tf.setBackground(Color.GREEN);
            } else {
                tf.setBackground(Color.WHITE);
            }
        } else {
            tf.setBackground(Color.WHITE);
        }
    }

    private int[][] generateRandomPuzzle() {
        int[][] fullGrid = new int[9][9];
        generateFullGrid(fullGrid);
        int[][] puzzle = copyGrid(fullGrid);

        Random rand = new Random();
        int cluesToKeep = 30;
        Set<String> kept = new HashSet<>();

        while (kept.size() < cluesToKeep) {
            int i = rand.nextInt(9);
            int j = rand.nextInt(9);
            kept.add(i + "," + j);
        }

        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                if (!kept.contains(i + "," + j))
                    puzzle[i][j] = 0;

        return puzzle;
    }

    private boolean generateFullGrid(int[][] grid) {
        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= 9; i++) nums.add(i);
        return fillGrid(grid, 0, 0, nums);
    }

    private boolean fillGrid(int[][] grid, int row, int col, List<Integer> nums) {
        if (row == 9) return true;
        if (col == 9) return fillGrid(grid, row + 1, 0, nums);

        Collections.shuffle(nums);
        for (int num : nums) {
            if (isSafe(grid, row, col, num)) {
                grid[row][col] = num;
                if (fillGrid(grid, row, col + 1, nums)) return true;
                grid[row][col] = 0;
            }
        }
        return false;
    }

    private boolean isSafe(int[][] grid, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (grid[row][i] == num || grid[i][col] == num) return false;
            if (grid[(row / 3) * 3 + i / 3][(col / 3) * 3 + i % 3] == num) return false;
        }
        return true;
    }

    private int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[9][9];
        for (int i = 0; i < 9; i++)
            System.arraycopy(grid[i], 0, copy[i], 0, 9);
        return copy;
    }

    private void checkSolution() {
        int[][] userSolution = new int[9][9];

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                String val = cells[i][j].getText();
                if (!val.matches("[1-9]")) {
                    JOptionPane.showMessageDialog(this, "Invalid value at " + (i + 1) + "," + (j + 1));
                    return;
                }
                userSolution[i][j] = Integer.parseInt(val);
            }
        }

        if (!isValidSudoku(userSolution)) {
            JOptionPane.showMessageDialog(this, "Incorrect Solution!");
        } else {
            long endTime = System.currentTimeMillis();
            long timeTaken = (endTime - startTime) / 1000;
            String name = JOptionPane.showInputDialog(this, "Solved! Enter your name:");
            if (name != null && !name.isEmpty()) {
                saveScore(name, timeTaken);
                JOptionPane.showMessageDialog(this, "Time: " + timeTaken + " seconds\nSaved to leaderboard.");
            }
        }
    }

    private boolean isValidSudoku(int[][] board) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                int num = board[i][j];
                if (num < 1 || num > 9) return false;
                String row = num + " in row " + i;
                String col = num + " in col " + j;
                String box = num + " in box " + (i / 3) + "-" + (j / 3);
                if (!seen.add(row) || !seen.add(col) || !seen.add(box)) return false;
            }
        return true;
    }

    private void saveScore(String name, long timeTaken) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = client.getDatabase("sudoku_game");
            MongoCollection<Document> scores = db.getCollection("scores");

            Document doc = new Document("name", name)
                    .append("time", timeTaken)
                    .append("timestamp", new Date());
            scores.insertOne(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLeaderboard() {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = client.getDatabase("sudoku_game");
            MongoCollection<Document> scores = db.getCollection("scores");

            FindIterable<Document> topScores = scores.find()
                    .sort(Sorts.ascending("time"))
                    .limit(5);

            StringBuilder sb = new StringBuilder("Top 5 Fastest Players:\n\n");
            for (Document doc : topScores) {
                sb.append(doc.getString("name"))
                        .append(" - ")
                        .append(doc.getLong("time"))
                        .append(" sec\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString(), "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Sudoku::new);
    }
}
