package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFilter;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class FileEvent extends ManzanRoute {

    public static final String FILE_DATA = "FILE_DATA";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String FILE_PATH = "FILE_PATH";
    private final String fileName;
    private final String absPath;
    private final ManzanMessageFilter m_filter;
    private final ManzanMessageFormatter m_formatter;
    private int m_interval;
    private long lastPosition;
    private RandomAccessFile raf;

    public FileEvent(final String _name, final String file, final String _format, final List<String> _destinations,
                     final String _filter, final int _interval) throws IOException {
        super(_name);
        absPath = file;
        Path filePath = Paths.get(absPath);
        fileName = filePath.getFileName().toString();
        super.setRecipientList(_destinations);
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        m_filter = new ManzanMessageFilter(_filter);
        m_interval = _interval;
        setRandomAccessFile();
    }

    private void setRandomAccessFile() {
        // Initialize to end of file
        Path filePath = Paths.get(absPath);
        try {
            raf = new RandomAccessFile(filePath.toFile(), "r");
            lastPosition = raf.length();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                raf.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }));
    }

    @Override
    public void configure() {

        String ABORT = "ABORT";
        String CONTINUE = "CONTINUE";

       from("timer://foo?period=" + m_interval + "&synchronous=true")
                .routeId(m_name)
                .setHeader(EVENT_TYPE, constant(ManzanEventType.FILE))
                .process(exchange -> {
                    try {
                        raf.seek(lastPosition);
                        StringBuilder newContent = new StringBuilder();
                        String line;
                        while ((line = raf.readLine()) != null) {
                            newContent.append(line).append(System.lineSeparator());
                        }
                        lastPosition = raf.getFilePointer(); // Update for next read
                        String body = newContent.toString();

                        // Set new lines as message body
                        exchange.getIn().setBody(body);

                        exchange.getIn().setHeader(ABORT,
                                m_filter.matches(body) && StringUtils.isNonEmpty(body) ? CONTINUE : ABORT);
                    } catch (Exception e) {
                        exchange.getIn().setHeader(ABORT, ABORT);
                    }
                })
                .filter(exchange -> {
                    String abortHeader = exchange.getIn().getHeader(ABORT, String.class);
                    return abortHeader.equals(CONTINUE); // or use regex for pattern matching
                })

                .split(body().tokenize("\n")).streaming().parallelProcessing(false).stopOnException()
                .convertBodyTo(String.class)
                .process(exchange -> {
                    final Map<String, Object> data_map = new LinkedHashMap<>();
                    data_map.put(FILE_NAME, fileName);
                    data_map.put(FILE_PATH, absPath);
                    data_map.put(FILE_DATA, getBody(exchange, String.class)
                            .replace("\r", "")
                            .replace("\n", ""));
                    exchange.getIn().setHeader("data_map", data_map);
                    exchange.getIn().setBody(data_map);
                })
                .marshal().json(true) // TODO: skip this if we are applying a format
                .setBody(simple("${body}"))
                .process(exchange -> {
                    if (null != m_formatter) {
                        exchange.getIn().setHeader("format_applied", true);
                        exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    }
                })
                .convertBodyTo(String.class, "UTF-8")
                .recipientList(constant(getRecipientList())).stopOnException();
    }
}
