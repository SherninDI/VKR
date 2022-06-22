package com.example.therapyapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class RecyclerViewSupport extends RecyclerView {

    private View emptyView;

    private RecyclerView.AdapterDataObserver emptyObserver = new RecyclerView.AdapterDataObserver() {

        @Override
        public void onChanged() {
            RecyclerView.Adapter<?> adapter = getAdapter();
            if (adapter != null && emptyView != null) {
                if (adapter.getItemCount() == 0) {
                    emptyView.setVisibility(View.VISIBLE);
                    RecyclerViewSupport.this.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    RecyclerViewSupport.this.setVisibility(View.VISIBLE);
                }
            }

        }
    };



    public RecyclerViewSupport(Context context) {
        super(context);
    }

    public RecyclerViewSupport(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewSupport(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(emptyObserver);
        }

        emptyObserver.onChanged();
    }

    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
    }

    public void startLoading() {
        // Hides the empty view.
        if (this.emptyView != null) {
            this.emptyView.setVisibility(GONE);
        }

    }

    public void endLoading() {

        // Forces the view refresh.
        emptyObserver.onChanged();
    }
}
