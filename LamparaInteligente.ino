//--------------------------------------------------------------------------
//------------------  Declaro variables globales ---------------------------
//--------------------------------------------------------------------------

const int diferenciaSonido = 100; // Esto es la diferenciaSonido de sonido que tiene que haber entre el promedio y el valor actual, para que sea considerado aplauso
const int duracionPalmada = 200; // Esto es la duración mínima, en milisegundos, que debe durar una palmada para que sea considerada como tal
const int duracionAplauso = 1500; // Esto es la duración máxima que puede haber entre 2 palmadas, desde el inicio de la primera y el fin de la segunda

int vectorSonido [10]; // A partir de este vector, calculamos el valor promedio de la lectura de sonido
int posicionVectorSonido = 1; // Va a apuntar a una posición del vector anterior
int promedioSonido;
int cantidadPosiciones = 1; // Para saber cuantas posiciones del vectorSonido se han leido (Para cuando no llega a 10)
char input;
unsigned long tiempoInicial;
unsigned long tiempoActual;
unsigned long inicioPalmada = 0; //Si está en 0 es porque no estaba aplaudiendo, cuando se empieza a aplaudir, se toma el tiempo actual
unsigned long inicioPalmada2; //Necesito un backup del último inicio de palmada
unsigned long inicioAplauso;
int cantidadPalmadas = 0; // Cada vez que se chocan las manos debería incrementarse y si llega a 2 debería resetearse
int valorPulsador = 0;
bool ledEncendido = false;

int pinLuz = A0; // selecciona el pin de entrada para el sensor de luz
int pinSonido = A3;   // Entrada para nuestro sensor de sonido
int pinPotenciometro = A5; // Declaramos la variable del potenciómetro
int pulsadorPin = 8;
int pinLed = 13;    // Pin donde estará conectado el led
int pinLedTestigo=10;
int valorSonido = 0;  // variable para almacenar el valor del sensor de sonido
int valorLuz = 0; // variable para almacenar el valor capturado desde el sensor de Luz
int ValorPotenciometro= 0; // variable para almacenar el valor capturado desde el sensor del potenciometro
int aux=0;

int pulsador=0;              //almacena el estado del botón
int estado=0;                //0=led apagado, 1=led encendido
int pulsadorAnt=0;           //almacena el estado anterior del boton
//--------------------------------------------------------------------------
//------------------ Inicia la configuración inicial -----------------------
//--------------------------------------------------------------------------
unsigned long dummy=0;
void setup() {
  // declarar la pinLed como salida:
  pinMode(pinLed, OUTPUT);
  pinMode(pinLedTestigo, OUTPUT);
  pinMode(pulsadorPin, INPUT);
  digitalWrite(pinLed, LOW);
  Serial.begin(19200);
  tiempoInicial = millis(); //tomo el tiempo en que inicia el programa
  vectorSonido [0] = analogRead(pinSonido); // Guardo la primer lectura del sonido en el primer espacio del vector
  promedioSonido = vectorSonido[0]; // También tomo la primer lectura como promedio
  Serial1.begin(9600);//Ver mensaje de Log del bluetooth
}

//--------------------------------------------------------------------------
//--------------------------- Inicia el loop -------------------------------
//--------------------------------------------------------------------------

void loop() {
  leerSensores();
  tiempoActual = millis();
  logicaSonido();
  logicaLuz();
  logicaPotenciometro();
  logicabluetooth();
  logicaPulsador();
  
}

//--------------------------------------------------------------------------
//--------------------------- Finaliza el loop -----------------------------
//--------------------------------------------------------------------------

void logicaSonido() {
   // Vamos a leer cada medio segundo (500 milisegundos), si pasó el tiempo y leemos, actualizamos el tiempoInicial
   if (tiempoActual - tiempoInicial >= 200) {  //Todo esto si tiene que capturar el sonido
      vectorSonido[posicionVectorSonido] = valorSonido;
      posicionVectorSonido++;
      cantidadPosiciones++;
      
      if (posicionVectorSonido >= 10) {
         posicionVectorSonido = 0;
         calcularPromedioSonido();
      }
      
      tiempoInicial = tiempoActual;      
   } // Hasta acá si tiene que calcular el sonido

   // Si no estaba dando una palmada y se empezó ahora, tomo el tiempo
   if ( valorSonido - promedioSonido > diferenciaSonido && (tiempoActual - inicioPalmada) >= duracionPalmada) {
       Serial.println("Promedio");
       Serial.println(promedioSonido);
       inicioPalmada = millis();
      //Serial.println(promedioSonido);
      if (cantidadPalmadas == 0) 
         inicioAplauso = inicioPalmada; //Tomo el tiempo donde se inició el aplauso
      cantidadPalmadas+=1;            
      Serial.println(cantidadPalmadas);      
      if (cantidadPalmadas >= 2 ){
         cantidadPalmadas =0;
          Serial.println("++++++++++++++");
          Serial.println(tiempoActual);
          Serial.println(inicioAplauso);
          dummy=tiempoActual-inicioAplauso;
          Serial.println(dummy);         
          Serial.println("----------------"); 
          if (tiempoActual - inicioAplauso <= duracionAplauso) {
            cantidadPalmadas = 0;
            cambiarEstadoLuz();
            enviarAndroid();
            Serial.println("Debio cambiar de estado");
          }
          else {
            // Si el aplauso duró mucho, tomo la segunda palmada como si fuese la primera
            Serial.println("Palmada duro mucho");
            cantidadPalmadas = 1;
            inicioAplauso = inicioPalmada;
          }
      }
   }

}

void calcularPromedioSonido() {
  if (cantidadPosiciones > 10)
    cantidadPosiciones = 10;

  int suma = 0;
  for (int i = 0; i < cantidadPosiciones - 1; i++) {
    suma += vectorSonido[i];
  }
  promedioSonido = suma / cantidadPosiciones;
}

void logicaPotenciometro(){
  analogWrite(pinLedTestigo, ValorPotenciometro / 2); //Valor analogico para la salida
}

void logicaLuz(){
  // Guardamos el valor leido del ADC en una variable
  // El valor leido por el ADC (voltaje) aumenta de manera directamente proporcional
  // con respecto a la luz percibida por el LDR
  valorLuz= analogRead(pinLuz);
  if(valorLuz > 150 && ledEncendido)
  {
    cambiarEstadoLuz();
    enviarAndroid();
  }
}
void leerSensores() {
  valorSonido = analogRead(pinSonido);
  valorLuz = analogRead(pinLuz); //lee el valor del sensor
  ValorPotenciometro = analogRead(pinPotenciometro);
  pulsador = digitalRead(pulsadorPin); //lee si el botón está pulsado
}

void logicabluetooth(){
   //Controlo si tenemos novedades en el bluetooth      
   
   if (Serial1.available()>0) {
      input=char(Serial1.read()) ;

      switch(input){
      case 'A':
         ledEncendido = true;
         Serial.println("Recibio A");
         cambiarEstadoLuz();
         break;
      case 'B':
         ledEncendido = false;
         Serial.println("Recibio B");
         cambiarEstadoLuz();
      break;
         default:
         delay(500);
         Serial.println("Orden valida. Introduzca A o B.");
         Serial.println(input);
      }
      
      
   }   
}

//enviar los valores por el dipositivo android por el modulo Bluetooth
void enviarAndroid()
{Serial.println("Envio Android");
   if (ledEncendido) {
      //Envio el mensaje al BT
      Serial1.write("B");
      Serial1.write("~");
   }
   else{
      Serial1.write("A");
      Serial1.write("~");
   }
}

void logicaPulsador(){
  
  if((pulsador==HIGH)&&(pulsadorAnt==LOW)){  //si el boton es pulsado y antes no lo estaba
    estado=1-estado;
    //delay(40);               //pausa de 40 ms
    cambiarEstadoLuz();    
    enviarAndroid();
  }
  pulsadorAnt=pulsador;      //actualiza el nuevo estado del boton          
}
void cambiarEstadoLuz(){
  
  if (ledEncendido) {
    ledEncendido = false;
    digitalWrite(pinLed, LOW);
    Serial.println("apaga");
  }
  else {
    ledEncendido = true;
    digitalWrite(pinLed, HIGH);
    Serial.println("prende");
  }
  
}
