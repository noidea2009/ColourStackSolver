import java.util.*;

public class ColumnSorterGreedy {
    static final int COLS = 6;
    static final int COLORS = 4;

    static final String[] NAMES = {"Pink", "Yellow", "Green", "Blue"};
    static final char[] SYMBOLS = {'P', 'Y', 'G', 'B'};

    static int x;
    static int[] order = {0, 1, 2, 3};   // order[i] = color that belongs in column i (EDIT THIS)

    static final long TIME_LIMIT_MS = 10_000;   // how long to keep trying for shorter solutions

    // Moves of the attempt in progress
    static int[] pathFrom = new int[5000];
    static int[] pathTo = new int[5000];
    static int pathLen = 0;

    // Best solution found so far
    static int[] bestFrom = new int[5000];
    static int[] bestTo = new int[5000];
    static int bestLen = Integer.MAX_VALUE;

    // Stats from the last findBestSolution call
    static int firstLen;
    static int afterOrders;
    static long tries;

    static class State {
        int[][] cols = new int[COLS][x];
        int[] h = new int[COLS];

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

        boolean isSolved() {
            for (int i = 0; i < COLORS; i++) {
                if (h[i] != x) return false;
                for (int j = 0; j < x; j++) {
                    if (cols[i][j] != order[i]) return false;
                }
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

    static void doMove(State s, int a, int b) {
        s.move(a, b);
        pathFrom[pathLen] = a;
        pathTo[pathLen] = b;
        pathLen++;
    }

    // rnd == null -> always deterministic; otherwise ties are broken randomly
    static int pickDest(State s, int color, boolean[] done, int ex1, int ex2, Random rnd) {
        int best = -1, bestKey = Integer.MAX_VALUE;

        for (int b = 0; b < COLS; b++) {
            if (done[b] || b == ex1 || b == ex2 || s.h[b] == x) continue;

            int score;
            if (s.h[b] > 0 && s.cols[b][s.h[b] - 1] == color) score = 0;
            else if (s.h[b] == 0) score = 1;
            else if (b >= COLORS) score = 2;
            else score = 3;

            int key = score * 4 + (rnd == null ? 0 : rnd.nextInt(5));
            if (key < bestKey) {
                bestKey = key;
                best = b;
            }
        }
        if (best == -1) throw new IllegalStateException("No room to dump a block");
        return best;
    }

    // Finish the target columns in the order given by seq
    static boolean solve(State s, int[] seq, Random rnd) {
        boolean[] done = new boolean[COLS];

        for (int t : seq) {
            int color = order[t];

            // Step 1: keep the correct bottom run, move everything above it away
            int keep = 0;
            while (keep < s.h[t] && s.cols[t][keep] == color) keep++;

            while (s.h[t] > keep) {
                int blk = s.cols[t][s.h[t] - 1];
                int dest = pickDest(s, blk, done, t, -1, rnd);
                doMove(s, t, dest);
            }

            // Step 2: pull blocks of this color into t until it is full
            while (s.h[t] < x) {
                int bestCol = -1, bestKey = Integer.MAX_VALUE;

                for (int c = 0; c < COLS; c++) {
                    if (c == t || done[c]) continue;
                    for (int j = s.h[c] - 1; j >= 0; j--) {
                        if (s.cols[c][j] == color) {
                            int depth = s.h[c] - 1 - j;
                            int key = depth * 2 + (rnd == null ? 0 : rnd.nextInt(3));
                            if (key < bestKey) {
                                bestKey = key;
                                bestCol = c;
                            }
                            break;
                        }
                    }
                }
                if (bestCol == -1) return false;

                while (s.cols[bestCol][s.h[bestCol] - 1] != color) {
                    int blk = s.cols[bestCol][s.h[bestCol] - 1];
                    int dest = pickDest(s, blk, done, t, bestCol, rnd);
                    doMove(s, bestCol, dest);
                }
                doMove(s, bestCol, t);
            }
            done[t] = true;
        }
        return s.isSolved();
    }

    // Remove wasted move pairs: (a->b, b->c) becomes (a->c), or disappears if a == c.
    // Returns the new number of moves.
    static int optimize(int[] from, int[] to, int n) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i + 1 < n; i++) {
                if (to[i] == from[i + 1]) {
                    int removeFrom;
                    int removeCount;
                    if (from[i] == to[i + 1]) {          // a->b then b->a: cancel both
                        removeFrom = i;
                        removeCount = 2;
                    } else {                              // merge into a->c
                        to[i] = to[i + 1];
                        removeFrom = i + 1;
                        removeCount = 1;
                    }
                    for (int k = removeFrom; k + removeCount < n; k++) {
                        from[k] = from[k + removeCount];
                        to[k] = to[k + removeCount];
                    }
                    n -= removeCount;
                    changed = true;
                    break;
                }
            }
        }
        return n;
    }

    // One full attempt; keeps it if it beats the best so far
    static void attempt(State start, int[] seq, Random rnd) {
        State s = start.copy();
        pathLen = 0;
        try {
            if (!solve(s, seq, rnd)) return;
        } catch (IllegalStateException e) {   // ran out of room to dump blocks: skip this attempt
            return;
        }

        int n = optimize(pathFrom, pathTo, pathLen);
        if (n < bestLen) {
            bestLen = n;
            System.arraycopy(pathFrom, 0, bestFrom, 0, n);
            System.arraycopy(pathTo, 0, bestTo, 0, n);
        }
    }

    static void permutations(int[] a, int k, List<int[]> out) {
        if (k == a.length) {
            out.add(a.clone());
            return;
        }
        for (int i = k; i < a.length; i++) {
            int tmp = a[k]; a[k] = a[i]; a[i] = tmp;
            permutations(a, k + 1, out);
            tmp = a[k]; a[k] = a[i]; a[i] = tmp;
        }
    }

    // Runs the whole search (baseline, all 24 orders, random restarts) on the given start state.
    // The result is left in bestFrom / bestTo / bestLen. Returns true if any solution was found.
    static boolean findBestSolution(State start, long timeLimitMs) {
        bestLen = Integer.MAX_VALUE;

        // 1. Baseline: the original column order 1,2,3,4
        attempt(start, new int[]{0, 1, 2, 3}, null);
        firstLen = bestLen;

        // 2. Every possible column order
        List<int[]> perms = new ArrayList<>();
        permutations(new int[]{0, 1, 2, 3}, 0, perms);
        for (int[] p : perms) attempt(start, p, null);
        afterOrders = bestLen;

        // 3. Random restarts until the time limit
        Random rnd = new Random();
        int[] seq = {0, 1, 2, 3};
        long end = System.currentTimeMillis() + timeLimitMs;
        tries = 0;
        while (System.currentTimeMillis() < end) {
            for (int i = COLORS - 1; i > 0; i--) {          // shuffle the order
                int j = rnd.nextInt(i + 1);
                int tmp = seq[i]; seq[i] = seq[j]; seq[j] = tmp;
            }
            attempt(start, seq, rnd);
            tries++;
        }

        return bestLen != Integer.MAX_VALUE;
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

    static void printSolution(State start, int moves) {
        State s = start.copy();

        System.out.println("\nInitial state:");
        print(s);

        for (int i = 0; i < moves; i++) {
            int from = bestFrom[i];
            int to = bestTo[i];

            s.move(from, to);
            int movedBlock = s.cols[to][s.h[to] - 1];

            System.out.printf("Move %d: %s (%c), Column %d -> Column %d%n",
                    i + 1, NAMES[movedBlock], SYMBOLS[movedBlock], from + 1, to + 1);
            print(s);
        }
        System.out.println("Solved in " + moves + " moves!");
    }

    // Console version with a random start (the GUI is in ColumnSorterGUI)
    public static void main(String[] args) {
        x = 10;

        State start = randomStart();
        System.out.println("Starting arrangement:");
        print(start);

        if (!findBestSolution(start, TIME_LIMIT_MS)) {
            System.out.println("No solution found.");
            return;
        }

        System.out.println("First attempt:        " + firstLen + " moves");
        System.out.println("After all 24 orders:  " + afterOrders + " moves");
        System.out.println("After " + tries + " random tries: " + bestLen + " moves");

        printSolution(start, bestLen);
    }
}