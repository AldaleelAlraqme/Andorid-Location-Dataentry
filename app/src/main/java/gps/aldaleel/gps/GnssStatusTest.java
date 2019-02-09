package gps.aldaleel.gps;
/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
import android.app.Activity;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 * Unit tests for {@link GnssStatus}.
 */
public class GnssStatusTest extends Activity implements LocationListener {
    GnssStatus.Callback mGnssStatusCallback;
    LocationManager mLocationManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
        mGnssStatusCallback = new GnssStatus.Callback() {
            // TODO: add your code here!
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 30000, 0, this
        );
    }

    @Override
    protected void onStop() {
        mLocationManager.removeUpdates(this);
        mLocationManager.unregisterGnssStatusCallback(
                mGnssStatusCallback
        );
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status,
                                Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }


}