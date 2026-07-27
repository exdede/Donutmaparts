package dev.exdede.donutmaparts.net;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinkCodesTest {
    @Test
    void acceptsExactly8HexChars() {
        assertEquals("A1B2C3D4", LinkCodes.normalize("A1B2C3D4"));
    }

    @Test
    void lowercaseIsUppercased() {
        assertEquals("A1B2C3D4", LinkCodes.normalize("a1b2c3d4"));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals("A1B2C3D4", LinkCodes.normalize("  a1b2c3d4  "));
    }

    @Test
    void rejectsWrongLength() {
        assertNull(LinkCodes.normalize("A1B2C3D"));
        assertNull(LinkCodes.normalize("A1B2C3D44"));
    }

    @Test
    void rejectsNonHexCharacters() {
        assertNull(LinkCodes.normalize("A1B2C3DG"));
    }

    @Test
    void rejectsNullAndEmpty() {
        assertNull(LinkCodes.normalize(null));
        assertNull(LinkCodes.normalize(""));
        assertNull(LinkCodes.normalize("   "));
    }
}
