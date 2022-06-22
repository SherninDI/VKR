package com.example.therapyapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.navigation.fragment.NavHostFragment;
import com.example.therapyapp.databinding.FragmentAddGroupBinding;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GroupAddFragment extends Fragment {
    private static final String TAG = "GroupAddFragment";
    private FragmentAddGroupBinding binding;

    private DatabaseAdapter databaseAdapter;
    private List<String> groups = new ArrayList<String>();
    private ArrayList<ItemSubgroup> ept = new ArrayList<ItemSubgroup>();
    private EptRecyclerViewAdapter adapterEPT;
    private SubgroupRecyclerViewAdapter adapterSubgroup;
    private RecyclerView rvEPT;
    private RecyclerView rvSubGroups;
    private String search_text = null;

    boolean enableEPT = false;

    boolean saveTitle = true;
    boolean saveTime = true;

    boolean saveFreqId = true;
    boolean saveAmpl = true;
    boolean saveStepTime = true;

    private String strTitleWin = null;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAddGroupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        databaseAdapter = new DatabaseAdapter(getActivity());
        databaseAdapter.createDataBase();
        databaseAdapter.openDataBase();

        GroupHelper groupHelper = new GroupHelper();
        InputValidatorHelper inputValidatorHelper = new InputValidatorHelper();

        adapterEPT = new EptRecyclerViewAdapter(getContext(),ept);
        rvEPT = getView().findViewById(R.id.ept_list);
        rvEPT.setLayoutManager(new LinearLayoutManager(getActivity()));

        String sql = "SELECT * FROM groups";
        Cursor cursor = databaseAdapter.getData(sql);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            groups.add(name);
        }
        cursor.close();

        int pos = this.getArguments().getInt("group_count");
        int groupSize = 512;
        byte[] readGroup = readFromFile();
        byte[] group = Arrays.copyOfRange(readGroup, groupSize * pos, groupSize + groupSize * pos);
        ByteBuffer groupBuffer = ByteBuffer.wrap(group);
        Log.e(TAG, "group " + pos + " " + groupHelper.bytesToHex(group));

        byte[] file_id = new byte[4];
        groupBuffer.get(file_id, 0, file_id.length);

        byte[] len = new byte[1];
        groupBuffer.get(len);

        if (len[0] != 0x00) {

            byte[] title = new byte[15];
            groupBuffer.get(title, 0, title.length);
            byte[] offTitle = Arrays.copyOfRange(title, 0, len[0]);
            strTitleWin = new String(offTitle, Charset.forName("windows-1251"));
        }

        byte[] time = new byte[2];
        groupBuffer.get(time, 0, time.length);
        byte[] revTime = groupHelper.reverseByteArray(time);
        int timeInt = new BigInteger(revTime).intValue();

        byte[] mode = new byte[1];
        groupBuffer.get(mode,0, mode.length);
        int modeInt = new BigInteger(mode).intValue();

        Log.e(TAG, String.valueOf(modeInt));

        byte[] spre = new byte[1];
        groupBuffer.get(spre,0, spre.length);
        int spreInt = new BigInteger(spre).intValue();

        byte[] maxf = new byte[1];
        groupBuffer.get(maxf,0, maxf.length);
        int maxfInt = new BigInteger(maxf).intValue();

        byte[] space = new byte[1];
        groupBuffer.get(space,0, space.length);
        int spaceInt = new BigInteger(space).intValue();

        byte[] count = new byte[1];
        groupBuffer.get(count,0, count.length);
        int countInt = new BigInteger(count).intValue();

        byte[] steps = new byte[480];
        groupBuffer.get(steps, 0, steps.length);

        for (int i = 0; i < countInt; i++) {
            int start = (i * 6);
            int end = (i * 6) + 6;
            byte[] step = Arrays.copyOfRange(steps, start, end);
            ByteBuffer stepBuffer = ByteBuffer.wrap(step);

            byte[] type = new byte[1];
            stepBuffer.get(type, 0, type.length);

            byte[] code = new byte[2];
            stepBuffer.get(code, 0, code.length);
            byte[] revFreq_id = groupHelper.reverseByteArray(code);
            int codeInt = new BigInteger(revFreq_id).intValue();

            String sqlEPT = "SELECT * FROM vals WHERE value=" + codeInt + ";";
            Cursor cursorEPT = databaseAdapter.getData(sqlEPT);
            while (cursorEPT.moveToNext()) {
                int value_id = cursorEPT.getInt(0);
                int sub_group_id = cursorEPT.getInt(1);
                int value = cursorEPT.getInt(2);

                String sqlName = "SELECT * FROM sub_groups WHERE id=" + sub_group_id + ";";
                Cursor cursorName = databaseAdapter.getData(sqlName);
                while (cursorName.moveToNext()) {
                    int sub_id = cursorName.getInt(0);
                    int group_id = cursorName.getInt(1);
                    String name = cursorName.getString(2);
                    ItemSubgroup ept_group = new ItemSubgroup(value_id, sub_group_id, group_id, name, value);
                    ept.add(ept_group);
                    Log.e(TAG, name);
                }
            }

            byte[] ampl = new byte[1];
            stepBuffer.get(ampl, 0, ampl.length);
            int amplInt = new BigInteger(ampl).intValue();

            byte[] stepTime = new byte[2];
            stepBuffer.get(stepTime, 0, stepTime.length);
            byte[] revStepTime = groupHelper.reverseByteArray(stepTime);
            int stepTimeInt = new BigInteger(revStepTime).intValue();
        }

        rvEPT.setAdapter(adapterEPT);
        adapterEPT.notifyDataSetChanged();

        adapterEPT.setOnEptClickListener(new EptRecyclerViewAdapter.EptClickListener() {
            @Override
            public void onEptClick(int positionGroup, View itemView) {
                int start = (positionGroup * 6);
                int end = (positionGroup * 6) + 6;
                byte[] step = Arrays.copyOfRange(steps, start, end);
                ByteBuffer stepBuffer = ByteBuffer.wrap(step);

                AlertDialog.Builder builderStepSettings = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getLayoutInflater();
                View dialogViewCode = inflater.inflate(R.layout.dialog_view_step, null);
                builderStepSettings.setView(dialogViewCode);
                builderStepSettings.setTitle("Настройки воздействия");
                builderStepSettings.setPositiveButton("Сохранить", null);
                builderStepSettings.setNegativeButton("Удалить", null);
                builderStepSettings.setNeutralButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog dialogViewStep = builderStepSettings.create();
                dialogViewStep.show();

                Spinner spinnerType = dialogViewStep.findViewById(R.id.type_s);
                List<String> types = new ArrayList<>();
                types.add("f");
                types.add("d");
                types.add("m");
                types.add("p");
                ArrayAdapter<String> spinnerTypeAdapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_spinner_item, types);
                spinnerTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerType.setAdapter(spinnerTypeAdapter);

                byte[] type = new byte[1];
                stepBuffer.get(type, 0, type.length);
                String typeStr = new String(type, Charset.forName("windows-1251"));
                int posType = spinnerTypeAdapter.getPosition(typeStr);
                spinnerType.setSelection(posType);

                byte[] code = new byte[2];
                stepBuffer.get(code, 0, code.length);
                byte[] revFreq_id = groupHelper.reverseByteArray(code);
                int codeInt = new BigInteger(revFreq_id).intValue();
                EditText editFreqId = dialogViewStep.findViewById(R.id.freq_id_e);
                editFreqId.setText(String.valueOf(codeInt));

                byte[] ampl = new byte[1];
                stepBuffer.get(ampl, 0, ampl.length);
                int amplInt = new BigInteger(ampl).intValue();
                EditText editAmpl = dialogViewStep.findViewById(R.id.ampl_e);
                editAmpl.setText(String.valueOf(amplInt));

                byte[] stepTime = new byte[2];
                stepBuffer.get(stepTime, 0, stepTime.length);
                byte[] revStepTime = groupHelper.reverseByteArray(stepTime);
                int stepTimeInt = new BigInteger(revStepTime).intValue();
                EditText editStepTime = dialogViewStep.findViewById(R.id.step_time_e);
                editStepTime.setText(String.valueOf(stepTimeInt));

                ImageButton qType = dialogViewStep.findViewById(R.id.type_q);
                qType.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.type_t);
                        builder.setMessage(R.string.type_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qFreqId = dialogViewStep.findViewById(R.id.freq_id_q);
                qFreqId.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.freq_id_t);
                        builder.setMessage(R.string.freq_id_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qAmpl = dialogViewStep.findViewById(R.id.ampl_q);
                qAmpl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.ampl_t);
                        builder.setMessage(R.string.ampl_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qStepTime = dialogViewStep.findViewById(R.id.step_time_q);
                qStepTime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.step_time_t);
                        builder.setMessage(R.string.step_time_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                Button codeButton = dialogViewStep.findViewById(R.id.freq_id_b);
                codeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builderCode = new AlertDialog.Builder(getActivity());
                        LayoutInflater inflater = getLayoutInflater();
                        View dialogViewCode = inflater.inflate(R.layout.dialog_view_subgroup, null);
                        builderCode.setView(dialogViewCode);
                        builderCode.setTitle(R.string.code_title);
                        builderCode.setNeutralButton("Отмена", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                        Dialog dialog = builderCode.create();

                        Spinner spinnerGroup = dialogViewCode.findViewById(R.id.groups_spinner);
                        ArrayAdapter<String> spinnerGroupAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,groups);
                        spinnerGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerGroup.setAdapter(spinnerGroupAdapter);
                        spinnerGroup.setSelection(0);

                        EditText search = dialogViewCode.findViewById(R.id.search_text);
                        ImageButton search_button = dialogViewCode.findViewById(R.id.search_button);

                        rvSubGroups = dialogViewCode.findViewById(R.id.subgroups_list);
                        rvSubGroups.setLayoutManager(new LinearLayoutManager(view.getContext()));

                        spinnerGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
//                                   Log.e(TAG, "pos " + id);
                                ArrayList<ItemSubgroup> subGroupsList =  new ArrayList<>();
                                adapterSubgroup = new SubgroupRecyclerViewAdapter(getContext(),subGroupsList);
                                String sql = "SELECT * FROM sub_groups WHERE group_id=" + (id + 1) + ";";
                                Cursor cursor = databaseAdapter.getData(sql);
                                while (cursor.moveToNext()) {
                                    int subgroup_id = cursor.getInt(0);
                                    int group_id = cursor.getInt(1);
                                    String name = cursor.getString(2);
                                    String sql1 = "SELECT * FROM vals WHERE sub_group_id="+subgroup_id+";";
                                    Cursor cursor1 = databaseAdapter.getData(sql1);
                                    while (cursor1.moveToNext()) {
                                        int value_id = cursor1.getInt(0);
                                        int sub_group_id = cursor1.getInt(1);
                                        int value = cursor1.getInt(2);
                                        ItemSubgroup sub_group = new ItemSubgroup(value_id, sub_group_id, group_id, name, value);
                                        subGroupsList.add(sub_group);
                                        rvSubGroups.setAdapter(adapterSubgroup);
                                        adapterSubgroup.notifyDataSetChanged();
                                    }
                                    cursor1.close();
                                }
                                cursor.close();

                                adapterSubgroup.setOnSubGroupClickListener(new SubgroupRecyclerViewAdapter.SubGroupClickListener() {
                                    @Override
                                    public void onSubGroupClick(int position, View itemView) {
                                        ItemSubgroup selectedSubGroup = subGroupsList.get(position);
                                        int value = selectedSubGroup.getValue();
                                        editFreqId.setText(String.valueOf(value));
                                        dialog.dismiss();
                                    }
                                });

                                search.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dialogViewCode.findViewById(R.id.subgroups_list).setVisibility(View.GONE);
                                    }
                                });

                                search_button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        groupHelper.hideKeyboardFrom(getActivity(), view);
                                        dialogViewCode.findViewById(R.id.subgroups_list).setVisibility(View.VISIBLE);
                                        search_text = search.getText().toString();
                                        if (!search_text.isEmpty()) {
                                            ArrayList<ItemSubgroup>  searchList = groupHelper.searchData(subGroupsList, search_text);
                                            adapterSubgroup = new SubgroupRecyclerViewAdapter(getContext(),searchList);
                                            rvSubGroups.setAdapter(adapterSubgroup);
                                            adapterSubgroup.notifyDataSetChanged();
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onNothingSelected (AdapterView < ? > adapterView){
                            }
                        });


                        dialog.show();
                    }
                });

                dialogViewStep.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        InputValidatorHelper inputValidatorHelper = new InputValidatorHelper();
                        ByteBuffer stepEdit = ByteBuffer.allocate(6);

                        String editTypeStr = spinnerType.getSelectedItem().toString();
                        byte[] editTypeBytes = editTypeStr.getBytes(Charset.forName("windows-1251"));

                        stepEdit.put(editTypeBytes, 0, editTypeBytes.length);

                        String editFreqIdStr = editFreqId.getText().toString();
                        if (inputValidatorHelper.isNumeric(editFreqIdStr)) {
                            if (!inputValidatorHelper.isNullOrEmpty(editFreqIdStr)) {
                                int editFreqIdValue = Integer.parseInt(editFreqIdStr);
                                ByteBuffer editFreqIdBuffer = ByteBuffer.allocate(4);
                                editFreqIdBuffer.putInt(editFreqIdValue);
                                byte[] editFreqIdBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(editFreqIdBuffer.array()),0,2);
                                if (editFreqIdValue == 0 && editFreqIdValue > 65535) {
                                    saveFreqId = false;
                                    editFreqId.setError("Неверный формат");
                                } else {
                                    saveFreqId = true;
                                    stepEdit.put(editFreqIdBytes, 0, editFreqIdBytes.length);
                                }
                            } else {
                                saveFreqId = false;
                                editFreqId.setError("Поле не должно быть пустым");
                            }
                        } else {
                            saveFreqId = false;
                            editFreqId.setError("Значение должно быть числовым");
                        }

                        String editAmplStr = editAmpl.getText().toString();
                        if (inputValidatorHelper.isNumeric(editAmplStr)) {
                            if (!inputValidatorHelper.isNullOrEmpty(editAmplStr)) {
                                int editAmplValue = Integer.parseInt(editAmplStr);
                                if (editAmplValue >= 0 && editAmplValue <= 100) {
                                    saveAmpl = true;
                                    byte editAmplValueByte = (byte) editAmplValue;
                                    stepEdit.put(editAmplValueByte);
                                } else {
                                    saveAmpl = false;
                                    editAmpl.setError("Значение должно быть числовым в диапазоне 0 - 100");
                                }
                            } else {
                                saveAmpl = false;
                                editAmpl.setError("Поле не должно быть пустым");
                            }
                        } else {
                            saveAmpl = false;
                            editAmpl.setError("Значение должно быть числовым в диапазоне 0 - 100");
                        }

                        String editStepTimeStr = editStepTime.getText().toString();
                        if (inputValidatorHelper.isNumeric(editStepTimeStr)) {
                            if (!inputValidatorHelper.isNullOrEmpty(editAmplStr)) {
                                int editStepTimeValue = Integer.parseInt(editStepTimeStr);
                                ByteBuffer editStepTimeBuffer = ByteBuffer.allocate(4);
                                editStepTimeBuffer.putInt(editStepTimeValue);
                                byte[] editStepTimeBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(editStepTimeBuffer.array()),0,2);
                                if (editStepTimeValue >= 0 && editStepTimeValue <= 65535) {
                                    saveStepTime = true;
                                    stepEdit.put(editStepTimeBytes, 0, editStepTimeBytes.length);
                                } else {
                                    saveStepTime = false;
                                    editStepTime.setError("Значение должно быть числовым в диапазоне 0 - 65535");
                                }
                            } else {
                                saveStepTime = false;
                                editStepTime.setError("Поле не должно быть пустым");
                            }
                        } else {
                            saveStepTime = false;
                            editStepTime.setError("Значение должно быть числовым в диапазоне 0 - 65535");
                        }
                        if (saveFreqId && saveAmpl && saveStepTime) {
                            dialogViewStep.dismiss();
                            Log.e(TAG, "steps good " + groupHelper.bytesToHex(steps) + " " + steps.length);
                            int stepPos = groupSize * pos + 27 + positionGroup * 6;

                            File file = new File(getContext().getFilesDir(), "groups.grf");
                            try {
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                try {
                                    Log.e(TAG, String.valueOf(positionGroup));
                                    randomAccessFile.seek(stepPos);
                                    randomAccessFile.write(stepEdit.array(),0,stepEdit.array().length);
                                } finally {
                                    randomAccessFile.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else {
                            Log.e(TAG, "steps bad");
                        }

                        ept.clear();
                        refreshFragment();

                    }
                });

                dialogViewStep.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        InputValidatorHelper inputValidatorHelper = new InputValidatorHelper();
                        ByteBuffer stepEdit = ByteBuffer.allocate(6);
                        dialogViewStep.dismiss();

                        File file = new File(getContext().getFilesDir(), "groups.grf");
                        byte[] delStep = new byte[stepEdit.array().length];
                        ByteBuffer delStepBuffer = ByteBuffer.wrap(steps);

                        if (positionGroup == 0) {
                            int stepAfterPos = groupSize * pos + 27 + (positionGroup + 1) * 6;
                            int stepCountPos = groupSize * pos + 26;
                            byte[] stepAfter = new byte[steps.length - (positionGroup + 1) * 6];

                            ByteBuffer saveCountBuffer = ByteBuffer.allocate(4);
                            saveCountBuffer.putInt(countInt - 1);
                            byte[] saveCountBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(saveCountBuffer.array()),0,1);
                            try {
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                try {
                                    randomAccessFile.seek(stepCountPos);
                                    randomAccessFile.write(saveCountBytes,0,saveCountBytes.length);
                                    randomAccessFile.seek(stepAfterPos);
                                    randomAccessFile.read(stepAfter);
                                    Log.e(TAG, "new step " + groupHelper.bytesToHex(stepAfter) + " " + stepAfter.length );
                                } finally {
                                    randomAccessFile.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            delStepBuffer.put(stepAfter);
                            delStepBuffer.put(delStep);
                            try {
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                try {
                                    randomAccessFile.seek(groupSize * pos + 27);
                                    randomAccessFile.write(steps);
                                    Log.e(TAG, "new step " + groupHelper.bytesToHex(stepAfter) + " " + stepAfter.length );
                                } finally {
                                    randomAccessFile.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        } else {

                            int stepBeforePosStart = groupSize * pos + 27;
                            int stepBeforePosEnd = groupSize * pos + 27 + (positionGroup) * 6;
                            byte[] stepBefore = new byte[positionGroup * 6];

                            int stepAfterPos = groupSize * pos + 27 + (positionGroup + 1) * 6;
                            byte[] stepAfter = new byte[steps.length - (positionGroup + 1) * 6];
                            int stepCountPos = groupSize * pos + 26;

                            ByteBuffer saveCountBuffer = ByteBuffer.allocate(4);
                            saveCountBuffer.putInt(countInt - 1);
                            byte[] saveCountBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(saveCountBuffer.array()),0,1);
                            try {
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                try {
                                    randomAccessFile.seek(stepCountPos);
                                    randomAccessFile.write(saveCountBytes,0,saveCountBytes.length);
                                    randomAccessFile.seek(stepBeforePosStart);
                                    randomAccessFile.read(stepBefore);
                                    randomAccessFile.seek(stepAfterPos);
                                    randomAccessFile.read(stepAfter);
                                } finally {
                                    randomAccessFile.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            delStepBuffer.put(stepBefore);
                            delStepBuffer.put(stepAfter);
                            delStepBuffer.put(delStep);
                            try {
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                try {
                                    randomAccessFile.seek(groupSize * pos + 27);
                                    randomAccessFile.write(steps);
                                } finally {
                                    randomAccessFile.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Log.e(TAG, "before " + groupHelper.bytesToHex(stepBefore) + " " + stepBefore.length );
                            Log.e(TAG, "after " + groupHelper.bytesToHex(stepAfter) + " " + stepAfter.length );

                        }
                        ept.clear();
                        refreshFragment();
                    }
                });

            }
        });



        Button addEPT = getView().findViewById(R.id.add_ept);
        if (enableEPT) {
            addEPT.setEnabled(true);
        } else {
            addEPT.setEnabled(false);
        }

        addEPT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int start = (ept.size() * 6);
                int end = (ept.size() * 6) + 6;
                byte[] step = Arrays.copyOfRange(steps, start, end);
                ByteBuffer stepBuffer = ByteBuffer.wrap(step);

                AlertDialog.Builder builderStepSettings = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getLayoutInflater();
                View dialogViewCode = inflater.inflate(R.layout.dialog_view_step, null);
                builderStepSettings.setView(dialogViewCode);
                builderStepSettings.setTitle("Добавить воздействие");
                builderStepSettings.setPositiveButton("Сохранить", null);
                builderStepSettings.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog dialogViewStep = builderStepSettings.create();
                dialogViewStep.show();

                Spinner spinnerType = dialogViewStep.findViewById(R.id.type_s);
                List<String> types = new ArrayList<>();
                types.add("f");
                types.add("d");
                types.add("m");
                types.add("p");
                ArrayAdapter<String> spinnerTypeAdapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_spinner_item, types);
                spinnerTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerType.setAdapter(spinnerTypeAdapter);

                byte[] type = new byte[1];
                stepBuffer.get(type, 0, type.length);
                String typeStr = new String(type, Charset.forName("windows-1251"));
                int posType = spinnerTypeAdapter.getPosition(typeStr);
                spinnerType.setSelection(posType);

                byte[] code = new byte[2];
                stepBuffer.get(code, 0, code.length);
                byte[] revFreq_id = groupHelper.reverseByteArray(code);
                int codeInt = new BigInteger(revFreq_id).intValue();
                EditText editFreqId = dialogViewStep.findViewById(R.id.freq_id_e);
                editFreqId.setText(String.valueOf(codeInt));

                byte[] ampl = new byte[1];
                stepBuffer.get(ampl, 0, ampl.length);
                int amplInt = new BigInteger(ampl).intValue();
                EditText editAmpl = dialogViewStep.findViewById(R.id.ampl_e);
                editAmpl.setText(String.valueOf(amplInt));

                byte[] stepTime = new byte[2];
                stepBuffer.get(stepTime, 0, stepTime.length);
                byte[] revStepTime = groupHelper.reverseByteArray(stepTime);
                int stepTimeInt = new BigInteger(revStepTime).intValue();
                EditText editStepTime = dialogViewStep.findViewById(R.id.step_time_e);
                editStepTime.setText(String.valueOf(stepTimeInt));

                ImageButton qType = dialogViewStep.findViewById(R.id.type_q);
                qType.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.type_t);
                        builder.setMessage(R.string.type_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qFreqId = dialogViewStep.findViewById(R.id.freq_id_q);
                qFreqId.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.freq_id_t);
                        builder.setMessage(R.string.freq_id_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qAmpl = dialogViewStep.findViewById(R.id.ampl_q);
                qAmpl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.ampl_t);
                        builder.setMessage(R.string.ampl_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qStepTime = dialogViewStep.findViewById(R.id.step_time_q);
                qStepTime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.step_time_t);
                        builder.setMessage(R.string.step_time_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                Button codeButton = dialogViewStep.findViewById(R.id.freq_id_b);
                codeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builderCode = new AlertDialog.Builder(getActivity());
                        LayoutInflater inflater = getLayoutInflater();
                        View dialogViewCode = inflater.inflate(R.layout.dialog_view_subgroup, null);
                        builderCode.setView(dialogViewCode);
                        builderCode.setTitle(R.string.code_title);
                        builderCode.setNeutralButton("Отмена", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                        Dialog dialog = builderCode.create();

                        Spinner spinnerGroup = dialogViewCode.findViewById(R.id.groups_spinner);
                        ArrayAdapter<String> spinnerGroupAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,groups);
                        spinnerGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerGroup.setAdapter(spinnerGroupAdapter);
                        spinnerGroup.setSelection(0);

                        EditText search = dialogViewCode.findViewById(R.id.search_text);
                        ImageButton search_button = dialogViewCode.findViewById(R.id.search_button);

                        rvSubGroups = dialogViewCode.findViewById(R.id.subgroups_list);
                        rvSubGroups.setLayoutManager(new LinearLayoutManager(view.getContext()));

                        spinnerGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                                ArrayList<ItemSubgroup> subGroupsList =  new ArrayList<>();
                                adapterSubgroup = new SubgroupRecyclerViewAdapter(getContext(),subGroupsList);
                                String sql = "SELECT * FROM sub_groups WHERE group_id=" + (id + 1) + ";";
                                Cursor cursor = databaseAdapter.getData(sql);
                                while (cursor.moveToNext()) {
                                    int subgroup_id = cursor.getInt(0);
                                    int group_id = cursor.getInt(1);
                                    String name = cursor.getString(2);
                                    String sql1 = "SELECT * FROM vals WHERE sub_group_id="+subgroup_id+";";
                                    Cursor cursor1 = databaseAdapter.getData(sql1);
                                    while (cursor1.moveToNext()) {
                                        int value_id = cursor1.getInt(0);
                                        int sub_group_id = cursor1.getInt(1);
                                        int value = cursor1.getInt(2);
                                        ItemSubgroup sub_group = new ItemSubgroup(value_id, sub_group_id, group_id, name, value);
                                        subGroupsList.add(sub_group);
                                        rvSubGroups.setAdapter(adapterSubgroup);
                                        adapterSubgroup.notifyDataSetChanged();
                                    }
                                    cursor1.close();
                                }
                                cursor.close();

                                adapterSubgroup.setOnSubGroupClickListener(new SubgroupRecyclerViewAdapter.SubGroupClickListener() {
                                    @Override
                                    public void onSubGroupClick(int position, View itemView) {
                                        ItemSubgroup selectedSubGroup = subGroupsList.get(position);
                                        int value = selectedSubGroup.getValue();
                                        editFreqId.setText(String.valueOf(value));
                                        dialog.dismiss();
                                    }
                                });

                                search.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dialogViewCode.findViewById(R.id.subgroups_list).setVisibility(View.GONE);
                                    }
                                });

                                search_button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        groupHelper.hideKeyboardFrom(getActivity(), view);
                                        dialogViewCode.findViewById(R.id.subgroups_list).setVisibility(View.VISIBLE);
                                        search_text = search.getText().toString();
                                        if (!search_text.isEmpty()) {
                                            ArrayList<ItemSubgroup>  searchList = groupHelper.searchData(subGroupsList, search_text);
                                            adapterSubgroup = new SubgroupRecyclerViewAdapter(getContext(),searchList);
                                            rvSubGroups.setAdapter(adapterSubgroup);
                                            adapterSubgroup.notifyDataSetChanged();
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onNothingSelected (AdapterView < ? > adapterView){
                            }
                        });
                        dialog.show();
                    }
                });

                dialogViewStep.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        InputValidatorHelper inputValidatorHelper = new InputValidatorHelper();
                        ByteBuffer stepEdit = ByteBuffer.allocate(6);

                        String editTypeStr = spinnerType.getSelectedItem().toString();
                        byte[] editTypeBytes = editTypeStr.getBytes(Charset.forName("windows-1251"));
                        stepEdit.put(editTypeBytes, 0, editTypeBytes.length);

                        String editFreqIdStr = editFreqId.getText().toString();
                        if (inputValidatorHelper.isNumeric(editFreqIdStr)) {
                            if (!inputValidatorHelper.isNullOrEmpty(editFreqIdStr)) {
                                int editFreqIdValue = Integer.parseInt(editFreqIdStr);
                                ByteBuffer editFreqIdBuffer = ByteBuffer.allocate(4);
                                editFreqIdBuffer.putInt(editFreqIdValue);
                                byte[] editFreqIdBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(editFreqIdBuffer.array()),0,2);
                                if (editFreqIdValue >= 0 && editFreqIdValue <= 65535) {
                                    saveFreqId = true;
                                    stepEdit.put(editFreqIdBytes, 0, editFreqIdBytes.length);
                                } else {
                                    saveFreqId = false;
                                    editFreqId.setError("Неверный формат");
                                }
                            } else {
                                saveFreqId = false;
                                editFreqId.setError("Поле не должно быть пустым");
                            }
                        } else {
                            saveFreqId = false;
                            editFreqId.setError("Значение должно быть числовым");
                        }

                        String editAmplStr = editAmpl.getText().toString();
                        if (inputValidatorHelper.isNumeric(editAmplStr)) {
                            if (!inputValidatorHelper.isNullOrEmpty(editAmplStr)) {
                                int editAmplValue = Integer.parseInt(editAmplStr);
                                if (editAmplValue >= 0 && editAmplValue <= 100) {
                                    saveAmpl = true;
                                    byte editAmplValueByte = (byte) editAmplValue;
                                    stepEdit.put(editAmplValueByte);
                                } else {
                                    saveAmpl = false;
                                    editAmpl.setError("Значение должно быть числовым в диапазоне 0 - 100");
                                }
                            } else {
                                saveAmpl = false;
                                editAmpl.setError("Поле не должно быть пустым");
                            }
                        } else {
                            saveAmpl = false;
                            editAmpl.setError("Значение должно быть числовым в диапазоне 0 - 100");
                        }

                        String editStepTimeStr = editStepTime.getText().toString();
                        if (inputValidatorHelper.isNumeric(editStepTimeStr)) {
                            if (!inputValidatorHelper.isNullOrEmpty(editAmplStr)) {
                                int editStepTimeValue = Integer.parseInt(editStepTimeStr);
                                ByteBuffer editStepTimeBuffer = ByteBuffer.allocate(4);
                                editStepTimeBuffer.putInt(editStepTimeValue);
                                byte[] editStepTimeBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(editStepTimeBuffer.array()),0,2);
                                if (editStepTimeValue >= 0 && editStepTimeValue <= 65535) {
                                    saveStepTime = true;
                                    stepEdit.put(editStepTimeBytes, 0, editStepTimeBytes.length);
                                } else {
                                    saveStepTime = false;
                                    editStepTime.setError("Значение должно быть числовым в диапазоне 0 - 65535");
                                }
                            } else {
                                saveStepTime = false;
                                editStepTime.setError("Поле не должно быть пустым");
                            }
                        } else {
                            saveStepTime = false;
                            editStepTime.setError("Значение должно быть числовым в диапазоне 0 - 65535");
                        }
                        if (saveFreqId && saveAmpl && saveStepTime) {
                            dialogViewStep.dismiss();
                            Log.e(TAG, "steps good " + groupHelper.bytesToHex(steps) + " " + steps.length);

                            int stepPos = groupSize * pos + 27 + ept.size()*6;
                            int stepCountPos = groupSize * pos + 26;

                            ByteBuffer saveCountBuffer = ByteBuffer.allocate(4);
                            saveCountBuffer.putInt(countInt + 1);
                            byte[] saveCountBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(saveCountBuffer.array()),0,1);

                            File file = new File(getContext().getFilesDir(), "groups.grf");
                            try {
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                try {
                                    randomAccessFile.seek(stepCountPos);
                                    randomAccessFile.write(saveCountBytes,0,saveCountBytes.length);
                                    randomAccessFile.seek(stepPos);
                                    randomAccessFile.write(stepEdit.array(), 0,stepEdit.array().length);

                                    Log.e(TAG, "steps read " + groupHelper.bytesToHex(stepEdit.array()));
                                } finally {
                                    randomAccessFile.close();
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else {
                            Log.e(TAG, "steps bad");
                        }
                        ept.clear();
                        refreshFragment();
                    }
                });
            }
        });


        Button groupSettings = getView().findViewById(R.id.group_settings);
        groupSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builderGroupSettings = new AlertDialog.Builder(view.getContext());
                LayoutInflater inflater = getLayoutInflater();
                View dialogViewCode = inflater.inflate(R.layout.dialog_view_settings_group, null);

                builderGroupSettings.setView(dialogViewCode);
                builderGroupSettings.setTitle("Настройки группы");
                builderGroupSettings.setPositiveButton("Сохранить", null);
                builderGroupSettings.setNeutralButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                AlertDialog dialogGroupSettings = builderGroupSettings.create();
                dialogGroupSettings.show();


                ImageButton qTitle = dialogGroupSettings.findViewById(R.id.title_q);
                qTitle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.title_t);
                        builder.setMessage(R.string.title_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qTime = dialogGroupSettings.findViewById(R.id.time_q);
                qTime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.time_t);
                        builder.setMessage(R.string.time_q)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qModeSerial = dialogGroupSettings.findViewById(R.id.mode_serial_q);
                qModeSerial.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.mode_serial);
                        builder.setMessage(R.string.mode_q_serial)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qModeCycle = dialogGroupSettings.findViewById(R.id.mode_cycle_q);
                qModeCycle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.mode_cycle);
                        builder.setMessage(R.string.mode_q_cycle)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });

                ImageButton qModeComplex = dialogGroupSettings.findViewById(R.id.mode_complex_q);
                qModeComplex.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.mode_complex);
                        builder.setMessage(R.string.mode_q_complex)
                                .setCancelable(false)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                });


                EditText editTitle = dialogGroupSettings.findViewById(R.id.title_e);
                editTitle.setText(strTitleWin);


                EditText editTime = dialogGroupSettings.findViewById(R.id.time_e);
                editTime.setText(String.valueOf(timeInt));

                CheckBox checkModeSerial = dialogGroupSettings.findViewById(R.id.mode_serial);
                CheckBox checkModeCycle = dialogGroupSettings.findViewById(R.id.mode_cycle);
                CheckBox checkModeComplex = dialogGroupSettings.findViewById(R.id.mode_complex);

                switch (modeInt) {
                    case 0:
                        checkModeSerial.setChecked(true);
                        checkModeCycle.setChecked(false);
                        checkModeComplex.setChecked(false);
                        break;
                    case 1:
                        checkModeCycle.setChecked(true);
                        checkModeSerial.setChecked(false);
                        checkModeComplex.setChecked(false);
                        break;
                    case 2:
                        checkModeComplex.setChecked(true);
                        checkModeSerial.setChecked(false);
                        checkModeCycle.setChecked(false);
                        break;
                }
                checkModeSerial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkModeCycle.setChecked(false);
                            checkModeComplex.setChecked(false);
                        }
                    }
                });
                checkModeCycle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkModeSerial.setChecked(false);
                            checkModeComplex.setChecked(false);
                        }
                    }
                });
                checkModeComplex.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkModeSerial.setChecked(false);
                            checkModeCycle.setChecked(false);
                        }
                    }
                });

                CheckBox checkSpreDisable = dialogGroupSettings.findViewById(R.id.spre_disable);
                CheckBox checkSpreEnable1 = dialogGroupSettings.findViewById(R.id.spre_enable1);
                CheckBox checkSpreEnable2 = dialogGroupSettings.findViewById(R.id.spre_enable2);

                switch (spreInt) {
                    case 0:
                        checkSpreDisable.setChecked(true);
                        checkSpreEnable1.setChecked(false);
                        checkSpreEnable2.setChecked(false);
                        break;
                    case 1:
                        checkSpreDisable.setChecked(false);
                        checkSpreEnable1.setChecked(true);
                        checkSpreEnable2.setChecked(false);
                        break;
                    case 2:
                        checkSpreDisable.setChecked(false);
                        checkSpreEnable1.setChecked(false);
                        checkSpreEnable2.setChecked(true);
                        break;
                }
                checkSpreDisable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkSpreEnable1.setChecked(false);
                            checkSpreEnable2.setChecked(false);
                        }
                    }
                });
                checkSpreEnable1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkSpreEnable2.setChecked(false);
                            checkSpreDisable.setChecked(false);
                        }
                    }
                });
                checkSpreEnable2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkSpreEnable1.setChecked(false);
                            checkSpreDisable.setChecked(false);
                        }
                    }
                });

                CheckBox checkMaxf15k = dialogGroupSettings.findViewById(R.id.maxf_15k);
                CheckBox checkMaxf1100 = dialogGroupSettings.findViewById(R.id.maxf_1100);
                CheckBox checkMaxf1200 = dialogGroupSettings.findViewById(R.id.maxf_1200);
                CheckBox checkMaxf1500 = dialogGroupSettings.findViewById(R.id.maxf_1500);

                switch (maxfInt) {
                    case 0:
                        checkMaxf15k.setChecked(true);
                        checkMaxf1100.setChecked(false);
                        checkMaxf1200.setChecked(false);
                        checkMaxf1500.setChecked(false);
                        break;
                    case 1:
                        checkMaxf15k.setChecked(false);
                        checkMaxf1100.setChecked(true);
                        checkMaxf1200.setChecked(false);
                        checkMaxf1500.setChecked(false);
                        break;
                    case 2:
                        checkMaxf15k.setChecked(false);
                        checkMaxf1100.setChecked(false);
                        checkMaxf1200.setChecked(true);
                        checkMaxf1500.setChecked(false);
                        break;
                    case 3:
                        checkMaxf15k.setChecked(false);
                        checkMaxf1100.setChecked(false);
                        checkMaxf1200.setChecked(false);
                        checkMaxf1500.setChecked(true);
                        break;
                }
                checkMaxf15k.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkMaxf1100.setChecked(false);
                            checkMaxf1200.setChecked(false);
                            checkMaxf1500.setChecked(false);
                        }
                    }
                });
                checkMaxf1100.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkMaxf15k.setChecked(false);
                            checkMaxf1200.setChecked(false);
                            checkMaxf1500.setChecked(false);
                        }
                    }
                });
                checkMaxf1200.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkMaxf1100.setChecked(false);
                            checkMaxf15k.setChecked(false);
                            checkMaxf1500.setChecked(false);
                        }
                    }
                });

                checkMaxf1500.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            checkMaxf1100.setChecked(false);
                            checkMaxf1200.setChecked(false);
                            checkMaxf15k.setChecked(false);
                        }
                    }
                });

                dialogGroupSettings.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialogGroupSettings.dismiss();
                        byte[] newGroup = new byte[26];
                        ByteBuffer newGroupBuffer = ByteBuffer.wrap(newGroup);
                        InputValidatorHelper inputValidatorHelper = new InputValidatorHelper();
                        String saveFileIdStr = "EG01";
                        byte[] saveFileIdBytes = saveFileIdStr.getBytes(Charset.forName("windows-1251"));
                        newGroupBuffer.put(saveFileIdBytes);

                        String title = editTitle.getText().toString();
                        if (!inputValidatorHelper.isNullOrEmpty(title)) {
                            if (title.length() <= 15) {
                                byte[] saveTitleBytes = title.getBytes(Charset.forName("windows-1251"));
                                ByteBuffer saveLenBuffer = ByteBuffer.allocate(4);
                                saveLenBuffer.putInt(title.length());
                                byte[] saveLenBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(saveLenBuffer.array()),0,1);
                                if (inputValidatorHelper.isValidTitle(title)) {
                                    newGroupBuffer.put(saveLenBytes);
                                    newGroupBuffer.put(saveTitleBytes, 0, saveTitleBytes.length);
                                    byte[] nulls = new byte[15];
                                    newGroupBuffer.put(nulls, 0, 15 - saveTitleBytes.length);

                                    enableEPT = true;

                                    saveTitle = true;
                                } else {
                                    saveTitle = false;
                                    editTitle.setError("Неверное название");
                                }
                            } else {
                                saveTitle = false;
                                editTitle.setError("Название не должно превышать 15 символов");
                            }
                        } else {
                            saveTitle = false;
                            editTitle.setError("Поле не должно быть пустым");
                        }

                        String saveTimeStr = editTime.getText().toString();
                        if (inputValidatorHelper.isNumeric(saveTimeStr)) {
                            if (!inputValidatorHelper.isNullOrEmpty(saveTimeStr)) {
                                int saveTimeValue = Integer.parseInt(saveTimeStr);
                                ByteBuffer saveTimeBuffer = ByteBuffer.allocate(4);
                                saveTimeBuffer.putInt(saveTimeValue);
                                byte[] saveTimeBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(saveTimeBuffer.array()),0,2);
                                if (saveTimeValue >= 0 && saveTimeValue <= 65535) {
                                    newGroupBuffer.put(saveTimeBytes);
                                    saveTime = true;
                                } else {
                                    saveTime = false;
                                    editTime.setError("Значение должно быть числовым в диапазоне 0 - 65535");
                                }
                            } else {
                                saveTime = false;
                                editTime.setError("Поле не должно быть пустым");
                            }
                        } else {
                            saveTime = false;
                            editTime.setError("Значение должно быть числовым в диапазоне 0 - 65535");
                        }

                        byte saveModeValueByte = 0x00;
                        if (checkModeSerial.isChecked()) {
                            Log.e(TAG, "check 0");
                            saveModeValueByte = (byte) 0x00;
                        } else if (checkModeCycle.isChecked()) {
                            Log.e(TAG, "check 1");
                            saveModeValueByte = (byte) 0x01;
                        } else if (checkModeComplex.isChecked()) {
                            Log.e(TAG, "check 2");
                            saveModeValueByte = (byte) 0x02;
                        }
                        newGroupBuffer.put(saveModeValueByte);

                        byte saveSpreValueByte = 0x00;
                        if (checkSpreDisable.isChecked()) {
                            saveSpreValueByte = (byte) 0x00;
                        } else if (checkSpreEnable1.isChecked()) {
                            saveSpreValueByte = (byte) 0x01;
                        } else if (checkSpreEnable2.isChecked()) {
                            saveSpreValueByte = (byte) 0x02;
                        }
                        newGroupBuffer.put(saveSpreValueByte);

                        byte saveMaxfValueByte = 0x00;
                        if (checkMaxf15k.isChecked()) {
                            saveMaxfValueByte = (byte) 0x00;
                        } else if (checkMaxf1100.isChecked()) {
                            saveMaxfValueByte = (byte) 0x01;
                        } else if (checkMaxf1200.isChecked()) {
                            saveMaxfValueByte = (byte) 0x02;
                        } else if (checkMaxf1500.isChecked()) {
                            saveMaxfValueByte = (byte) 0x03;
                        }
                        newGroupBuffer.put(saveMaxfValueByte);

                        newGroupBuffer.put((byte) 0x00);



                        if (saveTitle && saveTime) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                            builder.setTitle(R.string.save_group_alert);
                            builder.setMessage(R.string.group_mes)
                                    .setCancelable(false)
                                    .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int i) {
                                            dialog.cancel();
                                        }
                                    })
                                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Log.e(TAG, "old group " + groupHelper.bytesToHex(group) + newGroup.length);
                                            Log.e(TAG, "new group " + groupHelper.bytesToHex(newGroup) + newGroup.length);

                                            File file = new File(getContext().getFilesDir(), "groups.grf");
                                            try {
                                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                                try {
                                                    randomAccessFile.seek(groupSize * pos);
                                                    randomAccessFile.write(newGroup);
                                                } finally {
                                                    randomAccessFile.close();
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();
                        } else {
                            Log.e(TAG, "bad ");
                        }

                        ept.clear();
                        refreshFragment();

                    }
                });

            }
        });

        Button saveGroup = getView().findViewById(R.id.save_group);
        saveGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(getContext().getFilesDir(), "groups.grf");
                byte[] newGroup = new byte[groupSize];
                ByteBuffer newGroupBuffer = ByteBuffer.wrap(newGroup);

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.save_group_alert);
                builder.setMessage(R.string.group_mes)
                        .setCancelable(false)
                        .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                    try {
                                        randomAccessFile.seek(groupSize * pos);
                                        randomAccessFile.read(newGroup);
                                    } finally {
                                        randomAccessFile.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

//                                Log.e(TAG, "new group " + groupHelper.bytesToHex(newGroup) + " " + newGroup.length);

                                byte[] saveNewGroup = new byte[groupSize];
                                ByteBuffer saveNewGroupBuffer = ByteBuffer.wrap(saveNewGroup);

                                byte[] newFileId = new byte[4];
                                newGroupBuffer.get(newFileId,0,newFileId.length);
                                saveNewGroupBuffer.put(newFileId);

                                byte[] sum = Arrays.copyOfRange(newGroup,4,507);
                                saveNewGroupBuffer.put(sum,0, sum.length);
                                byte[] crcByte = ByteBuffer.allocate(4).putInt(groupHelper.CRC32sum(sum)).array();
                                byte[] revCrcByte = groupHelper.reverseByteArray(crcByte);
                                saveNewGroupBuffer.put(revCrcByte);

                                byte exec = (byte) 0x00;
                                saveNewGroupBuffer.put(exec);

                                Log.e(TAG, "new group " + groupHelper.bytesToHex(saveNewGroup) + " " + saveNewGroup.length);

                                File file = new File(getContext().getFilesDir(), "groups.grf");
                                try {
                                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                    try {
                                        randomAccessFile.seek(groupSize * pos);
                                        randomAccessFile.write(saveNewGroup);
                                    } finally {
                                        randomAccessFile.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                NavHostFragment.findNavController(GroupAddFragment.this)
                                        .navigate(R.id.action_AddGroupFragment_to_GroupFragment);

                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

            }
        });


        Button cancelGroup = getView().findViewById(R.id.cancel_group);
        cancelGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.del_group_alert);
                builder.setMessage(R.string.group_mes)
                        .setCancelable(false)
                        .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                File file = new File(getContext().getFilesDir(), "groups.grf");
                                byte[] delGroup = new byte[groupSize];
                                try {
                                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                    try {
                                        randomAccessFile.seek(groupSize * pos);
                                        randomAccessFile.write(delGroup);
                                    } finally {
                                        randomAccessFile.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Log.e(TAG, "new group " + groupHelper.bytesToHex(delGroup) + " " + delGroup.length + " " + file.length());

                                NavHostFragment.findNavController(GroupAddFragment.this)
                                        .navigate(R.id.action_AddGroupFragment_to_GroupFragment);
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });


    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private byte[] readFromFile() {
        File file = new File(getContext().getFilesDir(), "groups.grf");
        int size = (int) file.length();
        byte[] buffer = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(buffer,0,buffer.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    private void writeToFile(byte[] bytes) {
        FileOutputStream fOut = null;
        OutputStreamWriter osw = null;

        try {
            fOut = getContext().openFileOutput("groups.grf", Context.MODE_PRIVATE);
            osw = new OutputStreamWriter(fOut);
            try {

                fOut.write(bytes);
                fOut.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fOut.close();
                osw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshFragment(){
        getFragmentManager()
                .beginTransaction()
                .detach(this)
                .attach(this)
                .addToBackStack(null)
                .commit();
    }
}

