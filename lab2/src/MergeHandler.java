import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.MergeView;
import org.jgroups.View;

import java.util.List;

public class MergeHandler extends Thread{
    private final JChannel channel;
    private final MergeView mergeView;

    public MergeHandler(JChannel channel, MergeView mergeView){
        this.channel = channel;
        this.mergeView = mergeView;
    }

    @Override
    public void run() {
        System.out.println("MergeHandler run.");

        List<View> subgroups = mergeView.getSubgroups();
        subgroups.sort((gr1, gr2) -> {
            if (gr1.size() == gr2.size()) {
                return gr1.compareTo(gr2);
            } else {
                return gr1.size() - gr2.size();
            }
        });
        View mainView = subgroups.get(0);
        Address local_addr=channel.getAddress();

        if(!mainView.getMembers().contains(local_addr)) {
            System.out.println("Not a member of main view after merge, resetting state.");
            try {
                channel.getState(null, 0);
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Already a member of main view after merge.");
        }
    }
}
