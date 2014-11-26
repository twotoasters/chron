package com.twotoasters.chron.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.twotoasters.chron.R;

public class HeaderFooterListAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_FOOTER = 1;
    private static final int VIEW_TYPE_LIST_ITEM = 2;

    private RecyclerView.Adapter wrappedAdapter;
    private CharSequence headerText;

    public HeaderFooterListAdapter(@NonNull RecyclerView.Adapter wrappedAdapter, CharSequence headerText) {
        this.wrappedAdapter = wrappedAdapter;
        this.headerText = headerText;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: // fall through
            case VIEW_TYPE_FOOTER:
                return new HeaderFooterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.wearable_list_header_footer, parent, false));
            case VIEW_TYPE_LIST_ITEM:
                return wrappedAdapter.onCreateViewHolder(parent, viewType);
        }
        throw new IllegalArgumentException("Invalid wearable list view type");
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                ((HeaderFooterViewHolder) holder).text.setText(headerText);
                break;
            case VIEW_TYPE_FOOTER:
                ((HeaderFooterViewHolder) holder).text.setText(null);
                break;
            case VIEW_TYPE_LIST_ITEM:
                wrappedAdapter.onBindViewHolder(holder, position - 1); // offset position due to header
                break;
            default:
                throw new IllegalArgumentException("Invalid wearable list view type");
        }
    }

    @Override
    public int getItemCount() {
        return wrappedAdapter.getItemCount() + 2; // adjust for header and footer
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else {
            int wrappedCount = wrappedAdapter.getItemCount();
            if (position > wrappedCount) {
                return VIEW_TYPE_FOOTER;
            } else {
                return VIEW_TYPE_LIST_ITEM;
            }
        }
    }

    public void setHeaderText(CharSequence headerText) {
        this.headerText = headerText;
    }

    static class HeaderFooterViewHolder extends WearableListView.ViewHolder {
        TextView text;
        public HeaderFooterViewHolder(View view) {
            super(view);
            text = (TextView) view.findViewById(R.id.text);
        }
    }
}
