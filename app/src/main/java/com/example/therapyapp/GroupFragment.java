package com.example.therapyapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.*;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.widget.Toast;
import androidx.navigation.fragment.NavHostFragment;
import com.example.therapyapp.databinding.FragmentGroupBinding;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GroupFragment extends Fragment {
    private static String TAG = "Data Fragment";
    private String connectedDeviceName = null;
    private BluetoothConnectHandler bluetoothConnectHandler = null;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private static final int SYS_NOP = 0x00;
    private static final int SYS_RESET = 0x01;
    private static final int SYS_PUT = 0x02;
    private static final int SYS_GET = 0x03;
    private static final int SYS_CANCEL = 0x04;

    private static final int SYS_STAT = 0x0;
    private static final int SYS_RUN = 0xFD;
    private static final int SYS_FILE = 0xFE;

    private static final int SYS_DATA = 0xFF;


    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    public ArrayList<ItemSubgroup> dataList = new ArrayList<>();;

    public List<String> groupList = new ArrayList<>();
    RecyclerView rvGroupList;
    GroupsRecyclerViewAdapter adapter;


    ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();

    private FragmentGroupBinding binding;

    // State variables
    private boolean paused = false;
    private boolean connected = false;

    private static final byte[] CRC8Table = new byte[]
            {
                    (byte)0, (byte)94, (byte)188, (byte)226, 97, 63, (byte)221, (byte)131, (byte)194, (byte)156, (byte)126, (byte)32, (byte)163, (byte)253, (byte)31, (byte)65,
                    (byte)157, (byte)195, (byte)33, (byte)127, (byte)252, (byte)162, (byte)64, (byte)30, (byte)95, (byte)1, (byte)227, (byte)189, (byte)62, (byte)96, (byte)130, (byte)220,
                    (byte)35, (byte)125, (byte)159, (byte)193, (byte)66, (byte)28, (byte)254, (byte)160, (byte)225, (byte)191, (byte)93, (byte)3, (byte)128, (byte)222, (byte)60, (byte)98,
                    (byte)190, (byte)224, (byte)2, (byte)92, (byte)223, (byte)129, (byte)99, (byte)61, (byte)124, (byte)34, (byte)192, (byte)158, (byte)29, (byte)67, (byte)161, (byte)255,
                    (byte)70, (byte)24, (byte)250, (byte)164, (byte)39, (byte)121, (byte)155, (byte)197, (byte)132, (byte)218, (byte)56, (byte)102, (byte)229, (byte)187, (byte)89, (byte)7,
                    (byte)219, (byte)133, (byte)103, (byte)57, (byte)186, (byte)228, (byte)6, (byte)88, (byte)25, (byte)71, (byte)165, (byte)251, (byte)120, (byte)38, (byte)196, (byte)154,
                    (byte)101, (byte)59, (byte)217, (byte)135, (byte)4, (byte)90, (byte)184, (byte)230, (byte)167, (byte)249, (byte)27, (byte)69, (byte)198, (byte)152, (byte)122, (byte)36,
                    (byte)248, (byte)166, (byte)68, (byte)26, (byte)153, (byte)199, (byte)37, (byte)123, (byte)58, (byte)100, (byte)134, (byte)216, (byte)91, (byte)5, (byte)231, (byte)185,
                    (byte)140, (byte)210, (byte)48, (byte)110, (byte)237, (byte)179, (byte)81, (byte)15, (byte)78, (byte)16, (byte)242, (byte)172, (byte)47, (byte)113, (byte)147, (byte)205,
                    (byte)17, (byte)79, (byte)173, (byte)243, (byte)112, (byte)46, (byte)204, (byte)146, (byte)211, (byte)141, (byte)111, (byte)49, (byte)178, (byte)236, (byte)14, (byte)80,
                    (byte)175, (byte)241, (byte)19, (byte)77, (byte)206, (byte)144, (byte)114, (byte)44, (byte)109, (byte)51, (byte)209, (byte)143, (byte)12, (byte)82, (byte)176, (byte)238,
                    (byte)50, (byte)108, (byte)142, (byte)208, (byte)83, (byte)13, (byte)239, (byte)177, (byte)240, (byte)174, (byte)76, (byte)18, (byte)145, (byte)207, (byte)45, (byte)115,
                    (byte)202, (byte)148, (byte)118, (byte)40, (byte)171, (byte)245, (byte)23, (byte)73, (byte)8, (byte)86, (byte)180, (byte)234, (byte)105, (byte)55, (byte)213, (byte)139,
                    (byte)87, (byte)9, (byte)235, (byte)181, (byte)54, (byte)104, (byte)138, (byte)212, (byte)149, (byte)203, (byte)41, (byte)119, (byte)244, (byte)170, (byte)72, (byte)22,
                    (byte)233, (byte)183, (byte)85, (byte)11, (byte)136, (byte)214, (byte)52, (byte)106, (byte)43, (byte)117, (byte)151, (byte)201, (byte)74, (byte)20, (byte)246, (byte)168,
                    (byte)116, (byte)42, (byte)200, (byte)150, (byte)21, (byte)75, (byte)169, (byte)247, (byte)182, (byte)232, (byte)10, (byte)84, (byte)215, (byte)137, (byte)107, (byte)53
            };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvGroupList = getView().findViewById(R.id.groups_list);
        rvGroupList.setLayoutManager(new LinearLayoutManager(getActivity()));

        groupList = new ArrayList<>();
        int groupSize = 512;
        byte[] readGroup = readFromFile();

        for (int i = 0; i < readGroup.length / groupSize; i++) {
            byte[] group = Arrays.copyOfRange(readGroup, groupSize * i, groupSize + i * groupSize);
            ByteBuffer groupBuffer = ByteBuffer.wrap(group);

            byte[] file_id = new byte[4];
            groupBuffer.get(file_id,0,file_id.length);

            byte[] len = new byte[1];
            groupBuffer.get(len);
            if (len[0] != 0x00) {
                byte[] title = new byte[15];
                groupBuffer.get(title, 0, title.length);
                byte[] offTitle = Arrays.copyOfRange(title, 0, len[0]);
                String strTitleWin = new String(offTitle, Charset.forName("windows-1251"));
                groupList.add(strTitleWin);
                System.out.println("gr " + i + " " + strTitleWin + " " + bytesToHex(len));

            }
        }

        adapter = new GroupsRecyclerViewAdapter(getActivity(),groupList);
        rvGroupList.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        adapter.setOnGroupClickListener(new GroupsRecyclerViewAdapter.GroupClickListener() {
            @Override
            public void onGroupClick(int position, View itemView) {
                Log.e(TAG, String.valueOf(position));
                Bundle bundle = new Bundle();
                bundle.putInt("group_position", position);
                NavHostFragment.findNavController(GroupFragment.this)
                        .navigate(R.id.action_GroupFragment_to_EditGroupFragment, bundle);
            }
        });


        binding.addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int groupCount = groupList.size();
                Bundle bundle = new Bundle();
                bundle.putInt("group_count", groupCount);
                NavHostFragment.findNavController(GroupFragment.this)
                        .navigate(R.id.action_GroupFragment_to_AddGroupFragment, bundle);
            }
        });

        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] groups = readFromFile();
//                System.out.println(" " + groups.length);
                commandsHandler.obtainMessage(SYS_PUT, groups.length,-1, groups).sendToTarget();
            }
        });

        binding.receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commandsHandler.obtainMessage(SYS_GET).sendToTarget();
            }
        });

        binding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commandsHandler.obtainMessage(SYS_CANCEL).sendToTarget();
            }
        });

        binding.resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commandsHandler.obtainMessage(SYS_RESET).sendToTarget();
            }
        });

        binding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeToFile(getEPT(dataBuffer.toByteArray()));

                groupList = new ArrayList<>();
                int groupSize = 512;
                byte[] readGroup = readFromFile();

                for (int i = 0; i < readGroup.length / groupSize; i++) {
                    byte[] group = Arrays.copyOfRange(readGroup, groupSize * i, groupSize + i * groupSize);
                    ByteBuffer groupBuffer = ByteBuffer.wrap(group);

                    byte[] file_id = new byte[4];
                    groupBuffer.get(file_id,0,file_id.length);

                    byte[] len = new byte[1];
                    groupBuffer.get(len);
                    if (len[0] != 0x00) {
                        byte[] title = new byte[15];
                        groupBuffer.get(title, 0, title.length);
                        byte[] offTitle = Arrays.copyOfRange(title, 0, len[0]);
                        String strTitleWin = new String(offTitle, Charset.forName("windows-1251"));
                        groupList.add(strTitleWin);
                        System.out.println("gr " + i + " " + strTitleWin + " " + bytesToHex(len));

                    }
                }
                adapter = new GroupsRecyclerViewAdapter(getActivity(),groupList);
                rvGroupList.setAdapter(adapter);
                adapter.notifyDataSetChanged();
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
        BluetoothManager bluetoothManager = (BluetoothManager) Objects.requireNonNull(getActivity()).getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null) {
            bluetoothConnectHandler = new BluetoothConnectHandler(bluetoothHandler, bluetoothAdapter);
            String address = ((Globals) getActivity().getApplication()).getGlobalDeviceAddress();
            System.out.println(address);
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            bluetoothConnectHandler.connect(bluetoothDevice);
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothConnectHandler != null)
        {
            bluetoothConnectHandler.stop();
        }
        binding = null;
    }


    private byte CRC8sum(byte[] buffer, int len) {
        byte crc = 0;
        for (int i = 0; i < len; i++) {
            crc = CRC8Table[(crc ^ buffer[i]) & 0xFF];
        }
        return crc;
    }


    public static String bytesToHex(byte[] byteArray)
    {
        String hex = "";

        // Iterating through each byte in the array
        for (byte i : byteArray) {
            hex += String.format("%02X", i);
            hex += " ";
        }

        return hex;
    }
    // Функция отправки сообщения
    private void sendMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (bluetoothConnectHandler.getState() != BluetoothConnectHandler.STATE_CONNECTED) {
            Toast.makeText(getActivity(), "BT NOT CON", Toast.LENGTH_SHORT).show();
            return;
        }
        if(message.length > 0) {
            bluetoothConnectHandler.write(message);
        }
    }


    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            commandsHandler.obtainMessage(SYS_NOP).sendToTarget();
        }
    };


    private byte[] getEPT(byte[] ept) {
        byte[] groupData = new byte[51200];
        ByteBuffer groupDataBuffer = ByteBuffer.wrap(groupData);

        int blockSize = 11;
        int blockCount = ept.length / blockSize;

        byte[] range = null;

        for (int i = 0; i < blockCount; i++) {
            int idx = i * blockSize;
            range = Arrays.copyOfRange(ept, idx, idx + blockSize);
            //           System.out.println("Chunk " + i + ": " + bytesToHex(range));
            byte[] result = new byte[10];
            switch (range[1]) {
                case (byte) SYS_STAT:
                    result = Arrays.copyOfRange(range,0,10);
                    if (CRC8sum(result, result.length) == range[10]) {
//                        Log.e(TAG, "SYS_STAT " + bytesToHex(range));
                    }
                    break;
                case (byte) SYS_RUN:
                    result = Arrays.copyOfRange(range,0,10);
                    if (CRC8sum(result, result.length) == range[10]) {
                        Log.e(TAG, "SYS_RUN " + bytesToHex(range));
                    }

                    break;
                case (byte) SYS_FILE:
                    result = Arrays.copyOfRange(range,0,10);
                    if (CRC8sum(result, result.length) == range[10]) {
                        Log.e(TAG, "SYS_FILE " + bytesToHex(range));
                    }
                    break;
                case (byte) SYS_DATA: result = Arrays.copyOfRange(range,0,10);
                    if (CRC8sum(result, result.length) == range[10]) {
                        Log.e(TAG, "SYS_DATA " + bytesToHex(range));
                        byte[] data = Arrays.copyOfRange(range,2,10);
                        groupDataBuffer.put(data);
                    }
                    break;
                default: break;
            }
        }
        return groupData;
    }

    private byte[] genCommand(byte[] data) {
        byte[] command = new byte[11];
        byte header = 0x01;
        ByteBuffer bb = ByteBuffer.allocate(10);
        bb.put(header);
        bb.put(data);
        byte checkSum = CRC8sum(bb.array(), bb.array().length);
        ByteBuffer commandBuffer = ByteBuffer.wrap(command);
        commandBuffer.put(bb.array());
        commandBuffer.put(checkSum);
        return commandBuffer.array();
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

    private final Handler commandsHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            byte[] data = new byte[9];
            byte[] result = new byte[9];
            switch (message.what) {
                case SYS_NOP:
                    data[0] = SYS_NOP;
                    result = genCommand(data);
                    sendMessage(result);
                    break;
                case SYS_RESET:
                    data[0] = SYS_RESET;
                    result = genCommand(data);
                    sendMessage(result);
                    Log.e(TAG, "SYS_RESET " + bytesToHex(result));
                    break;
                case SYS_PUT:
                    byte[] putBuffer = (byte[]) message.obj;
                    int putLength = message.arg1;

                    data[0] = SYS_PUT;
                    ByteBuffer putDataBuffer = ByteBuffer.allocate(9);
                    putDataBuffer.put(data[0]);
                    putDataBuffer.put("EG**".getBytes(StandardCharsets.UTF_8));
                    byte[] lenByte = ByteBuffer.allocate(4).putInt(putLength).array();
                    byte[] revLenByte = new byte[4];
                    for(int i = 0; i < lenByte.length; i++) {
                        revLenByte[i] = lenByte[lenByte.length - 1 - i];
                    }
                    putDataBuffer.put(revLenByte);
                    data = putDataBuffer.array();
                    result = genCommand(data);
                    sendMessage(result);
                    Log.e(TAG, "SYS_PUT " + bytesToHex(result));

                    int blockSize = 8;
                    int blockCount = putBuffer.length / blockSize;
                    byte[] range = null;
                    for (int i = 0; i < blockCount; i++) {
                        int idx = i * blockSize;
                        range = Arrays.copyOfRange(putBuffer, idx, idx + blockSize);
                        commandsHandler.obtainMessage(SYS_DATA, range).sendToTarget();
                    }
                    break;
                case SYS_GET:
                    data[0] = SYS_GET;
                    ByteBuffer getBuffer = ByteBuffer.allocate(9);
                    getBuffer.put(data[0]);
                    getBuffer.put("EG**".getBytes(StandardCharsets.UTF_8));
                    byte[] nulls = new byte[4];
                    getBuffer.put(nulls);

                    data = getBuffer.array();
                    result = genCommand(data);
                    sendMessage(result);

                    Log.e(TAG, "SYS_GET " + bytesToHex(result));
                    break;
                case SYS_CANCEL:
                    data[0] = SYS_CANCEL;

                    result = genCommand(data);
                    sendMessage(result);
                    Log.e(TAG, "SYS_CANCEL"  + bytesToHex(result));
                    break;
                case SYS_DATA:
                    data[0] = (byte) SYS_DATA;
                    byte[] sys_data = (byte[]) message.obj;
                    ByteBuffer dataBuffer = ByteBuffer.allocate(9);
                    dataBuffer.put(data[0]);
                    dataBuffer.put(sys_data);
                    data = dataBuffer.array();
                    result = genCommand(data);
                    sendMessage(result);
                    Log.e(TAG, "SYS_DATA "  + bytesToHex(result));
                    break;
            }
            return false;
        }
    });



    private final Handler bluetoothHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (message.arg1) {
                        case BluetoothConnectHandler.STATE_CONNECTED:
                            connected = true;
                            Log.e(TAG, "STATE_CONNECTED");
                            if (connected) {
//                                commandsHandler.obtainMessage(SYS_RESET).sendToTarget();
                                timer.scheduleAtFixedRate(timerTask,100,400);
                            }

                            //                        mTitle.setText(mConnectedDeviceName);
                            break;
                        case BluetoothConnectHandler.STATE_CONNECTING:
                            Log.e(TAG, "STATE_CONNECTING");
                            //                        mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothConnectHandler.STATE_NONE:
                            if (timer != null) {
                                timer.cancel();
                                timer = null;
                            }
                            connected = false;
                            Log.e(TAG, "STATE_NONE");
                            //                        mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    //				onBluetoothStateChanged();
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) message.obj;
                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
//                    Log.e(TAG + " MESSAGE WRITE",Arrays.toString(writeBuf));
                    break;
                case MESSAGE_READ:
                    if (paused) break;
                    byte[] readBuf = (byte[]) message.obj;
                    if (readBuf.length > 22) {
                        try {
                            dataBuffer.write(readBuf);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("READ  " + bytesToHex(readBuf) + " len  " +readBuf.length);
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    connectedDeviceName = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(getContext(), "Connected to "
                            + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getContext(), message.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

}