#include <SoftwareSerial.h>

//- Connect to multiple buzzers via Bluetooth
//- Listen for "BUZZED" messages
//- First buzzer to send the message gets priority
//- Send "RESET" to all buzzers after each round


const int buttonPin = 2;
const int ledPin = 3;
SoftwareSerial BTSerial(4, 5); // RX, TX

bool hasBuzzed = false;

void setup() {
  pinMode(buttonPin, INPUT_PULLUP);
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);

  BTSerial.begin(9600); // Match with your module's baud rate
  delay(1000); // Give Bluetooth time to initialize
}

void loop() {
  if (!hasBuzzed && digitalRead(buttonPin) == LOW) {
    hasBuzzed = true;
    BTSerial.println("BUZZED");
    digitalWrite(ledPin, HIGH); // Light up to show buzzed
  }

  // Listen for reset command from app
  if (BTSerial.available()) {
    String command = BTSerial.readStringUntil('\n');
    if (command == "RESET") {
      hasBuzzed = false;
      digitalWrite(ledPin, LOW);
    }
  }
}