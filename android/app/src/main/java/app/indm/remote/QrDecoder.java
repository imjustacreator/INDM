package app.indm.remote;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.EnumMap;
import java.util.Map;

/** Finds a QR code in one camera picture (NV21: the first w*h bytes are the brightness of each pixel). */
final class QrDecoder {
    private QrDecoder() {}

    static String decode(byte[] nv21, int w, int h) {
        if (nv21 == null || w <= 0 || h <= 0 || nv21.length < w * h) return null;
        PlanarYUVLuminanceSource src = new PlanarYUVLuminanceSource(nv21, w, h, 0, 0, w, h, false);
        String t = attempt(new BinaryBitmap(new HybridBinarizer(src)));
        if (t == null) t = attempt(new BinaryBitmap(new HybridBinarizer(src.invert())));
        return t;
    }

    private static String attempt(BinaryBitmap bm) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        try {
            return new QRCodeReader().decode(bm, hints).getText();
        } catch (ReaderException e) {
            return null;
        }
    }
}
