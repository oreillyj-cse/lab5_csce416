import java.net.*;
import java.net.SocketException;
import java.util.*;
import java.io.*;

/**
 * Class Network simulates network conditions
 * Like a proxy server, all traffic between the client
 * and server goes through the Network.
 */
public class Network{
    private String clientIP; 
    private int clientPort;
    private String serverIP;
    private int serverPort;
    private int proxyPort;
    private int proxyControlPort;

    private int currRTT;

    private int maxRTT;

    private DatagramSocket udpSocket = null;

    private Socket controlSocket = null;
    private DataInputStream controlIn = null;
    private DataOutputStream controlOut = null;

    private Metrics clientMetrics;
    private Metrics serverMetrics;

    private String file = null;

    private Random rand = null;

    private static final long randomSeed = 1234;

    private double probDelay;
    private double probDup;
    private double probDrop;
    private int min;
    private int max;

    private List<Message> delayQ_C2S;
    private List<Message> delayQ_S2C;

    private Map<String,String> scenario;

    public static final String NetworkLogDir = "NetworkLogs/";
    BufferedWriter logger = null;

    public Network(String clientIP, int clientPort,
                   String serverIP, int serverPort,
                   String proxyIP, int proxyPort, int proxyControlPort)
    {
        this.clientIP = clientIP;
        this.clientPort = clientPort;
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        this.proxyPort = proxyPort;
        this.proxyControlPort = proxyControlPort;

        reset();
        /*
        clientMetrics = new Metrics();
        serverMetrics = new Metrics();


        rand = new Random(Network.randomSeed);

        this.probDrop = this.probDup = this.propDelay = 0.0;
        */

        try{
            udpSocket = new DatagramSocket(proxyPort);

            controlSocket = new Socket(proxyIP,proxyControlPort);
            controlIn = new DataInputStream( 
                new BufferedInputStream(controlSocket.getInputStream()));
            controlOut = new DataOutputStream(controlSocket.getOutputStream());





        } catch(SocketException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }catch(UnknownHostException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }catch(IOException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * resets metrics and restarts scenario at RTT 0
     */
    public void reset(){
        rand = new Random(Network.randomSeed);

        this.probDrop = this.probDup = this.probDelay = 0.0;

        max = 10;
        min = -1;

        clientMetrics = new Metrics();
        serverMetrics = new Metrics();

        delayQ_C2S = new ArrayList<Message>();
        delayQ_S2C = new ArrayList<Message>();

        this.scenario = new HashMap<String,String>();
        
    }

    public void setDelayProb(double p){
        this.probDelay = p;
    }

    public void setPacketLossProb(double p){
        this.probDrop = p;
    }

    public void setDuplicatetProb(double p){
        this.probDup =  p;
    }

    public void setMin(int p){
        this.min = p;
    }

    public void setMax(int p){
        this.max = p;
    }

    public double getDelayProb(){
        return probDelay;
    }

    public double getPacketLossProb(){
        return probDrop;
    }

    public double getDuplicateProb(){
        return probDup;
    }

    public int getMin(){
        return min;
    }

    public int getMax(){
        return max;
    }

    public List<Message> applyConditions(List<Message> msgs, List<Message> delayQ){
        List<Message> outMessages = new ArrayList<Message>();

        int temp_max = this.getMax();
        //process min/max here
        if(min>0){
            int range = max-min+1;
            temp_max = min + rand.nextInt(range);
        }

        while(msgs.size()>temp_max){
            int target = (int)(msgs.size()*rand.nextDouble());
            msgs.remove(target);
        }


        //push old delayQ into new
        for(Message m : delayQ){
            msgs.add( (int)(rand.nextDouble()*msgs.size()) ,  m);
        }
        delayQ.clear();

        for (Message m: msgs){
            if(eventHappens(getPacketLossProb() )) {
                System.out.println("should have dropped...");
                continue; //don't insert
            }
            else if(eventHappens(getDelayProb() )) {
                delayQ.add(m); //see it again next RTT
            }
            else if(eventHappens(getDuplicateProb())){
                Message n = new Message(m.num,m.msg);
                outMessages.add(m);
                outMessages.add(n);
            }else{
                outMessages.add(m);
            }
        }

        return outMessages;
    }

    public void beginSim(){
        try{
            controlOut.writeUTF("Network");
            System.out.println(controlIn.readUTF());

            //String test = TCPoverUDPUtils.receiveDatagram(this.udpSocket, this.clientIP, this.clientPort);
            //System.out.println(test);

            System.out.println("Waiting to load Scenario...");
            String ctrlMsg;
            ctrlMsg = controlIn.readUTF();
            System.out.println("Working on file:" + ctrlMsg);
            file = ctrlMsg;

            this.readScenario(this.controlIn);
            this.loadScenario();

            File log_file = new File(this.NetworkLogDir+"LOG_"+file);
            log_file.delete();

            logger = new BufferedWriter(new FileWriter(log_file));
            for( String k : scenario.keySet() ){
                logger.write(k+"="+scenario.get(k)+'\n');
            }


            List<Message> previousClient2ServerMsgs = new ArrayList<Message>() ;
            //begin simulation loop
            while( !(ctrlMsg = controlIn.readUTF()).equals("Done") )  {
                logger.write("RTT = " + clientMetrics.getRTTs()+'\n') ;
                boolean serverSent = false;
                List<Message> cond_previousClient2ServerMsgs = applyConditions(previousClient2ServerMsgs,delayQ_C2S);
                serverMetrics.addReceived(cond_previousClient2ServerMsgs.size());
                String sPreviousClient2ServerMsgs = TCPoverUDPUtils.messagesToString(cond_previousClient2ServerMsgs);
                logger.write("Server Received: "+sPreviousClient2ServerMsgs+'\n');
                TCPoverUDPUtils.sendDatagram(udpSocket, sPreviousClient2ServerMsgs, serverIP, serverPort);
                String sServerResponse = TCPoverUDPUtils.receiveDatagram(udpSocket, serverIP, serverPort);
                logger.write("Server Replied: "+sServerResponse+'\n');
                List<Message> serverResponses = TCPoverUDPUtils.stringToMessages(sServerResponse);
                serverMetrics.addSent(serverResponses.size());
                if(serverResponses.size()>0) serverSent = true;
                List<Message> cond_serverResponses = applyConditions(serverResponses,delayQ_S2C);
                clientMetrics.addReceived(cond_serverResponses.size());
                String sServerResponses = TCPoverUDPUtils.messagesToString(cond_serverResponses);
                logger.write("Client Received: "+sServerResponses+'\n');
                TCPoverUDPUtils.sendDatagram(udpSocket, sServerResponses, clientIP, clientPort);
                String sClientResponse = TCPoverUDPUtils.receiveDatagram(udpSocket, clientIP, clientPort);
                logger.write("Client Responded: "+sClientResponse+'\n');
                previousClient2ServerMsgs = TCPoverUDPUtils.stringToMessages(sClientResponse);
                clientMetrics.addSent(previousClient2ServerMsgs.size());

                clientMetrics.incRTTs();
                serverMetrics.incRTTs();
                
                if(clientMetrics.getRTTs()> Integer.parseInt(scenario.get("length"))*10){
                    controlOut.writeUTF("MAX_RTTS_EXCEEDED");
                }
                else if(serverSent) controlOut.writeUTF("SERVER_BUSY");
                else           controlOut.writeUTF("SERVER_IDLE");
                
            }

            //last round is bogus (need server to idle for one round)
            clientMetrics.decRTTs();
            serverMetrics.decRTTs();

            logger.write("Client Metrics\n");
            logger.write(clientMetrics.getMetricReport());
            logger.write("Server Metrics\n");
            logger.write(serverMetrics.getMetricReport());
            logger.flush();
            logger.close();

        } catch(IOException e){
            System.err.println(e.getMessage());
            e.printStackTrace();

        }
    }

    private boolean eventHappens(double prob){
        return rand.nextDouble() < prob;
    }

    private void readScenario(DataInputStream dis) throws IOException {
        String prop = "";
        while( ! (prop=dis.readUTF()).equals("END")){
            String value = dis.readUTF();
            this.scenario.put(prop,value);
        }
    }

    private void loadScenario(){
        this.file = scenario.get("file_name");
        //this.length = scenario.get("length");
        if(scenario.get("min")!=null && scenario.get("max")==null){
            throw new RuntimeException("Scenarios: if min is set, max must be too");
        }
        if( scenario.get("min")!=null ) this.setMin(Integer.parseInt(scenario.get("min")) );
        if( scenario.get("max")!=null ) this.setMax(Integer.parseInt(scenario.get("max")) ); 
        if( scenario.get("delay")!=null ) this.setDelayProb(Double.parseDouble(scenario.get("delay")) );
        if( scenario.get("duplicate")!=null ) this.setDuplicatetProb(Double.parseDouble(scenario.get("duplicate")) );
        if( scenario.get("drop")!=null ) this.setPacketLossProb(Double.parseDouble(scenario.get("drop")) );
    }

    public static void main(String args[]){
        Map<String,String> connSettings = 
                TCPoverUDPUtils.parseSettings("connectionSettings");
        Network net = new Network(
            connSettings.get("client_ip"),
            Integer.parseInt(connSettings.get("client_port")),
            connSettings.get("server_ip"),
            Integer.parseInt(connSettings.get("server_port")),
            connSettings.get("proxy_ip"),
            Integer.parseInt(connSettings.get("proxy_port")),
            Integer.parseInt(connSettings.get("proxy_control_port"))
        );

        net.beginSim();
    }
}