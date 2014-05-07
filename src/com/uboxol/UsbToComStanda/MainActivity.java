package com.uboxol.UsbToComStanda;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.uboxol.serialcomport.SerialComPortControl;
import com.uboxol.serialcomport.WriteSerialDataException;

import static com.uboxol.serialcomport.SerialComPort.*;

public class MainActivity extends Activity {

    private int bitRate = 9600;    /* 110 / 300 / 9600/ 115200 ...  */
    private STOP_BITS stopBits = STOP_BITS.BIT_1;   /* 0=1 stop bit, 1=1.5 stop bit, 2=2 stop bit;  */
    private DATA_BITS dataType = DATA_BITS.BIT_8;   /* 8:8bit, 7: 7bit */
    private PARITY parityType = PARITY.NONE; /* 0: none, 1: odd, 2: even */
    private COM_ID com = COM_ID.COM_1;       /* 串口编号:1~5 代表串口1到串口5)*/

    private COM_ID currCom;

    private  int writeSize = 0;
    private int readSize = 0;

    private Button configButton = null;
    private  TextView readTextView = null,WriteBytes = null,ReadBytes = null;
    private EditText writeEditText = null;

    private SerialComPortControl serialComPortControl;

    private ReadMessageThread readThread = null;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        currCom = com = COM_ID.COM_1;
        try {

            /********   初始化UI    *************/

             /* setup the baud rate list */
            Spinner bitRateSpinner = (Spinner) findViewById(R.id.baudRateValue);
            ArrayAdapter<CharSequence> baudAdapter = ArrayAdapter.createFromResource(this, R.array.baud_rate,
                    R.layout.my_spinner_textview);
            baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
            bitRateSpinner.setAdapter(baudAdapter);
            bitRateSpinner.setGravity(0x10);
            bitRateSpinner.setSelection(3);

            bitRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                    if(parent != null && parent.getItemAtPosition(pos) != null){

                        String bitRateString = parent.getItemAtPosition(pos).toString();
                        if ( bitRateString != null) {
                            bitRate = Integer.parseInt(bitRateString);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });


		    /* setup stop bits */
            Spinner stopBitsSpinner = (Spinner) findViewById(R.id.stopBitValue);
            ArrayAdapter<CharSequence> stopAdapter = ArrayAdapter.createFromResource(this, R.array.stop_bits,
                    R.layout.my_spinner_textview);
            stopAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
            stopBitsSpinner.setAdapter(stopAdapter);
            stopBitsSpinner.setGravity(0x01);
            stopBitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                    String s_stopBit = parent.getItemAtPosition(pos).toString();
                    Log.i("stopBitsSpinner select ", s_stopBit);
                    if (s_stopBit.equals("1")) {
                        stopBits = STOP_BITS.BIT_1;
                    } else if (s_stopBit.equals("1.5")) {
                        stopBits = STOP_BITS.BIT_1_5;
                    } else if (s_stopBit.equals("2")) {
                        stopBits = STOP_BITS.BIT_2;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

		    /* data bits */
            Spinner dataTypeSpinner = (Spinner) findViewById(R.id.dataBitValue);
            ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this, R.array.data_bits,
                    R.layout.my_spinner_textview);
            dataAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
            dataTypeSpinner.setAdapter(dataAdapter);
            dataTypeSpinner.setSelection(1);

            dataTypeSpinner.setGravity(0x11);
            dataTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                    String s_dataType = parent.getItemAtPosition(pos).toString();
                    Log.i("dataTypeSpinner select", s_dataType);

                    if ("7".equals(s_dataType)) {
                        dataType = DATA_BITS.BIT_7;
                    } else if ("8".equals(s_dataType)) {
                        dataType = DATA_BITS.BIT_8;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

		    /* parity */
            Spinner parityTypeSpinner = (Spinner) findViewById(R.id.parityValue);
            ArrayAdapter<CharSequence> parityAdapter = ArrayAdapter.createFromResource(this, R.array.parity,
                    R.layout.my_spinner_textview);
            parityAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
            parityTypeSpinner.setAdapter(parityAdapter);
            parityTypeSpinner.setGravity(0x11);
            parityTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {


                    String parityString = parent.getItemAtPosition(pos).toString();
                    Log.i("parityTypeSpinner select ", parityString);
                    if (parityString.compareTo("None") == 0) {
                        parityType = PARITY.NONE;
                    }

                    if (parityString.compareTo("Odd") == 0) {
                        parityType = PARITY.ODD;
                    }

                    if (parityString.compareTo("Even") == 0) {
                        parityType = PARITY.EVEN;
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

		       /* com port to pen */
            Spinner comSpinner = (Spinner) findViewById(R.id.flowControlValue);
            ArrayAdapter<CharSequence> flowAdapter = ArrayAdapter.createFromResource(this, R.array.flow_control,
                    R.layout.my_spinner_textview);
            flowAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
            comSpinner.setAdapter(flowAdapter);
            comSpinner.setGravity(0x11);
            comSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                    String comString =  parent.getItemAtPosition(pos).toString() ;
                    Log.i("comSpinner Select", comString);
                    if (comString.compareTo("COM1") == 0) {
                        com = COM_ID.COM_1;
                    }

                    if (comString.compareTo("COM2") == 0) {
                        com = COM_ID.COM_2;
                    }

                    if (comString.compareTo("COM3") == 0) {
                        com = COM_ID.COM_3;
                    }

                    if (comString.compareTo("COM4") == 0) {
                        com = COM_ID.COM_4;
                    }

                    if (comString.compareTo("COM5") == 0) {
                        com = COM_ID.COM_5;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            /**
             * UI init
             */
            configButton = (Button) findViewById(R.id.configButton);
            Button writeButton = (Button) findViewById(R.id.WriteButton);
            readTextView = (TextView) findViewById(R.id.readTextView);
            readTextView.setText("");

            WriteBytes = (TextView) findViewById(R.id.WriteBytes);
            ReadBytes = (TextView) findViewById(R.id.ReadBytes);
            writeEditText = (EditText) findViewById(R.id.WriteValues);

            configButton.setText("打开");
            configButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    try {
                        //先停止接收数据
                        if (readThread != null) {
                            readThread.cancel();
                            readThread = null;
                        }
                        configButton.setText("打开");


                        if (serialComPortControl.status(currCom).equals(SerialComPortStatus.CONNECTED))
                        {
                            serialComPortControl.close(currCom);
                            readTextView.setText(readTextView.getText().toString() + "已关闭串口\n");
                        }
                        else
                        {
                            switch ( serialComPortControl.open( com, bitRate, dataType, stopBits, parityType)) {
                                case DEVICE_NO_PERMISSION: {
                                    readTextView.setText(readTextView.getText().toString() + "设备没有权限...\n");
                                    break;
                                }
                                case CONNECTED: {
                                    readTextView.setText(readTextView.getText().toString() + "打开串口成功\n");

                                    //开启接受数据线程
                                    readThread = new ReadMessageThread();
                                    readThread.start();
                                    configButton.setText("关闭");
                                    break;
                                }
                                case NOT_CONNECT: {
                                    readTextView.setText(readTextView.getText().toString() + "打开串口失败\n");
                                    break;
                                }
                                case DEVICE_NOT_CONNECT: {
                                    readTextView.setText(readTextView.getText().toString() + "设备没有连接\n");
                                    break;
                                }
                                default:{
                                    readTextView.setText(readTextView.getText().toString() + "打开串口失败\n");
                                    break;
                                }
                            }

                        }
                        currCom = com;  //更新当前操作 com id
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
            });
            /**
             * 监听 ENTER键
             */
            writeEditText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        sendMessage();
                    }
                    return false;
                }
            });
            writeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    sendMessage();
                }
            });

            serialComPortControl = new SerialComPortControl(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    public void sendMessage()
    {
        String message = writeEditText.getText().toString();
        if(message.length() > 0){
            switch (serialComPortControl.status(com))
            {
                case CONNECTED:{



                    try {
                        serialComPortControl.send(com, message.getBytes(), message.getBytes().length);
                    } catch (WriteSerialDataException e) {
                        e.printStackTrace();
                    }
                    writeSize += message.getBytes().length;
                    WriteBytes.setText("send\n(" + writeSize + ")");

                    break;
                }
                case NOT_CONNECT:
                case DEVICE_NO_PERMISSION:
                case DEVICE_NOT_CONNECT:{
                    readTextView.setText(readTextView.getText().toString() + com +" 未打开\n");
                    break;
                }
            }
        }
    }

    /**
     * UI线程接收消息
     */
    Handler messageHandler = new Handler(){
        @Override
        public void dispatchMessage(Message msg) {

            switch (msg.what)
            {
                case 0: {
                    if (msg.obj instanceof byte[]) {
                        byte[] data = (byte[]) msg.obj;
                        readTextView.setText(readTextView.getText().toString() + "receive: " + new String(data) + "\n");
                        readSize += msg.arg1;           //data length
                        ReadBytes.setText("read\n(" + readSize + ")");
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    class ReadMessageThread extends Thread {
        private boolean isRunning;
        ReadMessageThread()
        {
            isRunning = true;
        }

        @Override
        public void run() {
            while (isRunning && serialComPortControl.status(com).equals(SerialComPortStatus.CONNECTED))
            {
                try {
                    byte[] data = new byte[100];
                    int length = serialComPortControl.read( com, data, 100, 100);
                    if (length > 0)
                    {
                        Message msg = new Message();
                        msg.what = 0;
                        msg.obj = data;
                        msg.arg1 = length;
                        messageHandler.sendMessage(msg);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
            isRunning = false;
        }

        public void cancel()
        {
            isRunning = false;
        }
    }

    @Override
    protected void onDestroy() {
        // 关闭读消息的线程和串口
        if(readThread != null)
        {
            readThread.cancel();
            readThread = null;
        }
        serialComPortControl.close(com);
        super.onDestroy();
    }
}
