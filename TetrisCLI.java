import java.io.IOException;
import java.util.*;

public class TetrisCLI {
    static final int WIDTH = 10, HEIGHT = 20;
    static char[][] board = new char[HEIGHT][WIDTH];
    static Tetromino current;
    static int curX, curY, rotation, score, level = 1, linesCleared;

    static Random rand = new Random();

    public static void main(String[] args) throws Exception {
        enableRawMode();
        spawnPiece();
        long lastFall = System.currentTimeMillis();

        while (true) {
            if (System.in.available() > 0) {
                char c = (char) System.in.read();
                handleInput(c);
            }

            long now = System.currentTimeMillis();
            if (now - lastFall > Math.max(100, 600 - (level * 40))) {
                if (!move(0, 1)) {
                    mergePiece();
                    clearLines();
                    spawnPiece();
                    if (!valid(curX, curY, rotation)) {
                        disableRawMode();
                        System.out.println("\nGame Over! Final Score: " + score);
                        return;
                    }
                }
                lastFall = now;
            }
            draw();
            Thread.sleep(50);
        }
    }

    static void handleInput(char c) {
        switch (c) {
            case 'a': move(-1, 0); break;
            case 'd': move(1, 0); break;
            case 's': move(0, 1); break;
            case 'w': rotate(); break;
            case 'q': 
                disableRawMode();
                System.exit(0);
        }
    }

    static void draw() {
        StringBuilder sb = new StringBuilder("\033[H\033[2J"); // clear screen
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (occupied(x, y)) sb.append("#");
                else sb.append(board[y][x] == 0 ? "." : board[y][x]);
            }
            sb.append("\n");
        }
        sb.append("Score: ").append(score).append("  Level: ").append(level)
          .append("  Lines: ").append(linesCleared).append("\n");
        sb.append("Controls: a=Left d=Right s=Down w=Rotate q=Quit\n");
        System.out.print(sb.toString());
    }

    static void spawnPiece() {
        current = Tetromino.values()[rand.nextInt(Tetromino.values().length)];
        curX = WIDTH / 2 - 2;
        curY = 0;
        rotation = 0;
    }

    static boolean move(int dx, int dy) {
        if (valid(curX + dx, curY + dy, rotation)) {
            curX += dx; curY += dy; return true;
        }
        return false;
    }

    static void rotate() {
        int r = (rotation + 1) % 4;
        if (valid(curX, curY, r)) rotation = r;
    }

    static void mergePiece() {
        boolean[][] shape = current.shape(rotation);
        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++)
                if (shape[y][x] && curY + y >= 0)
                    board[curY + y][curX + x] = '#';
    }

    static void clearLines() {
        int cleared = 0;
        for (int y = HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < WIDTH; x++)
                if (board[y][x] == 0) full = false;
            if (full) {
                cleared++;
                for (int yy = y; yy > 0; yy--)
                    board[yy] = Arrays.copyOf(board[yy - 1], WIDTH);
                board[0] = new char[WIDTH];
                y++;
            }
        }
        if (cleared > 0) {
            linesCleared += cleared;
            int added = (cleared == 1 ? 100 : cleared == 2 ? 300 :
                         cleared == 3 ? 500 : 800);
            score += added * level;
            level = 1 + linesCleared / 10;
        }
    }

    static boolean occupied(int x, int y) {
        boolean[][] shape = current.shape(rotation);
        int relX = x - curX, relY = y - curY;
        if (relX >= 0 && relX < 4 && relY >= 0 && relY < 4 && shape[relY][relX])
            return true;
        return board[y][x] != 0;
    }

    static boolean valid(int nx, int ny, int rot) {
        boolean[][] shape = current.shape(rot);
        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++)
                if (shape[y][x]) {
                    int xx = nx + x, yy = ny + y;
                    if (xx < 0 || xx >= WIDTH || yy >= HEIGHT) return false;
                    if (yy >= 0 && board[yy][xx] != 0) return false;
                }
        return true;
    }

    enum Tetromino {
        I(new int[][]{{0,1},{1,1},{2,1},{3,1}}),
        O(new int[][]{{1,0},{2,0},{1,1},{2,1}}),
        T(new int[][]{{1,0},{0,1},{1,1},{2,1}}),
        S(new int[][]{{1,0},{2,0},{0,1},{1,1}}),
        Z(new int[][]{{0,0},{1,0},{1,1},{2,1}}),
        J(new int[][]{{0,0},{0,1},{1,1},{2,1}}),
        L(new int[][]{{2,0},{0,1},{1,1},{2,1}});

        int[][] cells;
        Tetromino(int[][] c){cells=c;}
        boolean[][] shape(int rot){
            boolean[][] s=new boolean[4][4];
            for(int[]p:cells){
                int x=p[0],y=p[1];
                for(int r=0;r<rot;r++){int t=x;x=3-y;y=t;}
                s[y][x]=true;
            }
            return s;
        }
    }

    // raw mode helpers for linux
    static void enableRawMode() {
        try {
            String[] cmd = {"/bin/sh","-c","stty -echo -icanon min 1 time 0"};
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {}
    }
    static void disableRawMode() {
        try {
            String[] cmd = {"/bin/sh","-c","stty sane"};
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {}
    }
}

