package pm.gnosis.android.app.wallet.util.rlp;

import java.io.Serializable;

/**
 * Wrapper class for decoded elements from an RLP encoded byte array.
 * <p>
 * www.ethereumJ.com
 *
 * @author: Roman Mandeleil
 * Created on: 01/04/2014 10:45
 */
public interface RLPElement extends Serializable {
    byte[] getRLPData();
}
