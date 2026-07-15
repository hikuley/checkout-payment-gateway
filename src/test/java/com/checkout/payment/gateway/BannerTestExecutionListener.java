package com.checkout.payment.gateway;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Prints a large ASCII-art banner at the start of each Surefire test execution so the
 * unit and integration phases are visually separated in the {@code mvn test} output.
 *
 * <p>Which banner is shown is driven by the {@code testBanner} system property, set per
 * execution in the Surefire plugin configuration ({@code UNIT} or {@code INTEGRATION}).
 * The listener is auto-registered via {@code META-INF/services} on the JUnit Platform.
 */
public class BannerTestExecutionListener implements TestExecutionListener {

    /** Five-row block-font glyphs for the characters used by the banners. */
    private static final Map<Character, String[]> FONT = buildFont();

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        String banner = System.getProperty("testBanner");
        if (banner == null) {
            return;
        }

        List<String> words = switch (banner.toUpperCase()) {
            case "UNIT" -> List.of("UNIT", "TESTS", "STARTED");
            case "INTEGRATION" -> List.of("INTEGRATION", "TESTS", "STARTED");
            default -> List.of(banner.toUpperCase());
        };

        StringBuilder out = new StringBuilder(System.lineSeparator());
        for (String word : words) {
            out.append(render(word)).append(System.lineSeparator());
        }
        System.out.println(out);
    }

    /** Renders a single word as five rows of block-font text. */
    private static String render(String word) {
        StringBuilder[] rows = new StringBuilder[5];
        for (int r = 0; r < 5; r++) {
            rows[r] = new StringBuilder();
        }
        for (char c : word.toCharArray()) {
            String[] glyph = FONT.getOrDefault(c, FONT.get(' '));
            for (int r = 0; r < 5; r++) {
                rows[r].append(glyph[r]).append(' ');
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 5; r++) {
            sb.append(rows[r]).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static Map<Character, String[]> buildFont() {
        Map<Character, String[]> f = new LinkedHashMap<>();
        f.put(' ', new String[]{"     ", "     ", "     ", "     ", "     "});
        f.put('A', new String[]{" ### ", "#   #", "#####", "#   #", "#   #"});
        f.put('D', new String[]{"#### ", "#   #", "#   #", "#   #", "#### "});
        f.put('E', new String[]{"#####", "#    ", "#### ", "#    ", "#####"});
        f.put('G', new String[]{" ####", "#    ", "#  ##", "#   #", " ####"});
        f.put('I', new String[]{"#####", "  #  ", "  #  ", "  #  ", "#####"});
        f.put('N', new String[]{"#   #", "##  #", "# # #", "#  ##", "#   #"});
        f.put('O', new String[]{" ### ", "#   #", "#   #", "#   #", " ### "});
        f.put('R', new String[]{"#### ", "#   #", "#### ", "#  # ", "#   #"});
        f.put('S', new String[]{" ####", "#    ", " ### ", "    #", "#### "});
        f.put('T', new String[]{"#####", "  #  ", "  #  ", "  #  ", "  #  "});
        f.put('U', new String[]{"#   #", "#   #", "#   #", "#   #", " ### "});
        return f;
    }
}
