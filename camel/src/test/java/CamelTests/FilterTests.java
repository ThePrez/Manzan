package CamelTests;
import org.junit.jupiter.api.Test;
import com.github.theprez.manzan.ManzanMessageFilter;

import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.*;


class FilterTests {
    // --- REGEX MATCHING TESTS ---

    @Test
    void testRegexMatchBasic() {
        ManzanMessageFilter filter = new ManzanMessageFilter("re:apple[1-9]banana");
        assertTrue(filter.matches("apple1banana"));
        assertTrue(filter.matches("apple9banana"));
        assertFalse(filter.matches("applebanana"));
    }

    @Test
    void testRegexMatchAtBeginning() {
        ManzanMessageFilter filter = new ManzanMessageFilter("re:^apple");
        assertTrue(filter.matches("applepie"));
        assertFalse(filter.matches("pineapple"));
    }

    @Test
    void testRegexMatchAtEnd() {
        ManzanMessageFilter filter = new ManzanMessageFilter("re:banana$");
        assertTrue(filter.matches("pineapplebanana"));
        assertFalse(filter.matches("bananabread"));
    }

    @Test
    void testRegexMultipleMatchesInText() {
        ManzanMessageFilter filter = new ManzanMessageFilter("re:[0-9]+");
        assertTrue(filter.matches("abc123xyz456")); // should match any digit sequence
    }

    @Test
    void testRegexWithEscapedCharacters() {
        ManzanMessageFilter filter = new ManzanMessageFilter("re:\\.txt$");
        assertTrue(filter.matches("document.txt"));
        assertFalse(filter.matches("document.txt.bak"));
    }

    @Test
    void testRegexEmptyFilterShouldThrow() {
        assertThrows(PatternSyntaxException.class, () -> {
            new ManzanMessageFilter("re:");
        });
    }

    @Test
    void testRegexInvalidPattern() {
        assertThrows(PatternSyntaxException.class, () -> {
            new ManzanMessageFilter("re:[unclosed");
        });
    }

    // --- SUBSTRING MATCHING TESTS ---

    @Test
    void testSubstringMatchBasic() {
        ManzanMessageFilter filter = new ManzanMessageFilter("apple");
        assertTrue(filter.matches("applebanana"));
        assertTrue(filter.matches("bananaapple"));
        assertTrue(filter.matches("pineapplejuice"));
        assertFalse(filter.matches("grapefruit"));
    }

    @Test
    void testSubstringMatchCaseSensitive() {
        ManzanMessageFilter filter = new ManzanMessageFilter("Apple");
        assertFalse(filter.matches("applepie"));
    }

    @Test
    void testCaseInsensitiveRegex() {
        ManzanMessageFilter filter = new ManzanMessageFilter("re:(?i)Apple");
        assertTrue(filter.matches("applepie"));
    }

    @Test
    void testSubstringFilterWithRegexMetaCharacters() {
        ManzanMessageFilter filter = new ManzanMessageFilter("apple[1-9]");
        assertTrue(filter.matches("this contains apple[1-9] text"));
        assertFalse(filter.matches("this contains apple1 text")); // literal match required
    }

    // --- EDGE CASES ---

    @Test
    void testEmptyInputWithNonEmptyFilter() {
        ManzanMessageFilter filter = new ManzanMessageFilter("apple");
        assertFalse(filter.matches(""));
    }

    @Test
    void testEmptyInputAndEmptyFilter() {
        ManzanMessageFilter emptyFilter = new ManzanMessageFilter("");
        ManzanMessageFilter nullFilter = new ManzanMessageFilter(null);


        assertTrue(emptyFilter.matches(""));
        assertTrue(emptyFilter.matches("asdfnjkaerfaoe"));

        assertTrue(nullFilter.matches(""));
        assertTrue(nullFilter.matches("asdfnjkaerfaoe"));

    }

    @Test
    void testNullInputThrowsException() {
        ManzanMessageFilter filter = new ManzanMessageFilter("apple");
        assertThrows(NullPointerException.class, () -> filter.matches(null));
    }

    @Test
    void testNullFormatStringMatchesAll() {
        ManzanMessageFilter filter = new ManzanMessageFilter(null);
        assertTrue(filter.matches(""));
        assertTrue(filter.matches("asdfnjkaerfaoe"));
    }

    @Test
    void testRegexAgainstNullInputThrowsException() {
        ManzanMessageFilter filter = new ManzanMessageFilter("re:apple");
        assertThrows(NullPointerException.class, () -> filter.matches(null));
    }

    @Test
    void testWhitespaceOnlyFilter() {
        ManzanMessageFilter filter = new ManzanMessageFilter(" ");
        assertTrue(filter.matches("this has space"));
        assertFalse(filter.matches("thishasnospace"));
    }


    @Test
    void testRegexMatchOnMultilineInput() {
        ManzanMessageFilter filter = new ManzanMessageFilter("re:foo.*bar");
        assertTrue(filter.matches("foo\nmiddle\nbar"));
    }
}
