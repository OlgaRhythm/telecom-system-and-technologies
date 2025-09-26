package org.jakuba.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WalshCodeGeneratorTest {

    @Test
    void testGenerateSize1() {
        int[][] matrix = WalshCodeGenerator.generate(1);
        assertArrayEquals(new int[][]{{1}}, matrix);
    }

    @Test
    void testGenerateSize2() {
        int[][] matrix = WalshCodeGenerator.generate(2);
        int[][] expected = {
                {1, 1},
                {1, -1}
        };
        assertArrayEquals(expected, matrix);
    }

    @Test
    void testGenerateSize4() {
        int[][] matrix = WalshCodeGenerator.generate(4);
        int[][] expected = {
                {1, 1, 1, 1},
                {1, -1, 1, -1},
                {1, 1, -1, -1},
                {1, -1, -1, 1}
        };
        assertArrayEquals(expected, matrix);
    }

    @Test
    void testGenerateSize8_hasCorrectDimensions() {
        int[][] matrix = WalshCodeGenerator.generate(8);
        assertEquals(8, matrix.length);
        for (int[] row : matrix) {
            assertEquals(8, row.length);
        }
    }

    @Test
    void testGenerateThrowsForNonPowerOfTwo() {
        assertThrows(IllegalArgumentException.class, () -> WalshCodeGenerator.generate(3));
        assertThrows(IllegalArgumentException.class, () -> WalshCodeGenerator.generate(6));
    }
}