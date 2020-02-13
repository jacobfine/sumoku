package com.example.table;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SaveAdapter extends RecyclerView.Adapter<SaveAdapter.SaveViewHolder> {

    private Context mContext;
    private Cursor mCursor;

    public SaveAdapter(Context context, Cursor cursor){
        mContext = context;
        mCursor = cursor;
    }

    public class SaveViewHolder extends RecyclerView.ViewHolder{
        public TextView savenameText;
        public TextView savedateText;
        public SaveViewHolder(@NonNull View itemView) {
            super(itemView);

            savenameText = itemView.findViewById(R.id.textview_save_name);
            savedateText = itemView.findViewById(R.id.textview_save_date);
        }
    }

    @NonNull
    @Override
    public SaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.save_item, parent, false);
        return new SaveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SaveViewHolder holder, int position) {
        if(!mCursor.move(position)){
            return;
        }

        String save_name = mCursor.getString(mCursor.getColumnIndex(DatabaseHelper.COL_1));
        String save_date = mCursor.getString(mCursor.getColumnIndex(DatabaseHelper.COL_3));

        holder.savenameText.setText(save_name);
        holder.savedateText.setText(save_date);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor){
        if (mCursor != null){
            mCursor.close();
        }

        mCursor = newCursor;

        if (newCursor != null) {
            notifyDataSetChanged();
        }
    }
}
