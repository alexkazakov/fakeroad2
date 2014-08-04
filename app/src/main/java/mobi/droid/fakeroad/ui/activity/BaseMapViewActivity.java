package mobi.droid.fakeroad.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;

/**
 * @author ak
 */
public abstract class BaseMapViewActivity extends Activity{

    protected MapView mMapView;
    protected GoogleMap mMap;

    private void assignViews(){
        mMapView = (MapView) findViewById(R.id.mapView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assignViews();

        configureMap(savedInstanceState);
    }

    protected void configureMap(Bundle savedInstanceState){
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();//needed to get the map to display immediately

        //todo Проверка на актуальность Google Play
        MapsInitializer.initialize(this);
        mMap = mMapView.getMap();
        mMap.setMyLocationEnabled(true);
        mMap.setTrafficEnabled(false);
//            mMap.animateCamera(
//                    CameraUpdateFactory.zoomBy(12f));
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);

    }

    protected abstract void onAddMarker(LatLng aLatLng);

    @Override
    public void onDestroy(){
        if(mMapView != null){
            mMapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        if(mMapView != null){
            mMapView.onLowMemory();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mMapView != null){
            mMapView.onResume();
        }
        MapsHelper.checkIfMockEnabled(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mMapView != null){
            mMapView.onPause();
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState){
        super.onSaveInstanceState(outState);
        if(mMapView != null){
            mMapView.onSaveInstanceState(outState);
        }
    }
}
