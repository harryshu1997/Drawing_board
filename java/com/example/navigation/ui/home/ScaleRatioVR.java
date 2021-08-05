package com.example.navigation.ui.home;

/**
 * class for create a variable with click listener
 */
public class ScaleRatioVR {
    private float scale = 1.0f;
    private ScaleRatioVR.ChangeListener listener;

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        if (listener != null) listener.onChange();
    }

    public ScaleRatioVR.ChangeListener getListener() {
        return listener;
    }

    public void setListener(ScaleRatioVR.ChangeListener listener) {
        this.listener = listener;
    }

    public interface ChangeListener {
        void onChange();
    }
}
