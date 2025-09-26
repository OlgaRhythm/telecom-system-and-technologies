package org.jakuba.model;

public class WalshCodeGenerator {

    /**
     * Генерирует матрицу Уолша (матрицу Хэдемарда) размера n x n.
     * n должен быть степенью двойки: 2, 4, 8, ...
     */
    public static int[][] generate(int n) {
        if (n < 1 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Размер n должен быть степенью двойки");
        }
        return hadamard(n);
    }

    private static int[][] hadamard(int n) {
        if (n == 1) {
            return new int[][]{{1}};
        }
        int[][] prev = hadamard(n / 2);
        int[][] result = new int[n][n];

        for (int i = 0; i < n / 2; i++) {
            for (int j = 0; j < n / 2; j++) {
                int val = prev[i][j];
                // верхний левый
                result[i][j] = val;
                // верхний правый
                result[i][j + n / 2] = val;
                // нижний левый
                result[i + n / 2][j] = val;
                // нижний правый (отрицание)
                result[i + n / 2][j + n / 2] = -val;
            }
        }
        return result;
    }

}
