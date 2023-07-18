
public class StudentStrategy implements TransportStrategy{
    public ClientStrategy getClientStrategy(){
        return new StudentClientStrategy();
    }

    public ServerStrategy getServerStrategy(){
        return new StudentServerStrategy(); 
    }
}