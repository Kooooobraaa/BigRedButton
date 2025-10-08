#include <SoftwareSerial.h>

// Unique ID for this Arduino, that will represent button
// This ID must be "hard coded" so it is unique for the device
const String buttonID = "BigRedB_2"; // "" , "BigRedB_3" "BigRedB_1"

// Consts and variables needed
const int buttonPin = 2;            // Button is connected to digital pin 2
bool lastButtonState = HIGH;        // Initial state (not pressed)
const int ledPin = 3;               // LED is connected to digital pin 3
const int buzzPin = 4;              // Buzz speaker is connected to digital pin 4

bool gameStart = false;             // If the games can start

const int btStatePin = 5;           // Bluetooth module HC-05 STATE to digital pin 5
const String resetString = "reset"; // Reset message to restore the button for next game
const int blinkNumber = 5;          // How many times it will slowly blink
bool wasConnected = false;          // Tracks previous connection state
bool blinkedAlready = false;        // Tracks if already blinked

// Create SoftwareSerial object on pins 10 (RX) and 11 (TX)
// This pins are used to connect BT module
SoftwareSerial BTSerial(10, 11);   // RX, TX

// Reset the button: LED and Buzzer 
void resetButton(){
  digitalWrite(ledPin, LOW);
  digitalWrite(buzzPin, LOW);
}

void slowBlink(){
  digitalWrite(ledPin, HIGH);
  delay(500);
  digitalWrite(ledPin, LOW);
  delay(500);
}

void fastBlink(){
  digitalWrite(ledPin, HIGH);
  delay(100);
  digitalWrite(ledPin, LOW);
  delay(100);
}

void btStatusBlink(bool isBtConnected){
  // BT blinking based on connection
  if (isBtConnected && !wasConnected){
    wasConnected = true;
    blinkedAlready = false;
  }

  if(isBtConnected && !blinkedAlready){
    for (int i = 0; i < blinkNumber; i++){
      slowBlink();
    }
    blinkedAlready = true;
  }

  if (!isBtConnected) {
    // BT is NOT connected, blink fast
    fastBlink();
    wasConnected = false;
    blinkedAlready = false;
  }
}

void setup() {
  pinMode(buttonPin, INPUT_PULLUP); // Use internal pull-up resistor
  pinMode(ledPin, OUTPUT);          // LED output
  pinMode(buzzPin,OUTPUT);          // Buzz speaker
  pinMode(btStatePin, INPUT);       // Bluetooth HC-05 state
  BTSerial.begin(9600);             // HC-05 default baud rate
}

void loop() {
  bool currentButtonState = digitalRead(buttonPin);
  bool isBtConnected = digitalRead(btStatePin);

    btStatusBlink(isBtConnected);
  
    // Detect button press (transition from HIGH to LOW)
    if (lastButtonState == HIGH && currentButtonState == LOW) {
      BTSerial.println(buttonID); // Send message via Bluetooth
      delay(50); // Debounce delay
    }

  lastButtonState = currentButtonState;

  // --- Listen for incoming messages ---
  if (BTSerial.available()) {
    String incoming = BTSerial.readStringUntil('\n'); // read message
    incoming.trim(); // clean up extra spaces/newlines

    if (incoming == buttonID) {
      digitalWrite(ledPin, HIGH);   // light up if winner
      digitalWrite(buzzPin, HIGH);  // buzz up if winner
      //tone(buzzPin,1000,1000);
      delay(1000);
      digitalWrite(buzzPin, LOW);   // turn of buzzer after some time
    } else {
      digitalWrite(ledPin, LOW);    // turn off otherwise
      digitalWrite(buzzPin, LOW);   // no buzz otherwise
    }

    if (incoming == resetString) {
      resetButton();
    }
  }
    // Start game after Bluetooth connection and blinking
  if (!gameStart && isBtConnected && blinkedAlready) {
    gameStart = true;
  }

}
