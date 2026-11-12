package dev.exdede.donutmaparts.net;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinkCodesTest {
    private static final String CODE = "A1B2C3D4E5F60718293A4B5C6D7E8F90";

    @Test
    void acceptsExactly32HexChars() {
        assertEquals(CODE, LinkCodes.normalize(CODE));
    }

    @Test
    void lowercaseIsUppercased() {
        assertEquals(CODE, LinkCodes.normalize(CODE.toLowerCase()));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals(CODE, LinkCodes.normalize("  " + CODE.toLowerCase() + "  "));
    }

    @Test
    void rejectsWrongLength() {
        assertNull(LinkCodes.normalize(CODE.substring(0, CODE.length() - 1)));
        assertNull(LinkCodes.normalize(CODE + "0"));
    }

    @Test
    void rejectsNonHexCharacters() {
        assertNull(LinkCodes.normalize(CODE.substring(0, CODE.length() - 1) + "G"));
    }

    @Test
    void rejectsNullAndEmpty() {
        assertNull(LinkCodes.normalize(null));
        assertNull(LinkCodes.normalize(""));
        assertNull(LinkCodes.normalize("   "));
    }
}
