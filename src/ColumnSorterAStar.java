
import java.util.*;

public class ColumnSorterAStar {
    static final int COLS = 6;
    static final int COLORS = 4;

    static final String[] NAMES = {
            "Pink", "Yellow", "Green", "Blue"
    };

    static final char[] SYMBOLS = {'P', 'Y', 'G', 'B'};

    static int x;
    static int[] order = {0, 1, 2, 3};

    static class State {
        int[][] cols = new int[COLS][x];
        int[] h = new int[COLS];

        int g, hCost, f;
        String key;
        State parent;

        int from = -1, to = -1;

        State copy() {
            State s = new State();

            for (int i = 0; i < COLS; i++) {
                System.arraycopy(cols[i], 0, s.cols[i], 0, h[i]);
            }

            s.h = h.clone();
            return s;
        }


        String makeKey() { //out of memory fix
            StringBuilder sb = new StringBuilder(COLS);

            for (int c = 0; c < COLS; c++) {
                // Store the column height in the lowest 3 bits.
                int packed = h[c];

                // Store each block's color using 2 bits.
                for (int j = 0; j < h[c]; j++) {
                    packed |= cols[c][j] << (3 + 2 * j);
                }

                // One character encodes the entire column.
                sb.append((char) packed);
            }

            return sb.toString();
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

        boolean isGoal() {
            return hCost == 0;
        }

        boolean isPure(int c) {
            if (h[c] == 0) return false;

            for (int j = 1; j < h[c]; j++) {
                if (cols[c][j] != cols[c][0]) {
                    return false;
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
            for (int k = 0; k < x; k++) {
                items.add(c);
            }
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

    static State solve(State start) {
        PriorityQueue<State> open = new PriorityQueue<>(
                (p, q) -> p.f != q.f
                        ? Integer.compare(p.f, q.f)
                        : Integer.compare(q.g, p.g)
        );

        Map<String, Integer> bestG = new HashMap<>();

        start.key = start.makeKey();
        start.g = 0;
        start.hCost = start.heuristic();
        start.f = start.g + start.hCost;

        open.add(start);
        bestG.put(start.key, 0);

        while (!open.isEmpty()) {
            State cur = open.poll();

            if (cur.g > bestG.get(cur.key)) continue;

            if (cur.isGoal()) return cur;

            for (int a = 0; a < COLS; a++) {
                if (cur.h[a] == 0) continue;

                for (int b = 0; b < COLS; b++) {
                    if (a == b || cur.h[b] == x) continue;

                    if (cur.h[b] == 0 && cur.isPure(a)) {
                        continue;
                    }

                    State next = cur.copy();

                    // Move the top block from column a to column b.
                    next.cols[b][next.h[b]++] =
                            next.cols[a][--next.h[a]];

                    next.key = next.makeKey();
                    next.g = cur.g + 1;

                    Integer old = bestG.get(next.key);

                    if (old == null || next.g < old) {
                        bestG.put(next.key, next.g);

                        next.hCost = next.heuristic();
                        next.f = next.g + next.hCost;

                        next.parent = cur;
                        next.from = a;
                        next.to = b;

                        open.add(next);
                    }
                }
            }
        }

        return null;
    }

    // Print the board with symbols instead of color names.
    static void print(State s) {
        // Column numbers
        for (int c = 0; c < COLS; c++) {
            System.out.printf("%3d", c + 1);
        }
        System.out.println();

        // Blocks, from top row to bottom row
        for (int row = x - 1; row >= 0; row--) {
            for (int c = 0; c < COLS; c++) {
                char cell = row < s.h[c]
                        ? SYMBOLS[s.cols[c][row]]
                        : '.';

                System.out.printf("%3c", cell);
            }

            System.out.println();
        }

        System.out.println("-".repeat(COLS * 3));
    }

    // Reconstruct and display every state in the solution.
    static void printSolution(State goal) {
        List<State> path = new ArrayList<>();

        State current = goal;

        while (current != null) {
            path.add(current);
            current = current.parent;
        }

        Collections.reverse(path);

        System.out.println("\nInitial state:");
        print(path.get(0));

        for (int i = 1; i < path.size(); i++) {
            State previous = path.get(i - 1);
            State currentState = path.get(i);

            int from = currentState.from;
            int to = currentState.to;

            // The moved block is now at the top of the destination.
            int movedBlock =
                    currentState.cols[to][currentState.h[to] - 1];

            System.out.printf(
                    "Move %d: %s (%c), Column %d -> Column %d%n",
                    i,
                    NAMES[movedBlock],
                    SYMBOLS[movedBlock],
                    from + 1,
                    to + 1
            );

            print(currentState);
        }

        System.out.println(
                "Solved in " + (path.size() - 1) + " moves!"
        );
    }

    public static void main(String[] args) {
        x = 10;

        State start = randomStart();

        System.out.println("Starting arrangement:");
        print(start);

        System.out.println("Searching for a solution...");

        State goal = solve(start);

        if (goal == null) {
            System.out.println("No solution found.");
            return;
        }

        printSolution(goal);
    }
}
