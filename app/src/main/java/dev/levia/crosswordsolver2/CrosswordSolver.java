package dev.levia.crosswordsolver2;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CrosswordSolver {
    public static class Point {
        private final short x;
        private final short y;
        private final short off_x;
        private final short off_y;
        public Point(int x, int y) { this.x = (short) x; this.y = (short) y; this.off_x = 0; this.off_y = 0; }
        public Point(int x, int y, int off_x, int off_y) {
            this.x = (short) x; this.y = (short)y; this.off_x = (short)off_x; this.off_y = (short)off_y;
        }
        public Point(int x, int y, int off_x, int off_y, Direction dir) {
            this.x = (short) x; this.y = (short)y; this.off_x = (short)off_x; this.off_y = (short)off_y; this.direction = dir;
        }
        public Point add(short x, short y) {
            return new Point(this.x + x, this.y + y, this.off_x, this.off_y);
        }
        public Point getOrigin(short distance) {
            Direction dir = null;
            if (this.off_y != 0 && this.off_x == 0) dir = Direction.Horizontal;
            else if (this.off_y == 0 && this.off_x != 0) dir = Direction.Vertical;
            else if (this.off_y == -1 && this.off_x == -1) dir = Direction.DiagonalFallX;
            else dir = Direction.DiagonalFallY;

            return new Point(this.x-(this.off_x*(distance-1)), this.y-(this.off_y*(distance-1)), this.off_x, this.off_y, dir);
        }
        public enum Direction { Horizontal, Vertical, DiagonalFallX, DiagonalFallY }
        public Direction direction = null;
        public short x() {return x;}
        public short y() {return y;}
        public short ox() {return off_x;}
        public short oy() {return off_y;}

        @SuppressLint("DefaultLocale")
        @NonNull @Override public String toString() {
            return String.format("{x: %d, y: %d, off_x: %d, off_y; %d}" , this.x, this.y, this.off_x, this.off_y);
        }
    }


    private final List<List<Character>> grid;
    private final short[][] inc_pos = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
    public CrosswordSolver(List<String> grid) {
        this.grid = grid.stream().map(i -> i.chars().mapToObj(j -> (char) j).collect(Collectors.toList())).collect(Collectors.toList());
    }

    public ArrayList<Point> first(char x) {
        var list = new ArrayList<Point>();
        for (int i = 0; i < grid.size(); i++) {
            for (int j = 0; j < grid.get(0).size(); j++) {
                if (grid.get(i).get(j) == x) list.add(new Point(i, j));
            }
        }
        Log.d("EMPTY", String.format("char: %s %d", x, list.size()));
        return list;
    }
    public ArrayList<Point> second(ArrayList<Point> list, char x) {
        var out = new ArrayList<Point>();
        for (var item: list) {
            for (var pos: inc_pos) {
                try {
                    if (grid.get((item.x + pos[0])).get(item.y + pos[1]) == x) {
                        out.add(new Point(item.x()+pos[0], item.y()+pos[1], pos[0], pos[1]));
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
        }
        Log.d("EMPTY", String.format("updating: %d %d", list.size(), out.size()));
        return out;
    }
    public ArrayList<Point> next(ArrayList<Point> list, char x) {
        var out = new ArrayList<Point>();
        for (var point: list) {
            try {
                if (grid.get(point.x()+point.ox()).get(point.y()+point.oy()) == x) {
                    out.add(point.add(point.ox(), point.oy()));
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        Log.d("EMPTY", String.format("updating: %d %d", list.size(), out.size()));
        if (!list.isEmpty() && out.isEmpty()) Log.d("EMPTY", String.format("NAME: %s", list.get(0)));
        return out;
    }
    public List<Point> find(String word) {
        if (word.isEmpty()) return List.of();
        if (word.length() == 1) return first(word.charAt(0));
        if (word.length() == 2) return second(first(word.charAt(0)), word.charAt(1));
        var list = second(first(word.charAt(0)), word.charAt(1));
        for (int i = 2; i < word.length(); i++) list = next(list, word.charAt(i));
        return list;
    }
}
