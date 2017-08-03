package pm.gnosis.android.app.wallet.util.rlp;

import java.util.ArrayList;

/**
 * www.ethereumJ.com
 *
 * @author: Roman Mandeleil
 * Created on: 21/04/14 16:26
 */
public class RLPList extends ArrayList<RLPElement> implements RLPElement {
    private byte[] rlpData;

    void setRLPData(byte[] rlpData) {
        this.rlpData = rlpData;
    }

    public byte[] getRLPData() {
        return rlpData;
    }

    public static void recursivePrint(RLPElement element) {
        if (element == null)
            throw new RuntimeException("RLPElement object can't be null");
        if (element instanceof RLPList) {

            RLPList rlpList = (RLPList) element;
            System.out.print("[");
            for (RLPElement singleElement : rlpList) {
                recursivePrint(singleElement);
            }
            System.out.print("]");
        } else {
            String hex = RLPUtils.toHexString(((RLPItem) element).getRLPData());
            System.out.print(hex + ", ");
        }
    }
}
