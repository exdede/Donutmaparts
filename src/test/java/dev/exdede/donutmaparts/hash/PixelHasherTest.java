package dev.exdede.donutmaparts.hash;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class PixelHasherTest {
    @Test
    void hashesAllZeros() {
        assertEquals(
            "4fe7b59af6de3b665b67788cc2f99892ab827efae3a467342b3bb4e3bc8e5bfe",
            PixelHasher.sha256Hex(new byte[16384]));
    }

    @Test
    void hashesAllOnes() {
        byte[] pixels = new byte[16384];
        Arrays.fill(pixels, (byte) 1);
        assertEquals(
            "111ce3c2a38d83a2e4706bde4abddd509d7f8248116c6832b06745bdc349e09f",
            PixelHasher.sha256Hex(pixels));
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(IllegalArgumentException.class,
            () -> PixelHasher.sha256Hex(new byte[16383]));
        assertThrows(IllegalArgumentException.class,
            () -> PixelHasher.sha256Hex(new byte[0]));
    }
}
