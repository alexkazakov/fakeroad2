package mobi.droid.fakeroad.location;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.provider.Settings;
import android.util.Pair;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.service.LocationDbHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MapsHelper{

    public static final String TAG = "mobi.droid.fakeroad";

    /**
     * Calculate the bearing between two points.
     */
    public static float bearing(LatLng p1, LatLng p2){
        float[] result = new float[2];
        Location.distanceBetween(p1.latitude, p1.longitude,
                                 p2.latitude, p2.longitude, result);
        return result[1];
    }

    /**
     * Calculate the distance between two points.
     */
    public static float distance(LatLng p1, LatLng p2){
        float[] result = new float[1];
        Location.distanceBetween(p1.latitude, p1.longitude,
                                 p2.latitude, p2.longitude, result);
        return result[0];
    }

    /**
     * Calculate the distance between all points in a line. ie: calculate the
     * distance between each successive point and sum them.
     */
    public static double distance(Collection<LatLng> locations){
        double result = 0;
        Iterator<LatLng> iterator = locations.iterator();
        if(iterator.hasNext()){
            LatLng p1 = iterator.next();
            while(iterator.hasNext()){
                LatLng p2 = iterator.next();
                result += distance(p1, p2);
                if(iterator.hasNext()){
                    p1 = p2;
                }
            }
        }
        return result;
    }

    /**
     * Calculate next point.
     *
     * @param aLastPoint 'first' contains user selected point, 'second' contains calculated point at distance from previous lastpoint's 'first'.
     * @param aSourcePoints user selected points.
     * @param aDistance distance to travel from lastPoint.second position.
     * @return 'first' contains nearest user selected point, 'second' contains calculated point at distance from previous lastpoint's 'first'.
     */
    public static Pair<LatLng, LatLng> nextLatLng(Pair<LatLng, LatLng> aLastPoint, final List<LatLng> aSourcePoints,
                                                  final int aDistance){
        int totalDistance = aDistance;
        int startIndex = -1;
        for(LatLng l : aSourcePoints){
            startIndex++;
            if(l.equals(aLastPoint.first)){
                break;
            }
        }
        boolean noNextPoint = aLastPoint.second.equals(aSourcePoints.get(aSourcePoints.size() - 1));
        if(noNextPoint){
            return aLastPoint;
        }
        for(int i = startIndex; i < aSourcePoints.size() - 1; i++){
            LatLng p1;
            if(i == startIndex){
                p1 = aLastPoint.second;
            } else{
                p1 = aSourcePoints.get(i);
            }
            LatLng p2 = aSourcePoints.get(i + 1);

            double distance = distance(p1, p2);
            if(distance < totalDistance){
                totalDistance -= distance; // skip to next points
            } else{
                LatLng point = calcLngLat(p1, totalDistance, MapsHelper.bearing(p1, p2));
                return Pair.create(aSourcePoints.get(i), point);
            }
        }
        LatLng sourcePoint = aSourcePoints.get(aSourcePoints.size() - 1);
        return Pair.create(sourcePoint, sourcePoint);
    }

    public static Pair<LatLng, LatLng> nextLatLng(Pair<LatLng, LatLng> aLastPoint,
                                                  final Cursor aPoints,
                                                  LatLng aFinalPoint,
                                                  LocationDbHelper aHelper,
                                                  final int aDistance){

        if(aFinalPoint.equals(aLastPoint.second)){
            return aLastPoint;
        }

        int totalDistance = aDistance;
        if(!(aPoints.isLast() || aPoints.isAfterLast())){
            LatLng p1 = null;
            do{
                if(p1 == null){
                    p1 = aLastPoint.second;
                } else{
                    p1 = aHelper.readLatLng(aPoints);
                }
                if( aPoints.moveToNext() ){
                    LatLng p2 = aHelper.readLatLng(aPoints);
                    float distance = distance(p1, p2);

                    aPoints.moveToPrevious();

                    if(distance < totalDistance){
                        totalDistance -= distance; // skip to next points
                    } else{
                        LatLng point = calcLngLat(p1, totalDistance, MapsHelper.bearing(p1, p2));
//                        distance = distance(p1, point);
//                        Log.v(TAG, "p1->p2 distance = " + distance);
                        return Pair.create(p1, point);
                    }
                }
            } while(aPoints.moveToNext());
        }
        return Pair.create(aFinalPoint, aFinalPoint);
    }


    public static LatLng calcLngLat(final LatLng aStart, double distance, final float bearing){
        if(distance == 0){
            return new LatLng(aStart.latitude, aStart.longitude);
        }
        double R = 6378100; // meters , earth Radius approx
        double PI = 3.1415926535;
        double RADIANS = PI / 180;
        double DEGREES = 180 / PI;

        double lat2;
        double lon2;

        double lat1 = aStart.latitude * RADIANS;
        double lon1 = aStart.longitude * RADIANS;
        double radbear = bearing * RADIANS;

        lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / R) +
                                 Math.cos(lat1) * Math.sin(distance / R) * Math.cos(radbear));
        lon2 = lon1 + Math.atan2(Math.sin(radbear) * Math.sin(distance / R) * Math.cos(lat1),
                                 Math.cos(distance / R) - Math.sin(lat1) * Math.sin(lat2));

        return new LatLng(lat2 * DEGREES, lon2 * DEGREES);
    }

    public static void calculateRoute(final Routing.TravelMode aDriving, final LatLng aStart, final LatLng aEnd,
                                      final RoutingListener aRoutingListener){
        Routing routing = new Routing(aDriving);
        routing.registerListener(aRoutingListener);
        routing.execute(aStart, aEnd);
    }

    public static void checkIfMockEnabled(final Activity aContext){
        try{
            int mock_location = Settings.Secure.getInt(aContext.getContentResolver(), "mock_location");
            if(mock_location == 0){
                try{
                    Settings.Secure.putInt(aContext.getContentResolver(), "mock_location", 1);
                } catch(Exception ignored){
                }
                mock_location = Settings.Secure.getInt(aContext.getContentResolver(), "mock_location");
            }

            if(mock_location == 0){
                AlertDialog.Builder ab = new AlertDialog.Builder(aContext);
                ab.setCancelable(false);
                ab.setMessage("Enable 'Mock locations' to use this application");
                ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(final DialogInterface dialog, final int which){
                        aContext.startActivity(new Intent().setClassName("com.android.settings",
                                                                         "com.android.settings.DevelopmentSettings"));
                    }
                });
                ab.show();
            }
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }


/*
    public static LinkedList<LatLng> getPathPointsForTime(long aTime, final List<LatLng> aPoints){
        double fullPathMeters = distance(aPoints);
        int speed = (int) (fullPathMeters / aTime);
        return getPathPointsForSpeed(speed, aPoints);
    }

    public static LinkedList<LatLng> getPathPointsForSpeed(final int aSpeedMetersPerSecond, final List<LatLng> aPoints){
        double fullPathMeters = distance(aPoints);
        if(aSpeedMetersPerSecond < 1){
            throw new IllegalArgumentException("Speed must be > 1 m/s");
        }

        int pointCount = (int) (fullPathMeters / aSpeedMetersPerSecond) + 1;
        if(pointCount < 2){
            pointCount = 2;
        }

        LinkedList<LatLng> points = new LinkedList<LatLng>();

        addPoint(points, aPoints.get(0));
        if(pointCount == 2){
            addPoint(points, aPoints.get(aPoints.size() - 1));
        } else{
            Pair<LatLng, LatLng> lastPoint = Pair.create(aPoints.get(0), aPoints.get(0));
            while(lastPoint.first != aPoints.get(aPoints.size() - 1)){
                lastPoint = nextLatLng(lastPoint, points, aPoints, aSpeedMetersPerSecond);
            }
        }
        return points;
    }

    private static void addPoint(final LinkedList<LatLng> aPoints, final LatLng aPoint){
        aPoints.addLast(aPoint);
        Log.v(TAG, "Added [" + aPoints.size() + "] location: " + aPoint);
    }
        private static Pair<LatLng, LatLng> nextLatLng(final Pair<LatLng, LatLng> aLastPoint,
                                                   final LinkedList<LatLng> aResultPoints,
                                                   final List<LatLng> aSourcePoints,
                                                   final int aDistance){
        int totalDistance = aDistance;
        int startIndex = -1;
        for(LatLng l : aSourcePoints){
            startIndex++;
            if(l.equals(aLastPoint.first)){
                break;
            }
        }
        for(int i = startIndex; i < aSourcePoints.size() - 1; i++){
            LatLng p1;
            if(i == startIndex){
                p1 = aLastPoint.second;
            } else{
                p1 = aSourcePoints.get(i);
            }
            LatLng p2 = aSourcePoints.get(i + 1);

            double distance = distance(p1, p2);
            if(distance < totalDistance){
                totalDistance -= distance; // skip to next points
            } else{
                LatLng point = calcLngLat(p1, totalDistance, MapsHelper.bearing(p1, p2));
                addPoint(aResultPoints, point);
                return Pair.create(aSourcePoints.get(i), point);
            }
        }
        LatLng sourcePoint = aSourcePoints.get(aSourcePoints.size() - 1);
        addPoint(aResultPoints, sourcePoint);
        return Pair.create(sourcePoint, sourcePoint);
    }

*/

}
