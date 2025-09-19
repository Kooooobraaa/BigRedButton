#include <SoftwareSerial.h>

// Unique ID for this Arduino, that will represent button
// This ID must be "hard coded" so it is unique for the device

//const String buttonID = "BigRedB_1";
const String buttonID = "BigRedB_2";
//const String buttonID = "BigRedB_3";

// Consts and variables needed
const int buttonPin = 2;           // Button connected to digital pin 2
bool lastButtonState = HIGH;       // Initial state (not pressed)
const int ledPin = 13; // 13 is integrated LED, testing purpose             // LED connected to digital pin 4
const String resetString = "reset"; // Reset message to restore the button for next game

// Create SoftwareSerial object on pins 10 (RX) and 11 (TX)
// This pins are used to connect BT module
SoftwareSerial BTSerial(10, 11);   // RX, TX

void setup() {
  pinMode(buttonPin, INPUT_PULLUP); // Use internal pull-up resistor
  pinMode(ledPin, OUTPUT);          // LED output
  BTSerial.begin(9600);             // HC-05 default baud rate
}

void loop() {
  bool currentButtonState = digitalRead(buttonPin);

  // Detect button press (transition from HIGH to LOW)
  if (lastButtonState == HIGH && currentButtonState == LOW) {
    BTSerial.println(buttonID); // Send message via Bluetooth
    delay(200); // Debounce delay
  }

  lastButtonState = currentButtonState;

  // --- Listen for incoming messages ---
  if (BTSerial.available()) {
    String incoming = BTSerial.readStringUntil('\n'); // read message
    incoming.trim(); // clean up extra spaces/newlines

    if (incoming == buttonID) {
      digitalWrite(ledPin, HIGH);  // light up if winner
    } else {
      digitalWrite(ledPin, LOW);   // turn off otherwise
    }

    if (incoming == resetString) {
      digitalWrite(ledPin, LOW);
    }
  }
}