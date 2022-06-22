package com.example.therapyapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class EptRecyclerViewAdapter extends RecyclerView.Adapter<EptRecyclerViewAdapter.ViewHolder>{
    private final ArrayList<ItemSubgroup> subGroups;
    private final LayoutInflater mInflater;
    private static EptClickListener eptClickListener;

    // data is passed into the constructor
    EptRecyclerViewAdapter(Context context, ArrayList<ItemSubgroup> subGroups) {
        this.mInflater = LayoutInflater.from(context);
        this.subGroups = subGroups;

    }

    // inflates the row layout from xml when needed
    @Override
    public EptRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item_subgroup, parent, false);
        return new EptRecyclerViewAdapter.ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final ItemSubgroup itemList = subGroups.get(position);
        holder.tvSubGroupName.setText(itemList.getSubGroupName());
        holder.tvSubGroupValue.setText(itemList.getSubGroupValue());
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return subGroups.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView tvSubGroupName;
        public TextView tvSubGroupValue;
        public ViewHolder(View itemView) {
            super(itemView);
            tvSubGroupName = itemView.findViewById(R.id.tvSubGroupName);
            tvSubGroupValue = itemView.findViewById(R.id.tvSubGroupValue);
            itemView.setOnClickListener(this::onClick);
        }

        @Override
        public void onClick(View itemView) {

            eptClickListener.onEptClick(getAdapterPosition(),itemView);
        }
    }

    public void setOnEptClickListener(EptRecyclerViewAdapter.EptClickListener eptClickListener) {
        EptRecyclerViewAdapter.eptClickListener = eptClickListener;
    }

    public interface EptClickListener {
        void onEptClick(int position, View itemView);

    }
}
