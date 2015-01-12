package ch.dissem.apps.down;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import ch.dissem.libraries.math.Quaternion;
import rajawali.BaseObject3D;
import rajawali.lights.DirectionalLight;
import rajawali.materials.DiffuseMaterial;
import rajawali.primitives.Sphere;
import rajawali.renderer.RajawaliRenderer;

import javax.microedition.khronos.opengles.GL10;

import static java.lang.Math.PI;

/**
 * Created by chris on 02.01.15.
 */
public class HorizonRenderer extends RajawaliRenderer {
    public static final double DEGREES = 180 / PI;
    private BaseObject3D sphere;
    private SensorService sensorService;

    public HorizonRenderer(Context context, SensorService service) {
        super(context);
        setFrameRate(60);
        sensorService = service;
    }

    protected void initScene() {
        setBackgroundColor(1, 1, 1, 1);
        DirectionalLight light = new DirectionalLight(1f, 0.2f, -1.0f); // set the direction
        light.setColor(1.0f, 1.0f, 1.0f);
        light.setPower(2);

        Bitmap bg = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.horizon);
        DiffuseMaterial material = new DiffuseMaterial();
        sphere = new Sphere(1, 18, 18);
        sphere.setMaterial(material);
        sphere.addLight(light);
        sphere.addTexture(mTextureManager.addTexture(bg));
        addChild(sphere);

        mCamera.setZ(4.2f);
    }

    public void onDrawFrame(GL10 glUnused) {
        super.onDrawFrame(glUnused);

        Quaternion orientation = sensorService.getOrientation();
        double[] rot = orientation.getEulerAngles();
        sphere.setRotation((float) (rot[0] * DEGREES - 90), (float) (rot[2] * DEGREES - 90), (float) (-rot[1] * DEGREES));
    }
}
