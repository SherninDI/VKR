package com.example.therapyapp;

public interface DeviceListInteractionListener<T> {
    /**
     * Called when a list element is clicked.
     *
     * @param item the clicked item.
     */
    void onItemClick(T item);

    /**
     * Called when the list elements are being fetched.
     */
    void startLoading();

    /**
     * Called when one or all the list elements have been fetched.
     *
     * @param partialResults true if the results are partial and
     *                       the fetching is still going, false otherwise.
     */
    void endLoading(boolean partialResults);

    /**
     * Called to dismiss a loading dialog.
     *
     * @param error   true if an error has occurred, false otherwise.
     * @param element the list element processed.
     */
    void endLoadingWithDialog(boolean error, T element);
}
