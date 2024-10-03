import com.github.theprez.manzan.ManzanMessageFormatter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestManzanMessageFormatter {

    @Test
    public void testMessageFormattedCorrectly() throws Exception {
        String message = "$message_id$ (severity $SeverIty$): $MESSAGE$";
        String expectedFormat = "$MESSAGE_ID$ (severity $SEVERITY$): $MESSAGE$";
        ManzanMessageFormatter manzanMessageFormatter = new ManzanMessageFormatter(message);
        assertEquals(expectedFormat, manzanMessageFormatter.getM_fmtStr());
    }

    @Test
    public void testIncorrectlyFormattedMessage() throws Exception {
        String message = "$message_id$ (severity $$$";
        String expectedFormat = "$MESSAGE_ID$ (severity $$$";
        ManzanMessageFormatter manzanMessageFormatter = new ManzanMessageFormatter(message);
        assertEquals(expectedFormat, manzanMessageFormatter.getM_fmtStr());
    }

}