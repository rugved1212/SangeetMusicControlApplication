package com.example.serversidemusiccontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements MusicAdapter.OnItemClickListener {

    FloatingActionButton upload_btn;
    androidx.appcompat.widget.Toolbar toolbar;
    SearchView search;
    SwipeRefreshLayout refresh;

    private RecyclerView recyclerView;
    private MusicAdapter musicAdapter;
    private DatabaseReference databaseReference;
    private List<MusicFile> musicList;
    private MediaPlayer mediaPlayer;
    private static final int PICK_IMG_FILE_REQUEST_CODE = 2;
    Uri IMGuri;
    ImageView disImg;
    CardView cardView;
    TextView playingsong, playingArtist, playingMovie;
    ImageView playingImg, PlayPauseBtn;
    Space space;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        int nightModeFlag = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        upload_btn = findViewById(R.id.upload_btn);

        upload_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent upload_page = new Intent(MainActivity.this, Upload_Music.class);
                startActivity(upload_page);
            }
        });

        refresh = findViewById(R.id.refresh);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetch_data();
            }
        });

        recyclerView = findViewById(R.id.recycler_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicList = new ArrayList<>();
        musicAdapter = new MusicAdapter(musicList, this);
        recyclerView.setAdapter(musicAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("musics");
        DatabaseReference albumsRef = FirebaseDatabase.getInstance().getReference("albums");
        DatabaseReference playlistRef = FirebaseDatabase.getInstance().getReference("playlist");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot musicSnapshot : snapshot.getChildren()) {
                    String musicKey = musicSnapshot.getKey();
                    Map<String, Object> currentData = musicSnapshot.getValue(new GenericTypeIndicator<Map<String, Object>>() {});
                    if (currentData != null) {
                        String movieName = (String) currentData.get("movieName");

                        if (currentData.get("movieName") != null && !((String) currentData.get("movieName")).isEmpty()) {
                            DatabaseReference movieRef = albumsRef.child(movieName).child(musicKey);
                            movieRef.setValue(currentData);
                        }

                        String musicType = (String) currentData.get("musicType");

                        if (currentData.get("musicType") != null && !((String) currentData.get("musicType")).isEmpty()) {
                            DatabaseReference typeRef = playlistRef.child(musicType).child(musicKey);
                            typeRef.setValue(currentData);
                        }
//                        if (((String) currentData.get("movieName")).isEmpty()) {
//                            albumsRef.child(musicType).removeValue();
//                        }
                        albumsRef.child(musicType).removeValue();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        fetch_data();

        search = findViewById(R.id.search);

        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMusicList(newText);
                return true;
            }
        });

        cardView = findViewById(R.id.cardview);
        playingsong = findViewById(R.id.cardview_name);
        playingArtist = findViewById(R.id.cardview_artist);
        playingMovie = findViewById(R.id.cardview_movie);
        playingImg = findViewById(R.id.cardview_image);
        space = findViewById(R.id.space);

        if (mediaPlayer == null) {
            cardView.setVisibility(View.GONE);
            space.setVisibility(View.GONE);
        }

        PlayPauseBtn = findViewById(R.id.cardview_playbutton);
        PlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                        PlayPauseBtn.setImageResource(R.drawable.play);
                    } else {
                        mediaPlayer.start();
                        PlayPauseBtn.setImageResource(R.drawable.pause);
                    }
                }
            }
        });

    }

    private void filterMusicList(String newText) {
        List<MusicFile> filterList = new ArrayList<>();
        for (MusicFile musicFile : musicList) {
            if (musicFile.getMusic_name().toLowerCase().contains(newText.toLowerCase())) {
                filterList.add(musicFile);
            }
        }

        if (filterList.isEmpty()) {
            Toast.makeText(this, "NO DATA FOUND", Toast.LENGTH_SHORT).show();
        } else {
            musicAdapter.setFilteredList(filterList, this);
        }
    }

    private void fetch_data(){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                musicList.clear();
                for (DataSnapshot postSnapshot: snapshot.getChildren()) {
                    MusicFile musicFile = postSnapshot.getValue(MusicFile.class);
                    musicList.add(musicFile);
                }
                musicAdapter.notifyDataSetChanged();
                refresh.setRefreshing(false);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                refresh.setRefreshing(false);
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        MusicFile selectedMusic;// = musicList.get(position);
        if (musicAdapter.getFilteredmusicList() != null && !musicAdapter.getFilteredmusicList().isEmpty()) {
            selectedMusic = musicAdapter.getFilteredmusicList().get(position);
        } else {
            selectedMusic = musicList.get(position);
        }

        String musicURL = selectedMusic.getMusicURL();
        String musicName = selectedMusic.getMusic_name();
        String musicArtist = selectedMusic.getArtist_name();
        String musicMovie = selectedMusic.getMovieName();
        String musicImg = selectedMusic.getImgURL();

        cardView.setVisibility(View.VISIBLE);
        space.setVisibility(View.VISIBLE);
        playingsong.setText(musicName);
        playingArtist.setText("Artist: " + musicArtist);
        playingMovie.setText("Movie: " + musicMovie);
        PlayPauseBtn.setImageResource(R.drawable.pause);

        Glide.with(this)
                        .load(musicImg)
                                .placeholder(R.drawable.default_music_background1)
                                        .error(R.drawable.default_music_background1)
                                                .into(playingImg);


        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(musicURL);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Toast.makeText(this, "Unable to play music : " + e, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPopupMenuClick(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.music_popup_option);
        int edit = R.id.edit;
        int delete = R.id.delete;
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == edit) {
                showEditDialog(position);
                return true;
            } else if (itemId == delete) {
                deleteMusic(position);
                return true;
            } else {
                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMG_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            IMGuri = data.getData();
            if (IMGuri != null) {
                Glide.with(this)
                        .load(IMGuri)
                        .placeholder(R.drawable.default_music_background1)
                        .error(R.drawable.default_music_background1)
                        .into(disImg);
            }
        }
    }

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit");

        MusicFile select = musicList.get(position);
        View dialogView = getLayoutInflater().inflate(R.layout.alert_box, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.nameofmusic);
        EditText editArtist = dialogView.findViewById(R.id.nameofartist);
        EditText editMovie = dialogView.findViewById(R.id.nameofmovie);

        Button editImg = dialogView.findViewById(R.id.editButton);
        disImg = dialogView.findViewById(R.id.editImage);

        editName.setText(select.getMusic_name());
        editArtist.setText(select.getArtist_name());
        editMovie.setText(select.getMovieName());

        String imageuri = select.getImgURL();
        if (imageuri != null) {
            Glide.with(this)
                    .load(imageuri)
                    .placeholder(R.drawable.default_music_background1)
                    .error(R.drawable.default_music_background1)
                    .into(disImg);
        } else {
            Glide.with(this)
                    .load(R.drawable.default_music_background1)
                    .into(disImg);
        }

        editImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent open_img_storage = new Intent(Intent.ACTION_GET_CONTENT);
                open_img_storage.setType("image/*");
                startActivityForResult(open_img_storage, PICK_IMG_FILE_REQUEST_CODE);
            }
        });

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String newMusicName = editName.getText().toString();
            String newArtistName = editArtist.getText().toString();
            String newMovieName = editMovie.getText().toString();
            if (!TextUtils.isEmpty(newMusicName)) {
                if (disImg != null) {
                    editMusic(position, newMusicName, newArtistName, IMGuri, newMovieName);
                    IMGuri = null;
                } else {
                    Toast.makeText(MainActivity.this, "Image is not selected", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Please enter a new name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void editMusic(int position, String newMusicName, String newArtistName, Uri imguri, String newmovieName) {
        MusicFile selectedMusic = musicList.get(position);
        String musicId = selectedMusic.getMusic_ID();
        String oldMusicName = selectedMusic.getMusic_name();
        Uri oldImg = Uri.parse(selectedMusic.getImgURL());

        DatabaseReference musicRef = databaseReference.child(musicId);
        musicRef.child("music_name").setValue(newMusicName);
        musicRef.child("movieName").setValue(newmovieName);
        musicRef.child("artist_name").setValue(newArtistName);

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();

        if (oldImg != null) {
            StorageReference oldImgFileRef = storageReference.child("images/" + oldMusicName);
            oldImgFileRef.delete();
        }

        if (imguri != null) {
            StorageReference newimgFileRef = storageReference.child("images/" + newMusicName);
            UploadTask uploadImg = newimgFileRef.putFile(imguri);
            uploadImg.addOnSuccessListener(taskSnapshot -> {
                newimgFileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String img = uri.toString();
                    musicRef.child("imgURL").setValue(img).addOnCompleteListener(databaseTask -> {
                        if (databaseTask.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "ImgURL added to firebase database successfully ", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to add imgURL to database", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(MainActivity.this, "Failed to upload image: " + e, Toast.LENGTH_SHORT).show();
            });
        }

    }

    private void deleteMusic(int position) {
        MusicFile selectedMusic = musicList.get(position);
        String musicId = selectedMusic.getMusic_ID();
        String musicName = selectedMusic.getMusic_name();
        String storageFile = selectedMusic.getStorageFile();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference musicFileRef = storageReference.child("musics/" + storageFile);

        musicFileRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference musicRef = databaseReference.child(musicId);
                musicRef.removeValue().addOnCompleteListener(databasetask -> {
                    if (databasetask.isSuccessful()) {
                        Toast.makeText(MainActivity.this, musicName + " is Deleted !!", Toast.LENGTH_SHORT).show();
                        fetch_data();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to delete from database !!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "Failed to delete from storage !!", Toast.LENGTH_SHORT).show();
            }
        });
    }

}