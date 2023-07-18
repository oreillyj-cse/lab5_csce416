import java.util.*;

public interface ClientStrategy{
    public void reset();
    public List<String> getFile();
    public List<Message> sendRcv(List<Message> serverMsgs);
}