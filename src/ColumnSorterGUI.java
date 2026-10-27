import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Deque;

public class ColumnSorterGUI extends JFrame {
    static final int COLS = ColumnSorterGreedy.COLS;
    static final int COLORS = ColumnSorterGreedy.COLORS;
    static final String[] NAMES = ColumnSorterGreedy.NAMES;
    static final Color[] SHADES = {
            new Color(255, 105, 180),   // Pink
            new Color(255, 215, 0),     // Yellow
            new Color(50, 180, 75),     // Green
            new Color(30, 100, 230)};   // Blue

    static final long SEARCH_MS = 5_000;   // how long the solver keeps looking for shorter solutions

    private ColumnSorterGreedy.State start;   // arrangement the user builds
    private ColumnSorterGreedy.State view;    // what the board currently shows
    private int step = 0;                   // current move in the replay
    private boolean replaying = false;      // false = setup mode, true = replay mode
    private int selected = 0;               // selected palette color
    private final javax.swing.Timer playTimer;

    private final BoardPanel board = new BoardPanel();
    private final JToggleButton[] paletteBtns = new JToggleButton[COLORS];
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JButton undoBtn = new JButton("Undo");
    private final JButton clearBtn = new JButton("Clear");
    private final JButton solveBtn = new JButton("Solve");
    private final JButton prevBtn = new JButton("< Prev");
    private final JButton playBtn = new JButton("Play");
    private final JButton nextBtn = new JButton("Next >");
    private final JButton editBtn = new JButton("Edit again");

    private final Deque<Integer> placedOrder = new ArrayDeque<>();   // columns, for Undo

    public ColumnSorterGUI() {
        super("Column Sorter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        start = new ColumnSorterGreedy.State();
        view = start;

        // palette
        JPanel palette = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < COLORS; i++) {
            final int idx = i;
            paletteBtns[i] = new JToggleButton();
            paletteBtns[i].setBackground(SHADES[i]);
            paletteBtns[i].setOpaque(true);
            paletteBtns[i].setFocusPainted(false);
            paletteBtns[i].addActionListener(e -> selected = idx);
            group.add(paletteBtns[i]);
            palette.add(paletteBtns[i]);
        }
        paletteBtns[0].setSelected(true);

        // controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        controls.add(undoBtn);
        controls.add(clearBtn);
        controls.add(solveBtn);
        controls.add(prevBtn);
        controls.add(playBtn);
        controls.add(nextBtn);
        controls.add(editBtn);

        JPanel south = new JPanel(new BorderLayout());
        south.add(status, BorderLayout.NORTH);
        south.add(controls, BorderLayout.CENTER);

        add(palette, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        undoBtn.addActionListener(e -> undo());
        clearBtn.addActionListener(e -> clearAll());
        solveBtn.addActionListener(e -> startSolving());
        prevBtn.addActionListener(e -> stepBack());
        nextBtn.addActionListener(e -> stepForward());
        playBtn.addActionListener(e -> togglePlay());
        editBtn.addActionListener(e -> backToEdit());

        playTimer = new javax.swing.Timer(500, e -> {
            if (step < ColumnSorterGreedy.bestLen) stepForward();
            else stopPlay();
        });

        refresh();
        pack();
        setLocationRelativeTo(null);
    }

    // ---------- setup ----------

    private int placed(int color) {
        int n = 0;
        for (int c = 0; c < COLS; c++)
            for (int j = 0; j < start.h[c]; j++) if (start.cols[c][j] == color) n++;
        return n;
    }

    private int totalPlaced() {
        int n = 0;
        for (int c = 0; c < COLS; c++) n += start.h[c];
        return n;
    }

    private void place(int col) {
        if (replaying || start.h[col] >= ColumnSorterGreedy.x || placed(selected) >= ColumnSorterGreedy.x) return;
        start.cols[col][start.h[col]++] = selected;
        placedOrder.push(col);
        refresh();
    }

    private void removeTop(int col) {
        if (replaying || start.h[col] == 0) return;
        start.h[col]--;
        placedOrder.removeFirstOccurrence(col);
        refresh();
    }

    private void undo() {
        if (replaying || placedOrder.isEmpty()) return;
        int col = placedOrder.pop();
        start.h[col]--;
        refresh();
    }

    private void clearAll() {
        if (replaying) return;
        start = new ColumnSorterGreedy.State();
        view = start;
        placedOrder.clear();
        refresh();
    }

    // ---------- solving / replay ----------

    private void startSolving() {
        solveBtn.setEnabled(false);
        undoBtn.setEnabled(false);
        clearBtn.setEnabled(false);
        status.setText("Searching for a short solution (about " + SEARCH_MS / 1000 + " s)...");

        final ColumnSorterGreedy.State snapshot = start.copy();
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return ColumnSorterGreedy.findBestSolution(snapshot, SEARCH_MS);
            }

            @Override protected void done() {
                try {
                    if (get()) {
                        replaying = true;
                        view = start.copy();
                        step = 0;
                    } else {
                        JOptionPane.showMessageDialog(ColumnSorterGUI.this,
                                "No solution found for this arrangement.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ColumnSorterGUI.this,
                            "Error while solving: " + ex.getMessage());
                }
                refresh();
            }
        }.execute();
    }

    private void stepForward() {
        if (!replaying || step >= ColumnSorterGreedy.bestLen) return;
        view.move(ColumnSorterGreedy.bestFrom[step], ColumnSorterGreedy.bestTo[step]);
        step++;
        refresh();
    }

    private void stepBack() {
        if (!replaying || step <= 0) return;
        step--;
        view.move(ColumnSorterGreedy.bestTo[step], ColumnSorterGreedy.bestFrom[step]);   // reverse the move
        refresh();
    }

    private void togglePlay() {
        if (playTimer.isRunning()) {
            stopPlay();
        } else {
            if (step >= ColumnSorterGreedy.bestLen) {
                view = start.copy();
                step = 0;
            }
            playTimer.start();
            playBtn.setText("Pause");
        }
    }

    private void stopPlay() {
        playTimer.stop();
        playBtn.setText("Play");
    }

    private void backToEdit() {
        stopPlay();
        replaying = false;
        view = start;
        refresh();
    }

    // ---------- update the window ----------

    private void refresh() {
        int x = ColumnSorterGreedy.x;
        int total = ColumnSorterGreedy.bestLen;

        // if the selected color ran out, jump to one that still has blocks
        if (!replaying && placed(selected) >= x) {
            for (int i = 0; i < COLORS; i++) {
                if (placed(i) < x) {
                    selected = i;
                    paletteBtns[i].setSelected(true);
                    break;
                }
            }
        }

        for (int i = 0; i < COLORS; i++) {
            int left = x - placed(i);
            paletteBtns[i].setText(NAMES[i] + " (" + left + " left)");
            paletteBtns[i].setEnabled(!replaying && left > 0);
        }

        undoBtn.setVisible(!replaying);
        clearBtn.setVisible(!replaying);
        solveBtn.setVisible(!replaying);
        prevBtn.setVisible(replaying);
        playBtn.setVisible(replaying);
        nextBtn.setVisible(replaying);
        editBtn.setVisible(replaying);

        undoBtn.setEnabled(!placedOrder.isEmpty());
        clearBtn.setEnabled(totalPlaced() > 0);
        solveBtn.setEnabled(totalPlaced() == COLORS * x);
        prevBtn.setEnabled(replaying && step > 0);
        nextBtn.setEnabled(replaying && step < total);

        if (replaying) {
            if (step == 0) {
                status.setText("Solution found: " + total + " moves. Use Next or Play.");
            } else {
                int to = ColumnSorterGreedy.bestTo[step - 1];
                int blk = view.cols[to][view.h[to] - 1];
                status.setText("Move " + step + "/" + total + ": " + NAMES[blk]
                        + ", column " + (ColumnSorterGreedy.bestFrom[step - 1] + 1)
                        + " -> column " + (to + 1) + (step == total ? "  (solved!)" : ""));
            }
        } else {
            status.setText("Pick a color, then click a column to add it on top. Right-click removes the top block. ("
                    + totalPlaced() + "/" + COLORS * x + " placed)");
        }

        board.repaint();
        pack();
    }

    // ---------- board drawing ----------

    private class BoardPanel extends JPanel {
        static final int COL_W = 80, GAP = 10, MARGIN = 20, LABEL_H = 30;

        BoardPanel() {
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    int col = columnAt(e.getX());
                    if (col < 0) return;
                    if (SwingUtilities.isRightMouseButton(e)) removeTop(col);
                    else place(col);
                }
            });
        }

        int columnAt(int mx) {
            for (int c = 0; c < COLS; c++) {
                int x0 = MARGIN + c * (COL_W + GAP);
                if (mx >= x0 && mx < x0 + COL_W) return c;
            }
            return -1;
        }

        int cellH() {
            return Math.max(12, Math.min(40, 600 / Math.max(1, ColumnSorterGreedy.x)));
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(MARGIN * 2 + COLS * COL_W + (COLS - 1) * GAP,
                    cellH() * ColumnSorterGreedy.x + LABEL_H + 30);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = ColumnSorterGreedy.x;
            int ch = cellH();
            int bottom = 15 + ch * x;

            for (int c = 0; c < COLS; c++) {
                int x0 = MARGIN + c * (COL_W + GAP);
                g.setColor(new Color(235, 235, 235));
                g.fillRoundRect(x0, 15, COL_W, ch * x, 12, 12);
                g.setColor(Color.GRAY);
                g.drawRoundRect(x0, 15, COL_W, ch * x, 12, 12);

                for (int r = 0; r < view.h[c]; r++) {
                    int y = bottom - (r + 1) * ch;
                    g.setColor(SHADES[view.cols[c][r]]);
                    g.fillRoundRect(x0 + 4, y + 2, COL_W - 8, ch - 4, 10, 10);
                    g.setColor(Color.DARK_GRAY);
                    g.drawRoundRect(x0 + 4, y + 2, COL_W - 8, ch - 4, 10, 10);
                }

                g.setColor(Color.BLACK);
                String label = String.valueOf(c + 1);
                int w = g.getFontMetrics().stringWidth(label);
                g.drawString(label, x0 + (COL_W - w) / 2, bottom + 20);
            }
        }
    }

    // ---------- main ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int value = 0;
            while (value < 1) {
                String input = JOptionPane.showInputDialog(null,
                        "How many blocks of each color (x)?", "Column Sorter",
                        JOptionPane.QUESTION_MESSAGE);
                if (input == null) return;   // cancelled
                try {
                    value = Integer.parseInt(input.trim());
                    if (value < 1 || value > 60) {
                        JOptionPane.showMessageDialog(null, "Please enter a number from 1 to 60.");
                        value = 0;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Please enter a whole number.");
                }
            }
            ColumnSorterGreedy.x = value;   // must be set before any State is created
            new ColumnSorterGUI().setVisible(true);
        });
    }
}