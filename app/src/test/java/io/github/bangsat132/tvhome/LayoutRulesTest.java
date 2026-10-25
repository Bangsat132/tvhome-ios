package io.github.bangsat132.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** The column clamp and the label fit: two rules the grid depends on and nothing else tests. */
public class LayoutRulesTest {

    @Test
    public void columnsStayBetweenThreeAndEight() {
        assertEquals(3, Config.clampColumns(1));
        assertEquals(3, Config.clampColumns(0));
        assertEquals(5, Config.clampColumns(5));
        assertEquals(8, Config.clampColumns(12));
    }

    @Test
    public void fitEllipsisesOnceTheNameIsTooLong() {
        assertEquals("Netflix", AppList.fit("Netflix", 10));
        String cut = AppList.fit("Amazon Prime Video", 10);
        assertEquals(10, cut.length());
        assertEquals("…", cut.substring(cut.length() - 1));
    }

    @Test
    public void fitHandlesNull() {
        assertEquals("", AppList.fit(null, 5));
    }
}
