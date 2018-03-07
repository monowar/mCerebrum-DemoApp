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
import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

// Java imports
import java.util.ArrayList;
import java.util.Random;

// DataKitAPI imports
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.source.application.Application;
import org.md2k.datakitapi.source.application.ApplicationBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;

public class MainActivity extends AppCompatActivity {

    public static final String INTENT_MESSAGE = "com.md2korg.demoapp.MESSAGE";
    final String CONNECT_MESSAGE = "Connected to DataKitAPI";
    DataKitAPI datakitapi;
    DataSourceClient dataSourceClientRegister = null;
    DataTypeInt dataTypeIntInsert = null;
    DataSourceClient dataSourceClientSubscribe = null;
    DataTypeInt dataTypeIntSubscribe = null;
    ArrayList<DataType> dataTypeIntQuery = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void connectButton (View view){
        datakitapi = datakitapi.getInstance(this);
        try {
            if (datakitapi.isConnected()) {
                datakitapi.disconnect();
                dataSourceClientRegister = null;
                dataTypeIntInsert = null;
                dataSourceClientSubscribe = null;
                dataTypeIntSubscribe = null;
                dataTypeIntQuery = null;
                printMessage("DataKit disconnected");
            } else {
                datakitapi.connect(new OnConnectionListener() {
                    @Override
                    public void onConnected() {
                        printMessage(CONNECT_MESSAGE);
                    }
                });
            }
        } catch (DataKitException ignored) {}
    }

    public void registerButton (View view){
        datakitapi = datakitapi.getInstance(this);
        try {
            if (dataSourceClientRegister == null) {
                DataSourceBuilder dataSourceBuilder =
                        new DataSourceBuilder().setType(DataSourceType.STATUS);
                dataSourceClientRegister = datakitapi.register(dataSourceBuilder);
            } else {
                datakitapi.unregister(dataSourceClientRegister);
                dataSourceClientRegister = null;
                printMessage("DataSource unregistered");
            }
        } catch (DataKitException ignored) {
            dataSourceClientRegister = null;
            printMessage("Registration failed");
        }
        if (dataSourceClientRegister != null) {
            printMessage("Registration successful");
        }
    }

    public void insertButton (View view){
        datakitapi = datakitapi.getInstance(this);
        try {
            if(dataSourceClientRegister != null) {
                dataTypeIntInsert = new DataTypeInt(DateTime.getDateTime(),
                        Math.abs(new Random().nextInt() % 10000));
                datakitapi.insert(dataSourceClientRegister, dataTypeIntInsert);
                printMessage("Insertion successful");
            } else {
                printMessage("DataSource not registered yet.");
            }
        } catch (DataKitException ignored) {
            dataTypeIntInsert = null;
            printMessage("Insertion failed");
        }
    }

    public void subscribeButton (View view){
        datakitapi = datakitapi.getInstance(this);
        try {
            if (dataSourceClientSubscribe == null) {
                Application application =
                        new ApplicationBuilder().setId(MainActivity.this.getPackageName()).build();
                DataSourceBuilder dataSourceBuilder =
                        new DataSourceBuilder().setType(DataSourceType.STATUS).setApplication(application);
                ArrayList<DataSourceClient> dataSourceClients = datakitapi.find(dataSourceBuilder);
                if(dataSourceClients.size() == 0) {
                    printMessage("DataSource not registered yet");
                } else {
                    dataSourceClientSubscribe = dataSourceClients.get(0);
                    datakitapi.subscribe(dataSourceClientSubscribe, new OnReceiveListener() {
                        @Override
                        public void onReceived(DataType dataType) {
                            dataTypeIntSubscribe = (DataTypeInt) dataType;
                            printMessage(dataTypeIntSubscribe.toString());
                        }
                    });
                    printMessage("DataSource subscribed");
                }
            } else {
                datakitapi.unsubscribe(dataSourceClientSubscribe);
                dataSourceClientSubscribe = null;
                dataTypeIntSubscribe = null;
                printMessage("DataSource unsubscribed");
            }
        } catch (DataKitException ignored) {
            dataSourceClientSubscribe = null;
            dataTypeIntSubscribe = null;
        }
    }

    public void queryButton (View view){
        datakitapi = datakitapi.getInstance(this);
        try {
            Application application =
                    new ApplicationBuilder().setId(MainActivity.this.getPackageName()).build();
            DataSourceBuilder dataSourceBuilder =
                    new DataSourceBuilder().setType(DataSourceType.STATUS).setApplication(application);
            ArrayList<DataSourceClient> dataSourceClients = datakitapi.find(dataSourceBuilder);
            if(dataSourceClients.size() == 0) {
                printMessage("DataSource not registered yet.");
            } else {
                dataTypeIntQuery = datakitapi.query(dataSourceClients.get(0), 3);
                printMessage(dataTypeIntQuery.toString());
            }
        } catch (DataKitException ignored) {
            dataTypeIntQuery = null;
        }
    }

    public void printMessage (String message) {
        TextView textView = findViewById(R.id.outputTextView);
        textView.setText(message);
    }

}