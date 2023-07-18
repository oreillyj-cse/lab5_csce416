import java.util.*;

public class OneByOneClientStrategy implements ClientStrategy{
    ArrayList<String> file;


    public OneByOneClientStrategy(){
        reset();
    }

    public void reset(){
        file = new ArrayList<String>();
    }

    public List<String> getFile(){
        return file;
    }

    public List<Message> sendRcv(List<Message> serverMsgs){
        for(Message m : serverMsgs){
            while(file.size() < m.num+1) file.add(null);
            file.set(m.num,m.msg);
            System.out.println(m.num+","+m.msg);
        }

        int nextNeeded = 0;
        while(nextNeeded <file.size() && file.get(nextNeeded)!=null)
            ++nextNeeded;

        Message m=new Message(nextNeeded,"ACK");

        List<Message> ack = new ArrayList<Message>();
        ack.add(m);
        return ack;

    }

}