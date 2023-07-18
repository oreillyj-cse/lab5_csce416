import java.net.*;
import java.net.SocketException;
import java.util.*;
import java.io.*;

public class clientTCPoverUDP{
    String proxyIP;
    int proxyPort;
    int proxyControlPort;
    int clientPort;

    Metrics metrics;

    DatagramSocket udpSocket = null;

    Socket controlSocket = null;
    DataInputStream controlIn = null;
    DataOutputStream controlOut = null;

    String file = null;

    public static final String ClientDownloadsDir = "ClientDownloads/";

    public clientTCPoverUDP(String proxyIP, int proxyPort, 
                                   int proxyControlPort, int clientPort)
    {
        this.proxyIP =proxyIP;
        this.proxyPort =proxyPort;
        this.proxyControlPort =proxyControlPort;
        this.clientPort =clientPort;

        metrics = new Metrics();

        try{
            udpSocket = new DatagramSocket(clientPort);

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
            controlOut.writeUTF("Client");
            System.out.println(controlIn.readUTF());

            //TCPoverUDPUtils.sendDatagram(udpSocket, "fred", proxyIP, proxyPort);

            System.out.println("Waiting to load Scenario...");
            String ctrlMsg;

            ctrlMsg = controlIn.readUTF();
            System.out.println("Working on file:" + ctrlMsg);
            file = ctrlMsg;


            //ClientStrategy cstrat = new ClientStrategy();
            ClientStrategy cstrat = TCPoverUDPUtils.getTransportStrategy().getClientStrategy();

            //begin simulation loop
            while( !(ctrlMsg = controlIn.readUTF()).equals("Done") ){
                String sserverMsgs = TCPoverUDPUtils.receiveDatagram(udpSocket, proxyIP, proxyPort);
                List<Message> serverMsgs = TCPoverUDPUtils.stringToMessages(sserverMsgs);
                List<Message> clientmsgs = cstrat.sendRcv(serverMsgs);
                metrics.addSent(clientmsgs.size());
                metrics.addReceived(serverMsgs.size());
                metrics.incRTTs();
                String sclientmsgs = TCPoverUDPUtils.messagesToString(clientmsgs);
                TCPoverUDPUtils.sendDatagram(udpSocket, sclientmsgs, proxyIP, proxyPort);
                
            }
            metrics.decRTTs();
            String metricReport = metrics.getMetricReport(); 
            System.out.println(metricReport);
            BufferedWriter log = new BufferedWriter(new FileWriter(ClientDownloadsDir+"LOG_"+file));
            log.write(metricReport);
            log.close();

            TCPoverUDPUtils.writeMessageStringsFile(ClientDownloadsDir+this.file, cstrat.getFile());

            TCPoverUDPUtils.sendFile(cstrat.getFile(), controlOut);
            

        } catch(IOException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String args[]){
        Map<String,String> connSettings = 
                TCPoverUDPUtils.parseSettings("connectionSettings");
        clientTCPoverUDP client = new clientTCPoverUDP(
            connSettings.get("proxy_ip"),
            Integer.parseInt(connSettings.get("proxy_port")),
            Integer.parseInt(connSettings.get("proxy_control_port")),
            Integer.parseInt(connSettings.get("client_port"))
        );

        client.beginSim();
    }
}