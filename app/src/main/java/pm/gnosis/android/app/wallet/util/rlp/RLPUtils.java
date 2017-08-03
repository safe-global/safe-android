package pm.gnosis.android.app.wallet.util.rlp;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

class RLPUtils {
    //Util
    static int byteArrayToInt(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).intValue();
    }

    /**
     * Convert a byte-array into a hex String.<br>
     * Works similar to {@link Hex#toHexString}
     * but allows for <code>null</code>
     *
     * @param data - byte-array to convert to a hex-string
     * @return hex representation of the data.<br>
     * Returns an empty String if the input is <code>null</code>
     * @see Hex#toHexString
     */
    static String toHexString(byte[] data) {
        return data == null ? "" : Hex.toHexString(data);
    }
}
