package edu.cs.dartmouth.inyourface;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import com.soundcloud.android.crop.Crop;

//For select from gallery
import android.app.AlertDialog;
import android.content.DialogInterface;

public class PhotoActivity extends AppCompatActivity {

    public static final String USER_DETAILS = "MyPrefs";
    private SharedPreferences myPrefs;
    private SharedPreferences.Editor myEditor;

    // Parameters for the camera function
    private static final int REQUEST_CODE_TAKE_FROM_CAMERA = 0;
    private static final int REQUEST_CODE_SELECT_GALLERY = 1;
    // an integer to identify the activity in onActivityResult() as it returns
    private static final String SAVED_URI = "saved_uri";        // prefix of unsaved URI

    // ------------ GLOBAL VARIABLES ----------------
    private Uri myImageCaptureUri;      // global image URI
    private ImageView mImageView;       // global image view
    private boolean takenFromCamera;
    private Uri imgUriAfterCropped;     // if user updated the URI, then save it as
    // the new destination so that loadImage() will
    // pull out the correct image (after cropped)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        mImageView = (ImageView)findViewById(R.id.photoTaken);

        // if the instance created is not empty
        if (savedInstanceState != null)
        {
            imgUriAfterCropped = savedInstanceState.getParcelable(SAVED_URI);

            if (imgUriAfterCropped == null)
                loadPicture();
            else
                mImageView.setImageURI(imgUriAfterCropped);
        }
        else {
            loadPicture();      // load the picture saved from the taken picture
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        // save the new image
        outState.putParcelable(SAVED_URI, imgUriAfterCropped);
    }

    /*
     * When the "Change" button gets clicked
     */
    public void cameraIntent(View v)
    {
        // ask to take a picture, and pass that as the intent
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // setup temporary image path to save the newly added image
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        // add to this path as the new image URI
        myImageCaptureUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, myImageCaptureUri);
        intent.putExtra("return-data", true);
        try
        {
            // getting a result from the activity: taking the picture
            startActivityForResult(intent, REQUEST_CODE_TAKE_FROM_CAMERA);
        }
        catch (ActivityNotFoundException e)
        {
            e.printStackTrace();
        }
        takenFromCamera = true;
    }

    /*============================ Select from Gallery ============================*/
    public void clickChangeButton(View view)
    {
        final String CAMERA_OPTION = "Open Camera";
        final String GALLERY_OPTION = "Select from Gallery";
        final String DIALOG_TITLE = "Pick Profile Picture";
        final String[] userOptions = {CAMERA_OPTION, GALLERY_OPTION};
        final View myView = view;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(DIALOG_TITLE);
        builder.setItems(userOptions, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int pos)
            {
                if (userOptions[pos].equals(CAMERA_OPTION))
                {
                    cameraIntent(myView);
                }
                else if (userOptions[pos].equals(GALLERY_OPTION))
                {
                    galleryIntent();
                }
            }
        });
        builder.show();
    }


    // Reference:
    // http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
    // http://stackoverflow.com/questions/5309190/android-pick-images-from-gallery
    public void galleryIntent()
    {
//        // all image files from gallery
//        Intent camImgIntent = new Intent();
//        camImgIntent.setType("image/*");              // only select the image files
//        camImgIntent.setAction(Intent.ACTION_GET_CONTENT);
//
//        // all image files from any apps
//        Intent allAppIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        allAppIntent.setType("image/*");
//
//        Intent allImgIntent = Intent.createChooser(getIntent(), "Select Picture");
//        allImgIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {allAppIntent});
//
//        // show the chooser
//        startActivityForResult(allImgIntent, REQUEST_CODE_SELECT_GALLERY);

        Intent allAppIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        allAppIntent.setType("image/*");

        startActivityForResult(Intent.createChooser(allAppIntent, "Select Picture"),
                REQUEST_CODE_SELECT_GALLERY);
    }
    /*============================ END Select from Gallery ============================*/



    /*
     * When activities get returnedd
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK)        // unsuccessful results
            return;

        if(requestCode == REQUEST_CODE_TAKE_FROM_CAMERA)
        { // go crop the image
            beginCrop(myImageCaptureUri);
        }
        // added for the select from gallery option
        else if (requestCode == REQUEST_CODE_SELECT_GALLERY && data != null && data.getData() != null)
        {
            Uri dataUri = data.getData();

            // Crop the image first
            beginCrop(dataUri);

            // Then show the (cropped) image
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), dataUri);
                ImageView imgView = (ImageView) findViewById(R.id.photoTaken);
                imgView.setImageBitmap(bitmap);
            }
            catch (IOException ex) {
                System.err.println("Error in selecting from gallery, cannot select the from gallery");
                ex.printStackTrace();
            }

        }
        else if(requestCode == Crop.REQUEST_CROP)
        {
            // Do the crop and update the image, based on the request code.
            picCropAndUpdated(resultCode, data);

            // delete the image, since it's been cropped.
            if(takenFromCamera)
            {
                // get the absolute path of the picture and delete it
                getContentResolver().delete(myImageCaptureUri,null,null);
            }
        }// end else if

    }

    // --------------- Self Defined Functions -----------------
    /*
     * Load the picture from where it is stored.
     */
    private void loadPicture()
    {
        try
        {   // open the the file where the photo is stored
            FileInputStream fis = openFileInput(getString(R.string.photo_name));
            Bitmap bmap = BitmapFactory.decodeStream(fis);

            mImageView.setImageBitmap(bmap);
            fis.close();
        }
        catch (IOException e)   // if there is no pictures saved
        {
            // use default pictures
            mImageView.setImageResource(R.drawable.default_pic);
        }
    }

    /*
     * Saved the picture to local cache.
     */
    private void savePicture()
    {
        // build and save into the pictures
        mImageView.buildDrawingCache();
        Bitmap bmap = mImageView.getDrawingCache();

        try
        {
            FileOutputStream fOutStream = openFileOutput(getString(R.string.photo_name), MODE_PRIVATE);
            bmap.compress(Bitmap.CompressFormat.PNG, 100, fOutStream);
            fOutStream.flush();
            fOutStream.close();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }


    /*
     * Since we library to crop the image, so this function is the implementation of
     * the cropping.
     */
    private void beginCrop(Uri sourceFilePath)
    {
        // get the destination of thwe cropped image
        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
        Crop.of(sourceFilePath, destination).asSquare().start(this);
        imgUriAfterCropped = destination;
    }


    /*
     * Crop the picture and update the original picture
     */
    private void picCropAndUpdated(int returnCode, Intent result)
    {
        if (returnCode == RESULT_OK)
        {
            mImageView.setImageURI(imgUriAfterCropped);
        }
    }

}
