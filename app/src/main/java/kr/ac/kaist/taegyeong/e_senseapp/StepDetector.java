package kr.ac.kaist.taegyeong.e_senseapp;

public class StepDetector {
    private final String TAG = "StepDetector";

    private final double STEP_THREASHOLD = 1.2;

    private StepEventListener mStepEventListener;

    private boolean inStep = false;

    public StepDetector(StepEventListener listener) {
        mStepEventListener = listener;
    }

    public void feedData(int timestamp, double accel_mag, double[] accel, double[] gyro) {
        if (accel_mag > STEP_THREASHOLD && !inStep) {
            inStep = true;
            mStepEventListener.onStepDetected();
        } else if (accel_mag < STEP_THREASHOLD) {
            inStep = false;
        }
    }

    public interface StepEventListener {
        void onStepDetected();
    }
}
