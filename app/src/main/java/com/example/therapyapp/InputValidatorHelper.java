package com.example.therapyapp;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputValidatorHelper {
    public boolean isValidFileId(String string){
        final String FILE_ID_PATTERN = "^[E][G][\\d][\\d][\\d]?";
        Pattern pattern = Pattern.compile(FILE_ID_PATTERN);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }

    public boolean isValidTitle(String string){
        final String TITLE_PATTERN = "^[а-яА-ЯёЁa-zA-Z0-9]{0,15}$";
        Pattern pattern = Pattern.compile(TITLE_PATTERN);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }

    public boolean isNullOrEmpty(String string){
        return TextUtils.isEmpty(string);
    }

    public boolean isNumeric(String string){
        return TextUtils.isDigitsOnly(string);
    }


}
