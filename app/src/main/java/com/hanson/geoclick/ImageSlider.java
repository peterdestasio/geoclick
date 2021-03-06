package com.hanson.geoclick;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hanson.geoclick.Adapters.CityGalleryAdapter;
import com.hanson.geoclick.Adapters.MyImageAdapter;
import com.hanson.geoclick.Helper.DBHelper;
import com.hanson.geoclick.Model.PictureItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

/*
This activity represent a slideshow of a list of pictures, it is called by CityGalleryActivity
It use a widget called ViewPager instantiated through a custom adapter
ViewPager associates each page with a key Object instead of working with Views directly.
This key is used to track and uniquely identify a given page independent of its position in the adapter.
 */

public class ImageSlider extends AppCompatActivity {

    ViewPager mViewPager;
    DBHelper dbHelper;
    MyImageAdapter adapterView;

    String choiseCity;
    ArrayList<PictureItem> PicList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_slider);


        //Show backbutton
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        //Set page title color
        Spannable text = new SpannableString(actionBar.getTitle());
        text.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        actionBar.setTitle(text);


        Intent intent = getIntent();
        int choise = intent.getIntExtra("idPic", 0);
        choiseCity = intent.getStringExtra("cityChosen");

        dbHelper = new DBHelper(this, "Picture.db", null, 1);
        PicList = dbHelper.selectPicFromCity(choiseCity);

        mViewPager = (ViewPager)findViewById(R.id.viewPageAndroid);
        adapterView = new MyImageAdapter(this);
        mViewPager.setAdapter(adapterView);


        //choose the current item inside the image slider
        mViewPager.setCurrentItem(choise);

        dbHelper.close();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.picture_side_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        /*
             Delete and Share menu buttons
        */
        //undestand if the arrow on the left is clicked
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        if (id == R.id.delete) {

            //Create an Alert Dialog to make a decision of delete or not a picture
            new AlertDialog.Builder(this).setMessage("Are you sure you want to delete this picture?")

                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            deletePic();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.cancel();
                        }
                    }).show();

            return true;
        }

        //share picture throug shareIntent
        if (id == R.id.share) {

            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);

            PictureItem selectPic = (PictureItem) PicList.get(mViewPager.getCurrentItem());
            String file = selectPic.get_mainImg();
            String geoShare = selectPic.get_country() + " " + selectPic.get_city() +" via Geoclick® "+ year;


            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(file)));
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    geoShare);
            startActivity(Intent.createChooser(shareIntent, "Share Image"));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //delete selected picture
    private void deletePic() {

        PictureItem selectPic = (PictureItem) PicList.get(mViewPager.getCurrentItem());


        dbHelper.deleteRow(selectPic.get_id());

        int pageIndex = mViewPager.getCurrentItem();

        // You might want to choose what page to display, if the current page was "defunctPage".
        adapterView = new MyImageAdapter(this);
        mViewPager.setAdapter(adapterView);

        if (mViewPager.getChildCount() == 0) {
            Toast.makeText(getApplicationContext(), "Doesn't have Picture in " + choiseCity, Toast.LENGTH_SHORT).show();
            finish();
        }
        else if(pageIndex == adapterView.getCount()){
            pageIndex--;
            mViewPager.setCurrentItem(pageIndex);
        }
        else {
            pageIndex++;
            mViewPager.setCurrentItem(pageIndex);
        }

    }

}
