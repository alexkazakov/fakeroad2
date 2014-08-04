package mobi.droid.fakeroad.service;

import android.annotation.TargetApi;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.Actions;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;
import mobi.droid.fakeroad.ui.activity.MainActivity;

import java.lang.reflect.Method;
import java.util.Random;

import static mobi.droid.fakeroad.location.MapsHelper.*;

public class FakeLocationService extends Service{

    public static final String EXTRA_ROUTE_ID = "points";
    public static final String EXTRA_SPEED = "speed";
    public static final String EXTRA_MIN_SPEED = "min.speed";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_RANDOM_SPEED = "random.speed";
    //
    public static int LOCATION_UPDATE_INTERVAL = 1000;
    //
    private Handler mHandler = new Handler();
    private boolean mMoving;
    private LocationGenerator mGenerator;
    private int mSpeed = 0;
    private int mRouteID = -1;
    private boolean mUseRandomSpeed;
    private int mMinSpeed = 0;

    public static void start(Context aContext, int aSpeed, int aMinSpeed, long aTime, int aRoute, boolean aRandomSpeed){
        Intent intent = new Intent(Actions.ACTION_START_MOVING);
        intent.setClass(aContext, FakeLocationService.class);
        intent.putExtra(EXTRA_ROUTE_ID, aRoute);
        intent.putExtra(EXTRA_SPEED, aSpeed);
        intent.putExtra(EXTRA_TIME, aTime);
        intent.putExtra(EXTRA_MIN_SPEED, aMinSpeed);
        intent.putExtra(EXTRA_RANDOM_SPEED, aRandomSpeed);
        aContext.startService(intent);
    }

    public static void stop(Context aContext){
        Intent intent = new Intent(Actions.ACTION_STOP_MOVING);
        intent.setClass(aContext, FakeLocationService.class);
        aContext.startService(intent);
    }

    public static boolean isRunning(Context aContext){
        ActivityManager manager = (ActivityManager) aContext.getSystemService(Context.ACTIVITY_SERVICE);
        //noinspection ConstantConditions
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(FakeLocationService.class.getName().equals(service.service.getClassName())){
                return true;
            }
        }
        return false;

    }

    @Override
    public IBinder onBind(final Intent intent){
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId){
        if(Actions.ACTION_START_MOVING.equals(intent.getAction())){
            if(!mMoving){
                startMoving(intent);
            }
        } else if(Actions.ACTION_STOP_MOVING.equals(intent.getAction())){
            if(mMoving){
                stopMoving();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopMoving(){
        mHandler.removeCallbacks(mGenerator);
        mMoving = false;
        mGenerator.stop();

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
        locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = createNotification(null);
        nm.notify(1, notification);
    }

    private void startMoving(final Intent aIntent){
        mMoving = true;

        mRouteID = aIntent.getIntExtra(EXTRA_ROUTE_ID, mRouteID);
        mUseRandomSpeed = aIntent.getBooleanExtra(EXTRA_RANDOM_SPEED, mUseRandomSpeed);

        mSpeed = aIntent.getIntExtra(EXTRA_SPEED, mSpeed);
        mMinSpeed = aIntent.getIntExtra(EXTRA_MIN_SPEED, mSpeed);
        long time = aIntent.getLongExtra(EXTRA_TIME, 0);

        if(mSpeed < 1 && time < 1 || mRouteID == -1){
            mMoving = false;
            stopSelf();
            return;
        }

        startForeground(1, createNotification(null));

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        lm.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, false, true, true, 1, 0);
        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        mGenerator = new LocationGenerator(mRouteID, mUseRandomSpeed);
        mHandler.post(mGenerator);
    }

    private Notification createNotification(String aText){
        Notification.Builder builder = new Notification.Builder(this);
        builder.setAutoCancel(true);
        builder.setOngoing(true);
        //noinspection ConstantConditions
        builder.setContentTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));
        if(TextUtils.isEmpty(aText)){
            builder.setContentText("Stopped");
        } else{
            builder.setContentText(aText);
        }

        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setPriority(Notification.PRIORITY_HIGH);

        if(mMoving){
            PendingIntent pi = PendingIntent.getService(this, 0, new Intent(Actions.ACTION_STOP_MOVING),
                                                        PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_media_pause, "Stop", pi);
        } else{
            PendingIntent pi = PendingIntent.getService(this, 1, new Intent(Actions.ACTION_START_MOVING),
                                                        PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_media_play, "Start", pi);
        }

        builder.setWhen(System.currentTimeMillis());
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        //noinspection deprecation
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            return builder.build();
        }
        //noinspection deprecation
        return builder.getNotification();
    }

    //
    private class LocationGenerator implements Runnable{
        private boolean mUseRandomSpeed;
        private Random mSpeedRandom = new Random();
        private Method mLocationJellyBeanFixMethod;

        private LatLng mFinalPoint;
        private Pair<LatLng, LatLng> mCurrentPoints;

        private final LocationDbHelper mDbHelper;
        private final Cursor mRouteCursor;

        private LocationGenerator(final int aRouteID, final boolean aRandomSpeed){
            mUseRandomSpeed = aRandomSpeed;
            mDbHelper = new LocationDbHelper(FakeLocationService.this);
            mRouteCursor = mDbHelper.routeCursor(aRouteID);

            if(mRouteCursor == null || !mRouteCursor.moveToLast()){
                Toast.makeText(FakeLocationService.this, "No route data is available: " + aRouteID,
                               Toast.LENGTH_LONG).show();
                stopMoving();
                return;
            }
            mFinalPoint = mDbHelper.readLatLng(mRouteCursor);

            mRouteCursor.moveToFirst();
            LatLng startLatLng = mDbHelper.readLatLng(mRouteCursor);
            mCurrentPoints = Pair.create(startLatLng, startLatLng);

            try{
                mLocationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
            } catch(NoSuchMethodException ignored){
            }
        }

        @Override
        public void run(){
            if(!mMoving){
                return;
            }

            int speed = calculateSpeed();

            mCurrentPoints = nextLatLng(mCurrentPoints, mRouteCursor, mFinalPoint, mDbHelper, speed);
            LatLng currentPoint = mCurrentPoints.second;
            // save current point position
            int position = mRouteCursor.getPosition();

            Pair<LatLng, LatLng> nextPoints = nextLatLng(mCurrentPoints, mRouteCursor, mFinalPoint, mDbHelper, speed);
            LatLng nextPoint = nextPoints.second;

            // restore current point position
            mRouteCursor.moveToPosition(position);

            Location location = createLocation(currentPoint, speed, nextPoint);

            String speedInfo;
            String units = PreferenceManager.getDefaultSharedPreferences(FakeLocationService.this).getString(
                    "speed.units", "m/s");

            switch(units){
                case "km/h":
                    speedInfo = "Moving: " + (int) (speed * 3.6) + " km/h";
                    break;
                case "mph":
                    speedInfo = "Moving: " + (int) (speed * 2.23) + " mph";
                    break;
                default:
                    speedInfo = "Moving: " + speed + " m/s";
            }

            Notification notification = createNotification(speedInfo);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(1, notification);

            Log.v(TAG, "curr=" + currentPoint + " next=" + nextPoint + " " + speedInfo);

            if(!publishLocation(location) || currentPoint.equals(nextPoint)){
                stopMoving();
            } else{
                mHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }

        }

        private int calculateSpeed(){
            int speed;
            if(mUseRandomSpeed){
                speed = (mMinSpeed + (mSpeedRandom.nextInt(mSpeed - mMinSpeed) + 1));
            } else{
                speed = mSpeed;
            }
            return speed;
        }

        private boolean publishLocation(final Location aLocation){
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            try{
                lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, aLocation);
            } catch(Exception e){
                e.printStackTrace();
                Toast.makeText(FakeLocationService.this, "Stopped movement: " + e.getMessage(),
                               Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        private Location createLocation(final LatLng aCurrentPoint, final int aSpeed, final LatLng aNextPoint){
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(aCurrentPoint.latitude);
            location.setLongitude(aCurrentPoint.longitude);
            location.setAccuracy(0.0f);

            location.setSpeed(aSpeed);
            if(!aCurrentPoint.equals(aNextPoint)){
                location.setBearing(MapsHelper.bearing(aCurrentPoint, aNextPoint));
            }

            location.setTime(System.currentTimeMillis());
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }

            try{ // trick to initialize all last fields with default values
                if(mLocationJellyBeanFixMethod != null){
                    mLocationJellyBeanFixMethod.invoke(location);
                }
            } catch(Exception ignored){
            }
            return location;
        }

        public void stop(){
            if(mRouteCursor != null){
                mRouteCursor.close();
            }
            try{
                mDbHelper.close();
            } catch(Exception ignored){
            }
        }
    }
}
