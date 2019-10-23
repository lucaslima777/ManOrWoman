package lln.man.woman.view.camera;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import lln.man.woman.R;
import lln.man.woman.contract.CameraContract;
import lln.man.woman.tensorflow.Classifier;
import lln.man.woman.tensorflow.TensorFlowImageClassifier;
import lln.man.woman.utils.TensorFlowUtils;

/**
 * @author lucaslima
 */
public class CameraView extends AppCompatActivity implements CameraContract {

    private TensorFlowUtils tensorFlowUtils;
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private com.wonderkiln.camerakit.CameraView cameraView;

    private FloatingActionButton btnCapture;
    private FloatingActionButton btnTurn;
    private LottieAnimationView scanView;
    private LottieAnimationView scanFocusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);
        bindViews();
        setListeners();
        tensorFlowUtils = new TensorFlowUtils();

        initTensorFlowAndLoadModel();
    }

    private void bindViews() {
        cameraView = findViewById(R.id.cameraView);
        btnCapture = findViewById(R.id.btnCapture);
        btnTurn = findViewById(R.id.btnTurn);
        scanView = findViewById(R.id.viewScan);
        scanFocusView = findViewById(R.id.viewScanFocus);
    }

    private void setListeners() {
        btnCapture.setOnClickListener(new onCaptureImage());
        btnTurn.setOnClickListener(new onTurnCamera());
        cameraView.addCameraKitListener(new CameraListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void showCamera() {
        btnCapture.setVisibility(View.VISIBLE);
        btnTurn.setVisibility(View.VISIBLE);
        scanView.setVisibility(View.GONE);
        scanFocusView.setVisibility(View.VISIBLE);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void showScan() {
        btnCapture.setVisibility(View.GONE);
        btnTurn.setVisibility(View.GONE);
        scanView.setVisibility(View.VISIBLE);
        scanFocusView.setVisibility(View.GONE);
    }

    private class CameraListener implements CameraKitEventListener {

        @Override
        public void onEvent(CameraKitEvent cameraKitEvent) {
            //do nothing
        }

        @Override
        public void onError(CameraKitError cameraKitError) {
            //do nothing
        }

        @Override
        public void onImage(CameraKitImage cameraKitImage) {
            Bitmap bitmap = cameraKitImage.getBitmap();

            bitmap = Bitmap.createScaledBitmap(bitmap, tensorFlowUtils.INPUT_SIZE, tensorFlowUtils.INPUT_SIZE, false);

            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

            String resultsString = results.toString();
            resultsString = resultsString.replace("[", "");
            resultsString = resultsString.replace("]", "");
            resultsString = resultsString.replace(",", "\n");

            if (resultsString.substring(0, 1).equals("f")) {
                Toast.makeText(getApplicationContext(), "Feminino", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Masculino", Toast.LENGTH_LONG).show();
            }
            showCamera();
        }

        @Override
        public void onVideo(CameraKitVideo cameraKitVideo) {
            //do nothing
        }
    }

    private class onCaptureImage implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            showScan();
            cameraView.captureImage();
        }
    }

    private class onTurnCamera implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            cameraView.toggleFacing();
        }
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            tensorFlowUtils.MODEL_PATH,
                            tensorFlowUtils.LABEL_PATH,
                            tensorFlowUtils.INPUT_SIZE);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

}
