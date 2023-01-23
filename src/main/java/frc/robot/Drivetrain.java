package frc.robot;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

// This class encompasses the Drivetrain system
// Motor configuration and drive methods go here
// 

public class Drivetrain {
    // ============================================== Public Variables
    // What we want the rest of the robot to know
    
    public boolean are_drive_motors_happy = true;

    // ============================================== Private Variables
    // What the rest of the robot does not care about
    private double max_drive_speed = 0.7;
    
    

    public void Init(){
        // put one-time setup steps here
        SmartDashboard.putBoolean("are the drive motors happy?",are_drive_motors_happy);
    }

    // ============================================= Public Functions
    public void Move(double x,double y,double rot){
        // This could be a function that we call when we want to move the robot.
        // We will "pass in" three values from our main Robot Class,
        // And this function will use those values
        // x = forward, y = strafe, rot = rotate the bot

    }

    // ============================================= Private Functions
    
}
