/**
 * Taken from https://itssmee.wordpress.com/2010/06/28/java-example-of-damerau-levenshtein-distance/
 */
package io.goobi.viewer.controller;

public class DamerauLevenshtein {
    private String compOne;
    private String compTwo;
    private int[][] matrix;
    private boolean calculated = false;

    /**
     * 
     * @param a
     * @param b
     */
    public DamerauLevenshtein(String a, String b) {
        if ((a.length() > 0 || !a.isEmpty()) || (b.length() > 0 || !b.isEmpty())) {
            compOne = a;
            compTwo = b;
        }
    }

    public int[][] getMatrix() {
        setupMatrix();
        return matrix;
    }

    /**
     * Calculate distance according to Optimal String Alignment Distance Algorithm
     * 
     * @return Calculated distance
     */
    public int getSimilarity() {
        if (!calculated) {
            setupMatrix();
        }

        return matrix[compOne.length()][compTwo.length()];
    }

    /**
     * Calculate distance according to the actual Damerau-Levenshtein distance
     * 
     * @return Calculated distance
     */
    public int getDHSimilarity() {
        int inf = compOne.length() + compTwo.length();

        matrix = new int[compOne.length() + 1][compTwo.length() + 1];

        for (int i = 0; i < compOne.length(); i++) {
            matrix[i + 1][1] = i;
            matrix[i + 1][0] = inf;
        }

        for (int i = 0; i < compTwo.length(); i++) {
            matrix[1][i + 1] = i;
            matrix[0][i + 1] = inf;
        }

        int[] da = new int[24];

        for (int i = 0; i < 24; i++) {
            da[i] = 0;
        }

        for (int i = 1; i < compOne.length(); i++) {
            int db = 0;

            for (int j = 1; j < compTwo.length(); j++) {

                int i1 = da[compTwo.indexOf(compTwo.charAt(j - 1))];
                int j1 = db;
                int d = ((compOne.charAt(i - 1) == compTwo.charAt(j - 1)) ? 0 : 1);
                if (d == 0) {
                    db = j;
                }

                matrix[i + 1][j + 1] = Math.min(Math.min(matrix[i][j] + d, matrix[i + 1][j] + 1),
                        Math.min(matrix[i][j + 1] + 1, matrix[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1)));
            }
            da[compOne.indexOf(compOne.charAt(i - 1))] = i;
        }

        return matrix[compOne.length()][compTwo.length()];
    }

    private void setupMatrix() {
        int cost = -1;
        int del;
        int sub;
        int ins;

        matrix = new int[compOne.length() + 1][compTwo.length() + 1];

        for (int i = 0; i <= compOne.length(); i++) {
            matrix[i][0] = i;
        }

        for (int i = 0; i <= compTwo.length(); i++) {
            matrix[0][i] = i;
        }

        for (int i = 1; i <= compOne.length(); i++) {
            for (int j = 1; j <= compTwo.length(); j++) {
                if (compOne.charAt(i - 1) == compTwo.charAt(j - 1)) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                del = matrix[i - 1][j] + 1;
                ins = matrix[i][j - 1] + 1;
                sub = matrix[i - 1][j - 1] + cost;

                matrix[i][j] = minimum(del, ins, sub);

                if ((i > 1) && (j > 1) && (compOne.charAt(i - 1) == compTwo.charAt(j - 2)) && (compOne.charAt(i - 2) == compTwo.charAt(j - 1))) {
                    matrix[i][j] = minimum(matrix[i][j], matrix[i - 2][j - 2] + cost);
                }
            }
        }

        calculated = true;
    }

    @Override
    public String toString() {
        return displayMatrix();
    }

    private String displayMatrix() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y <= compTwo.length(); y++) {
            if (y - 1 < 0) {
                sb.append(" ");
            } else {
                sb.append(compTwo.charAt(y - 1));
            }
            for (int x = 0; x <= compOne.length(); x++) {
                sb.append(matrix[x][y]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 
     * @param d
     * @param i
     * @param s
     * @return Calculated minimum
     */
    private static int minimum(int d, int i, int s) {
        int m = Integer.MAX_VALUE;

        if (d < m) {
            m = d;
        }
        if (i < m) {
            m = i;
        }
        if (s < m) {
            m = s;
        }

        return m;
    }

    /**
     * 
     * @param d
     * @param t
     * @return Calculated minimum
     */
    private static int minimum(int d, int t) {
        int m = Integer.MAX_VALUE;

        if (d < m) {
            m = d;
        }
        if (t < m) {
            m = t;
        }

        return m;
    }
}