import java.util.*;

public class OneByOneServerStrategy implements ServerStrategy{
    List<String> file;
    boolean[] acks;

    public OneByOneServerStrategy(){
    }

    public void setFile(List<String> file){
        this.file = file;
        acks = new boolean[file.size()];
    }

    public void reset(){

    }

    public List<Message> sendRcv(List<Message> clientMsgs){
        for(Message m: clientMsgs){
            acks[m.num-1] =true;
            System.out.println(m.num+","+m.msg);
        }
        int firstUnACKed = 0;
        
        List<Message> msgs = new ArrayList<Message>();

        while( firstUnACKed < acks.length && acks[firstUnACKed]) ++firstUnACKed;

        if(firstUnACKed < acks.length) {
            msgs.add(new Message(firstUnACKed,file.get(firstUnACKed)));   
        }
        return msgs;


    }
    
}