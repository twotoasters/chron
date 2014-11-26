package com.twotoasters.chron.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.twotoasters.chron.R;
import com.twotoasters.chron.adapter.ColorListAdapter.ItemViewHolder;

public class ColorListAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private final String[] mColors;

    public ColorListAdapter(String[] colors) {
        mColors = colors;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wearable_list_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.name.setText(mColors[position]);
    }

    @Override
    public int getItemCount() {
        return mColors.length;
    }

    public static class ItemViewHolder extends WearableListView.ViewHolder {
        public ImageView circle;
        public TextView name;
        public ItemViewHolder(View view) {
            super(view);
            circle = (ImageView) view.findViewById(R.id.circle);
            name = (TextView) view.findViewById(R.id.name);
        }
    }
}
