import java.net.*;
import java.net.SocketException;
import java.util.Map;
import java.io.*;
import java.util.*;

public class serverTCPoverUDP{
    String proxyIP;
    int proxyPort;
    int proxyControlPort;
    int serverPort;

    Metrics metrics;

    DatagramSocket udpSocket = null;

    Socket controlSocket = null;
    DataInputStream controlIn = null;
    DataOutputStream controlOut = null;

    String file = null;

    public static final String ServerFilesDir = "ServerFiles/";

    public serverTCPoverUDP(String proxyIP, int proxyPort, 
                                   int proxyControlPort, int serverPort)
    {
        this.proxyIP =proxyIP;
        this.proxyPort =proxyPort;
        this.proxyControlPort =proxyControlPort;
        this.serverPort =serverPort;

        metrics = new Metrics();

        try{
            udpSocket = new DatagramSocket(serverPort);

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

    public void beginSim(){
        try{
            controlOut.writeUTF("Server");
            System.out.println(controlIn.readUTF());

            System.out.println("Waiting to load Scenario...");
            String ctrlMsg;
            ctrlMsg = controlIn.readUTF();
            System.out.println("Working on file:" + ctrlMsg);
            file = ctrlMsg;

            //read in file and save
            List<String> strFile = TCPoverUDPUtils.receiveFile(controlIn);
            TCPoverUDPUtils.writeMessageStringsFile(ServerFilesDir+this.file, strFile);
            ServerStrategy sstrat = TCPoverUDPUtils.getTransportStrategy().getServerStrategy();
            sstrat.setFile(strFile);

            //begin simulation loop
            while( !(ctrlMsg = controlIn.readUTF()).equals("Done") ){
                String sclientMsgs = TCPoverUDPUtils.receiveDatagram(udpSocket, proxyIP, proxyPort);
                List<Message> clientmsgs = TCPoverUDPUtils.stringToMessages(sclientMsgs);
                List<Message> servermsgs = sstrat.sendRcv(clientmsgs);
                metrics.addReceived(clientmsgs.size());
                metrics.addSent(servermsgs.size());
                metrics.incRTTs();
                String sservermsgs = TCPoverUDPUtils.messagesToString(servermsgs);
                TCPoverUDPUtils.sendDatagram(udpSocket, sservermsgs, proxyIP, proxyPort);
                
            }
            metrics.decRTTs();
            String metricReport = metrics.getMetricReport(); 
            System.out.println(metricReport);
            BufferedWriter log = new BufferedWriter(new FileWriter(ServerFilesDir+"LOG_"+file));
            log.write(metricReport);
            log.close();

        } catch(IOException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String args[]){
        Map<String,String> connSettings = 
                TCPoverUDPUtils.parseSettings("connectionSettings");
        serverTCPoverUDP server = new serverTCPoverUDP(
            connSettings.get("proxy_ip"),
            Integer.parseInt(connSettings.get("proxy_port")),
            Integer.parseInt(connSettings.get("proxy_control_port")),
            Integer.parseInt(connSettings.get("server_port"))
        );

        server.beginSim();
    }
}