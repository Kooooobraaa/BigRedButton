#include <SoftwareSerial.h>

SoftwareSerial BTSerial(10, 11); // RX, TX for HC-05

const int ledPin = 13;  // LED on Pro Mini (or external LED)

String inputString = "";  // to store incoming command

void setup() {
  pinMode(ledPin, OUTPUT);
  Serial.begin(9600);      // USB serial monitor
  BTSerial.begin(9600);    // HC-05 default baud
  Serial.println("Bluetooth command system ready.");
  BTSerial.println("Arduino ready. Type LED ON or LED OFF.");
}

void loop() {
  // Read from Bluetooth
  while (BTSerial.available()) {
    char c = BTSerial.read();

    if (c == '\n' || c == '\r') {   // command finished
      processCommand(inputString);
      inputString = "";
    } else {
      inputString += c;  // build command string
    }
  }

  // Optional: forward Serial Monitor → Bluetooth
  while (Serial.available()) {
    BTSerial.write(Serial.read());
  }
}

void processCommand(String cmd) {
  cmd.trim();  // remove spaces/newlines

  if (cmd.equalsIgnoreCase("LED ON")) {
    digitalWrite(ledPin, HIGH);
    BTSerial.println("LED is ON");
    Serial.println("LED is ON");
  }
  else if (cmd.equalsIgnoreCase("LED OFF")) {
    digitalWrite(ledPin, LOW);
    BTSerial.println("LED is OFF");
    Serial.println("LED is OFF");
  }
  else {
    BTSerial.println("Unknown command: " + cmd);
    Serial.println("Unknown command: " + cmd);
  }
}
