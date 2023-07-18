
public class Metrics{
    private int sent;
    private int received;
    private int RTTs;

    public Metrics(){
        reset();
    }

    public void reset(){
        sent = 0;
        received = 0;
        RTTs = 0;
    }

    public int getSent(){
        return sent;
    }

    public int getReceived(){
        return received;
    }

    public int getRTTs(){
        return RTTs;
    }

    public void addSent(int n){
        sent+=n;
    }

    public void addReceived(int n){
        received+=n;
    }

    public void incRTTs(){
        ++RTTs;
    }

    public void decRTTs(){
        --RTTs;
    }

    public String getMetricReport(){
        StringBuilder sb = new StringBuilder("Metric Report\n");
        sb.append("Received:" +getReceived()+"\n");
        sb.append("Sent:" +getSent()+"\n");
        sb.append("RTTs:" +getRTTs()+"\n");
        return sb.toString();
    }
}