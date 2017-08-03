package pm.gnosis.android.app.wallet.util.rlp;

/**
 * www.ethereumJ.com
 *
 * @author: Roman Mandeleil
 * Created on: 21/04/14 16:26
 */
public class RLPItem implements RLPElement {
    private byte[] rlpData;

    RLPItem(byte[] rlpData) {
        this.rlpData = rlpData;
    }

    public byte[] getRLPData() {
        if (rlpData.length == 0)
            return null;
        return rlpData;
    }
}
