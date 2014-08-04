package mobi.droid.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import mobi.droid.fakeroad.R;

/**
 * Created by max on 19.05.14.
 */

public class SpeedPicker extends FrameLayout{

    private final TextView mTvSpeed2;
    private NumberPicker mSpeedSpinner;
    private NumberPicker mNp2;
    private NumberPicker mNp3;
    private NumberPicker mSpMeasure;
    private int mCurrentSpeed;
    private TextView mTvSpeed1;

    public SpeedPicker(final Context context){
        this(context, null);
    }

    public SpeedPicker(final Context context, final AttributeSet attrs){
        this(context, attrs, 0);
    }

    public SpeedPicker(final Context context, final AttributeSet attrs, final int defStyle){
        super(context, attrs, defStyle);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.speed_picker, this, true);

        mSpeedSpinner = (NumberPicker) findViewById(R.id.npSpeed);

        mSpeedSpinner.setMinValue(0);
        mSpeedSpinner.setMaxValue(300);
        mSpeedSpinner.setOnLongPressUpdateInterval(50);
//        mSpeedSpinner.setFormatter(NumberPicker.getTwoDigitFormatter());
        mTvSpeed1 = (TextView) findViewById(R.id.tvSpeed1);
        mTvSpeed2 = (TextView) findViewById(R.id.tvSpeed2);
        mSpeedSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){

            public void onValueChange(NumberPicker spinner, int oldVal, int newVal){
//                updateInputState(); todo
                int minValue = mSpeedSpinner.getMinValue();
                int maxValue = mSpeedSpinner.getMaxValue();
                onSpeedChanged();
            }
        });
        mSpMeasure = (NumberPicker) findViewById(R.id.spMeasure);

        mSpMeasure.setMinValue(0);
        mSpMeasure.setMaxValue(SpeedType.values().length - 1);
        String[] values = new String[SpeedType.values().length];
        SpeedType[] values1 = SpeedType.values();
        for(int i = 0; i < values1.length; i++){
            values[i] = values1[i].name();
        }
        mSpMeasure.setDisplayedValues(values);
        mSpMeasure.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){

            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
//                updateInputState();
                picker.requestFocus();
                onSpeedChanged();
            }
        });

    }

    private void onSpeedChanged(){
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if(mOnSpeedChangedListener != null){
            mOnSpeedChangedListener.onTimeChanged(this, getCurrentSpeed(), getCurrentMeasure());
        }

        SpeedType currentMeasure = getCurrentMeasure();
        int currentSpeed = getCurrentSpeed();

        switch(currentMeasure){
            case ms:
                mTvSpeed1.setText(String.format("%.2f km/h", convertMStoKMH(currentSpeed)));
                mTvSpeed2.setText(String.format("%.2f mph", convertMStoMPH(currentSpeed)));
                break;
            case  mph:
                mTvSpeed1.setText(String.format("%.2f m/s", convertMPHtoMS(currentSpeed)));
                mTvSpeed2.setText(String.format("%.2f km/h", convertMPHtoKMH(currentSpeed)));
                break;
            case kmh:
                mTvSpeed1.setText(String.format("%.2f m/s", convertKMHtoMS(currentSpeed)));
                mTvSpeed2.setText(String.format("%.2f mph", convertKMHtoMPH(currentSpeed)));
                break;
        }

//        String formatMin = String.format("Min: %d m/s %d km/h %d mph", mMinSpeed, (int) (mMinSpeed * 3.6),
//                                         (int) (mMinSpeed * 2.23));

    }
    public static float convertKMHtoMS(float ms){
        return ms * 0.27777777777778f;
    }

    public static float convertKMHtoMPH(float ms){
        return ms * 0.62137119223733f;
    }


    public static float convertMPHtoMS(float ms){
        return ms * 0.44704f;
    }

    public static float convertMPHtoKMH(float ms){
        return ms * 1.609344f;
    }


    public static float convertMStoMPH(float ms){
        return ms * 2.2369362920544f;
    }

    public static float convertMStoKMH(float ms){
        return ms * 3.6f;
    }

    private SpeedType getCurrentMeasure(){
        return SpeedType.values()[mSpMeasure.getValue()];
    }

    public int getCurrentSpeed(){
        return mSpeedSpinner.getValue();
    }

    enum SpeedType{
        ms, mph, kmh
    }

    /**
     * A no-op callback used in the constructor to avoid null checks later in
     * the code.
     */
    private static final OnSpeedChangedListener NO_OP_CHANGE_LISTENER = new OnSpeedChangedListener(){

        @Override
        public void onTimeChanged(final SpeedPicker view, final int speed, final SpeedType aSpeedType){

        }
    };

    // callbacks
    private OnSpeedChangedListener mOnSpeedChangedListener;

    /**
     * The callback interface used to indicate the time has been adjusted.
     */
    public interface OnSpeedChangedListener{

        /**
         * @param view The view associated with this listener.
         * @param speed The current speed.
         * @param aSpeedType The current type.
         */
        void onTimeChanged(SpeedPicker view, int speed, SpeedType aSpeedType);
    }
}
