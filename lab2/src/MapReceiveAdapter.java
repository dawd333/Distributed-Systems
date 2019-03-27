import org.jgroups.*;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class MapReceiveAdapter extends ReceiverAdapter {
    final private HashMap<String, Integer> hashMap;
    final private JChannel channel;

    public MapReceiveAdapter(HashMap<String, Integer> hashMap, JChannel channel) {
        this.hashMap = hashMap;
        this.channel = channel;
    }

    @Override
    public void receive(Message message){
        try {
            ProtocolMessage protocolMessage = (ProtocolMessage) Util.objectFromByteBuffer(message.getBuffer());
            handleMessage(protocolMessage);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void getState(OutputStream output) throws Exception{
        synchronized (hashMap){
            Util.objectToStream(hashMap, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception{
        synchronized (hashMap) {
            HashMap<String, Integer> map = (HashMap<String, Integer>)Util.objectFromStream(new DataInputStream(input));
            hashMap.clear();
            hashMap.putAll(map);
        }
    }

    @Override
    public void viewAccepted(View view){
        super.viewAccepted(view);
        System.out.println("View accepted: " + view.toString());

        if(view instanceof MergeView){
            MergeView mergeView = (MergeView) view;
            MergeHandler mergeHandler = new MergeHandler(channel, mergeView);
            mergeHandler.run();
        }
    }

    private void handleMessage(ProtocolMessage message){
        switch(message.type){
            case PUT:
                hashMap.put(message.key, message.value);
                break;
            case REMOVE:
                hashMap.remove(message.key);
                break;
        }
    }
}
