package de.androidcrypto.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EntryRVAdapter extends RecyclerView.Adapter<EntryRVAdapter.ViewHolder> {

    // variable for our array list and context
    private ArrayList<EntryModel> entryModelArrayList;
    private Context context;

    // constructor
    public EntryRVAdapter(ArrayList<EntryModel> entryModelArrayList, Context context) {
        this.entryModelArrayList = entryModelArrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // on below line we are inflating our layout
        // file for our recycler view items.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // on below line we are setting data
        // to our views of recycler view item.
        EntryModel model = entryModelArrayList.get(position);
        holder.entryNameTV.setText(model.getEntryName());
        holder.entryLoginNameTV.setText(model.getEntryLoginName());
        String entryFavourite = model.getEntryFavourite();
        //holder.entryFavouriteTV.setText(entryFavourite);
        if (entryFavourite.equals("1")) {
            holder.entryFavouriteIV.setImageResource(R.drawable.ic_baseline_star_rate_24);
        } else {
            holder.entryFavouriteIV.setImageResource(R.drawable.ic_baseline_star_outline_24);
        }
        String entryId = model.getEntryId();
        holder.entryCategoryTV.setText(model.getEntryCategory());

        // long click means copy the entryPassword
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // There are other constants available in HapticFeedbackConstants like VIRTUAL_KEY, KEYBOARD_TAP
                //HapticFeedbackConstants.CONFIRM
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                //v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);

                // copy to clipboard
                // Gets a handle to the clipboard service.
                ClipboardManager clipboard = (ClipboardManager)
                        context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", Cryptography.decryptStringAesGcmFromBase64(model.getEntryLoginPassword()));
                // Set the clipboard's primary clip.
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Passwort kopiert", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // below line is to add on click listener for our recycler view item.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // on below line we are calling an intent.
                Intent i = new Intent(context, UpdateEntryActivity.class);
                // below we are passing all our values.
                i.putExtra("entryName", model.getEntryName());
                i.putExtra("entryLoginName", model.getEntryLoginName());
                i.putExtra("entryLoginPassword", model.getEntryLoginPassword());
                i.putExtra("entryCategory", model.getEntryCategory());
                i.putExtra("entryFavourite", model.getEntryFavourite());
                i.putExtra("entryId", model.getEntryId());
                // starting our activity.
                context.startActivity(i);
            }
        });
    }

    @Override
    public int getItemCount() {
        // returning the size of our array list
        return entryModelArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        // creating variables for our text views.
        private TextView entryNameTV, entryLoginNameTV, entryFavouriteTV, entryCategoryTV, entryIdTV;
        private ImageView entryFavouriteIV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // initializing our text views
            entryNameTV = itemView.findViewById(R.id.idTVEntryName);
            entryLoginNameTV = itemView.findViewById(R.id.idTVEntryLoginName);
            entryFavouriteTV = itemView.findViewById(R.id.idTVEntryFavourite);
            entryFavouriteIV = itemView.findViewById(R.id.idIVEntryFavourite);
            entryFavouriteIV.setImageResource(R.drawable.ic_baseline_star_outline_24); // default
            entryCategoryTV = itemView.findViewById(R.id.idTVEntryCategory);
            entryIdTV = itemView.findViewById(R.id.idTVEntryId);
            // ausgefüllt app:srcCompat="@drawable/ic_baseline_star_rate_24" />
            // leer       app:srcCompat="@drawable/ic_baseline_star_outline_24" />

        }
    }
}

