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

import io.github.theprez.dotenv_ibmi.IBMiDotEnv;

public class WatchStarter {
    private final String m_command;
    private final String m_stopCommand;
    private final String m_session_id;

    public WatchStarter(final String _session_id, final String _deets) throws InvalidFileFormatException, IOException {

        String command = _deets;
        String wchpgm = " WCHPGM(" + ApplicationConfig.get().getLibrary() + "/HANDLER) ";
        command = command.replaceAll("(?i)(^|\\s+)wchpgm\\([A-Z0-9\\/\\*\\s]+\\)\\s*", " ");

        String ssnid = " SSNID(" + _session_id + ") ";
        command = command.replaceAll("(?i)(^|\\s+)ssnid\\([A-Z0-9\\*\\s]+\\)\\s*", " ");

        command = command.replaceFirst("^(?i)([a-z0-9]+\\/)?strwch\\s+", " ");
        command = "QSYS/STRWCH" + ssnid + wchpgm + command;
        System.out.println("STRWCH Command:\n"+command);
        m_command = command;
        m_stopCommand = "QSYS/ENDWCH SSNID(" + _session_id.trim() + ")";
        m_session_id = _session_id;
    }

    public void endwch()
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException {
        System.out.println("ending watch");
        runCmd(m_stopCommand);
    }

    public void strwch()
            throws IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException,
            PropertyVetoException, ObjectDoesNotExistException {
        if (isRunning()) {
            System.err.println("Watch '" + m_session_id + "' already running");
            return;
        }
        runCmd(m_command);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                endwch();
            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }));
        System.out.println("Watch " + m_session_id + " started successfully");
    }

    public boolean isRunning()
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException,
            PropertyVetoException, ObjectDoesNotExistException {
        AS400 as400 = ApplicationConfig.get().getRemoteConnection();
        ProgramCall pc = new ProgramCall(as400);

        final ProgramCall program = new ProgramCall(as400);
        // Initialize the name of the program to run.
        final String programName = "/qsys.lib/QSCRWCHI.pgm";
        final String sessionParmString = (m_session_id + "           ").substring(0, 10).toUpperCase();

        // Set up the parms
        final ProgramParameter[] parameterList = new ProgramParameter[5];
        // 1 Receiver variable Output Char(*)
        byte[] output = new byte[8];
        parameterList[0] = new ProgramParameter(output);
        // 2 Length of receiver variable Input Binary(4)
        parameterList[1] = new ProgramParameter(new AS400Bin4().toBytes(output.length));
        // 3 Receiver format name Input Char(8)
        parameterList[2] = new ProgramParameter(new AS400Text(8).toBytes("WCHI0100"));
        // 4 Session ID Input Char(10)
        parameterList[3] = new ProgramParameter(new AS400Text(10).toBytes(sessionParmString));
        // 5 Error Code I/O Char(*)
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

    private static void runCmd(final String _command)
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException {
        AS400 as400 = ApplicationConfig.get().getRemoteConnection();
        CommandCall cmd = new CommandCall(as400, _command);
        boolean isSuccess = cmd.run();

        // Show the messages (returned whether or not there was an error.)
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
