import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import com.github.theprez.manzan.ManzanMessageFormatter;

import java.util.*;

class MessageFormatTests {

    @Test
    void replaceSingleString() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$SEVERITY$");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("SEVERITY", "10");
        String actual = formatter.format(mappings);
        assertEquals("10", actual);
    }

    @Test
    void replaceMultipleStrings() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("[$SEVERITY$] $MESSAGE$");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("SEVERITY", "80");
        mappings.put("MESSAGE", "Test complete");
        String actual = formatter.format(mappings);
        assertEquals("[80] Test complete", actual);
    }

    @Test
    void missingPlaceholderIsUnchanged() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("[$SEVERITY$] $MESSAGE$");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("SEVERITY", "80");
        // MESSAGE is missing
        String actual = formatter.format(mappings);
        assertEquals("[80] ", actual);
    }

    @Test
    void noPlaceholders() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("Just a static message");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("SEVERITY", "50");
        String actual = formatter.format(mappings);
        assertEquals("Just a static message", actual);
    }

    @Test
    void repeatedPlaceholders() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$WORD$ and again $WORD$!");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("WORD", "Echo");
        String actual = formatter.format(mappings);
        assertEquals("Echo and again Echo!", actual);
    }

    @Test
    void placeholderWithSpecialCharacters() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("Start-$VAR_1$-End");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("VAR_1", "xyz");
        String actual = formatter.format(mappings);
        assertEquals("Start-xyz-End", actual);
    }

    @Test
    void nullValueIsConvertedToStringNull() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$KEY$");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("KEY", null);
        String actual = formatter.format(mappings);
        assertEquals("", actual);
    }

    @Test
    void replaceNestedValue() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$person.address$");
        Map<String, Object> mappings = new HashMap<>();
        Map<String, Object> person = new HashMap<>();
        person.put("address", "123 main street");
        mappings.put("person", person);
        String actual = formatter.format(mappings);
        assertEquals("123 main street", actual);
    }

    @Test
    void TwoLevelNest() {

        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$user.name$");
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Alice");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("user", user);
        assertEquals("Alice", formatter.format(mappings));
    }

    @Test
    void ThreeLevelNest() {

        ManzanMessageFormatter formatter = new ManzanMessageFormatter("Location: $profile.contact.address.city$");
        Map<String, Object> address = new HashMap<>();
        address.put("city", "Toronto");

        Map<String, Object> contact = new HashMap<>();
        contact.put("address", address);

        Map<String, Object> profile = new HashMap<>();
        profile.put("contact", contact);

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("profile", profile);

        assertEquals("Location: Toronto", formatter.format(mappings));
    }

    @Test
    void PartialPathExists() {

        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$user.profile.name$");
        Map<String, Object> user = new HashMap<>();
        user.put("profile", null); // or omit "profile" entirely
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("user", user);
        assertEquals("", formatter.format(mappings));
    }

    @Test
    void NestedAndFlat() {

        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$greeting$, $user.name$!");
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Bob");

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("greeting", "Hello");
        mappings.put("user", user);

        assertEquals("Hello, Bob!", formatter.format(mappings));
    }

    @Test
    void RepeatedNestedPlaceholder() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$u.n$ and again $u.n$");
        Map<String, Object> u = new HashMap<>();
        u.put("n", "X");

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("u", u);

        assertEquals("X and again X", formatter.format(mappings));
    }

    @Test
    void ValueIsAMap() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$config$");
        Map<String, Object> config = new HashMap<>();
        config.put("version", "1.0");
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("config", config);

        assertEquals(config.toString(), formatter.format(mappings));
    }

    @Test
    void InvalidTraversal() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$user.name.first$");
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Alice"); // name is a String, not a Map

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("user", user);

        assertEquals("", formatter.format(mappings));
    }

    @Test
    void caseMismatchTopLevelKey() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$User.name$ $CITY$");
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Zoe");

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("user", user); // lowercase "user", not "User"
        mappings.put("city", "TORONTO"); // lowercase city, not CITY

        assertEquals(" ", formatter.format(mappings));
    }

    @Test
    void caseMismatchNestedKey() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$user.Name$");
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Carlos");

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("user", user);

        assertEquals("", formatter.format(mappings));
    }

    @Test
    void resolveValueFromFirstElementInList() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$person.name$");

        Map<String, Object> personObject = new HashMap<>();
        personObject.put("name", "jon");
        List<Map<String, Object>> personList = new ArrayList<>();
        personList.add(personObject);

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("person", personList);

        assertEquals("jon", formatter.format(mappings));
    }

    @Test
    void resolveTopLevelListValue() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$names$");

        Map<String, Object> mappings = new HashMap<>();
        List<String> names = new ArrayList<>();
        names.add("alpha");
        names.add("beta");
        names.add("gamma");

        mappings.put("names", names);

        assertEquals("[alpha, beta, gamma]", formatter.format(mappings));
    }

    @Test
    void FormatStringIsNull() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter(null);
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("X", "value");

        assertEquals("", formatter.format(mappings));
    }

    @Test
    void nullMappingsReturnsEmpty() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$X$");
        assertEquals("", formatter.format(null));
    }

    @Test
    void nullInsideListReturnsEmpty() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$a.b$");

        Map<String, Object> mappings = new HashMap<>();
        List<String> nullList = new ArrayList<>();
        nullList.add(null);
        mappings.put("a", nullList);

        assertEquals("", formatter.format(mappings));
    }

    @Test
    void unclosedPlaceholderDoesNothing() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("Total cost is $AMOUNT");

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("AMOUNT", "42");

        assertEquals("Total cost is $AMOUNT", formatter.format(mappings));
    }


    @Test
    void jsonIndicatorWithComplexValue() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$json:data$");

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("data", "Line1\nLine2\tTabbed");

        String actual = formatter.format(mappings);

        // JSON encode newline and tab chars
        assertEquals("\"Line1\\nLine2\\tTabbed\"", actual);
    }

    @Test
    void getNestedValueReturnsEmptyOnEmptyList() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$a.b$");

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("a", new ArrayList<>());  // empty list

        assertEquals("", formatter.format(mappings));
    }

    @Test
    void getNestedValueHandlesMapEntryMatchingKey() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$entry.value$");

        Map.Entry<String, Object> entry = new AbstractMap.SimpleEntry<>("value", "matched");

        Map<String, Object> mapWithEntry = new HashMap<>();
        mapWithEntry.put("entry", entry);

        assertEquals("matched", formatter.format(mapWithEntry));
    }

    @Test
    void getNestedValueHandlesMapEntryNonMatchingKey() {
        ManzanMessageFormatter formatter = new ManzanMessageFormatter("$entry.value$");

        Map.Entry<String, Object> entry = new AbstractMap.SimpleEntry<>("otherKey", "notmatched");

        Map<String, Object> mapWithEntry = new HashMap<>();
        mapWithEntry.put("entry", entry);

        assertEquals("", formatter.format(mapWithEntry));
    }
}
