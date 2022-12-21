package com.github.theprez.manzan;

import java.io.IOException;

import org.ini4j.InvalidFileFormatException;

import com.github.theprez.manzan.configuration.ApplicationConfig;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ErrorCompletingRequestException;

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
            throws IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException {
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
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException {
        // TODO: how to implement?
        return false;
    }

    private static void runCmd(final String _command)
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException {
        AS400 as400 = IBMiConnectionSupplier.getSystemConnection();
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
