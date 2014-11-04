import lejos.nxt.UltrasonicSensor;
import lejos.nxt.ColorSensor;
import lejos.nxt.Button;
import lejos.nxt.ColorSensor;
import lejos.nxt.*;
import lejos.robotics.navigation.*;
import lejos.robotics.localization.OdometryPoseProvider;


public class mazeNav extends Thread{

	//Setting up sensors
	private static ColorSensor light = new ColorSensor(SensorPort.S3);
	private static UltrasonicSensor sight = new UltrasonicSensor(SensorPort.S1);
	
	//Setting up controls
	private static DifferentialPilot pilot = new DifferentialPilot(1.75, 5.1, Motor.B, Motor.A); //Class to control movement    
	private static OdometryPoseProvider pose = new OdometryPoseProvider(pilot); // Keeps track of the location of the Robot 
	private static NavPathController nav = new NavPathController(pilot); //Controls the mapping of the maze 
	
	public static void mazeRunner() throws Exception{
		
		while(true) {
			move(); // move 11.5 inches while staying on the line. Makes use of the private method recenter(); 
			see();
		}
	} 
	
	private static void move() {// Method to move the robot
		pilot.setRotateSpeed(400);
		//pilot.setAcceleration(1900); set this if needed
		pilot.setTravelSpeed(3);
		
		final double TRAVEL_DISTANCE = 2.54 / 2.0; // 2.54 cm in an inch, we want this to go 11.5 inches before ending method
		
		for(double i = 0.5; i < 11.5; i+= 0.5) {
			displayDistance(i); // mostly for debugging
			pilot.travel(TRAVEL_DISTANCE); //2.54 inches in a cm
			recenter(); 
			displayClear(); 
		}
	}
	
	private static void see() {// Method to gather data surounding the robot
		//TODO SO FUCKING LONG
	}
  
	private static void recenter() {
		boolean onLine = false;
		
		while(true) {
			if(light.getColorID() != 7) {	 // if the light is not on black
			
				//FIXME IF TURN TOO MUCH
				pilot.steer(-100, -30); // rotates some degree counter clockwise
				if(light.getColorID() == 7) {
					break;
				}
				
				//FIXME IF TURN TOO MUCH
				pilot.steer(100, 45); // rotates back to origin and then some amount counter-clockwise
				/*
				* This is a better idea than checking left and then going all the way right till 360
				* because by overlapping the movements there is more opportunity to check the line
				* thus it is less error prone
				*/
				if(light.getColorID() == 7) {
					break;
				}
			} else {
				break;
			}
		}
	}

	private static void displayDistance(double currentDistance) {
		LCD.drawString("Distance trav: ", 0, 0);
		int curDistance = (int) (currentDistance * 10.0);
		LCD.drawInt(curDistance,0,1);
		LCD.drawString("Max dist: 115", 0, 2);
	}
	
	private static void displayClear() {
		LCD.clear();
	}
	
	private static void crossedFinish() {
		Sound.twoBeeps();
	}
	
   public static void main (String[] args) throws Exception {
   
		// Starts maze navigation
		mazeRunner(); //Starting maze run
		
	}
}
