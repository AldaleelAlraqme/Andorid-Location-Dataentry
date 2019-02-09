package gps.aldaleel.gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import gps.aldaleel.gps.BuildConfig;
import gps.aldaleel.gps.R;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = gps.aldaleel.gps.MainActivity.class.getSimpleName();
    // location last updated time
    private String mLastUpdateTime;

    // location updates interval - 10sec
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    private static final int REQUEST_CHECK_SETTINGS = 100;

    @BindView(R.id.btn_addschool)
    Button btnAddschool;

    @BindView(R.id.btn_data)
    Button btnData;

    // bunch of location related apis
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;

    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aldaleel_main);
        ButterKnife.bind(this);

        // initialize the necessary libraries
        init();

        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState);
    }


    private void init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                updateLocationUI();
            }
        };

        mRequestingLocationUpdates = false;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void updateLocationUI() {
        if (mCurrentLocation != null) {
//            txtLocationResult.setText(
//                    "Lat: " + mCurrentLocation.getLatitude() + ", " +
//                            "Lng: " + mCurrentLocation.getLongitude()
//            );
//
//            // giving a blink animation on TextView
//            txtLocationResult.setAlpha(0);
//            txtLocationResult.animate().alpha(1).setDuration(300);
//
//            // location last updated time
//            txtUpdatedOn.setText("Last updated on: " + mLastUpdateTime);
        }

//        toggleButtons();
    }

    private void restoreValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates")) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
            }

            if (savedInstanceState.containsKey("last_known_location")) {
                mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
            }

            if (savedInstanceState.containsKey("last_updated_on")) {
                mLastUpdateTime = savedInstanceState.getString("last_updated_on");
            }
        }

        updateLocationUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
        outState.putParcelable("last_known_location", mCurrentLocation);
        outState.putString("last_updated_on", mLastUpdateTime);

    }

    public void startLocationButtonClick() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mRequestingLocationUpdates = true;
                        startLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            // open device settings when the permission is
                            // denied permanently
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private void startLocationUpdates() {
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        Toast.makeText(getApplicationContext(), "Started location updates!", Toast.LENGTH_SHORT).show();

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(gps.aldaleel.gps.MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(gps.aldaleel.gps.MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        updateLocationUI();
                    }
                });
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @OnClick(R.id.btn_addschool)
    public void switchToForm(){
        Intent openThree = new Intent(getApplicationContext(),FormActivity.class);
        startActivity(openThree);
    }

    @OnClick(R.id.btn_data)
    public void switchToData(){
        Intent openThree2 = new Intent(getApplicationContext(),dbDataActivity.class);
        startActivity(openThree2);
    }

    @OnClick(R.id.btn_reset)
    public void resetDB(){
//        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//        conn.setDoOutput(true);
//        conn.setUseCaches(false);
//        conn.setFixedLengthStreamingMode(bytes.length);
//        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Content-Type",
//                "application/x-www-forurlencoded;charset=UTF-8");
//        // post the request
//        OutputStream out = conn.getOutputStream();
//
//        out.write(bytes);
    }

//    private InputStream getInputStream(String urlStr, String user, String password) throws IOException, KeyManagementException, NoSuchAlgorithmException {
//        URL url = new URL(urlStr);
//        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
//
//        // Create the SSL connection
//        SSLContext sc;
//        sc = SSLContext.getInstance("TLS");
//        sc.init(null, null, new java.security.SecureRandom());
//        conn.setSSLSocketFactory(sc.getSocketFactory());
//
//        // Use this if you need SSL authentication
//        String userpass = user + ":" + password;
//        String basicAuth = "Basic " + Base64.encodeToString(userpass.getBytes(), Base64.DEFAULT);
//        conn.setRequestProperty("Authorization", basicAuth);
//
//        // set Timeout and method
//        conn.setReadTimeout(7000);
//        conn.setConnectTimeout(7000);
//        conn.setRequestMethod("POST");
//        conn.setDoInput(true);
//
//        // Add any data you wish to post here
//
//        conn.connect();
//        Toast.makeText(getApplicationContext(),"status: fter cnct",Toast.LENGTH_LONG).show();
//        return conn.getInputStream();
//    }

    @OnClick(R.id.btn_upload)
    public void uploadDB(){
        try{
            final JSONArray result = getResults();
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(this);
            String url ="https://sofian.tru.io/gps2.php";//askfor url nigga

// Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                            //mTextView.setText("Response is: "+ response.substring(0,500));
                            Toast.makeText(getApplicationContext(),"Response is: "+ response,Toast.LENGTH_LONG).show();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //mTextView.setText("That didn't work!");
                    Toast.makeText(getApplicationContext(),"status: err",Toast.LENGTH_LONG).show();
                }
            })
            {
                @Override
                protected Map<String, String> getParams()
                {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("json", result.toString());
                    return params;
                }
            };

// Add the request to the RequestQueue.
            queue.add(stringRequest);
//            Dexter.withActivity(this)
//                    .withPermission(Manifest.permission.INTERNET)
//                    .withListener(new PermissionListener() {
//                        @Override
//                        public void onPermissionGranted(PermissionGrantedResponse response) {
//
//                            String stttr = "https://sofian.tru.io/gps2.php";
//                            String reso = new String();
//                            InputStream is = null;
//                            try {
//                                is = getInputStream(stttr, "hjkj", "dfgdf");
//                                Toast.makeText(getApplicationContext(),"status: ww",Toast.LENGTH_LONG).show();
//                                BufferedReader in = new BufferedReader(new InputStreamReader(is));
//                                String inputLine;
//                                while ((inputLine = in.readLine()) != null) {
//                                    reso += inputLine;
//                                }
//                                Toast.makeText(getApplicationContext(),"status: after close",Toast.LENGTH_LONG).show();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            } catch (KeyManagementException e) {
//                                e.printStackTrace();
//                            } catch (NoSuchAlgorithmException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void onPermissionDenied(PermissionDeniedResponse response) {
//                            if (response.isPermanentlyDenied()) {
//                                // open device settings when the permission is
//                                // denied permanently
//                                openSettings();
//                            }
//                        }
//
//                        @Override
//                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
//                            token.continuePermissionRequest();
//                        }
//                    }).check();

//            URL url = new URL(stttr);
//            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
//
//            // Create the SSL connection
//            SSLContext sc;
//            sc = SSLContext.getInstance("TLS");
//            sc.init(null, null, new java.security.SecureRandom());
//            conn.setSSLSocketFactory(sc.getSocketFactory());
////            Toast.makeText(getApplicationContext(),"status: after close",Toast.LENGTH_LONG).show();
//            // Use this if you need SSL authentication
//            String userpass = "hh" + ":" + "ll";
//            String basicAuth = "Basic " + Base64.encodeToString(userpass.getBytes(), Base64.DEFAULT);
//            conn.setRequestProperty("Authorization", basicAuth);
//
//            // set Timeout and method
//            conn.setReadTimeout(7000);
//            conn.setConnectTimeout(7000);
//            conn.setRequestMethod("POST");
//            conn.setDoInput(true);
//
//            // Add any data you wish to post here
//
//            conn.connect();
//            String reso =  conn.getInputStream().toString();
//            Toast.makeText(getApplicationContext(),"status: after close"+reso,Toast.LENGTH_LONG).show();
//            URL url = new URL("https://sofian.tru.io/gps2.php");
//            Map<String,Object> params = new LinkedHashMap<>();
//            params.put("name", "Freddie the Fish");
//            params.put("email", "fishie@seamail.example.com");
//            params.put("reply_to_thread", 10394);
//            params.put("message", "Shark attacks in Botany Bay have gotten out of control. We need more defensive dolphins to protect the schools here, but Mayor Porpoise is too busy stuffing his snout with lobsters. He's so shellfish.");
//
//            StringBuilder postData = new StringBuilder();
//            for (Map.Entry<String,Object> param : params.entrySet()) {
//                if (postData.length() != 0) postData.append('&');
//                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
//                postData.append('=');
//                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
//            }
//            byte[] postDataBytes = postData.toString().getBytes("UTF-8");
//
//            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
//            conn.setDoOutput(true);
//            conn.getOutputStream().write(postDataBytes);
//            Toast.makeText(getApplicationContext(),"status: after close",Toast.LENGTH_LONG).show();
//            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
//
//            for (int c; (c = in.read()) >= 0;)
//                System.out.print((char)c);
//            String otherParametersUrServiceNeed =  "Company=acompany&Lng=test&MainPeriod=test&UserID=123&CourseDate=8:10:10";
//            String request = "http://sofian.tru.io/gps2.php";
//
//            URL url = new URL(request);
//            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
//            connection.setDoOutput(true);
//            connection.setDoInput(true);
//            connection.setInstanceFollowRedirects(true);
//            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            connection.setRequestProperty("charset", "utf-8");
//            connection.setUseCaches (false);
//
//            DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
//            wr.writeBytes(otherParametersUrServiceNeed);
//            Toast.makeText(getApplicationContext(),"status: after close",Toast.LENGTH_LONG).show();
////            JSONObject jsonParam = new JSONObject();
////            jsonParam.put("ID", "25");
////            jsonParam.put("description", "Real");
////            jsonParam.put("enable", "true");
//
//            wr.writeBytes(result.toString());
//
//            wr.flush();
//            wr.close();
//            URL url;
//            URLConnection urlConn;
//            DataOutputStream printout;
//            DataInputStream input;
//            url = new URL ("env.tcgi");
//            urlConn = url.openConnection();
//            urlConn.setDoInput (true);
//            urlConn.setDoOutput (true);
//            urlConn.setUseCaches (false);
//            urlConn.setRequestProperty("Content-Type","application/json");
//            urlConn.setRequestProperty("Host", "https://sofian.tru.io/gps2.php");
//            urlConn.connect();
////Create JSONObject here result
//            printout = new DataOutputStream(urlConn.getOutputStream ());
//            Toast.makeText(getApplicationContext(),"status: after close",Toast.LENGTH_LONG).show();
//            printout.writeBytes(URLEncoder.encode(result.toString(),"UTF-8"));
//            printout.flush ();
//            printout.close ();

//            String url = "https://sofian.tru.io/gps2.php";
//            URL obj = new URL(url);
//            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
//            //add reuqest header
//            con.setRequestMethod("POST");
//
//            // Send post request
//            con.setDoOutput(true);
//
//            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
//            Toast.makeText(getApplicationContext(),"status: after close",Toast.LENGTH_LONG).show();
//            byte[] buf = ("json=" + URLEncoder.encode(result.toString(),"UTF-8")).getBytes("UTF-8");
//            wr.write(buf);
////            wr.writeBytes(URLEncoder.encode(result.toString(),"UTF-8"));
//            wr.flush();
//            wr.close();
//
//            int responseCode = con.getResponseCode();
//            //Log.e("responseCode",responseCode+"::");
////            if(responseCode==200){
//                Toast.makeText(getApplicationContext(),"status: "+responseCode,Toast.LENGTH_LONG).show();
////            }
//
//            BufferedReader in = new BufferedReader(
//                    new InputStreamReader(con.getInputStream()));
//            String inputLine;
//            StringBuffer response = new StringBuffer();
//
//            while ((inputLine = in.readLine()) != null) {
//                response.append(inputLine);
//            }
//            in.close();
//
//            //print result
////            Log.e("RESPONSE",response.toString());
//            if(response!=null){
//                Toast.makeText(getApplicationContext(),response.toString(),Toast.LENGTH_LONG).show();
//            }
//            else{
//                Toast.makeText(getApplicationContext(),"null",Toast.LENGTH_LONG).show();
//            }
        }
        catch(Exception exp){
            //Log.e("ERROR",exp.getMessage());
//            Toast.makeText(getApplicationContext(),exp.getMessage()+" :err",Toast.LENGTH_LONG).show();
        }

    }

    private JSONArray getResults()
    {
        SQLiteDatabase mydatabase = openOrCreateDatabase("aldaleel",MODE_PRIVATE,null);
        final String sqlGet = "SELECT * FROM `Schools`";

        Cursor cursor = mydatabase.rawQuery(sqlGet,null);

        JSONArray resultSet = new JSONArray();

        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {

            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();

            for( int i=0 ;  i< totalColumn ; i++ )
            {
                if( cursor.getColumnName(i) != null )
                {
                    try
                    {
                        if( cursor.getString(i) != null )
                        {
                            Log.d("TAG_NAME", cursor.getString(i) );
                            rowObject.put(cursor.getColumnName(i) ,  cursor.getString(i) );
                        }
                        else
                        {
                            rowObject.put( cursor.getColumnName(i) ,  "" );
                        }
                    }
                    catch( Exception e )
                    {
                        Log.d("TAG_NAME", e.getMessage()  );
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }
        cursor.close();
        mydatabase.close();
        Log.d("TAG_NAME", resultSet.toString() );
        return resultSet;
    }
}
