import com.ibm.as400.access.*;
public class sndmsg
{
    
    public static void main(String[] args) throws Exception{
        AS400 as400 = new AS400("localhost", "*CURRENT", "*CURRENT");
        MessageQueue mq = new MessageQueue(as400, "/qsys.lib/jesseg.lib/manzanq.msgq");
        mq.sendInformational("CAE0023", "/qsys.lib/qcpfmsg.msgf", "TABLE1".getBytes("Cp037"));
    }
}