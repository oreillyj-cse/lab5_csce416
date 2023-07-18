import java.util.*;

public interface ServerStrategy{


    public void setFile(List<String> file);

    public void reset();

    public List<Message> sendRcv(List<Message> clientMsgs);
}