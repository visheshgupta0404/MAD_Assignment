package com.example.sensorapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

/**
 * MainActivity: The main entry point of the application.
 * This activity listens to Accelerometer, Light, and Proximity sensors
 * and updates the UI with real-time smoothed data.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Sensor Management
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor lightSensor;
    private Sensor proximitySensor;

    // Smoothing (Low-Pass Filter) Constants
    // ALPHA determines the weight of the new reading vs the previous state.
    private static final float ACCEL_ALPHA = 0.12f; 
    private static final float PROX_ALPHA = 0.4f;   
    
    // Arrays and variables to store smoothed values
    private final float[] gravity = new float[3];
    private float smoothedProximity = -1.0f;

    // UI Components - Accelerometer
    private TextView tvAccelX, tvAccelY, tvAccelZ, tvAccelStatus;
    private CardView cardAccel;

    // UI Components - Light
    private TextView tvLightValue, tvLightStatus;
    private CardView cardLight;

    // UI Components - Proximity
    private TextView tvProximityValue, tvProximityStatus;
    private CardView cardProximity;

    // Max range of the proximity sensor used to determine NEAR/FAR
    private float proximityMaxRange = 1.0f;

    // Colors for status indication
    private int colorActive;
    private int colorUnavailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize colors from resources
        colorActive      = ContextCompat.getColor(this, R.color.sensorActive);
        colorUnavailable = ContextCompat.getColor(this, R.color.sensorUnavailable);

        // Initialize UI component references
        bindViews();

        // Initialize Sensor Manager and retrieve specific sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lightSensor         = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximitySensor     = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Get the maximum range of the proximity sensor if it exists
        if (proximitySensor != null) {
            proximityMaxRange = proximitySensor.getMaximumRange();
        }

        // Update UI to reflect whether sensors are available on this device
        updateAvailabilityUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register listeners for sensors when the activity comes to foreground
        registerSensorIfAvailable(accelerometerSensor);
        registerSensorIfAvailable(lightSensor);
        registerSensorIfAvailable(proximitySensor);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister listeners to save battery when the activity is not in focus
        sensorManager.unregisterListener(this);
    }

    /**
     * Helper to register a sensor listener only if the sensor is present on the device.
     */
    private void registerSensorIfAvailable(Sensor sensor) {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Callback triggered when sensor data changes.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                updateAccelerometer(event.values);
                break;
            case Sensor.TYPE_LIGHT:
                updateLight(event.values[0]);
                break;
            case Sensor.TYPE_PROXIMITY:
                updateProximity(event.values[0]);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this implementation
    }

    /**
     * Processes accelerometer data applying a low-pass filter for smoothing.
     * Updates the UI with X, Y, Z components and total magnitude.
     */
    private void updateAccelerometer(float[] values) {
        gravity[0] = ACCEL_ALPHA * values[0] + (1 - ACCEL_ALPHA) * gravity[0];
        gravity[1] = ACCEL_ALPHA * values[1] + (1 - ACCEL_ALPHA) * gravity[1];
        gravity[2] = ACCEL_ALPHA * values[2] + (1 - ACCEL_ALPHA) * gravity[2];

        tvAccelX.setText(String.format("X: %+.2f m/s²", gravity[0]));
        tvAccelY.setText(String.format("Y: %+.2f m/s²", gravity[1]));
        tvAccelZ.setText(String.format("Z: %+.2f m/s²", gravity[2]));

        double magnitude = Math.sqrt(
                gravity[0] * gravity[0] +
                gravity[1] * gravity[1] +
                gravity[2] * gravity[2]);
        tvAccelStatus.setText(String.format("Magnitude: %.2f m/s² (Active)", magnitude));
    }

    /**
     * Updates the light sensor UI with the current lux value and a human-readable description.
     */
    private void updateLight(float lux) {
        tvLightValue.setText(String.format("%.1f lux", lux));
        tvLightStatus.setText(describeLux(lux));
    }

    /**
     * Processes proximity data with smoothing.
     * Updates UI with distance and NEAR/FAR status.
     */
    private void updateProximity(float cm) {
        if (smoothedProximity < 0) {
            smoothedProximity = cm;
        }

        smoothedProximity = PROX_ALPHA * cm + (1 - PROX_ALPHA) * smoothedProximity;

        // Snapping values to prevent minor jitter
        if (smoothedProximity < 0.1f) smoothedProximity = 0.0f;
        if (Math.abs(smoothedProximity - cm) < 0.1f) smoothedProximity = cm;

        tvProximityValue.setText(String.format("%.1f cm", smoothedProximity));

        // Determine NEAR/FAR status based on sensor max range
        if (cm >= proximityMaxRange) {
            tvProximityStatus.setText("FAR — no object detected");
        } else {
            tvProximityStatus.setText("NEAR — object detected");
        }
    }

    /**
     * Links Java objects to their corresponding XML layout views.
     */
    private void bindViews() {
        cardAccel       = findViewById(R.id.cardAccelerometer);
        tvAccelX        = findViewById(R.id.tvAccelX);
        tvAccelY        = findViewById(R.id.tvAccelY);
        tvAccelZ        = findViewById(R.id.tvAccelZ);
        tvAccelStatus   = findViewById(R.id.tvAccelStatus);

        cardLight       = findViewById(R.id.cardLight);
        tvLightValue    = findViewById(R.id.tvLightValue);
        tvLightStatus   = findViewById(R.id.tvLightStatus);

        cardProximity       = findViewById(R.id.cardProximity);
        tvProximityValue    = findViewById(R.id.tvProximityValue);
        tvProximityStatus   = findViewById(R.id.tvProximityStatus);
    }

    /**
     * Sets the initial state of the UI based on sensor availability.
     */
    private void updateAvailabilityUI() {
        setSensorAvailability(accelerometerSensor, tvAccelStatus,    cardAccel,     "Accelerometer");
        setSensorAvailability(lightSensor,         tvLightStatus,    cardLight,     "Light");
        setSensorAvailability(proximitySensor,     tvProximityStatus, cardProximity,"Proximity");
    }

    /**
     * Configures a sensor's UI card and status text depending on its presence.
     */
    private void setSensorAvailability(Sensor sensor, TextView statusView, CardView card, String name) {
        if (sensor == null) {
            statusView.setText(name + " sensor NOT available");
            statusView.setTextColor(colorUnavailable);
            card.setAlpha(0.5f);
        } else {
            statusView.setText("Waiting for data…");
            statusView.setTextColor(colorActive);
            card.setAlpha(1.0f);
        }
    }

    /**
     * Converts raw lux values into a descriptive string for better user understanding.
     */
    private String describeLux(float lux) {
        if (lux < 1)     return "Very dark (night)";
        if (lux < 50)    return "Dim indoor / corridor";
        if (lux < 200)   return "Normal indoor lighting";
        if (lux < 1000)  return "Bright indoor / overcast";
        if (lux < 10000) return "Daylight (indirect sun)";
        return "Direct sunlight";
    }
}
