


package com.github.theprez.manzan.routes.event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;
import org.apache.camel.component.exec.ExecCommand;
import org.apache.camel.component.exec.ExecResult;

public class WatchCmd extends ManzanRoute {
    private final int m_interval;
    private final ManzanMessageFormatter m_formatter;
    private final String m_execCmdWithArgs;

    public WatchCmd(final String _name, final String _cmd, final String _args, final String _format,
                    final List<String> _destinations,
                    final int _interval)
            throws IOException {
        super(_name);
        m_interval = _interval;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        String execArgs = _args.length() > 0 ? "?args=" + _args:"";
        m_execCmdWithArgs = _cmd + execArgs;
        super.setRecipientList(_destinations);
    }

    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
                .routeId("manzan_cmd:" + m_name)
                .to("exec:" + m_execCmdWithArgs)
                .split(body()).streaming().parallelProcessing()
                .setHeader("exec_result", simple("${body}"))
                .setBody(simple("${body}\n"))
                .process(exchange -> {
                    if (null != m_formatter) {
                        ExecResult execResult = (ExecResult) exchange.getIn().getHeader("exec_result");
                        ExecCommand command = execResult.getCommand();
                        List<String> args = command.getArgs();
                        String executable = command.getExecutable();
                        String exitValue = String.valueOf(execResult.getExitValue());
                        InputStream stdErr = execResult.getStderr();
                        String stdErrStr = "";
                        if (stdErr != null) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(stdErr, StandardCharsets.UTF_8))) {
                                stdErrStr = reader.lines().collect(Collectors.joining("\n"));
                            }
                        }
                        InputStream stdout = execResult.getStdout();
                        String stdoutStr = "";
                        if (stdout != null) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(stdout, StandardCharsets.UTF_8))) {
                                stdoutStr = reader.lines().collect(Collectors.joining("\n"));
                            }
                        }


                        Map<String, Object> dataMap = new HashMap<>();
                        dataMap.put("CMD", executable);
                        dataMap.put("ARGS", args.toString());
                        dataMap.put("EXITVALUE", exitValue);
                        dataMap.put("STDERR", stdErrStr);
                        dataMap.put("STDOUT", stdoutStr);
                        exchange.getIn().setBody(m_formatter.format(dataMap));
                    }
                })
                .recipientList(constant(getRecipientList()))
                .parallelProcessing()
                .stopOnException()
                .end()
                .end();
    }
}