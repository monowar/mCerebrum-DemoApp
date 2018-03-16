/*
 * Copyright (c) 2018, The University of Memphis, MD2K Center of Excellence
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.md2k.demoapp;

// Android imports
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

// Java imports
import java.util.ArrayList;

// DataKitAPI imports
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.source.application.Application;
import org.md2k.datakitapi.source.application.ApplicationBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Variables for accelerometer data
    private SensorManager mSensorManager;
    private Sensor mSensor = null;
    private long lastSaved;
    private double minSampleTime = 100; // 100 milliseconds
    public static final double GRAVITY = 9.81;

    // Variables for DataKit objects
    private DataKitAPI datakitapi;
    private DataSourceClient dataSourceClientRegister = null;
    private DataTypeDoubleArray dataInsert = null;
    private DataSourceClient dataSourceClientSubscribe = null;
    private DataTypeDoubleArray dataTypeSubscribe = null;
    private ArrayList<DataType> dataTypeQuery = null;
    private DataTypeDoubleArray dataTypeDoubleArray;

    // Variables for the user view
    private TextView conButton;
    private TextView regButton;
    private TextView subButton;
    private TextView insButton;
    private TextView output;
    private TextView insOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializes button variables
        conButton = findViewById(R.id.conButton);
        regButton = findViewById(R.id.regButton);
        subButton = findViewById(R.id.subButton);
        insButton = findViewById(R.id.insButton);
        output = findViewById(R.id.outputTextView);
        insOutput = findViewById(R.id.insertTextView);


        // Gets sensor service
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Sets the desired sensor
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lastSaved = DateTime.getDateTime();
    }

    public void connectButton (View view){
        datakitapi = datakitapi.getInstance(this);
        try {
            if (datakitapi.isConnected()) {
                unregisterListener();
                datakitapi.disconnect();
                dataSourceClientRegister = null;
                dataInsert = null;
                dataSourceClientSubscribe = null;
                dataTypeSubscribe = null;
                dataTypeQuery = null;
                printMessage("DataKit disconnected", output);
                conButton.setText(R.string.connect_button);
            } else
                datakitapi.connect(new OnConnectionListener() {
                @Override
                public void onConnected() {
                    printMessage("DataKit connected", output);
                    conButton.setText(R.string.disconnect_button);
                }
            });
        } catch (DataKitException ignored) {}
    }

    public void registerButton (View view){
        datakitapi = datakitapi.getInstance(this);
        try {
            if (!(datakitapi.isConnected())) {
                printMessage("DataKit is not connected", output);
            }
            else if (dataSourceClientRegister == null) {
                DataSourceBuilder dataSourceBuilder =
                        new DataSourceBuilder().setType(DataSourceType.ACCELEROMETER);
                dataSourceClientRegister = datakitapi.register(dataSourceBuilder);
                regButton.setText(R.string.unregister_button);
                printMessage(dataSourceClientRegister.getDataSource().getType() +
                        " registration successful", output);
            } else {
                unregisterListener();
                datakitapi.unregister(dataSourceClientRegister);
                dataSourceClientRegister = null;
                regButton.setText(R.string.register_button);
                printMessage("DataSource unregistered", output);
            }
        } catch (DataKitException ignored) {
            unregisterListener();
            dataSourceClientRegister = null;
            regButton.setText(R.string.register_button);
            printMessage(dataSourceClientRegister.getDataSource().getType() +
                    " registration failed", output);
        }
    }

    public void subscribeButton (View view){
        datakitapi = datakitapi.getInstance(this);
        try {
            if (dataSourceClientSubscribe == null) {
                Application application =
                        new ApplicationBuilder().setId(MainActivity.this.getPackageName()).build();
                DataSourceBuilder dataSourceBuilder =
                        new DataSourceBuilder().setType(DataSourceType.ACCELEROMETER).setApplication(application);
                ArrayList<DataSourceClient> dataSourceClients = datakitapi.find(dataSourceBuilder);
                if(dataSourceClients.size() == 0) {
                    printMessage("DataSource not registered yet", output);
                } else {
                    dataSourceClientSubscribe = dataSourceClients.get(0);
                    datakitapi.subscribe(dataSourceClientSubscribe, new OnReceiveListener() {
                        @Override
                        public void onReceived(DataType dataType) {
                            dataTypeSubscribe = (DataTypeDoubleArray) dataType;
                            double[] sample = dataTypeSubscribe.getSample();
                            printMessage("[" + sample[0] + ", " + sample[1] + ", " + sample[2] + "]", output);
                        }
                    });
                    subButton.setText(R.string.unsubscribe_button);
                    printMessage("DataSource subscribed", output);
                }
            } else {
                unsubscribeDataSource();
            }
        } catch (DataKitException ignored) {
            subButton.setText(R.string.subscribe_button);
            dataSourceClientSubscribe = null;
            dataTypeSubscribe = null;
        }
    }

    public void insertButton (View view){
        datakitapi = datakitapi.getInstance(this);
        if(dataSourceClientRegister != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            insButton.setText(R.string.inserting);
        } else {
            printMessage("DataSource not registered yet.", output);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long curTime = DateTime.getDateTime();
        if ((double)(curTime - lastSaved) > minSampleTime) {
            lastSaved = curTime;
            double[] samples = new double[3];
            samples[0] = event.values[0] / GRAVITY; // X axis
            samples[1] = event.values[1] / GRAVITY; // Y axis
            samples[2] = event.values[2] / GRAVITY; // Z axis
            dataTypeDoubleArray = new DataTypeDoubleArray(curTime, samples);
            insertData(dataTypeDoubleArray);
        }
    }

    public void insertData(DataTypeDoubleArray data) {
        try {
            datakitapi.insert(dataSourceClientRegister, data);
            double[] sample = data.getSample();
            printMessage("[" + sample[0] + ", " + sample[1] + ", " + sample[2] + "]", insOutput);
        } catch (DataKitException ignored){
            Log.e("database insert", ignored.getMessage());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int x) {}

    public void queryButton (View view){
        datakitapi = datakitapi.getInstance(this);
        unsubscribeDataSource();
        try {
            Application application =
                    new ApplicationBuilder().setId(MainActivity.this.getPackageName()).build();
            DataSourceBuilder dataSourceBuilder =
                    new DataSourceBuilder().setType(DataSourceType.ACCELEROMETER).setApplication(application);
            ArrayList<DataSourceClient> dataSourceClients = datakitapi.find(dataSourceBuilder);
            if(dataSourceClients.size() == 0) {
                printMessage("DataSource not registered yet.", output);
            } else {
                dataTypeQuery = datakitapi.query(dataSourceClients.get(0), 3);
                String message = "[X axis, Y axis, Z axis]\n";
                for (DataType data : dataTypeQuery) {
                    if (data instanceof DataTypeDoubleArray) {
                        DataTypeDoubleArray dataArray = (DataTypeDoubleArray)data;
                        double[] sample = dataArray.getSample();
                        message += "[" + sample[0] + ", " + sample[1] + ", " + sample[2] + "]\n";
                    }
                }
                printMessage(message, output);
            }
        } catch (DataKitException ignored) {
            dataTypeQuery = null;
        }
    }

    public void unregisterListener() {
        mSensorManager.unregisterListener(this, mSensor);
        insButton.setText(R.string.insert_button);
        insOutput.setText("");
    }

    public void unsubscribeDataSource() {
        try {
            datakitapi.unsubscribe(dataSourceClientSubscribe);
            dataSourceClientSubscribe = null;
            dataTypeSubscribe = null;
            subButton.setText(R.string.subscribe_button);
            printMessage("DataSource unsubscribed", output);
        } catch (DataKitException ignored) {}
    }

    public void printMessage (String message, TextView output) {
        output.setText(message);
    }
}
