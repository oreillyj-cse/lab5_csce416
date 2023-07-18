import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;

public class TCPoverUDPUtils{
    public static final int DATAGRAM_BUFFER_SIZE = 4096;

    public static TransportStrategy getTransportStrategy(){
        return new OneByOneStrategy();
        //return new StudentStrategy();
    }

    public static Map<String,String> parseSettings(String fileName){
        //https://www.geeksforgeeks.org/different-ways-reading-text-file-java/
        Map<String,String> map = new HashMap<String,String>();
        BufferedReader fileInput = null;
        try{    
            fileInput = new BufferedReader(
                new FileReader(
                    new File(fileName)
                )
            );
            String line = "";
            while((line=fileInput.readLine() )!= null){
                int eqIndex = line.indexOf('=');
                if(eqIndex == -1) {
                    continue;//skip a line without an '=' symbol
                }
                String key = line.substring(0, eqIndex).trim();
                String val = line.substring(eqIndex+1).trim();
                map.put(key,val);
            }
            fileInput.close();
        } catch(FileNotFoundException e){
            System.err.println("e.getMessage()");
            e.printStackTrace();
        } catch(IOException e){
            System.err.println("e.getMessage()");
            e.printStackTrace();
        } 
        return map;
    }

    public static void writeMessageStringsFile(String filename, List<String> msgs) throws IOException{
        BufferedWriter br = new BufferedWriter(new FileWriter(filename));
        for(String msg : msgs){
            br.write(msg+"\n");
        }
        br.close();
    }

    public static void writeMessageFile(String filename, List<Message> msgs) throws IOException{
        BufferedWriter br = new BufferedWriter(new FileWriter(filename));
        for(Message m : msgs){
            br.write(m.msg+"\n");
        }
        br.close();
    }

    public static String messagesToString(List<Message> msgs){
        if(msgs.size() == 0) return ";";
        StringBuffer sb = new StringBuffer();
        for(Message m : msgs){
            sb.append(m.num+","+m.msg+";");
        }
        sb.delete(sb.length()-1,sb.length()); // ';' is separator
        return sb.toString();
    }

    public static List<Message> stringToMessages(String str){
        List<Message> msgs= new ArrayList<Message>();
        
        if(str.equals(";")) return msgs;//";" means nothing
        
        StringTokenizer st = new StringTokenizer(str, ";");
        String msgString;
        while(st.hasMoreTokens()){
            msgString = st.nextToken();
            msgString = msgString.trim();
            int commaIndex = msgString.indexOf(',');
            String seqString = msgString.substring(0, commaIndex);
            String payloadString = msgString.substring(commaIndex+1);
            int seq = Integer.parseInt(seqString.trim());
            payloadString = payloadString.trim();
            msgs.add(new Message(seq,payloadString));
        }
        return msgs;
    }

    public static void sendDatagram(DatagramSocket from_socket,String msg,
        String to_ip, int to_port) throws UnknownHostException, IOException
    {
        byte[] msgBytes = msg.getBytes(Charset.forName("UTF-8"));
        DatagramPacket dp = new DatagramPacket(msgBytes, msgBytes.length);
        dp.setAddress(InetAddress.getByName(to_ip));
        dp.setPort(to_port);
        from_socket.send(dp);
    }

    public static String receiveDatagram(DatagramSocket sock,
                            String expected_ip, int expected_port)
        throws IOException
    {
        byte[] buf = new byte[DATAGRAM_BUFFER_SIZE];
        DatagramPacket dp = new DatagramPacket(buf, DATAGRAM_BUFFER_SIZE);
        sock.receive(dp);
        int len = dp.getLength();
        byte[] subBuf = new byte[len];
        for(int i = 0; i < len; ++i){
            subBuf[i] = buf[i];
        }
        String s = new String (subBuf,Charset.forName("UTF-8") );
        String senderIP = dp.getAddress().getHostAddress();
        int senderPort = dp.getPort();
        if(!senderIP.equals(expected_ip) || senderPort != expected_port ) {
            System.out.println("received packet from unnexpected IP/port");
            System.out.println("Expected:");
            System.out.println(expected_ip +":"+expected_port);
            System.out.println("Actual:");
            System.out.println(senderIP+":"+senderPort);
        }
        return s;
    } 

    public static void sendFile(List<String> strmsgs, DataOutputStream dos) throws IOException{
        for(String s: strmsgs){
            dos.writeUTF(s);
        }
        dos.writeUTF("END");
    }

    public static List<String> receiveFile(DataInputStream dis) throws IOException{
        List<String> strmsgs = new ArrayList<String>();
        String msg;
        while(!(msg=dis.readUTF()).equals("END")){
            strmsgs.add(msg);
        }
        return strmsgs;
    }

    public static void main(String args[]){
        //These are just little unit tests -- no need to call a utilities file
        // ... obviously
        System.out.println("Demonstrating reading the connectionSettings file:");
        Map<String,String> connSettings = parseSettings("connectionSettings");
        
        for(String key: connSettings.keySet()){
            System.out.println(key+"="+connSettings.get(key));
        }

        List<String> strMsgs = 
                    Control.genMessages(10);
        List<Message> msgs = new ArrayList<Message>();
        for(int i = 0; i<strMsgs.size();++i){
            msgs.add(new Message(i,strMsgs.get(i)));
        }

            
        String strMessages = TCPoverUDPUtils.messagesToString(msgs);
        String reconStrMessages = TCPoverUDPUtils.messagesToString(
            TCPoverUDPUtils.stringToMessages(strMessages)
        );
        System.out.println(strMessages);
        System.out.println(reconStrMessages);
        System.out.println("strMessages.equals(reconStrMessages))=="+
            strMessages.equals(reconStrMessages));
    }
}