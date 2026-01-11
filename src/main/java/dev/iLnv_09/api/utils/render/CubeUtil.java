package dev.iLnv_09.api.utils.render;

import org.joml.Quaternionf;

public class CubeUtil {
    public static Quaternionf[] cubeletStatus = new Quaternionf[]{new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)};
    public static int[][][] cubletLookup = new int[][][]{new int[][]{{17, 9, 0}, {20, 16, 3}, {23, 15, 6}}, new int[][]{{18, 10, 1}, {21, -1, 4}, {24, 14, 7}}, new int[][]{{19, 11, 2}, {22, 12, 5}, {25, 13, 8}}};
    public static int[][] cubeSides = new int[][]{{0, 1, 2, 3, 4, 5, 6, 7, 8}, {19, 18, 17, 22, 21, 20, 25, 24, 23}, {0, 1, 2, 9, 10, 11, 17, 18, 19}, {23, 24, 25, 15, 14, 13, 6, 7, 8}, {17, 9, 0, 20, 16, 3, 23, 15, 6}, {2, 11, 19, 5, 12, 22, 8, 13, 25}};
    public static int[][] cubeSideTransforms = new int[][]{{0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}, {-1, 0, 0}, {1, 0, 0}};

    public static double easeInOutCubic(double d) {
        return d < 0.5 ? 4.0 * d * d * d : 1.0 - Math.pow(-2.0 * d + 2.0, 3.0) / 2.0;
    }
}

