package com.example.therapyapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class RecyclerViewSupport extends RecyclerView {
    /**
     * The view to show if the list is empty.
     */
    private View emptyView;

    /**
     * Observer for list data. Sets the empty view if the list is empty.
     */
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



    /**
     * @see RecyclerView#RecyclerView(Context)
     */
    public RecyclerViewSupport(Context context) {
        super(context);
    }

    /**
     * @see RecyclerView#RecyclerView(Context, AttributeSet)
     */
    public RecyclerViewSupport(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @see RecyclerView#RecyclerView(Context, AttributeSet, int)
     */
    public RecyclerViewSupport(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(emptyObserver);
        }

        emptyObserver.onChanged();
    }

    /**
     * Sets the empty view.
     *
     * @param emptyView the {@link #emptyView}
     */
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
    }



    /**
     * Shows the progress view.
     */
    public void startLoading() {
        // Hides the empty view.
        if (this.emptyView != null) {
            this.emptyView.setVisibility(GONE);
        }

    }

    /**
     * Hides the progress view.
     */
    public void endLoading() {

        // Forces the view refresh.
        emptyObserver.onChanged();
    }
}
