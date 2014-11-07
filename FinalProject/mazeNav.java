import lejos.nxt.UltrasonicSensor;
import lejos.nxt.Button; // For debugging purposes
import lejos.nxt.ColorSensor;
import lejos.nxt.*;
import lejos.robotics.navigation.*;
import java.util.Stack;

public class mazeNav extends Thread {

	//Final variables for fine tuning robot
	private static final int RIGHT = -150; // 150
	private static final int LEFT = 150; // was 150
	private static final double TRAVDISTANCE = 1.65;
	private static final int DO_180 = 300;

	//Setting up sensors
	private static ColorSensor light = new ColorSensor(SensorPort.S3);
	private static UltrasonicSensor sight = new UltrasonicSensor(SensorPort.S1);
	
	//Setting up controls
	private static DifferentialPilot pilot = new DifferentialPilot(1.75, 5.1, Motor.B, Motor.A); //Class to control movement    

	
	//Setting up win condition
	private static boolean won = false;
	
	
	
	private static void move () {
		pilot.setRotateSpeed(300); // was 200
		//pilot.setAcceleration(1900); set this if needed
		pilot.setTravelSpeed(3);
		
		for(int i = 0; i < 7; i ++) {
			recenter();
			pilot.travel(TRAVDISTANCE); 
		}
		pilot.travel(TRAVDISTANCE);
		pilot.travel(TRAVDISTANCE);
		pilot.travel(TRAVDISTANCE);
	}
	
	private static int[] see() {// Method to gather data surrounding the robot
		//calculate the distances left, front right. Using 8 different readings and taking the average of all
		int[]   leftDistances = new int[8];
		int[] frontDistances = new int[8];
		int[] rightDistances = new int[8];
		int avgLeft, avgFront, avgRight;
		
		getData(leftDistances, frontDistances, rightDistances); // Fills all three arrays
		avgLeft = calcAverages(leftDistances); // We now know if there is a wall in front of us or not.
		avgFront = calcAverages(frontDistances);
		avgRight = calcAverages(rightDistances);
		
		/*
		* OUR THRESHOLD IS 2. 
		* Anything above should not be considered a wall, thus open space
		* 2 or less is a wall
		*
		* Depending on how many open spaces we have, and where, we take action accordingly
		*/
		int[] paths = {0,0,0};
		if(avgLeft > 2) {
			paths[0] = 1;
		}
		if(avgFront > 2) {
			paths[1] = 1;
		}
		if(avgRight > 2) {
			paths[2] = 1;
		}
		return paths;
	}
	
	public static void think(int[] paths) {		
		LCD.drawInt(FakeStack.getSize(), 0, 0);
		/*
		* Check to see if we have 0 ways to go, 1 way to go, or 2 ways to go.
		*	0 ways: 180, return to last criticalPoint
		*		Turns right if we originally turned right, turns left if we originally went left
		*	1 way:	switch case tat move forward or turn. If a turn we also push a critical point
		*	2 ways: switch case that shifts through the three different possibilities of 2 ways to go. Pushes a critical point
		*/
		int openWays = getOpenWays(paths);
					
		switch(openWays) {
			
			case 0:
				//check for a win
				if(light.getColorID() != 7) {
					pilot.steer(200, 25);
					if(light.getColorID() != 7) 
						pilot.steer(200, -30);
					if(light.getColorID() != 7)
						pilot.steer(200, -20);
				}
				
				if(checkWin() == true) {
					startWinSequence(paths);
					break; //exits the method
				}
				
				// Gets us back to critical point
				pilot.rotate(DO_180);
				boolean backAtCrit = false;
				
				while(!backAtCrit) {

					paths = see();
					openWays = getOpenWays(paths);
					
					if(openWays == 2) { // if we are back at a valid critical point
						backAtCrit = true;							
						char c = FakeStack.pop() ;
						if(c == 'l') {
							FakeStack.push('r');
						} else {
							FakeStack.push('l');
						}
					} else if(openWays == 1) {
						if(paths[0] == 1) { // left turn
							pilot.rotate(LEFT);
							move();
						} else if (paths[2] == 1) { //right turn
							pilot.rotate(RIGHT);
							move();
						} else { // go forward
							move();
						}
					}
				}

				// Now we will go the the direction we have not taken yet
				if(FakeStack.peek() == 'l') {
					pilot.rotate(RIGHT);
					move();
				} else if(FakeStack.peek() == 'r') { 
					pilot.rotate(LEFT);
					move();
				}
				
				break;
				
			case 1:
				
				if(paths[0] == 1) { // left turn
					pilot.rotate(LEFT);
					move();
				} else if (paths[2] == 1) { //right turn
					pilot.rotate(RIGHT);
					move();
				} else { // go forward
					move();
				}
				
				break;
				
			case 2:
				
				// WE ARE A RIGHT PRIORITY ROBOT
				
				if(paths[0] == 1 && paths[1] == 1) { // left and ahead
					FakeStack.push('r');
					move();
				} else if(paths [2] == 1 && paths[1] == 1) { // right and aheah
					FakeStack.push('r');
					pilot.rotate(RIGHT);
					move();
				} else { // left and right
					FakeStack.push('r'); // an n is treated the same thing as a right but is not inverted on the way back.
					pilot.rotate(RIGHT);
					move();
				}
				
				break;
		}
		LCD.drawInt(FakeStack.getSize(), 0, 1);
	}


	
	private static int getOpenWays(int[] paths) {
		int openWays = 0;
		for(int i = 0; i < paths.length; i++)
			if(paths[i] == 1)
					openWays++;
		return openWays;
	}
	
	private static void getData(int[] leftDistances, int[] frontDistances, int[] rightDistances) {
		Motor.C.setSpeed(400);
		
		// Filling the array
		sight.getDistances(leftDistances);
		Motor.C.rotate(-90); // Rotates to the front
		sight.getDistances(frontDistances);
		Motor.C.rotate(-90);	// Rotates to the right
		sight.getDistances(rightDistances);
		Motor.C.rotate(180); // Rotates back to starting left position
	}
	
	private static int calcAverages(int[] data) {
		double total = 0.0;
		int avg;
		for(int i = 0; i < data.length; i++) {
			total += data[i]; 
		}
		avg = (int) (total / data.length);
		return avg;
	} 
  
	private static void recenter() {
		
		double currentAngle = 10.0;
		//boolean didFix= false;
		
		while(true) {
			if(light.getColorID() != 7) {	 // if the light is not on black
					
				//FIXME IF TURN TOO MUCH
				//pilot.steer(-200, currentAngle * -1.5); // rotates some degree counter clockwise
				pilot.rotate(currentAngle);
				currentAngle *= -1.3;
				if(light.getColorID() >= 7) {
					break;
				}
				
				//FIXME IF TURN TOO MUCH
				
				//pilot.steer(200, currentAngle * 1.5); // rotates back to origin and then some amount counter-clockwise
				/*
				* This is a better idea than checking left and then going all the way right till 360
				* because by overlapping the movements there is more opportunity to check the line
				* thus it is less error prone
				*/
			} else {
				break;
			}
		}
		//return didFix;
	}

	private static void startWinSequence(int [] paths) {
		
		//For debugging, to make sure we have found the finish line
		LCD.drawString("Win Sequence",0,0);
		LCD.drawString("initiated",1,1);
		
		pilot.rotate(DO_180);
		boolean canStop = false;
		
		while(true) { // will run until done
			boolean backAtCrit = false;
			
			if(FakeStack.isEmpty()) {
				int openWays = getOpenWays(see());
				while(openWays != 0) {
					if(openWays == 1) {
						if(paths[0] == 1) { // left turn
							pilot.rotate(LEFT);
							move();
						} else if (paths[2] == 1) { //right turn
							pilot.rotate(RIGHT);
							move();
						} else { // go forward
							move();
						}
					} else {
						move();
					}
					openWays = getOpenWays(see());
				}
				break;
			}
				
			while(!backAtCrit) {
				paths = see();
				int openWays = getOpenWays(paths);
				
				if(openWays == 2) { // if we are back at a valid critical point
					
					backAtCrit = true;							
					char c = FakeStack.pop() ;
					if(c == 'l') {
						if(paths[2] == 1) { // if there is no wall to the right
							pilot.rotate(RIGHT);
							move();
						} else { // if thre is a wall to the right, our "right" we need to take is straight
							move();
						}
					} else { // crit point at a right, so we want to go left
						if(paths[0] == 1) {
							pilot.rotate(LEFT);
							move();
						} else { // if there is a wall to the left, our "left" we need to take is straight
							move();
						}
					}
				
				} else if(openWays == 1) {
					if(paths[0] == 1) { // left turn
						pilot.rotate(LEFT);
						move();
					} else if (paths[2] == 1) { //right turn
						pilot.rotate(RIGHT);
						move();
					} else { // go forward
						move();
					}
				} else {
					move();
				}
			}
		}
		crossedFinish();
		won = true;
	}
	
	private static boolean checkWin() {
		if(light.getRawLightValue() >= 100 && light.getRawLightValue() <= 200) { //FIXME check what the winning condition is.
			Sound.twoBeeps();
			return true;
		} else { 
			return false;
		}
	}
	
	private static void crossedFinish() {
		Sound.twoBeeps();
		won = true;
	}
	
   public static void main (String[] args) throws Exception {				
		//while(true)
		//	LCD.drawInt(light.getRawLightValue(), 0, 0);
		
		// Starts maze navigation
		while(!won)
			think(see());
	}
	
	// Used for debugging
	private static void displayDistance(double currentDistance) {
		LCD.drawString("Distance trav: ", 0, 0);
		int curDistance = (int) (currentDistance * 10.0);
		LCD.drawInt(curDistance,0,1);
		LCD.drawString("Max dist: 115", 0, 2);
	}
}

// We need this becuase Lejos can't handle dynamic memory
class FakeStack {
	static char[] foo = new char[24];
	static int pos = 0;
	
	static public void push(char c) {
		if (pos > 23) {
			System.out.println("TOO MUICH");
			return;
		}
		
		foo[pos] = c;
		pos++;
	}
	
	static public char pop() {
		if (pos < 1) {
			System.out.println("TOO LITTLE");
			return (char) -1;
		}
		pos--;
		return foo[pos];
	}
	
	static public char peek() {
		if (pos < 1) {
			System.out.println("IT'S EMPTY");
			return (char) -1;
		}	
		return foo[pos - 1];
	}
		
	static public boolean isEmpty() {
		return (pos < 1);
	}
		
	static public boolean isFull() {
		return (pos > 23);
	}
		
	static public void reset() {
		pos = 0;
	}
		
	static public int getSize() {
		return pos;
	}
}