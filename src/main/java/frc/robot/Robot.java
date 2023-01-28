// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import org.photonvision.PhotonCamera;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.MecanumDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import com.kauailabs.navx.frc.AHRS;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");  
  NetworkTableEntry tx = table.getEntry("tx");
  NetworkTableEntry ty = table.getEntry("ty");
  NetworkTableEntry ta = table.getEntry("ta");

  double[] xAverage = {0,0,0,0,0};
  double[] yAverage = {0,0,0,0,0};

  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  // TODO: make a 'Tokyo Drift' option here!

  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  //Motors
  CANSparkMax left_front = new CANSparkMax(2, MotorType.kBrushless);
  CANSparkMax right_front = new CANSparkMax(3, MotorType.kBrushless);
  CANSparkMax left_back = new CANSparkMax(1, MotorType.kBrushless);
  CANSparkMax right_back = new CANSparkMax(4, MotorType.kBrushless);

 // CANSparkMax testcan = new CANSparkMax(1, MotorType.kBrushless);


  //Drive Train
  MecanumDrive drivetrain = new MecanumDrive(left_front, left_back, right_front, right_back);

  //Joysticks
  Joystick notjoystick = new Joystick(0);
  Joystick yesjoystick = new Joystick(1);

  //Photon vision camera 
  PhotonCamera camera = new PhotonCamera("photonvision"); 

  //NAVX
  AHRS navx = new AHRS();

  //Pnematics
  Compressor pnematic = new Compressor(0, PneumaticsModuleType.CTREPCM); 

  //Soleniod
  DoubleSolenoid pcmright = new DoubleSolenoid(PneumaticsModuleType.CTREPCM, 0, 7);
  DoubleSolenoid pcmleft = new DoubleSolenoid(PneumaticsModuleType.CTREPCM, 1, 6);
  boolean pump = false;

  //Distance
  AnalogInput sensordistant = new AnalogInput(3);
  double ultracm = 0;

  //Servo
  Servo theclaw = new Servo(0);
  Servo thegrab = new Servo(1);

  //Encoder 
  RelativeEncoder testenc;
  double testsp; 

  //Drive speed
  double driveSpeedMax = 0.7;

  // 5464 custom-defined subsystem classes
  // classes created with a leading lowercase m are our subsystems
  //Drivetrain mDrivetrain = new Drivetrain();
  //Elevator mElevator = new Elevator();
  // Eva and Gabe will fill in the rest here!


  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);


    // 5464 custom classes initial setup functions
    //mDrivetrain.Init();
    // Eva and Gabe will fill in the rest of subsystem inits here!






    // right_back.setInverted(false);
    right_front.setInverted(false);

    ///pnematic.enableDigital();
    pnematic.disable(); 
    //boolean enabled = pnematic.isEnabled();
    //boolean pressureSwitch = pnematic.getPressureSwitchValue();
    //double current = pnematic.getCurrent(); 

    //pcmsol.set(Value.kOff); 
    pcmright.set(Value.kForward); 
    pcmleft.set(Value.kForward);
    //pcmsol.set(Value.kReverse); 
    //Encodertest 
    //testenc = testcan.getEncoder();

    //motor ramp rate
    double ramp_rate = 0.5;

    left_front.setOpenLoopRampRate(ramp_rate);
    right_front.setOpenLoopRampRate(ramp_rate);
    left_back.setOpenLoopRampRate(ramp_rate);
    right_back.setOpenLoopRampRate(ramp_rate);
  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {

    var result = camera.getLatestResult();
    boolean hasTargets = result.hasTargets();
    SmartDashboard.putBoolean("Target?", hasTargets);
    double target = result.getLatencyMillis();
    SmartDashboard.putNumber("Latency", target);

    SmartDashboard.putNumber("Yaw", navx.getYaw());
    SmartDashboard.putNumber("Roll", navx.getRoll());
    SmartDashboard.putNumber("Pitch", navx.getPitch());
    SmartDashboard.putNumber("Distance in cm", ultracm);

    SmartDashboard.putNumber("SetPoint", testsp);

    //testsp = testenc.getPosition();

    if (notjoystick.getRawButtonPressed(4)) {
      pcmright.toggle();
      pcmleft.toggle(); 
    }

    //Servo claws
    if (notjoystick.getRawButton(3)) {
      theclaw.setAngle(75);
    }
    else{
      theclaw.setAngle(0);
    }

    if (notjoystick.getRawButton(2)){
      thegrab.set(75);
    }
    else{
      thegrab.setAngle(0);
    }

    //Enable/disable pneumatic
    if (notjoystick.getRawButtonPressed(1)){
      // if pump is false, then do stuff
      if( pump == false ){
        
        // set the value of 'pump' to true
        pump=true;
        System.out.println("turning pump on");
        pnematic.enableDigital();
      }
      else if(pump == true){
        pump=false;
        System.out.println("turning pump off");
        pnematic.disable();
      }
    }

    ultracm=(sensordistant.getAverageVoltage())*(1024);
  }

  // This is step 0 in 'Tokyo Drift' subroutine!
  // Drives forward with Limelight, so we can be at the correct distance to score
  public boolean scorePrep(){
    // flag indicating we are lined up
    boolean ready = false;

    // do all the stuff we want during this step
    // at some point, once we satisfy conditions, we will do the following:
    // TODO: drive forward and check distance with Vision.
    
    if(  /* check some limelight variable here */    ){
      ready = true;
    }

    // tell the parent routine if we are ready to move on
    return ready;
  }
  





  // This autonomous routine is for a start in front of a cone-scoring post
  // It scores a cone, then zooms around the charging station
  // It then drives us onto the charging station, keeping us there with a gyro/brake
  public void AutoTokyoDrift(){
    boolean ready = false;
    switch(autoStep){
      case 0:
        // this will check the return value of scorePrep() continuously until it says it's ready
        ready = scorePrep();
      case 1:
        // TODO: define more functions, like "sConeEl", just like we did for scorePrep()!
        ready = sConeEl();
      case 2:
        // ready = ?

    }
    // if an autonomous step is complete, move on to the next one!
    if(ready){
      ready = false;
      autoStep++;
    }
  }



  /**
   * This autonomous (along with the chooser code above) shows how to select between different
   * autonomous modes using the dashboard. The sendable chooser code works with the Java
   * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the chooser code and
   * uncomment the getString line to get the auto name from the text box below the Gyro
   *
   * <p>You can add additional auto modes by adding additional comparisons to the switch structure
   * below with additional strings. If using the SendableChooser make sure to add them to the
   * chooser code above as well.
   */
  public int autoStep=0;
  
   @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);
    
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
      
      case kCustomAuto:
        // Put custom auto code here
        
        break;

      case kDefaultAuto:
        // What is our default auto, if we don't pick one? Put it here!

        break;
        
      case kTokyoDriftAuto:
        // This is currently an error! kTokyoDriftAuto has to be declared before this!

        // If we select 'Tokyo Drift' on Drivers' station, it will run this function!

        AutoTokyoDrift();
        break;

      default:
        // Put default auto code here
        break;
    
    }
  }

  /** This function is called once when teleop is enabled. */
  @Override
  public void teleopInit() {}

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {

  //Drive Train
  drivetrain.driveCartesian(notjoystick.getRawAxis(1)*driveSpeedMax,
             -notjoystick.getRawAxis(0)*driveSpeedMax, -notjoystick.getRawAxis(4)*driveSpeedMax);

  if(notjoystick.getRawButton(5)){
  var aprilres = camera.getLatestResult();
  if(aprilres.hasTargets()){
    var bestarget = aprilres.getBestTarget();
    bestarget.getPitch();
    var pitchyaxis = bestarget.getPitch();
    SmartDashboard.putNumber("Pitch", pitchyaxis);
  }
  }

  if(notjoystick.getRawButton(6)){
  var aprilres = camera.getLatestResult();
  if(aprilres.hasTargets()){
    var bestarget = aprilres.getBestTarget();
    bestarget.getYaw();
    var pitchxaxis = bestarget.getYaw();
    SmartDashboard.putNumber("Yaw", pitchxaxis);
  }
  }

  if(yesjoystick.getRawButton(1)){
    //testcan.set(0.1);
  }
  else{
   // testcan.set(0);
  }
  }

  /** This function is called once when the robot is disabled. */
  @Override
  public void disabledInit() {}

  /** This function is called periodically when disabled. */
  @Override
  public void disabledPeriodic() {}

  /** This function is called once when test mode is enabled. */
  @Override
  public void testInit() {}

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {}

  /** This function is called once when the robot is first started up. */
  @Override
  public void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  @Override
  public void simulationPeriodic() {}
}