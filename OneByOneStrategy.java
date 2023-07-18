
public class OneByOneStrategy implements TransportStrategy{
    public ClientStrategy getClientStrategy(){
        return new OneByOneClientStrategy();
    }

    public ServerStrategy getServerStrategy(){
        return new OneByOneServerStrategy(); 
    }
}