package ch.dissem.apps.down;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import rajawali.RajawaliActivity;


public class MainActivity extends RajawaliActivity {
    private GLSurfaceView glView;
    private SensorService sensorService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorService = new SensorService(this);
        HorizonRenderer renderer = new HorizonRenderer(this, sensorService);
        renderer.setSurfaceView(mSurfaceView);
        setRenderer(renderer);
    }

    @Override
    protected void onPause() {
        sensorService.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        sensorService.resume();
        super.onResume();
    }
}
