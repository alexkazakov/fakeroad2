package mobi.droid.fakeroad;

import android.app.Application;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

/**
 * Created by max on 22.01.14.
 */
public class App extends Application{

    @Override
    public void onCreate(){
        super.onCreate();
    getOverflowMenu();
    }
    private void getOverflowMenu() {

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

