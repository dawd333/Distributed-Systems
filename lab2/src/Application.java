public class Application {
    public static void main(String[] args){
        DistributedMap map = new DistributedMap();
        Client client = new Client(map);
        client.start();
    }
}
