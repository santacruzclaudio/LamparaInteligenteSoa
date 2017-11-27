package com.lampara.soa.csantac.lamparaprueba;
/**
 * Created by csantac on 15/11/2017.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;


public class Control_BT extends AppCompatActivity {
    // Lista de dispositivos Bluetooth
    ListView devicelist;
    //Bluetooth

    // Declaramos las clases de Bluetooth para utilizar en la selección del dispositivo
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Mapeo de views con objetos button y devicelist
        final Button btnPaired = (Button)findViewById(R.id.btnpaired);
        devicelist = (ListView)findViewById(R.id.pairedlist);
        Toast.makeText(getApplicationContext(), "Dispositivo bluetooth no disponible", Toast.LENGTH_LONG).show();

        //Si el dispositivo es Bluetooth
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if(myBluetooth == null)
        {
            //Muestro un mensaje, que el dispositivo no está disponible
            Toast.makeText(getApplicationContext(), "Dispositivo bluetooth no disponible", Toast.LENGTH_LONG).show();

            //Termino aplicación
                finish();
        }
        else if(!myBluetooth.isEnabled())
        {
            //Mando al usuario a habilitar el bluetooth, por medio de un intent
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);
        }

        //Seteo que al clickear el boton de dispostivos, llame a la función que muestra los dispositivos vinculados
        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                pairedDevicesList();
            }
        });

    }

    private void pairedDevicesList()
    {
        // Arma una lista de dispositivos vinculados
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        // Si hay dispositivos, los va agregando a la lista, sino muestra un mensaje, que no encuentra dispositivos
        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the devices name and the address
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No se encontraron dispositivos vinculados.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        //Método que se llama cuando se clickea un dispositivo vinculado de la lista
        devicelist.setOnItemClickListener(myListClickListener);

    }

    public AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            // Devuelve la MAC Address del dispositivo, los ultimos 17 caracteres en el view
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Armo el intent para pasar a la próxima activity
            Intent i = new Intent(Control_BT.this, MainActivity.class);

            //Cambio la activity.
            i.putExtra(EXTRA_ADDRESS, address); //Esto será recibido por el Control_Main activity
            startActivity(i);
        }
    };


}
