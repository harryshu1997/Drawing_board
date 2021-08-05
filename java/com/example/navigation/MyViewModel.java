package com.example.navigation;

import android.content.ClipData;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.navigation.ui.Forb;
import com.example.navigation.ui.Point;

import java.util.ArrayList;

public class MyViewModel extends ViewModel {
    private  MutableLiveData<Boolean> isForb = new MutableLiveData<Boolean>();
    private  MutableLiveData<Integer> forbSize = new MutableLiveData<Integer>();
    private MutableLiveData<Integer> PGap = new MutableLiveData<Integer>();
    private MutableLiveData<Boolean> isGap = new MutableLiveData<Boolean>();
    private MutableLiveData<Integer> PSize = new MutableLiveData<Integer>();
    private MutableLiveData<Boolean> isSize = new MutableLiveData<Boolean>();
    private MutableLiveData<Float> scaleRatio = new MutableLiveData<Float>();


    public MutableLiveData<Float> getScaleRatio() {
        return scaleRatio;
    }

    public void setScaleRatio(float scale) {
        this.scaleRatio.setValue(scale);
    }

    public MutableLiveData<Boolean> getIsSize() {
        return isSize;
    }

    public void setIsSize(boolean isSize) {
        this.isSize.setValue(isSize);
    }

    public MutableLiveData<Boolean> getIsGap() {
        return isGap;
    }

    public void setIsGap(boolean isGap) {
        this.isGap.setValue(isGap);
    }

    public MutableLiveData<Integer> getPSize() {
        return PSize;
    }

    public void setPSize(int Size) {
        this.PSize.setValue(Size);
    }

    public MutableLiveData<Integer> getPGap() {
        return PGap;
    }

    public void setPGap(int gap) {
        this.PGap.setValue(gap);
    }

    public void setForbSize(int size){
        forbSize.setValue(size);
    }

    public LiveData<Integer> getForbSize(){
        return forbSize;
    }

    public void setForb(boolean b){
        isForb.setValue(b);
    }

    public LiveData<Boolean> getIsForb(){
        return isForb;
    }


}
