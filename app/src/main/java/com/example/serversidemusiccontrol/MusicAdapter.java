package com.example.serversidemusiccontrol;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private List<MusicFile> musicList;
    private List<MusicFile> filteredmusicList;
    private OnItemClickListener listener;


    public  interface OnItemClickListener {
        void onItemClick(int position);
        void onPopupMenuClick(View view, int position);
    }

    public MusicAdapter(List<MusicFile> musicList, OnItemClickListener listener) {
        this.musicList = musicList;
        this.listener = listener;
    }

    public void setFilteredList(List<MusicFile> filterList, OnItemClickListener listener) {
        this.musicList = filterList;
        this.filteredmusicList = filterList;
        this.listener = listener;
        notifyDataSetChanged();
    }

    public List<MusicFile> getFilteredmusicList() {
        return filteredmusicList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicFile musicFile = musicList.get(position);
        holder.musicName.setText(musicFile.getMusic_name());

        Glide.with(holder.itemView.getContext())
                        .load(musicFile.getImgURL())
                                .placeholder(R.drawable.default_music_background1)
                                        .error(R.drawable.default_music_background1)
                                                .into(holder.img);


        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });

        holder.popUpOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPopupMenuClick(holder.popUpOption, adapterPosition);
                }
            }
        });
        holder.itemView.setTag(musicFile.getMusic_ID());
    }


    @Override
    public int getItemCount() {
        return musicList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView img;
        TextView musicName;
        TextView popUpOption;
        LottieAnimationView playPauseAnim;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            musicName = itemView.findViewById(R.id.music_name_firebase);
            popUpOption = itemView.findViewById(R.id.pop_up_option);
            playPauseAnim = itemView.findViewById(R.id.play_pause_music);
            img = itemView.findViewById(R.id.music_image);
        }
    }
}
