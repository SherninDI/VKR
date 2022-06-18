package com.example.therapyapp;

import android.app.AlertDialog;
import android.app.Dialog;
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


public class AddGroupFragment extends Fragment {
    private static final String TAG = "AddGroupFragment";
    private FragmentAddGroupBinding binding;

    private DatabaseAdapter databaseAdapter;
    private List<String> groups = new ArrayList<String>();
    private SubgroupRecyclerViewAdapter adapterSubgroup;
    private RecyclerView rvSubGroups;
    private String search_text = null;

    boolean saveFileId = true;
    boolean saveTitle = true;
    boolean saveTime = true;
    boolean saveStepCount = true;

    boolean saveFreqId = true;
    boolean saveAmpl = true;
    boolean saveStepTime = true;

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

        String sql = "SELECT * FROM groups";
        Cursor cursor = databaseAdapter.getData(sql);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            ItemGroup group = new ItemGroup(id, name);
            groups.add(name);
        }
        cursor.close();

        int pos = this.getArguments().getInt("group_count");
        int groupSize = 512;
        byte[] readGroup = readFromFile();
        byte[] group = Arrays.copyOfRange(readGroup, groupSize * pos, groupSize + groupSize * pos);

        ByteBuffer groupBuffer = ByteBuffer.wrap(group);
        Log.e(TAG, "group " + pos + " " + groupHelper.bytesToHex(group));

        EditText editFileId = getView().findViewById(R.id.file_id_e);
        byte[] file_id = new byte[4];
        groupBuffer.get(file_id, 0, file_id.length);
        String fileId = new String(file_id, Charset.forName("windows-1251"));
        if (inputValidatorHelper.isValidFileId(fileId)) {
            editFileId.setText(fileId);
        }


        EditText editTitle = getView().findViewById(R.id.title_e);
        byte[] len = new byte[1];
        groupBuffer.get(len);
        if (len[0] != 0x00) {
            byte[] title = new byte[15];
            groupBuffer.get(title, 0, title.length);
            byte[] offTitle = Arrays.copyOfRange(title, 0, len[0]);
            String strTitleWin = new String(offTitle, Charset.forName("windows-1251"));
            editTitle.setText(strTitleWin);
        }

        EditText editTime = getView().findViewById(R.id.time_e);
        byte[] time = new byte[2];
        groupBuffer.get(time, 0, time.length);
        byte[] revTime = groupHelper.reverseByteArray(time);
        int timeInt = new BigInteger(revTime).intValue();
        editTime.setText(String.valueOf(timeInt));

        Spinner spinnerMode = getView().findViewById(R.id.mode_s);
        byte[] mode = new byte[1];

        groupBuffer.get(mode,0, mode.length);
        int modeInt = new BigInteger(mode).intValue();

        List<String> modes_spre = new ArrayList<>();
        modes_spre.add(String.valueOf(0));
        modes_spre.add(String.valueOf(1));
        modes_spre.add(String.valueOf(2));
        ArrayAdapter<String> spinnerModeSpreAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item, modes_spre);
        spinnerModeSpreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerMode.setAdapter(spinnerModeSpreAdapter);

        spinnerMode.setSelection(modeInt);
        Spinner spinnerSpre = getView().findViewById(R.id.spre_s);
        byte[] spre = new byte[1];
        groupBuffer.get(spre,0, spre.length);
        int spreInt = new BigInteger(spre).intValue();

        spinnerSpre.setAdapter(spinnerModeSpreAdapter);
        spinnerSpre.setSelection(spreInt);

        Spinner spinnerMaxf = getView().findViewById(R.id.maxf_s);
        byte[] maxf = new byte[1];
        groupBuffer.get(maxf,0, maxf.length);
        int maxfInt = new BigInteger(maxf).intValue();

        List<String> maxfs = new ArrayList<>();
        maxfs.add(String.valueOf(0));
        maxfs.add(String.valueOf(1));
        maxfs.add(String.valueOf(2));
        maxfs.add(String.valueOf(3));
        ArrayAdapter<String> spinnerMaxfAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item, maxfs);
        spinnerMaxfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerMaxf.setAdapter(spinnerMaxfAdapter);

        spinnerMaxf.setSelection(maxfInt);


        byte[] space = new byte[1];
        groupBuffer.get(space,0, space.length);
        int spaceInt = new BigInteger(space).intValue();

        EditText editCount = getView().findViewById(R.id.cnt_e);
        byte[] cnt = new byte[1];
        groupBuffer.get(cnt,0, cnt.length);
        int cntInt = new BigInteger(cnt).intValue();
        editCount.setText(String.valueOf(cntInt));

        byte[] steps = new byte[480];
        groupBuffer.get(steps, 0, steps.length);

        ByteBuffer stepsBuffer = ByteBuffer.allocate(480);

        Button editSteps = getView().findViewById(R.id.edit_step);
        editSteps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int count = Integer.parseInt(editCount.getText().toString());
                if (count == 0) {
                    editCount.setError("Количество шагов не может быть равным 0");
                } else {
                    for (int i = 0; i < count; i++) {
                        int start = (i * 6);
                        int end = (i * 6) + 6;
                        byte[] step = Arrays.copyOfRange(steps, start, end);
                        ByteBuffer stepBuffer = ByteBuffer.wrap(step);

                        AlertDialog.Builder builderStep = new AlertDialog.Builder(view.getContext());
                        LayoutInflater inflater = getLayoutInflater();
                        View dialogViewStep = inflater.inflate(R.layout.dialog_view_step, null);
                        builderStep.setView(dialogViewStep);

                        Spinner spinnerType = dialogViewStep.findViewById(R.id.type_s);
                        List<String> types = new ArrayList<>();
                        types.add(String.valueOf(0));
                        types.add(String.valueOf(1));
                        types.add(String.valueOf(2));
                        types.add(String.valueOf(3));
                        types.add("f");
                        types.add("d");
                        types.add("m");
                        types.add("p");
                        ArrayAdapter<String> spinnerTypeAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item, types);
                        spinnerTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerType.setAdapter(spinnerTypeAdapter);

                        byte[] type = new byte[1];
                        stepBuffer.get(type, 0, type.length);
                        if (type[0] <= 0x34) {
                            int typeInt = new BigInteger(type).intValue();
                            spinnerType.setSelection(typeInt);

                        } else {
                            String typeStr = new String(type, Charset.forName("windows-1251"));
                            int pos = spinnerTypeAdapter.getPosition(typeStr);
                            spinnerType.setSelection(pos);
                        }

                        EditText editFreqId = dialogViewStep.findViewById(R.id.freq_id_e);
                        byte[] freq_id = new byte[2];
                        stepBuffer.get(freq_id, 0, freq_id.length);
                        byte[] revFreq_id = groupHelper.reverseByteArray(freq_id);
                        int freq_idInt = new BigInteger(revFreq_id).intValue();
                        editFreqId.setText(String.valueOf(freq_idInt));

                        EditText editAmpl = dialogViewStep.findViewById(R.id.ampl_e);
                        byte[] ampl = new byte[1];
                        stepBuffer.get(ampl, 0, ampl.length);
                        int amplInt = new BigInteger(ampl).intValue();
                        editAmpl.setText(String.valueOf(amplInt));

                        EditText editStepTime = dialogViewStep.findViewById(R.id.step_time_e);
                        byte[] stepTime = new byte[2];
                        stepBuffer.get(stepTime, 0, stepTime.length);
                        byte[] revStepTime = groupHelper.reverseByteArray(stepTime);
                        int stepTimeInt = new BigInteger(revStepTime).intValue();
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
                                AlertDialog.Builder builderCode = new AlertDialog.Builder(view.getContext());
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
                                dialog.show();

                                Spinner spinnerGroup = dialogViewCode.findViewById(R.id.groups_spinner);
                                ArrayAdapter<String> spinnerGroupAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,groups);
                                spinnerGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerGroup.setAdapter(spinnerGroupAdapter);
                                spinnerGroup.setSelection(0);

                                EditText search = dialogViewCode.findViewById(R.id.search_text);

                                ImageButton search_button = dialogViewCode.findViewById(R.id.search_button);

                                rvSubGroups = dialogViewCode.findViewById(R.id.subgroups_list);
                                rvSubGroups.setLayoutManager(new LinearLayoutManager(getActivity()));

                                spinnerGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                                        ArrayList<ItemSubgroup> subGroupsList =  new ArrayList<>();
                                        adapterSubgroup = new SubgroupRecyclerViewAdapter(view.getContext(),subGroupsList);
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
                                                    adapterSubgroup = new SubgroupRecyclerViewAdapter(getActivity(),searchList);
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
                            }
                        });

                        builderStep.setTitle("Формат шага №" + i);
                        builderStep.setPositiveButton("Сохранить", null);

                        builderStep.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                        final AlertDialog dialogStep = builderStep.create();;
                        dialogStep.show();

                        int stepNum = i;
                        dialogStep.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                ByteBuffer step = ByteBuffer.allocate(6);
                                String editTypeStr = spinnerType.getSelectedItem().toString();
                                byte[] editTypeBytes = editTypeStr.getBytes(Charset.forName("windows-1251"));
                                int val = Byte.compare(editTypeBytes[0], (byte) 0x40);
                                if (val <= 0) {
                                    int editTypeValue = Integer.parseInt(editTypeStr);
                                    byte editTypeValueByte = (byte) editTypeValue;
                                    step.put(editTypeValueByte);
                                } else {
                                    step.put(editTypeBytes, 0, editTypeBytes.length);
                                }

                                String editFreqIdStr = editFreqId.getText().toString();
                                if (inputValidatorHelper.isNumeric(editFreqIdStr)) {
                                    if (!inputValidatorHelper.isNullOrEmpty(editFreqIdStr)) {
                                        int editFreqIdValue = Integer.parseInt(editFreqIdStr);
                                        ByteBuffer editFreqIdBuffer = ByteBuffer.allocate(4);
                                        editFreqIdBuffer.putInt(editFreqIdValue);
                                        byte[] editFreqIdBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(editFreqIdBuffer.array()),0,2);
                                        if (editFreqIdValue >= 0 && editFreqIdValue <= 65535) {
                                            saveFreqId = true;
                                            step.put(editFreqIdBytes, 0, editFreqIdBytes.length);
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
                                            step.put(editAmplValueByte);
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
                                            step.put(editStepTimeBytes, 0, editStepTimeBytes.length);
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
                                    System.arraycopy(step.array(),0, steps, stepNum * 6, step.array().length);
                                    dialogStep.dismiss();
                                    byte[] nulls = new byte[480 - count * 6];
                                    System.arraycopy(nulls,0, steps, count * 6, nulls.length);
                                    Log.e(TAG, "steps good " + groupHelper.bytesToHex(steps) + " " + steps.length + " " + count);
                                } else {
                                    Log.e(TAG, "steps bad");
                                }
                            }
                        });
                    }
                }
            }
        });

        Button addSave = getView().findViewById(R.id.add_save);
        addSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputValidatorHelper inputValidatorHelper = new InputValidatorHelper();
                byte[] newGroup = new byte[groupSize];
                ByteBuffer newGroupBuffer = ByteBuffer.wrap(newGroup);

                String saveFileIdStr = editFileId.getText().toString();
                if (!inputValidatorHelper.isNullOrEmpty(saveFileIdStr)) {
                    if (saveFileIdStr.length() <= 4 && inputValidatorHelper.isValidFileId(saveFileIdStr)) {

                        byte[] saveFileIdBytes = saveFileIdStr.getBytes(Charset.forName("windows-1251"));
                        newGroupBuffer.put(saveFileIdBytes);
                        saveFileId = true;
                    } else {
                        saveFileId = false;
                        editFileId.setError("Формат группы должен быть типа \"EG**\"");
                    }
                } else {
                    saveFileId = false;
                    editFileId.setError("Поле не должно быть пустым");
                }

                String saveTitleStr = editTitle.getText().toString();
                if (!inputValidatorHelper.isNullOrEmpty(saveTitleStr)) {
                    if (saveTitleStr.length() <= 15) {

                        byte[] saveTitleBytes = saveTitleStr.getBytes(Charset.forName("windows-1251"));
                        ByteBuffer saveLenBuffer = ByteBuffer.allocate(4);
                        saveLenBuffer.putInt(saveTitleStr.length());
                        byte[] saveLenBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(saveLenBuffer.array()),0,1);
                        if (inputValidatorHelper.isValidTitle(saveTitleStr)) {
                            newGroupBuffer.put(saveLenBytes);
                            newGroupBuffer.put(saveTitleBytes, 0, saveTitleBytes.length);
                            byte[] nulls = new byte[15];
                            newGroupBuffer.put(nulls, 0, 15 - saveTitleBytes.length);
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

                String saveModeStr = spinnerMode.getSelectedItem().toString();
                int saveModeValue = Integer.parseInt(saveModeStr);
                byte saveModeValueByte = (byte) saveModeValue;
                newGroupBuffer.put(saveModeValueByte);

                String saveSpreStr = spinnerSpre.getSelectedItem().toString();
                int saveSpreValue = Integer.parseInt(saveSpreStr);
                byte saveSpreValueByte = (byte) saveSpreValue;
                newGroupBuffer.put(saveSpreValueByte);

                String saveMaxfStr = spinnerMaxf.getSelectedItem().toString();
                int saveMaxfValue = Integer.parseInt(saveMaxfStr);
                byte saveMaxfValueByte = (byte) saveMaxfValue;
                newGroupBuffer.put(saveMaxfValueByte);

                newGroupBuffer.put((byte) 0x00);

                String saveCountStr = editCount.getText().toString();
                if (inputValidatorHelper.isNumeric(saveCountStr)) {
                    if (!inputValidatorHelper.isNullOrEmpty(saveCountStr)) {
                        int saveCountValue = Integer.parseInt(saveCountStr);
                        ByteBuffer saveCountBuffer = ByteBuffer.allocate(4);
                        saveCountBuffer.putInt(saveCountValue);
                        byte[] saveCountBytes = Arrays.copyOfRange(groupHelper.reverseByteArray(saveCountBuffer.array()),0,1);
                        if (saveCountValue >= 0 && saveCountValue <= 80) {
                            newGroupBuffer.put(saveCountBytes);
                            saveStepCount = true;
                        } else {
                            saveStepCount = false;
                            editCount.setError("Значение должно быть числовым в диапазоне 0 - 80");
                        }
                    } else {
                        saveStepCount = false;
                        editCount.setError("Поле не должно быть пустым");
                    }
                } else {
                    saveStepCount = false;
                    editCount.setError("Значение должно быть числовым в диапазоне 0 - 80");
                }

                newGroupBuffer.put(steps);

                byte[] sum = Arrays.copyOfRange(newGroupBuffer.array(),4,507);
                byte[] crcByte = ByteBuffer.allocate(4).putInt(groupHelper.CRC32sum(sum)).array();
                byte[] revCrcByte = groupHelper.reverseByteArray(crcByte);
                newGroupBuffer.put(revCrcByte);

                byte exec = (byte) 0x00;
                newGroupBuffer.put(exec);

                if (saveFileId && saveTitle && saveTime && saveStepCount) {

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

                                    File file = new File(getContext().getFilesDir(), "groups.grf");
                                    if (!file.exists()) {
                                        byte[] nulls = new byte[51200];
                                        FileOutputStream fos = null;
                                        try {
                                            fos = new FileOutputStream(file);
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            fos.write(nulls);
                                            fos.flush();
                                            fos.close();

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

                                    } else {
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


                                    NavHostFragment.findNavController(AddGroupFragment.this)
                                            .navigate(R.id.action_AddGroupFragment_to_GroupFragment);

                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        });

        ImageButton qFileId = getView().findViewById(R.id.file_id_q);
        qFileId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.file_id_t);
                builder.setMessage(R.string.file_id_q)
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

        ImageButton qTitle = getView().findViewById(R.id.title_q);
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

        ImageButton qTime = getView().findViewById(R.id.time_q);
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

        ImageButton qMode = getView().findViewById(R.id.mode_q);
        qMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.mode_t);
                builder.setMessage(R.string.mode_q)
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

        ImageButton qSpre = getView().findViewById(R.id.spre_q);
        qSpre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.spre_t);
                builder.setMessage(R.string.spre_q)
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

        ImageButton qMaxf = getView().findViewById(R.id.maxf_q);
        qMaxf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.maxf_t);
                builder.setMessage(R.string.maxf_q)
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

        ImageButton qCnt = getView().findViewById(R.id.cnt_q);
        qCnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.cnt_t);
                builder.setMessage(R.string.cnt_q)
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
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
}

