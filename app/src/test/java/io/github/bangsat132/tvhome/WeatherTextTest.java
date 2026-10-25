package io.github.bangsat132.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The card's text rules, tested on a plain JVM: no Android class is touched, so these run in a second.
 *
 * <p>They exist because the two things that went wrong on the reference weather add-on's first build were a
 * WMO code that read as rain in snow, and an "unavailable" that said nothing.
 */
public class WeatherTextTest {

    @Test
    public void clearIsClear() {
        assertEquals("Clear", Weather.conditionFor(0, 20));
    }

    @Test
    public void rainBecomesSnowAtOrBelowOneDegree() {
        // The models type precipitation by the air temperature and call snow "light rain" at 1 °C.
        assertEquals("Light snow", Weather.conditionFor(61, 1.0));
        assertEquals("Light rain", Weather.conditionFor(61, 1.1));
        assertEquals("Snow", Weather.conditionFor(63, -4.0));
        assertEquals("Rain", Weather.conditionFor(63, 12.0));
    }

    @Test
    public void snowCodesStaySnowAboveFreezing() {
        // A 71 is snow whatever the thermometer says.
        assertEquals("Light snow", Weather.conditionFor(71, 3.0));
        assertEquals("Heavy snow", Weather.conditionFor(75, 5.0));
    }

    @Test
    public void everyCodeHasACondition() {
        for (int code = 0; code <= 99; code++) {
            String text = Weather.conditionFor(code, 15);
            assertTrue("code " + code + " has no text", text != null && !text.isEmpty());
        }
    }

    @Test
    public void everyCodeHasAGlyph() {
        for (int code = 0; code <= 99; code++) {
            String glyph = Weather.glyphFor(code);
            assertTrue("code " + code + " has no glyph", glyph != null && !glyph.isEmpty());
        }
    }

    @Test
    public void thunderstormWithHail() {
        assertEquals("Thunderstorm", Weather.conditionFor(95, 20));
        assertEquals("Thunderstorm with hail", Weather.conditionFor(96, 20));
        assertEquals("Thunderstorm with hail", Weather.conditionFor(99, 20));
    }

    @Test
    public void fogAndDrizzle() {
        assertEquals("Fog", Weather.conditionFor(45, 10));
        assertEquals("Fog", Weather.conditionFor(48, 10));
        assertEquals("Freezing drizzle", Weather.conditionFor(56, -2));
        assertEquals("Drizzle", Weather.conditionFor(53, 8));
    }
}
