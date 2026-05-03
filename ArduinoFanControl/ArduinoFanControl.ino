#include <SoftwareSerial.h>

#define RX 10  // Pino RX do Arduino conectado ao TX do HC-05
#define TX 11  // Pino TX do Arduino conectado ao RX do HC-05
#define A_PIN_OUT 3  // Pino responsável pelo controle da fan
#define PIN_IN 4 // Pino de entrada do tacômetro

#define MODE_AUTO 0
#define MODE_CONSTANT 1

SoftwareSerial bluetooth(RX, TX); // Configuração da comunicação serial com o HC-05



int count = 0;
unsigned long start_time; 
int lastRpm = 0;            // Valor para armazenar ultimo rpm calculado para definir se deve enviar sinal para o java ou não da atualização
int rpm;                  // Valor para armazenar o cálculo da leitura do rpm a partir da leitura do tacômetro do pin 2
int targetPwm = 0;            // Valor de pwm (0-255) de escrita na saída analógica
int currentPwm = 0;
const int updateDelay = 16;

int cpuMinTemp = 30;     // Temperatura mínima CPU
int gpuMinTemp = 30;     // Temperatura mínima GPU
int cpuMaxTemp = 85;     // Temperatura máxima CPU
int gpuMaxTemp = 65;     // Temperatura máxima GPU


int pwmMinVel = 65;      // PWM mínimo Fan
int pwmMaxVel = 242;     // PWM máximo Fan
double k = 1;               // Fator de crescimento exponencial (1 = linear, >1 = mais lento no início, <1 = mais rápido no início)

// Define o modo de operação: MODE_AUTO ou MODE_CONSTANT
int opMode = MODE_AUTO;

void setup(){ 
  // Configura os pinos de controle como saída
  pinMode(A_PIN_OUT, OUTPUT);  // Define o pino IN1 como saída
  pinMode(4, OUTPUT);
  digitalWrite(4, HIGH);
  Serial.begin(9600);       // Inicia a comunicação serial com o PC
  bluetooth.begin(9600);    // Inicia a comunicação serial com o HC-05
  Serial.println("HC-05 pronto para comunicação");
  attachInterrupt(digitalPinToInterrupt(2), counter, RISING); // Configura o pino 2 para interrupção na borda de subida


  analogWrite(A_PIN_OUT, targetPwm);
}

void loop() {   
  if (Serial.available()){
    // Lê o valor PWM enviado pelo Serial Monitor e aplica na saída
    targetPwm = Serial.readString().toInt();
    if(targetPwm != currentPwm){
      // analogWrite(A_PIN_OUT, targetPwm);
      setFanSpeedGradual(targetPwm, updateDelay);
      Serial.println("Pwm da fan alterado: " + String(targetPwm));
      currentPwm = targetPwm;
    }
    Serial.println("Pwm da fan alterado: " + String(targetPwm)); 
  } else if (bluetooth.available()){
    String read = bluetooth.readStringUntil('\n');
    char command = read.charAt(0);
    if (command == 'T' && opMode == MODE_AUTO) { // Processa dados de temperatura para modo automático linear
      read = read.substring(1); // remove 'T'
      int pos1 = read.indexOf(':');
      int pos2 = read.indexOf(':', pos1+1);
      float cpuTemp = read.substring(0, pos1).toFloat();
      float gpuTemp = read.substring(pos1+1, pos2).toFloat();
      float diskTemp = read.substring(pos2+1).toFloat();
      Serial.println("BT Read (Temp): " + read);
      Serial.println("Cpu: " + String(cpuTemp) + " Gpu: " + String(gpuTemp) + " Disk: " + String(diskTemp));
      targetPwm = temperatureToPwm(cpuTemp, gpuTemp); 
      if(targetPwm != currentPwm ){
        Serial.println("Pwm da fan alterado: " + String(targetPwm));
        // analogWrite(A_PIN_OUT, targetPwm);
        setFanSpeedGradual(targetPwm, updateDelay);
        currentPwm = targetPwm;
      }
      Serial.println("Pwm da fan igual: " + String(targetPwm));
    } else if (command == 'C') { // Processa atualização de perfil para PWM constante
      // Processa comando PWM constante e altera o modo para constante
      opMode = MODE_CONSTANT;
      String percentStr = read.substring(1);
      int percent = percentStr.toInt();
      targetPwm = percentToPwm(percent, 0, pwmMaxVel);
      Serial.println("BT Read (PWM %): " + String(percent));
      Serial.println("Pwm da fan alterado: " + String(targetPwm));
      analogWrite(A_PIN_OUT, targetPwm);
    } else if (command == 'A'){ // Processa atualização de perfil para modo automático
      opMode = MODE_AUTO;
      // Trata configuração Polinomial: formato "A%d:%d:%d:%d:%d:%d:%.2f\n"
      Serial.println("BT Read : " + String(read));
      read.replace(",", ".");
      String config = read.substring(1); // remove 'A'
      int pos1 = config.indexOf(':');
      int pos2 = config.indexOf(':', pos1+1);
      int pos3 = config.indexOf(':', pos2+1);
      int pos4 = config.indexOf(':', pos3+1);
      int pos5 = config.indexOf(':', pos4+1);
      int pos6 = config.indexOf(':', pos5+1);
      cpuMinTemp = config.substring(0, pos1).toInt();
      gpuMinTemp = config.substring(pos1+1, pos2).toInt();
      cpuMaxTemp = config.substring(pos2+1, pos3).toInt();
      gpuMaxTemp = config.substring(pos3+1, pos4).toInt();
      pwmMinVel = percentToPwm(config.substring(pos4+1, pos5).toInt(), 60, 242);
      pwmMaxVel = percentToPwm(config.substring(pos5+1, pos6).toInt(), 60, 242);
      k = config.substring(pos6+1).toFloat(); // Fator de crescimento polinomial
      Serial.println("BT Config Updated: " + String(cpuMinTemp) + ":" + String(gpuMinTemp) + ":" + String(cpuMaxTemp) + ":" + String(gpuMaxTemp));
      Serial.println("Profile updated: " + String(pwmMinVel) + ":" + String(pwmMaxVel) + ":" + String(k));
    } else {
      Serial.println("BT Unknown command: " + read);
    }
    
  } else {
      start_time = millis();
      count = 0;
      while((millis() - start_time) < 1000);
      // Aplicando fator de correção baseado no PWM (leitura dos pulsos)
      rpm = (count * 60) / 2; 

      if(lastRpm < rpm-100 || lastRpm > rpm+100){ //ignorar pequenas flutuações no envio de rpm
        bluetooth.println(rpm);
        Serial.print(rpm);
        Serial.println(" rpm");
        analogWrite(A_PIN_OUT, targetPwm);
        lastRpm = rpm;
      }
      
  }
}

void counter(){
  count++;
}

int percentToPwm(int percent, int infLimit, int supLimit) {
    // Converte a porcentagem (0-100) para o valor PWM (0-255)
    if (percent < 0) percent = 0;
    if (percent > 100) percent = 100;
    return map(percent, 0, 100, infLimit, supLimit);
}

int temperatureToPwm(float cpuTemp, float gpuTemp) {
    // Verifica se a temperatura está dentro dos limites
    if (cpuTemp < 0 || gpuTemp < 0) return currentPwm; // Prevent read error
    if (cpuTemp <= cpuMinTemp && gpuTemp <= gpuMinTemp) return pwmMinVel;
    if (cpuTemp >= cpuMaxTemp || gpuTemp >= gpuMaxTemp) return pwmMaxVel;

    // Calcula a porcentagem de temperatura em relação aos limites (minTemp a maxTemp), resultando em um intervalo entre 0 e 1
    double percentageCpu = (cpuTemp - cpuMinTemp) / (cpuMaxTemp - cpuMinTemp);
    double percentageGpu = (gpuTemp - gpuMinTemp) / (gpuMaxTemp - gpuMinTemp);
    double percentage = max(percentageCpu, percentageGpu);
    
    double finalPercentage = pow(percentage, k); // Aplicando fator de crescimento exponencial
    // Se K = 1, o crescimento é linear
    // Se K > 1, resposta mais lenta no início, com aumento mais agressivo perto do limite — ideal se quiser silêncio até a temperatura ficar crítica.
    // Se K < 1, resposta mais rápida no início, com aumento mais lento perto do limite — ideal se quiser que o cooler trabalhe sempre em alta velocidade.


    return (int)(finalPercentage * (pwmMaxVel - pwmMinVel) + pwmMinVel);
}

double roundToInterval(double value) { 
  return floor(value * 10) / 10.0;
}

void setFanSpeedGradual(uint8_t targetPWM, uint16_t stepDelayMs) {
  while (currentPwm != targetPwm) {
    if (currentPwm < targetPWM) currentPwm++;
    else                         currentPwm--;
    analogWrite(A_PIN_OUT, currentPwm);
    delay(stepDelayMs);  // ex.: 10–50 ms para rampas perceptivelmente suaves
  }
}