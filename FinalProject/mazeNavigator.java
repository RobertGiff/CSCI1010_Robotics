import lejos.nxt.*;
import lejos.robotics.navigation.*;
import lejos.robotics.localization.OdometryPoseProvider;

public class mazeNavigator {
  
  
  
  public static void mazeRunner(DifferentialPilot pilot, OdometryPoseProvider pose, NavPathController nav) throws Exception{
      ColorSensor light = new ColorSensor(SensorPort.S3);
  
      pilot.setRotateSpeed(100);
      pilot.setAcceleration(600);
  } 

     
  public static void move() {// Method to move the robot
      //TODO
  }
  public static void sense() {// Method to gather data surounding the robot
      //TODO
  }
  public static void recenter() {
   //This MIGHT be used for our move method but it WILL be used
   // when we are headed back. We will interupt our wavepoint navigation to
   // recenter ourselfs on the line
   // we can use the resume() method to achieve this.
  }  

   public static void main (String[] args) throws Exception {
     
     DifferentialPilot pilot = new DifferentialPilot(1.75, 5.1, Motor.B, Motor.A); //Class to control movement     
     OdometryPoseProvider pose = new OdometryPoseProvider(pilot); // Keeps track of the location of the Robot 
     NavPathController nav = new NavPathController(pilot); //Controls the mapping of the maze 
       mazeRunner(pilot, pose, nav); //Starting maze run
   }
}