package ch.dissem.apps.down;

import android.util.Log;
import ch.dissem.libraries.math.Quaternion;

import static ch.dissem.apps.down.HorizonRenderer.DEGREES;
import static ch.dissem.libraries.math.Quaternion.H;
import static ch.dissem.libraries.math.Quaternion.IDENTITY;
import static java.lang.Math.copySign;
import static java.lang.Math.sqrt;

/**
 * Created by chris on 02.01.15.
 */
public class SensorFusionFilter implements Runnable {
    public final static Quaternion NORTH = H(0, 1, 0);
    public final static Quaternion WEST = H(1, 0, 0);
    public final static Quaternion UP = H(0, 0, 1);

    private final double fusionFactor;
    private final double fusionCoFactor;

    private final Quaternion lowPassFactor;
    private final Quaternion lowPassCoFactor;

    private Quaternion fusedOrientation = IDENTITY; // Rotation

    private volatile Quaternion gyroRotation = IDENTITY; // Rotation
    private volatile Quaternion acceleratorOrientation = UP; // Vector
    private volatile Quaternion compassOrientation = NORTH; // Vector

    /**
     * @param lowPassFactor [0..1]: higher value -> new accelerator/magnetic measurements have less impact
     * @param fusionFactor  [0..1]: higher value -> gyro measurements have less impact
     */
    public SensorFusionFilter(double lowPassFactor, double fusionFactor) {
        this.fusionFactor = fusionFactor;
        this.fusionCoFactor = 1 - fusionFactor;

        this.lowPassFactor = H(lowPassFactor);
        this.lowPassCoFactor = H(1 - lowPassFactor);
    }

    public void updateGyro(Quaternion gyroValue) {
        gyroRotation = gyroRotation.multiply(gyroValue);
    }

    public void updateAccelerator(Quaternion accValue) {
        if (acceleratorOrientation == UP) {
            acceleratorOrientation = accValue;
        } else {
            acceleratorOrientation = lowPassFactor.multiply(acceleratorOrientation).add(lowPassCoFactor.multiply(accValue));
        }
    }

    public void updateCompass(Quaternion magValue) {
        if (compassOrientation == NORTH) {
            compassOrientation = magValue;
        } else {
            compassOrientation = lowPassFactor.multiply(compassOrientation).add(lowPassCoFactor.multiply(magValue));
        }
    }

    public void update() {
        Quaternion a = acceleratorOrientation.normalize();
        // Quaternion e = compassOrientation.normalize();
        Quaternion h = compassOrientation.cross(acceleratorOrientation).normalize();
        Quaternion m = a.cross(h);

        // Q = [h, m, a]
        double Qxx = h.x;
        double Qyy = m.y;
        double Qzz = a.z;
        double Qzy = a.y;
        double Qyz = m.z;
        double Qxz = h.z;
        double Qzx = a.x;
        double Qyx = m.x;
        double Qxy = h.y;

        // Get rotation from Q
        double t = Qxx + Qyy + Qzz; // (trace of Q)
        double r = sqrt(1 + t);
        double w = 0.5 * r;
        double x = copySign(0.5 * sqrt(1 + Qxx - Qyy - Qzz), Qzy - Qyz);
        double y = copySign(0.5 * sqrt(1 - Qxx + Qyy - Qzz), Qxz - Qzx);
        double z = copySign(0.5 * sqrt(1 - Qxx - Qyy + Qzz), Qyx - Qxy);

        Quaternion rotAccCompass = H(w, x, y, z);

        Quaternion gyroOrientation = fusedOrientation.multiply(gyroRotation);
        fusedOrientation = gyroOrientation.getScaledRotation(fusionCoFactor)
                .multiply(rotAccCompass.getScaledRotation(fusionFactor));

        gyroRotation = IDENTITY;
    }

    private void logRot(String tag, Quaternion q) {
        Log.d(tag, q.getRotationAxis().getVector() + " / " + (q.getRotationAngle() * DEGREES));
    }

    private void logVec(Quaternion q) {
        Log.d("v", q.getVector().toString());
    }

    private void logQ(String tag, Quaternion q) {
        Log.d(tag, q.toString());
    }

    /**
     * Returns the rotation between device and world.
     */
    public Quaternion getOrientation() {
        return fusedOrientation;
    }

    @Override
    public void run() {
        update();
    }
}
