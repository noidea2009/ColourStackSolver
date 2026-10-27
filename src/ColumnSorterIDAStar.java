import java.util.*;

public class ColumnSorterIDAStar {
    static final int COLS = 6;
    static final int COLORS = 4;

    static final String[] NAMES = {"Pink", "Yellow", "Green", "Blue"};
    static final char[] SYMBOLS = {'P', 'Y', 'G', 'B'};

    static int x;
    static int[] order = {0, 1, 2, 3};   // order[i] = color that belongs in column i (EDIT THIS)

    static final int FOUND = -1;
    static final int INF = Integer.MAX_VALUE;

    // Path of moves found so far (a stack: depth = number of moves made)
    static int[] pathFrom = new int[10000];
    static int[] pathTo = new int[10000];
    static int pathLen = 0;
    static long nodes = 0;

    static class State {
        int[][] cols = new int[COLS][x];
        int[] h = new int[COLS];

        // Only used once at the start and when replaying the solution
        State copy() {
            State s = new State();
            for (int i = 0; i < COLS; i++) {
                System.arraycopy(cols[i], 0, s.cols[i], 0, h[i]);
            }
            s.h = h.clone();
            return s;
        }

        void move(int a, int b) {
            cols[b][h[b]++] = cols[a][--h[a]];
        }

        void undo(int a, int b) {          // reverse of move(a, b)
            cols[a][h[a]++] = cols[b][--h[b]];
        }

        int heuristic() {
            int count = 0;
            for (int c = 0; c < COLS; c++) {
                int firstWrong = h[c];
                if (c >= COLORS) {
                    firstWrong = 0;
                } else {
                    for (int j = 0; j < h[c]; j++) {
                        if (cols[c][j] != order[c]) {
                            firstWrong = j;
                            break;
                        }
                    }
                }
                count += h[c] - firstWrong;
            }
            return count;
        }

        boolean isPure(int c) {
            if (h[c] == 0) return false;
            for (int j = 1; j < h[c]; j++) {
                if (cols[c][j] != cols[c][0]) return false;
            }
            return true;
        }
    }

    static State randomStart() {
        State s = new State();
        Random r = new Random();
        List<Integer> items = new ArrayList<>();

        for (int c = 0; c < COLORS; c++) {
            for (int k = 0; k < x; k++) items.add(c);
        }
        Collections.shuffle(items);

        for (int item : items) {
            int col;
            do {
                col = r.nextInt(COLS);
            } while (s.h[col] == x);
            s.cols[col][s.h[col]++] = item;
        }
        return s;
    }

    // Depth-first search limited by f = g + h <= bound.
    // Returns FOUND, or the smallest f that went over the bound.
    static int search(State s, int g, int bound, int lastFrom, int lastTo) {
        nodes++;
        int hc = s.heuristic();
        int f = g + hc;

        if (f > bound) return f;
        if (hc == 0) return FOUND;

        int min = INF;

        for (int a = 0; a < COLS; a++) {
            if (s.h[a] == 0) continue;

            for (int b = 0; b < COLS; b++) {
                if (a == b || s.h[b] == x) continue;
                if (s.h[b] == 0 && s.isPure(a)) continue;
                if (a == lastTo && b == lastFrom) continue;   // don't undo the previous move

                s.move(a, b);
                pathFrom[pathLen] = a;
                pathTo[pathLen] = b;
                pathLen++;

                int t = search(s, g + 1, bound, a, b);
                if (t == FOUND) return FOUND;   // keep the path, leave the board as is
                if (t < min) min = t;

                pathLen--;
                s.undo(a, b);
            }
        }
        return min;
    }

    // Returns the number of moves, or -1 if no solution. Moves are in pathFrom/pathTo.
    static int solve(State s) {
        int bound = s.heuristic();

        while (true) {
            System.out.println("Searching depth limit " + bound + " (nodes so far: " + nodes + ")");
            pathLen = 0;
            int t = search(s, 0, bound, -1, -1);

            if (t == FOUND) return pathLen;
            if (t == INF) return -1;
            bound = t;                          // next limit = smallest f that was cut off
        }
    }

    static void print(State s) {
        for (int c = 0; c < COLS; c++) {
            System.out.printf("%3d", c + 1);
        }
        System.out.println();

        for (int row = x - 1; row >= 0; row--) {
            for (int c = 0; c < COLS; c++) {
                char cell = row < s.h[c] ? SYMBOLS[s.cols[c][row]] : '.';
                System.out.printf("%3c", cell);
            }
            System.out.println();
        }
        System.out.println("-".repeat(COLS * 3));
    }

    // Replay the stored moves on a copy of the starting board.
    static void printSolution(State start, int moves) {
        State s = start.copy();

        System.out.println("\nInitial state:");
        print(s);

        for (int i = 0; i < moves; i++) {
            int from = pathFrom[i];
            int to = pathTo[i];

            s.move(from, to);
            int movedBlock = s.cols[to][s.h[to] - 1];

            System.out.printf("Move %d: %s (%c), Column %d -> Column %d%n",
                    i + 1, NAMES[movedBlock], SYMBOLS[movedBlock], from + 1, to + 1);
            print(s);
        }
        System.out.println("Solved in " + moves + " moves!");
    }

    public static void main(String[] args) {
        x = 10;                                  // start small, then raise it (see notes)

        State start = randomStart();

        System.out.println("Starting arrangement:");
        print(start);

        State work = start.copy();              // search edits this one; 'start' stays intact
        int moves = solve(work);

        if (moves < 0) {
            System.out.println("No solution found.");
            return;
        }
        printSolution(start, moves);
    }
}
