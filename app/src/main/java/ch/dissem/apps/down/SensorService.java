package ch.dissem.apps.down;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import ch.dissem.libraries.math.Quaternion;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.content.Context.SENSOR_SERVICE;
import static android.util.FloatMath.sqrt;
import static ch.dissem.libraries.math.Quaternion.H;
import static ch.dissem.libraries.math.Vector.V;

/**
 * Created by chris on 02.01.15.
 */
public class SensorService implements SensorEventListener {
    /**
     * Lower bound for float based vector normals that should be normalizable.
     */
    private static final float EPSILON = 2E-38f;
    private static final float NS2S = 1.0f / 1000000000.0f; // conversion rate from ns to s

    private SensorManager sensorManager;

    private long lastGyroscopeTimestamp;

    private SensorFusionFilter filter;
    private ScheduledExecutorService timer;

    public SensorService(Context ctx) {
        sensorManager = (SensorManager) ctx.getSystemService(SENSOR_SERVICE);
        filter = new SensorFusionFilter(0.97f, 0.03f);
        initListeners();
        resume();
    }

    public void pause() {
        timer.shutdown();
        try {
            timer.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e("SensorService", e.getMessage(), e);
        }
        timer = null;
    }

    public void resume() {
        if (timer == null) {
            timer = Executors.newSingleThreadScheduledExecutor();
            timer.scheduleAtFixedRate(filter, 0, 30, TimeUnit.MILLISECONDS);
        }
    }

    public void initListeners() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                filter.updateAccelerator(H(event.values));
                break;
            case Sensor.TYPE_GYROSCOPE:
                onGyroscopeChanged(event);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                onCompassChanged(event);
                break;
        }
    }

    private void onCompassChanged(SensorEvent event) {
        filter.updateCompass(H(event.values));
    }

    private void onGyroscopeChanged(SensorEvent event) {
        filter.updateGyro(getRotation(event.values, lastGyroscopeTimestamp, event.timestamp));
        lastGyroscopeTimestamp = event.timestamp;
    }

    private Quaternion getRotation(float[] values, long lastTimestamp, long currentTimestamp) {
        if (lastTimestamp == 0) {
            return H(1);
        }
        float dT = (currentTimestamp - lastTimestamp) * NS2S;
        // Axis of the rotation sample, not normalized yet.
        float axisX = values[0];
        float axisY = values[1];
        float axisZ = values[2];

        // Calculate the angular speed of the sample
        float omegaMagnitude = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

        if (omegaMagnitude < EPSILON) {
            return H(1);
        }

        // Normalize the rotation vector if it's big enough to get the axis
        float theta = omegaMagnitude * dT;

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion.
        return H(theta, V(axisX, axisY, axisZ));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // NO OP
    }

    public Quaternion getOrientation() {
        return filter.getOrientation();
    }
}
