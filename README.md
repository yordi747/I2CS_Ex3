# Ex3 – Pac-Man Game 

## Overview
This project implements an **automatic Pac-Man algorithm** as part of **Ex3** in the Introduction to Computer Science course.

The main goal of the algorithm is to **win the Pac-Man game autonomously** by:
- Eating all pink dots (points)
- Avoiding dangerous ghosts
- Chasing ghosts when they are eatable
- Supporting cyclic game boards

The solution is based on **BFS distance calculations** and dynamic decision-making at every game step.

---

## Project Structure
The project is divided into logical components:

- **Ex3Algo.java** –  
  The main Pac-Man decision-making algorithm (client side).

- **Map / Map2D / Index2D / Pixel2D** –  
  Data structures and BFS utilities reused from Ex2.

- **GameInfo.java** –  
  Game configuration (algorithm selection, level, timing).

- **JUnit Tests** –  
  Tests for Map and Index2D classes.

---

## Algorithm Strategy

At each game step, Pac-Man performs the following logic:

### 1. Build a Danger Map
- For every **non-eatable ghost**, a BFS distance map is calculated.
- All ghost distance maps are merged into a single **danger map**.
- Each cell stores the **minimum distance to any ghost**.

This allows Pac-Man to know how risky each position is.

---

### 2. Chase Eatable Ghosts (Green Mode)
- If a ghost is currently eatable and **close enough**, Pac-Man switches to attack mode.
- A shortest path (BFS) is calculated toward the ghost.
- Pac-Man follows this path step by step.

---

### 3. Escape When Threatened
- If a ghost is closer than a predefined threshold:
    - Pac-Man enters **escape mode**
    - He chooses the neighboring cell with the **largest distance from ghosts**
    - Walls and invalid cells are ignored

This helps Pac-Man survive even with multiple ghosts.

---

### 4. Eat Pink Dots (Scoring Mode)
- When the situation is safe:
    - Pac-Man searches for the **closest pink dot**
    - BFS layers are used to locate the nearest target
    - A path is cached and reused to reduce computations

---

### 5. Cyclic Map Support
- All movement and distance calculations support **cyclic maps**
- Moving off one edge may wrap Pac-Man to the opposite side

---

## Key Design Decisions

- **BFS-based logic**  
  Ensures shortest and safest paths.

- **Greedy but safe movement**  
  Decisions are local, fast, and adapt to dynamic ghost movement.

- **Cached paths**  
  Prevent unnecessary BFS recalculations every step.

- **Non-deterministic behavior**  
  Due to ghost movement and tie-breaking, results may vary between runs.
  This reflects the dynamic nature of the game.

---

## Known Limitations
- With **four ghosts**, the game is highly dynamic:
    - Some runs succeed
    - Some runs fail due to unavoidable traps
- The algorithm does not predict far future ghost movement
- Despite this, the solution performs well and meets assignment requirements

---

## How to Run

1. Open the project in **IntelliJ IDEA**
2. Make sure `GameInfo.java` uses:
   ```java
   public static final PacManAlgo ALGO = new Ex3Algo();

---

🎬 Gameplay video (Level 4):  
[Download / view the MP4 (raw)](../../raw/main/assets/pacman_level4_demo.mp4)
