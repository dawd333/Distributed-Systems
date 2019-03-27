import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

import java.net.InetAddress;
import java.util.HashMap;

public class DistributedMap implements SimpleStringMap {
    private final static String CLUSTER_NAME = "HashMap";

    private final HashMap<String, Integer> hashMap = new HashMap<>();
    private JChannel channel;

    public DistributedMap(){
        System.setProperty("java.net.preferIPv4Stack","true");
        channel = new JChannel(false);
        ReceiverAdapter adapter = new MapReceiveAdapter(hashMap, channel);
        ProtocolStack stack = new ProtocolStack();
        channel.setProtocolStack(stack);
        try {
            stack.addProtocol(new UDP().setValue("mcast_group_addr", InetAddress.getByName("230.100.200.13")))
                    .addProtocol(new PING())
                    .addProtocol(new MERGE3())
                    .addProtocol(new FD_SOCK())
                    .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                    .addProtocol(new VERIFY_SUSPECT())
                    .addProtocol(new BARRIER())
                    .addProtocol(new NAKACK2())
                    .addProtocol(new UNICAST3())
                    .addProtocol(new STABLE())
                    .addProtocol(new GMS())
                    .addProtocol(new UFC())
                    .addProtocol(new MFC())
                    .addProtocol(new FRAG2())
                    .addProtocol(new STATE())
                    .addProtocol(new FLUSH());

            stack.init();
            channel.setReceiver(adapter);
            channel.connect(CLUSTER_NAME);
            channel.getState(null , 0);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean containsKey(String key){
        return hashMap.containsKey(key);
    }

    @Override
    public Integer get(String key){
        return hashMap.get(key);
    }

    @Override
    public void put(String key, Integer value){
        ProtocolMessage msg = ProtocolMessage.makePutMessage(key, value);
        sendMsg(msg);
    }

    @Override
    public Integer remove(String key){
        Integer removedValue = hashMap.get(key);
        ProtocolMessage msg = ProtocolMessage.makeRemoveMessage(key);
        sendMsg(msg);
        return removedValue;
    }

    public void close(){
        channel.close();
    }

    private void sendMsg(ProtocolMessage msg){
        try {
            byte[] buffer = Util.objectToByteBuffer(msg);
            Message jgroupsMsg = new Message(null, buffer);
            channel.send(jgroupsMsg);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
