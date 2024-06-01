package com.example.serversidemusiccontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.database.Cursor;
import android.provider.OpenableColumns;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;

public class Upload_Music extends AppCompatActivity {

    private static final int PICK_MUSIC_FILE_REQUEST_CODE = 1;
    private static final int PICK_IMG_FILE_REQUEST_CODE = 2;
    Button choose_music_file;
    Button choose_img_file;

    Button upload_button;

    EditText music_name;
    EditText artist_name;
    EditText movie_name;

    TextView songdetail;

    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private Uri musicuri;
    private Uri IMGuri;
    ImageView selectedImage;

    String[] type = {"Romantic", "Sad", "Rock", "Spiritual", "Chill", "Instrumental"};
    AutoCompleteTextView autoCompleteTextView;
    ArrayAdapter<String> arrayAdapter;
    String musictype;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_music);

        databaseReference = FirebaseDatabase.getInstance().getReference("musics");
        storageReference = FirebaseStorage.getInstance().getReference("musics");

        music_name = findViewById(R.id.music_name);
        artist_name = findViewById(R.id.artist_name);
        songdetail = findViewById(R.id._song_detail);
        movie_name = findViewById(R.id.movie_name);

        progressBar = findViewById(R.id.uploadprogressbar);

        choose_music_file = findViewById(R.id.choose_from_internal_storage);

        choose_music_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent open_storage = new Intent(Intent.ACTION_GET_CONTENT);
                open_storage.setType("audio/*");
                startActivityForResult(open_storage, PICK_MUSIC_FILE_REQUEST_CODE);
            }
        });

        choose_img_file = findViewById(R.id.choose_img_from_internal_storage);

        choose_img_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent open_img_storage = new Intent(Intent.ACTION_GET_CONTENT);
                open_img_storage.setType("image/*");
                startActivityForResult(open_img_storage, PICK_IMG_FILE_REQUEST_CODE);
            }
        });

        selectedImage = findViewById(R.id.image_display);


        autoCompleteTextView = findViewById(R.id.auto_complete_textview);
        arrayAdapter = new ArrayAdapter<String>(this,R.layout.type_selection ,type);

        autoCompleteTextView.setAdapter(arrayAdapter);
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                musictype = adapterView.getItemAtPosition(i).toString();
            }
        });

        upload_button = findViewById(R.id.upload_button);

        upload_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (musicuri != null && music_name!=null && musictype != null){
                    uploadMusicToFirebase(musicuri, IMGuri);
                    music_name.clearComposingText();
                    artist_name.clearComposingText();
                    autoCompleteTextView.clearListSelection();
                    musicuri = null;
                    IMGuri = null;
                } else {
                    Toast.makeText(Upload_Music.this, "Unsuccessfull", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_MUSIC_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            musicuri = data.getData();
            if (musicuri != null) {
                String filename = getFileName(musicuri);
                songdetail.setText(filename);
            }
        } else if (requestCode == PICK_IMG_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            IMGuri = data.getData();
            if (IMGuri != null) {
                selectedImage.setImageURI(IMGuri);
            }
        }
    }

    private void uploadMusicToFirebase(Uri musicURI, Uri IMGuri) {
        if (musicURI != null) {
            String musicName = music_name.getText().toString().trim();
            String artistName = artist_name.getText().toString().trim();
            String storageFile = music_name.getText().toString().trim();
            String movieName = movie_name.getText().toString().trim();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference reference = storage.getReference().child("musics/" + musicName);

            UploadTask uploadTask = reference.putFile(musicURI);
            progressBar.setVisibility(View.VISIBLE);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (IMGuri != null) {
                        StorageReference sr = storage.getReference().child("images/" + musicName);
                        UploadTask uploadTask1 = sr.putFile(IMGuri);
                        uploadTask1.addOnSuccessListener(taskSnapshot1 -> {
                            sr.getDownloadUrl().addOnSuccessListener(imguri -> {
                                String downloadURL = uri.toString();
                                String imageURL = imguri.toString();
                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("musics");
                                String key = databaseReference.push().getKey();
                                MusicFile musicFile = new MusicFile(key, musicName, artistName, downloadURL, musictype, storageFile, imageURL, movieName);
                                databaseReference.child(key).setValue(musicFile);
                                progressBar.setVisibility(View.GONE);
                            });
                        }).addOnFailureListener(exception -> {
                            Toast.makeText(this, "Failed to upload image due to: " + exception, Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        String downloadURL = uri.toString();
                        String imageURL = "https://firebasestorage.googleapis.com/v0/b/sangeet-234a2.appspot.com/o/images%2Fdefault_music_background1.jpg?alt=media&token=c03a2e9e-2c1b-4779-b477-80edc9d0e381";
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("musics");
                        String key = databaseReference.push().getKey();
                        MusicFile musicFile = new MusicFile(key, musicName, artistName, downloadURL, musictype, storageFile, imageURL, movieName);
                        databaseReference.child(key).setValue(musicFile);
                    }
                });
                Toast.makeText(Upload_Music.this, "Successfull", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Upload_Music.this, MainActivity.class);
                startActivity(intent);
                finish();
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to UPLOAD due to" + e, Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressBar.setProgress((int) progress);
                }
            });
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    // Retrieve the column index for DISPLAY_NAME
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        // Check if the column exists before retrieving its value
                        result = cursor.getString(nameIndex);
                    }
                }
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

}
