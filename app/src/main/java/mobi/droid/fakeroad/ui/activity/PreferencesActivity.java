package mobi.droid.fakeroad.ui.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import mobi.droid.fakeroad.R;

/**
 * @author ak
 */
public class PreferencesActivity extends PreferenceActivity{

    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_prefs);
    }
}