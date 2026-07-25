package dev.exdede.donutmaparts.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerDetectorTest {
    @Test
    void matchesDonutDomains() {
        assertTrue(ServerDetector.isDonutAddress("donutsmp.net"));
        assertTrue(ServerDetector.isDonutAddress("DonutSMP.NET"));
        assertTrue(ServerDetector.isDonutAddress("play.donutsmp.net"));
        assertTrue(ServerDetector.isDonutAddress("donutsmp.com:25565"));
    }

    @Test
    void rejectsOthers() {
        assertFalse(ServerDetector.isDonutAddress(null));
        assertFalse(ServerDetector.isDonutAddress(""));
        assertFalse(ServerDetector.isDonutAddress("hypixel.net"));
        assertFalse(ServerDetector.isDonutAddress("notdonutsmp.net"));
        assertFalse(ServerDetector.isDonutAddress("donutsmp.net.evil.com"));
        assertFalse(ServerDetector.isDonutAddress("localhost"));
    }
}
