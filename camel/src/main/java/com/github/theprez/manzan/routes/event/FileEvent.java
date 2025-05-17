package com.github.theprez.manzan.routes.event;

import java.io.FileNotFoundException;
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
    private final String directory;
    private final String fileName;
    private final String absPath;
    private final ManzanMessageFilter m_filter;
    private final ManzanMessageFormatter m_formatter;
    private long lastPosition;

    public FileEvent(final String _name, final String file, final String _format, final List<String> _destinations,
            final String _filter) throws IOException {
        super(_name);
        absPath = file;
        directory = file.substring(0, file.lastIndexOf("/"));
        fileName = file.substring(file.lastIndexOf("/") + 1);
        super.setRecipientList(_destinations);
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        m_filter = new ManzanMessageFilter(_filter);
    }

    @Override
    public void configure() {

        String ABORT = "ABORT";
        String CONTINUE = "CONTINUE";

        Path file = Paths.get(absPath);

        // Initialize to end of file
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            lastPosition = raf.length();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

       from(String.format("file-watch://%s?events=MODIFY", directory, fileName))
               .filter(exchange -> {
                   String fName = exchange.getIn().getHeader("CamelFileAbsolutePath", String.class);
                   return absPath.equals(fName); // or use regex for pattern matching
               })
                .routeId(m_name)
                .setHeader(EVENT_TYPE, constant(ManzanEventType.FILE))
                .process(exchange -> {
                    try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
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
                    } catch (Exception e){
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
                    final Map<String, Object> data_map = new LinkedHashMap<String, Object>();
                    data_map.put(FILE_NAME, fileName);
                    data_map.put(FILE_PATH, String.format("%s/%s", directory, fileName));
                    data_map.put(FILE_DATA, getBody(exchange, String.class).replace("\r", "").replace("\n",""));
                    exchange.getIn().setHeader("data_map", data_map);
                    exchange.getIn().setBody(data_map);
                })
                .marshal().json(true) // TODO: skip this if we are applying a format
                .setBody(simple("${body}\n"))
                .process(exchange -> {
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    }
                })
                .convertBodyTo(String.class, "UTF-8")
                .recipientList(constant(getRecipientList())).stopOnException();
    }
}
