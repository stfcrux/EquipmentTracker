package com.example.JinWei.imagetransferqr;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;

// take firebase reference from main activity
import static com.example.JinWei.imagetransferqr.MainActivity.mFirebaseDatabase;

public class UploadActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_SELECTION = 2;

    private BitmapConverter converter;

    private DatabaseReference mImageQrDatabaseReference;

    private ChildEventListener mChildEventListener;

    private String keyValue = "";
    private int qrSize = 0;
    private Bitmap qrImage;

    private TextView txtKey;
    private ImageView qrImageView;
    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        txtKey = (TextView) findViewById(R.id.txtKey);
        qrImageView = (ImageView) findViewById(R.id.qrImageView);
        layout = (LinearLayout) findViewById(R.id.linearLayout);

        mImageQrDatabaseReference = mFirebaseDatabase.getReference().child("qrMessage");

        converter = new BitmapConverter();

        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {


                txtKey.setText(keyValue);
                qrImage = encodeAsBitmap(keyValue, qrSize, qrSize);
                qrImageView.setImageBitmap(qrImage);


            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        };

        mImageQrDatabaseReference.addChildEventListener(mChildEventListener);

        showUploadOptions();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            qrSize = layout.getWidth() - 100;
            Bundle extras = data.getExtras();

            Bitmap imageBitmap = (Bitmap) extras.get("data");

            Image img = new Image(converter.bitmapToString(imageBitmap));
            mImageQrDatabaseReference = mFirebaseDatabase.getReference().child("qrMessage");

            keyValue = mImageQrDatabaseReference.push().getKey();
            mImageQrDatabaseReference.child(keyValue).setValue(img);
        }

        if (requestCode == REQUEST_IMAGE_SELECTION && resultCode == RESULT_OK){
            qrSize = layout.getWidth() - 100;
            Bundle extras = data.getExtras();
            Uri uri = data.getData();
            Bitmap imageBitmap = null;
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Image img = new Image(converter.bitmapToString(imageBitmap));
            mImageQrDatabaseReference = mFirebaseDatabase.getReference().child("qrMessage");

            keyValue = mImageQrDatabaseReference.push().getKey();
            mImageQrDatabaseReference.child(keyValue).setValue(img);

        }

    }

    private Bitmap encodeAsBitmap(String source, int width, int height) {
        BitMatrix result;

        try {
            result = new MultiFormatWriter().encode(source, BarcodeFormat.QR_CODE, width, height, null);
        } catch (IllegalArgumentException | WriterException e) {
            // Unsupported format
            return null;
        }

        final int w = result.getWidth();
        final int h = result.getHeight();
        final int[] pixels = new int[w * h];

        for (int y = 0; y < h; y++) {
            final int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, w, h);

        return bitmap;
    }

    public void saveCodeClick(View view) {
        MediaStore.Images.Media.insertImage(getContentResolver(), qrImage, keyValue , "Generated by ImageTransfer QR");  // Saves the image.
        Toast.makeText(this, "Image saved to camera roll", Toast.LENGTH_SHORT).show();
    }

    public void showUploadOptions(){
        String[] saveOptions = {"Take a Photo", "Select Photo from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select where to save the photo");
        builder.setItems(saveOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int selected) {

                if (selected == 0){
                    Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePhotoIntent.resolveActivity(getPackageManager()) != null){
                        startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
                if (selected == 1){
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, REQUEST_IMAGE_SELECTION);
                }

            }
        }).setCancelable(false);
        builder.show();
    }

}
