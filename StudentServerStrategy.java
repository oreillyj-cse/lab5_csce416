import java.util.*;

public class StudentServerStrategy implements ServerStrategy{
    List<String> file;


    public StudentServerStrategy(){
        reset();
    }

    public void setFile(List<String> file){
        this.file = file;
    }

    public void reset(){


    }

    public List<Message> sendRcv(List<Message> clientMsgs){
        
        return clientMsgs; //obviously wrong


    }
    
}