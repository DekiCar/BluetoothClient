package com.example.dejan.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.XmlRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.LogRecord;

import javax.net.ssl.HttpsURLConnection;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private static final byte MAIN_SWITCH_CODE = 12;
    private static final byte R_CODE = 13;
    private static final byte G_CODE = 14;
    private static final byte B_CODE = 15;
    private static final byte PERSIENNES_CODE = 16;
    private static final byte FIRE_CODE = 17;
    private static final byte TEMPERATURE_CODE = 18;
    private static final byte HUMIDITY_CODE = 19;

    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter adapter;
    private ConnectionThread connection;

    private Switch mainLightSwitch;

    private SeekBar rBar;
    private SeekBar gBar;
    private SeekBar bBar;

    private SeekBar persiennesBar;

    private TextView temperatureText;
    private TextView humidityText;

    private Button fireButton;

    private int temperature;
    private int humidity;

    private String text;

    private EditText input1;
    private Button b1;

    private int mistake = 0;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(mistake == 0) {

            }else {
                String message = "";
                switch (mistake){
                    case 1:{
                        message = "konekcija 1";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case 2:{
                        message = "konektovanje";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case 3:{
                        message = "stremovi";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case 4:{
                        message = "konekcija uspesna";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case 5:{
                        break;
                    }
                    default:{
                        message = Integer.toString(mistake);
                    }
                }
            }
            temperatureText.setText(Integer.toString(temperature));
            humidityText.setText(Integer.toString(humidity));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = BluetoothAdapter.getDefaultAdapter();

        connection = null;

        temperature = 0;
        humidity = 0;

        mainLightSwitch = (Switch) findViewById(R.id.mainLightSwitch);

        rBar = (SeekBar) findViewById(R.id.rBar);
        gBar = (SeekBar) findViewById(R.id.gBar);
        bBar = (SeekBar) findViewById(R.id.bBar);

        persiennesBar = (SeekBar) findViewById(R.id.persiennesBar);

        temperatureText = (TextView) findViewById(R.id.temperatureText);
        humidityText = (TextView) findViewById(R.id.humidityText);

        fireButton = (Button) findViewById(R.id.fireButton);

        input1 = (EditText) findViewById(R.id.input1);
        b1 = (Button) findViewById(R.id.b1);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL(input1.getText().toString());
                            URLConnection connection = url.openConnection();
                            connection.connect();
                            mistake = 4;
                            handler.sendEmptyMessage(0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });

        setEvents();

        if(adapter.isEnabled()){
            final Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            String [] items = new String[pairedDevices.size()];

            int i = 0;
            for(BluetoothDevice d:pairedDevices){
                items[i] = d.getName();
                i++;
            }

            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    BluetoothDevice device = null;
                    int i = 0;
                    for(BluetoothDevice d:pairedDevices) {
                        if (i == which) {
                            device = d;
                        }
                        i++;
                    }
                    connection = new ConnectionThread();
                    try {
                        connection.connectTo(device, uuid, ConnectionThread.CONNECTION_INSECURE);
                        connection.setReceiver(new DataReceive() {
                            @Override
                            public void dataReceived(byte[] buffer) {

                                if(buffer.length == 4 && buffer[0] == TEMPERATURE_CODE && buffer[2] == HUMIDITY_CODE){
                                    temperature = buffer[1];
                                    humidity = buffer[3];
                                    handler.sendEmptyMessage(0);
                                }

                            }
                        });
                    }catch(Exception e){
                    }
                    connection.start();

                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }

    @Override
    protected void onStop() {
        if(connection != null) {
            connection.disconnect();
        }
        super.onStop();
    }

    private void setEvents(){

        mainLightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                byte value;

                if(mainLightSwitch.isChecked()){
                    value = (byte)255;
                }else{
                    value = 0;
                }

                if(connection != null) {
                    connection.writeFormat(MAIN_SWITCH_CODE, value);
                }
            }
        });

        rBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                byte value = (byte)((float)rBar.getProgress()*2.55f);

                if(connection != null) {
                    connection.writeFormat(R_CODE, value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        gBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                byte value = (byte)((float)gBar.getProgress()*2.55f);

                if(connection != null) {
                    connection.writeFormat(G_CODE, value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        bBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                byte value = (byte)((float)bBar.getProgress()*2.55f);

                if(connection != null) {
                    connection.writeFormat(B_CODE, value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        persiennesBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                byte value = (byte)((float)persiennesBar.getProgress()*2.55f);

                if(connection != null) {
                    connection.writeFormat(PERSIENNES_CODE, value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        fireButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                byte value = (byte)255;

                if(connection != null) {
                    connection.writeFormat(FIRE_CODE, value);
                }
            }
        });

    }


}
