import com.ibm.as400.access.*;
public class sndmsg
{
    
    public static void main(String[] args) throws Exception{
        String lib = args[0];
        String q = args[1];
        AS400 as400 = new AS400("localhost", "*CURRENT", "*CURRENT");
        MessageQueue mq = new MessageQueue(as400, "/qsys.lib/"+lib+".lib/"+q+".msgq");
        mq.sendInformational("CAE0023", "/qsys.lib/qcpfmsg.msgf", "TABLE1".getBytes("Cp037"));
        System.out.println("msg sent to MSGQ");
    }
}