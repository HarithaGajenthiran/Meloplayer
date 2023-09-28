package com.example.meloplayer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.example.meloplayer.Model.UploadSong;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";

    TextView textViewImage;
    ProgressBar progressBar;
    Uri audioUri;
    StorageReference mStorageref;
    StorageTask mUploadsTask;
    DatabaseReference referenceSongs;
    String songsCategory;
    MediaMetadataRetriever metadataRetriever;
    byte[] art;
    String title1, artist1, album_art1 = "", duration1;
    TextView title, artist, album, durations, dataa;
    ImageView album_art;

    UploadTask uploadTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewImage = findViewById(R.id.textViewSongFileSelected);
        progressBar = findViewById(R.id.progressbar);
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);
        durations = findViewById(R.id.duration);
        album = findViewById(R.id.album);
        dataa = (TextView) findViewById(R.id.data);
        album_art = findViewById(R.id.Imageview);

        metadataRetriever = new MediaMetadataRetriever();
        referenceSongs = FirebaseDatabase.getInstance().getReference().child("songs");
        mStorageref = FirebaseStorage.getInstance().getReference().child("songs");

        Spinner spinner = findViewById(R.id.spinner);

        spinner.setOnItemSelectedListener(this);


        List<String> categories = new ArrayList<>();

        categories.add("Pop");
        categories.add("Hip-hop");
        categories.add("Country");
        categories.add("Indie");
        categories.add("Jazz");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {


        songsCategory = adapterView.getItemAtPosition(position).toString();
        Toast.makeText(this, "Selected: " + songsCategory, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void openAudioFiles(View v) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("audio/*");
        startActivityForResult(i, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data.getData() != null) {

            audioUri = data.getData();
            metadataRetriever.setDataSource(this, audioUri);

            art = metadataRetriever.getEmbeddedPicture();
            Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            album_art.setImageBitmap(bitmap);
            album.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
            artist.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            dataa.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
            durations.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            title.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));

            artist1 = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            title1 = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            duration1 = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        }

    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {

            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {

                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = (columnIndex>=0)? cursor.getString(columnIndex):"Default Value";


                }
            } finally {
                cursor.close();
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public void uploadFileToFireBase(View v) {
        if (textViewImage.equals("No file Selected")) {
            Toast.makeText(this, "Please selected an image", Toast.LENGTH_SHORT).show();
        } else {
            if (mUploadsTask != null && mUploadsTask.isInProgress()) {
                Toast.makeText(this, "Songs uploads in already progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadFiles();
            }
        }
    }

    private void uploadFiles(){
        if (audioUri != null) {
            Toast.makeText(this, "Uploading please wait", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.VISIBLE);
            final StorageReference storageReference = mStorageref.child(System.currentTimeMillis() + "." + getfileextension(audioUri));
            mUploadsTask = storageReference.putFile(audioUri).addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    UploadSong uploadSong = new UploadSong(songsCategory, title1, artist1, album_art1, duration1, uri.toString());
                    String uploadId = referenceSongs.push().getKey();
                    referenceSongs.child(uploadId).setValue(uploadSong);
                }
            }));

            uploadTask = mStorageref.putFile(audioUri);
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0* taskSnapshot.getBytesTransferred()/taskSnapshot.getBytesTransferred());
                Log.d(TAG, "Upload is " + progress +"% done");
                Toast.makeText(this, "Song Uploaded", Toast.LENGTH_SHORT).show();
            });

        }
        else {
            Toast.makeText(this, "No file Selected to Upload", Toast.LENGTH_SHORT).show();
        }

    }

    private String getfileextension(Uri audioUri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(audioUri));
    }

    public void openAlbumUploadActivity(View v){
        Intent in = new Intent(MainActivity.this, UploadAlbumActivity.class);
        startActivity(in);
    }

}











