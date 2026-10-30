package com.kms3735.threelinesfirst.ocr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ML Kit은 블록(덩어리) 단위로 결과를 주기 때문에, 표나 좌우로 나뉜 안내문에서는
 * 왼쪽 칸을 끝까지 읽은 뒤 오른쪽 칸을 읽는다. 줄의 위치를 보고
 * 같은 높이에 있는 줄을 왼쪽부터 이어 붙여 사람이 읽는 순서로 바꾼다.
 * 사진이 기울어 있으면 줄 기울기의 중앙값만큼 위치를 바로 세운 뒤 비교한다.
 */
public final class ReadingOrder {

    /** 화면 좌표를 가진 한 줄. 안드로이드 Rect 대신 써서 단위 테스트가 가능하게 한다. */
    public static final class Line {
        final String text;
        final int left, top, right, bottom;
        /** ML Kit Line.getAngle(): 시계 방향이 양수인 기울기(도) */
        final float angle;
        /** 기울기를 바로 세운 뒤의 세로 중심 */
        float levelY;

        public Line(String text, int left, int top, int right, int bottom, float angle) {
            this.text = text;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.angle = angle;
        }

        public Line(String text, int left, int top, int right, int bottom) {
            this(text, left, top, right, bottom, 0f);
        }

        int height() {
            return Math.max(1, bottom - top);
        }
    }

    private ReadingOrder() {
    }

    public static String arrange(List<Line> lines) {
        if (lines.isEmpty()) return "";

        // 시계 방향으로 기울면 오른쪽 줄이 더 아래에 찍힌다. 그만큼 위로 되돌린다
        double rad = Math.toRadians(medianAngle(lines));
        double sin = Math.sin(rad), cos = Math.cos(rad);
        for (Line l : lines) {
            double cx = (l.left + l.right) / 2.0, cy = (l.top + l.bottom) / 2.0;
            l.levelY = (float) (cy * cos - cx * sin);
        }

        List<Line> sorted = new ArrayList<>(lines);
        sorted.sort(Comparator.comparingDouble(l -> l.levelY));

        // 줄의 세로 중심이 현재 행 높이의 절반 안에 있으면 같은 행으로 본다
        List<List<Line>> rows = new ArrayList<>();
        for (Line line : sorted) {
            List<Line> row = rows.isEmpty() ? null : rows.get(rows.size() - 1);
            if (row != null && Math.abs(line.levelY - rowCenter(row)) <= rowHeight(row) / 2f) {
                row.add(line);
            } else {
                List<Line> newRow = new ArrayList<>();
                newRow.add(line);
                rows.add(newRow);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (List<Line> row : rows) {
            row.sort(Comparator.comparingInt(l -> l.left));
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) sb.append("  ");
                sb.append(row.get(i).text);
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private static float medianAngle(List<Line> lines) {
        List<Float> angles = new ArrayList<>();
        for (Line l : lines) angles.add(l.angle);
        angles.sort(null);
        return angles.get(angles.size() / 2);
    }

    private static float rowCenter(List<Line> row) {
        float sum = 0;
        for (Line l : row) sum += l.levelY;
        return sum / row.size();
    }

    private static float rowHeight(List<Line> row) {
        float sum = 0;
        for (Line l : row) sum += l.height();
        return sum / row.size();
    }
}
