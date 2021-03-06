package com.hanson.geoclick;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.model.LatLng;
import com.hanson.geoclick.Helper.DBHelper;
import com.hanson.geoclick.Helper.ImageHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {
    //view data
    private static final String DATA_LAT = "Lat";
    private static final String DATA_LON = "lon";
    private static final String DATA_CITY = "city";
    private static final String DATA_COUNTRY = "country";
    private static final String DATA_PIC = "picture";
    private static final String DATA_ISLOADING = "isLoading";

    //helper
    ImageHelper imageHelper;
    Bitmap icon;

    //Variable of Camera
    private static final int PICK_FROM_CAMERA = 1;


    //Geo Location
    private LocationManager locationManager;
    private LocationListener locationListener;

    ImageView photoImageView;
    String lat = "";
    String lon = "";
    String country = "";
    String city = "";
    Button save;
    ProgressBar load;
    boolean isLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);


            imageHelper = new ImageHelper();

            //Show backbutton
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);

            //Set page title color
            Spannable text = new SpannableString(actionBar.getTitle());
            text.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            actionBar.setTitle(text);

            photoImageView = (ImageView) findViewById(R.id.imageView_takecam);
            save = (Button) findViewById(R.id.button_save);

            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    //Connect with databases
                    DBHelper dbHelper = new DBHelper(getBaseContext(), "Picture.db", null, 1);

                    if(photoImageView.getDrawable() != null && city != null && !city.isEmpty() && country != null && !country.isEmpty() &&
                            lat != null && !lat.isEmpty() && lon != null && !lon.isEmpty())
                    {
                        setIcon();
                        byte[] makeThumbnail = imageHelper.getByteArrayFromBitmap(icon);


                        dbHelper.Picture_Insert(country.toString(), city.toString(),
                                lat.toString(),lon.toString(), makeThumbnail, mCurrentPhotoPath);
                        galleryAddPic();
                        Toast.makeText(v.getContext(), "Saving picture completed", Toast.LENGTH_SHORT).show();
                    }


                    else
                    {
                        Toast.makeText(v.getContext(), "Sorry, we couldn't retrieve your location!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    finish();
                }
            });
            load = (ProgressBar) findViewById(R.id.progressBar_loaction);


        //For Geo Location
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                lat = String.valueOf(location.getLatitude());
                lon = String.valueOf(location.getLongitude());


                Geocoder geocoder;
                List<Address> addresses;

                //Get current Geo location
                geocoder = new Geocoder(getBaseContext(), Locale.getDefault());

                //location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                try{
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    if (!addresses.isEmpty()){
                        //Get city from address
                        //if (addresses.get(0).getLocality() != null)
                            city = addresses.get(0).getLocality();
                        if (city == null){
                            city = addresses.get(0).getSubLocality();
                        }

//                        else
//                            city.setText("Toronto");
                        //Get country from address
                        country = addresses.get(0).getCountryName();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isLoading = false;
                load.setVisibility(View.GONE);
                save.setEnabled(true);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };

        if (savedInstanceState == null)
        {
            doTakePhotoAction();
        }
        else {
            mCurrentPhotoPath = savedInstanceState.getString(DATA_PIC);
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
            photoImageView.setImageBitmap(bitmap);
            lat = savedInstanceState.getString(DATA_LAT);
            lon = savedInstanceState.getString(DATA_LON);
            city = savedInstanceState.getString(DATA_CITY);
            country = savedInstanceState.getString(DATA_COUNTRY);
            isLoading = savedInstanceState.getBoolean(DATA_ISLOADING);
            if (isLoading){
                load.setVisibility(View.VISIBLE);
                save.setEnabled(false);
            }
            else {
                load.setVisibility(View.GONE);
                save.setEnabled(true);
            }

        }


    }

    private void doTakePhotoAction() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }catch (IOException ex){

            }

            //Continue only if the file was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(
                        this,
                        "com.hanson.geoclick.android.fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, PICK_FROM_CAMERA);
            }
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //from camera
            if (requestCode == PICK_FROM_CAMERA) {

                //Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
                setPic(); //size control function

                //photoImageView.setImageBitmap(bitmap);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, 10);
                    }
                    return;
                }

                //When took a picture, just one time It need Geo location that is taken picture location.
                locationManager.requestSingleUpdate( "gps", locationListener, null );
            }

        }
        else if (resultCode == RESULT_CANCELED) {
            finish();
        }
    }


    //full screen capture
    String mCurrentPhotoPath;

    private File createImageFile() throws IOException{
        //Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_" ;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        //Save a file: path for us with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void setPic(){
        //Get the dimensions of the View
        int targetW = photoImageView.getWidth();//photoImageView.getDrawable().getIntrinsicWidth();
        int targetH = photoImageView.getHeight(); //.getDrawable().getIntrinsicHeight();


        //Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        //Determine how much to scale down the image
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
        			scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }
        //Decode the image file into a bitmapsized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        //try this!!!!
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        //Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
        photoImageView.setImageBitmap(bitmap);
    }

    private void setIcon(){
        //Get the dimensions of the View
        int targetW = 200;//photoImageView.getDrawable().getIntrinsicWidth();
        int targetH = 200; //.getDrawable().getIntrinsicHeight();


        //Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        //Determine how much to scale down the image
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }
        //Decode the image file into a bitmapsized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        icon = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DATA_LAT, lat);
        outState.putString(DATA_LON, lon);
        outState.putString(DATA_CITY, city);
        outState.putString(DATA_COUNTRY, country);
        outState.putString(DATA_PIC, mCurrentPhotoPath);
        outState.putBoolean(DATA_ISLOADING, isLoading);
    }


}

