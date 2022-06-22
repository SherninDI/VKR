package com.example.therapyapp;

public interface DeviceListInteractionListener<T> {

    void onItemClick(T item);

    void startLoading();

    void endLoading(boolean partialResults);

    void endLoadingWithDialog(boolean error, T element);
}
