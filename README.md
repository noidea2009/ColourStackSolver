# Caution

AT THIS POINT I ONLY HAVE A ROUGH IDEA OF HOW THE CODE WORK, but here is a complete explanation .

---

# Steps to Solve a Stack

`solve()` takes a sequence `seq` (for example `0, 1, 2, 3`) and completes the target columns in that order. Once a column is finished, it is marked `done` and never touched again. A finished column can no longer be used to dump blocks, but there is also less left to sort.

For each target column `t` with color `color = order[t]`, there are two steps.

## Step 1: Clear out the wrong blocks

- First it counts `keep`, the number of blocks at the bottom of `t` that are already the right color. Those stay.
- Everything above that run gets moved off, one top block at a time, using `pickDest` to choose where each block goes.

## Step 2: Pull in the right color until the column is full

- It looks at every other unfinished column and finds the highest block of `color` in each. It measures how deep that block is, meaning how many blocks sit on top of it.
- It picks the column where the block is shallowest (fewest blocks to dig through). The `depth * 2` key is what does this.
- It then moves the blocks covering it onto other columns (again using `pickDest`), and finally moves the block itself into `t`.
- This repeats until `t` is full. If it can't find any block of the color, the attempt fails and returns `false`.

---

## Choosing where to dump a block: `pickDest`

When a block has to be moved out of the way, `pickDest` scores each possible destination (not done, not full, not the source or target) and takes the lowest score:

| Score | Destination |
|-------|-------------|
| 0 | A column whose top block is the same color, so the block stacks on a match and doesn't bury anything |
| 1 | An empty column |
| 2 | A spare column (5 or 6) |
| 3 | A target column (1-4) with something else on top |

The key is `score * 4` plus a random number from 0 to 4, so a lower score usually wins but randomness can occasionally break the order. With `rnd == null` there is no randomness, so the run is fully deterministic.

If no destination has room, it throws `"No room to dump a block"`, and `attempt` catches that and treats it as a failed try.

---

## Cleaning up wasted moves: `optimize`

The greedy approach often produces pointless sequences.

- If one move goes `a -> b` and the next goes `b -> c`, that is the same block moving twice, so it collapses into `a -> c`.
- If it goes `a -> b` then `b -> a`, the two moves cancel out and both are deleted.

It repeats until nothing more can be removed. This makes solutions noticeably shorter.

---

## Searching for a short solution: `findBestSolution`

A single greedy run isn't guaranteed to be short, so the program tries lots of variations and keeps the best one:

1. **Baseline.** Finish columns in the order 1, 2, 3, 4 with no randomness.
2. **All 24 orders.** Try every permutation of which column to finish first, second, and so on, still deterministic. A different order can make a big difference.
3. **Random restarts.** Until the time limit, shuffle the column order and run again with random tie-breaking in `pickDest` and in the digging choice. Each run is a new chance at a shorter path.

After every attempt, `optimize` shortens the path, and if the result beats `bestLen`, it is saved as the new best.

This isn't a guaranteed optimal solver (that would be a much heavier search). It is a fast heuristic plus many random tries, which usually lands on a good solution.

---
