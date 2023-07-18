import java.util.*;

public class StudentClientStrategy implements ClientStrategy{
    ArrayList<String> file;


    public StudentClientStrategy(){
        reset();
    }

    public void reset(){
        file = new ArrayList<String>();
    }

    public List<String> getFile(){
        return file;
    }

    public List<Message> sendRcv(List<Message> serverMsgs){

        return serverMsgs;//obviously wrong...


    }

}