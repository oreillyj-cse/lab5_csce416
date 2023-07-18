import java.net.*;
import java.net.SocketException;
import java.util.Map;
import java.io.*;
import java.util.*;

public class Control{
    int port;
    ServerSocket welcomeSock = null;
    Socket clientSock = null;
    Socket serverSock = null;
    Socket networkSock = null;

    DataInputStream clientIn = null;
    DataOutputStream clientOut = null;
    DataInputStream serverIn = null;
    DataOutputStream serverOut = null;
    DataInputStream networkIn = null;
    DataOutputStream networkOut = null;

    public static final String ScenarioDir = "Scenarios/";
    public static final String ControlOutDir = "ControlOut/";

    public Control(int port){
        this.port = port;
    }

    public void beginSim(){
        //setup
        try{
            Scanner sin = new Scanner(System.in);

            this.welcomeSock = new ServerSocket(port);

            int connections = 0;
            while(connections < 3){
                Socket sock = welcomeSock.accept();
                DataInputStream in = new DataInputStream( 
                    new BufferedInputStream(sock.getInputStream()));
                DataOutputStream out =
                    new DataOutputStream(sock.getOutputStream());

                String id = in.readUTF();
                if(id.equals("Network")){
                    networkSock = sock;
                    networkIn = in;
                    networkOut = out;
                }
                if(id.equals("Client")){
                    clientSock = sock;
                    clientIn = in;
                    clientOut = out;
                }
                if(id.equals("Server")){
                    serverSock = sock;
                    serverIn = in;
                    serverOut = out;
                }
                out.writeUTF("Connected to Control");
                System.out.println(id + " connected");
                ++connections;
            }
            if(clientSock==null||networkSock==null||serverSock==null){
                throw new RuntimeException("Three connections were made but"
                + "\n not all control sockets are set");
            }

            //TBD: Move this after control connection setup... just easier for 
            //troubleshooting at the moment
            //https://stackoverflow.com/questions/5694385/getting-the-filenames-of-all-files-in-a-folder
            File folder = new File(ScenarioDir);
            File[] listOfFiles = folder.listFiles();
            List<String> scenarioNames = new ArrayList<String>();
            for(File f : listOfFiles){
                if(f.isFile()) scenarioNames.add(f.getName());
            }
            //scenarioNames.add("<Run All Scenarios>");
            Collections.sort(scenarioNames);
            for(int i = 0; i<scenarioNames.size(); ++i ){
                System.out.println(i+". "+ scenarioNames.get(i));
            }
            
            System.out.print("\nPlease pick a Scenario # to run:");
            int selection = Integer.parseInt(sin.nextLine());

            String scenarioName = scenarioNames.get(selection);
            Map<String,String> scenario = TCPoverUDPUtils.parseSettings(
                ScenarioDir+scenarioName);
            for(String key: scenario.keySet()){
                System.out.println(key+"="+scenario.get(key));
            }

            List<String> strMsgs = 
                    Control.genMessages(Integer.parseInt(scenario.get("length")));
            List<Message> msgs = new ArrayList<Message>();
            for(int i = 0; i<strMsgs.size();++i){
                msgs.add(new Message(i,strMsgs.get(i)));
            }
            TCPoverUDPUtils.writeMessageStringsFile(
                            Control.ControlOutDir+scenarioName+".txt", strMsgs);

            //beginning simulation
            System.out.println("Beginning Simulation");
            sendMessageSCN(scenario.get("file_name"));

            TCPoverUDPUtils.sendFile(strMsgs, serverOut);

            sendScenario(this.networkOut,scenario);

            do{
                sendMessageSCN("Continue");
            } while(networkIn.readUTF().equals("SERVER_BUSY"));

            sendMessageSCN("Done");

            List<String> clientFile = TCPoverUDPUtils.receiveFile(clientIn);

            if(clientFile.size()!=strMsgs.size()){
                System.out.println("File lengths (number of messages) don't match");
            }else{
                int i = 0;
                for( ;i<clientFile.size();++i){
                    if (!clientFile.get(i).equals(strMsgs.get(i))) break;
                }
                if(i==clientFile.size()) System.out.println("SUCCESS!");
                else System.out.println("Error at file line "+i);
            }
        } catch(IOException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendScenario (DataOutputStream dos, Map<String,String> scenario) throws IOException{
        for(String k : scenario.keySet()){
            dos.writeUTF(k);
            dos.writeUTF(scenario.get(k));
        }
        dos.writeUTF("END");
    }

    public static List<String> genMessages(int length) {
        List<String> msgs = new ArrayList<String>();
        for(int i =0;i<length;++i){
            msgs.add("M_"+i);
        }
        return msgs;
    }

    /**
     * Send a message to Server, client, and Network _in_that_order_.
     */
    private void sendMessageSCN(String msg) throws IOException{
        this.serverOut.writeUTF(msg);
        this.clientOut.writeUTF(msg);
        this.networkOut.writeUTF(msg);
    }

    public static void main(String args[]){
        //setup
        Map<String,String> connSettings = 
                TCPoverUDPUtils.parseSettings("connectionSettings");
        Control control = new Control(
            Integer.parseInt(connSettings.get("proxy_control_port"))
        );

        control.beginSim();



    }
}