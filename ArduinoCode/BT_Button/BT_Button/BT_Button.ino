#include <SoftwareSerial.h>

const int buttonPin = 2;           // Button connected to digital pin 2
bool lastButtonState = HIGH;       // Initial state (not pressed)

// Create SoftwareSerial object on pins 10 (RX) and 11 (TX)
SoftwareSerial BTSerial(10, 11);   // RX, TX

void setup() {
  pinMode(buttonPin, INPUT_PULLUP); // Use internal pull-up resistor
  BTSerial.begin(9600);             // HC-05 default baud rate
}

void loop() {
  bool currentButtonState = digitalRead(buttonPin);

  // Detect button press (transition from HIGH to LOW)
  if (lastButtonState == HIGH && currentButtonState == LOW) {
    BTSerial.println("Button Pressed!"); // Send message via Bluetooth
    delay(200); // Debounce delay
  }

  lastButtonState = currentButtonState;
}