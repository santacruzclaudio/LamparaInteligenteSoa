package com.lampara.soa.csantac.lamparaprueba;

import android.support.v7.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.AsyncTask;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements  SensorEventListener {
    // Declaro las variables y objetos a utilizar
    String address = null;
    // Barra de progreso
    private ProgressDialog progress;
    // Debugging
    public static final boolean log = true;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Flag que indica si el bluetooth está conectado
    private boolean isBtConnected = false;
    public int Opcion=R.menu.activity_main;
    private boolean estadoLampara=true;

    // Adaptador local Bluetooth
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    LeerYEscribirBT LeeyEscribeBT;

    // Variables para el uso del acelerómetro
    float curX = 0, curY = 0, curZ = 0;
    float prevX = 0, prevY = 0, prevZ = 0;
    long last_update = 0, last_movement = 0;
    // Incializo el SensorManager
    private SensorManager msensorManager;
    // Declaro y registro el sensor de acelerómetro
    private Sensor sensorAcelerometro;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_luz );

        // Ejecuta la conexión al bluetooth
        // Obtengo el intent enviado por el ControlBT activity
        Intent newint = getIntent();
        address = newint.getStringExtra(Control_BT.EXTRA_ADDRESS); //Recibo la dirección del dispositivo bluetooth
        msensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorAcelerometro = msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msensorManager.registerListener( this, sensorAcelerometro,SensorManager.SENSOR_DELAY_UI);

        // Ejecuta la conexión al bluetooth
        new ConnectBT().execute();
        // Ejecuta el thread que lee y escribe el bluetooth
        LeerYEscribirBT LeeyEscribeBT = new LeerYEscribirBT();
        LeeyEscribeBT.execute();

        final ToggleButton BotonLed = (ToggleButton)findViewById(R.id.Led1);
        BotonLed.setOnClickListener(new View.OnClickListener() {
            public void onClick(View vv) {
                if (estadoLampara)
                    SendBt("B");
                else
                    SendBt("A");

            }
        });//fin de metodo de BotonLed

    }

    private void SendBt(String s)
    {   // Si el socket está conectado escribe el output del socket del bluetooth
        if (btSocket!=null)
        {   try
            {
                if(s.contains("B"))     {
                    Log.d("SendBT", "Prendioooooooooo");
                    findViewById(R.id.Led1).setBackgroundResource(R.drawable.luzon);
                    estadoLampara=false;
                }
                else {
                    Log.d("SendBT", "Se Apagooo");
                    findViewById(R.id.Led1).setBackgroundResource(R.drawable.luzoff );
                    estadoLampara=true;
                }
                btSocket.getOutputStream().write(s.getBytes());

            }
            catch (IOException e)
            {
                //msg("Error");
            }
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            Log.d("ConnectBT","onPreExecute");
            // Envío mensaje de please wait.
            progress = ProgressDialog.show(MainActivity.this, "Conectando...", "Por favor, espere!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {   try
            {
                // Si el socket es nulo y no está conectado
                if (btSocket == null || !isBtConnected)
                {   // Comienzo la rutina de conexión
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//obtengo el dispositivo movil bluetooth
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//conecto a la dirección del dispositivo y chequeo si está disponible
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//creo el socket
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//comienzo la conexión
                }
            }
            catch (IOException e)
            {   msg("Error en BT");
                ConnectSuccess = false;//Si el try falla, podemos chequear la excepción acá
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result)
        {   Log.d("ConnectBT","onPostExecute");
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                // Si no puede conectar, avisa con un mensaje y cierra el activity y vuelve al Device_List
                msg("Fallo la Conexión. Intente nuevamente.");
                finish();
            }
            else
            {
                // Sino, avisa que se ha conectado
                msg("Conectado");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
    private class LeerYEscribirBT extends AsyncTask<Void, String, Void>
    {
        private boolean LeeYEscribe = true;


        @Override
        protected Void doInBackground(Void... devices) // Se ejecuta todo el tiempo.
        {
            try
            {
                byte[] buffer = new byte[1024];
                int bytes = 0;
                int begin = 0;

                // Recibe los valores de arduino todo el tiempo, hasta que termine la aplicación
                while(true) {
                    // Leo el inputstram del Bluetooth
                    bytes += btSocket.getInputStream().read(buffer, bytes, buffer.length - bytes);
                    // Convierto a string los datos recibidos
                    String strReceived = new String(buffer, 0, bytes);
                    Log.d("Recibido BT",strReceived);
                    if (buffer[bytes - 1] == '~') {
                        // Publico el progreso
                        publishProgress(strReceived);
                        // Reinicio el buffer
                        buffer = new byte[1024];
                        bytes = 0;

                    }
                }
            }
            catch (IOException e)
            {
                LeeYEscribe = false;//Si el try falla, podemos tratarlo aquí
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(String... values) {
            if(values[0].substring(0,1).contains("B"))
                SendBt(values[0].substring(0,1));
            if(values[0].substring(0,1).contains("A"))
                SendBt(values[0].substring(0,1));
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if (!LeeYEscribe)
            {
                msg("Se dejo de leer y escribir el BT");
                finish();
            }
            progress.dismiss();
        }
    }

    // Mensaje en Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    public void onAccuracyChanged(Sensor sensor, int precision) {}

    public void onSensorChanged(SensorEvent evento) {
        synchronized (this) {
            long current_time = System.currentTimeMillis();
            curX = evento.values[0];
            curY = evento.values[1];
            curZ = evento.values[2];
            // Calculo de movimiento del acelerómetro
            if (prevX == 0 && prevY == 0 && prevZ == 0) {
                last_update = current_time;
                last_movement = current_time;
                prevX = curX;
                prevY = curY;
                prevZ = curZ;
            }

            long time_difference = current_time - last_update;
            if (time_difference > 150) {
                last_update = current_time;
                double normal = Math.sqrt(Math.pow(curX, 2) + Math.pow(curX, 2) + Math.pow(curX, 2));
                double normalAnterior = Math.sqrt(Math.pow(prevX, 2) + Math.pow(prevY, 2) + Math.pow(prevZ, 2));
                double movement = Math.abs(normal - normalAnterior);
                int limit = 10000000;
                if (movement > 60 )
                {   Log.d("SensorMovimiento","Se activo el sensor de movimientos:" +movement);
                    if (estadoLampara)
                        SendBt("B");
                    else
                        SendBt("A");
                }
                prevX = curX;
                prevY = curY;
                prevZ = curZ;
                last_update = current_time;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        msensorManager.registerListener(this, sensorAcelerometro, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d("onResume","Se activo:");

    }

    @Override
    protected void onPause() {
        super.onPause();
        msensorManager.unregisterListener(this);
    }
}
