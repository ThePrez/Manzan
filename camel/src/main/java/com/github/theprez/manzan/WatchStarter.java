package com.github.theprez.manzan;

import java.beans.PropertyVetoException;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.ini4j.InvalidFileFormatException;

import com.github.theprez.manzan.configuration.ApplicationConfig;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Bin4;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ErrorCodeParameter;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;

public class WatchStarter {
    private final String m_command;
    private final String m_stopCommand;
    private final String m_session_id;
    private final InstanceContext m_ctx;

    /**
     * Create a WatchStarter for a specific instance.
     * The session ID is generated from the instance context, making it unique
     * per instance and human-readable (e.g. "WATSON0001").
     *
     * @param ctx       Instance context — used for session ID generation and AS400 connection
     * @param _baseName Base name from data.ini (the user-configured watch ID)
     * @param _deets    Raw STRWCH command string from data.ini
     */
    public WatchStarter(final InstanceContext ctx, final String _baseName, final String _deets)
            throws InvalidFileFormatException, IOException {
        m_ctx = ctx;

        // Generate instance-aware session ID from context using the configured base name
        final String sessionId = ctx.generateSessionId(_baseName);

        String command = _deets;
        String wchpgm = " WCHPGM(" + ApplicationConfig.get(ctx).getLibrary() + "/HANDLER) ";
        command = command.replaceAll("(?i)(^|\\s+)wchpgm\\([A-Z0-9\\/\\*\\s]+\\)\\s*", " ");

        String ssnid = " SSNID(" + sessionId + ") ";
        command = command.replaceAll("(?i)(^|\\s+)ssnid\\([A-Z0-9\\*\\s]+\\)\\s*", " ");

        command = command.replaceFirst("^(?i)([a-z0-9]+\\/)?strwch\\s+", " ");
        command = "QSYS/STRWCH" + ssnid + wchpgm + command;
        System.out.println("STRWCH Command:\n" + command);
        m_command = command;
        m_stopCommand = "QSYS/ENDWCH SSNID(" + sessionId.trim() + ")";
        m_session_id = sessionId;
    }

    public String getSessionId() {
        return m_session_id;
    }

    /**
     * Create a WatchStarter for the default instance.
     *
     * @deprecated Use {@link #WatchStarter(InstanceContext, String, String)} for multi-instance support.
     */
    @Deprecated
    public WatchStarter(final String _session_id, final String _deets)
            throws InvalidFileFormatException, IOException {
        this(InstanceContext.getDefault(), _session_id, _deets);
    }

    public void endwch()
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException {
        System.out.println("ending watch");
        runCmd(m_ctx, m_stopCommand);
    }

    public void strwch()
            throws IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException,
            PropertyVetoException, ObjectDoesNotExistException {
        if (isRunning()) {
            System.err.println("Watch '" + m_session_id + "' already running");
            return;
        }
        runCmd(m_ctx, m_command);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                endwch();
            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }));
        System.out.println("Watch " + m_session_id + " started successfully");
    }

    public boolean isRunning()
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException,
            PropertyVetoException, ObjectDoesNotExistException {
        AS400 as400 = ApplicationConfig.get(m_ctx).getRemoteConnection();
        final ProgramCall program = new ProgramCall(as400);
        final String programName = "/qsys.lib/QSCRWCHI.pgm";
        final String sessionParmString = (m_session_id + "           ").substring(0, 10).toUpperCase();

        final ProgramParameter[] parameterList = new ProgramParameter[5];
        byte[] output = new byte[8];
        parameterList[0] = new ProgramParameter(output);
        parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(output.length));
        parameterList[2] = new ProgramParameter(new AS400Text(8).toBytes("WCHI0100"));
        parameterList[3] = new ProgramParameter(new AS400Text(10).toBytes(sessionParmString));
        final ErrorCodeParameter ec = new ErrorCodeParameter(true, true);
        parameterList[4] = ec;
        program.setProgram(programName, parameterList);
        boolean isSuccess = program.run();
        for (AS400Message msg : program.getMessageList()) {
            System.err.println(msg.getText());
        }
        if (!isSuccess) {
            return false;
        }
        String errmsg = ec.getMessageID();
        return StringUtils.isEmpty(errmsg);
    }

    private static void runCmd(final InstanceContext ctx, final String _command)
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException {
        AS400 as400 = ApplicationConfig.get(ctx).getRemoteConnection();
        CommandCall cmd = new CommandCall(as400, _command);
        boolean isSuccess = cmd.run();

        AS400Message[] messagelist = cmd.getMessageList();
        String messages = "";
        for (AS400Message msg : messagelist) {
            System.out.println(msg.getText());
            messages += msg;
            messages += "\n";
        }
        if (!isSuccess) {
            throw new IOException("Failed to start watch: " + messages);
        }
    }
}
