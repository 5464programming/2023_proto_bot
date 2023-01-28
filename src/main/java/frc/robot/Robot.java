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
import edu.wpi.first.wpilibj.motorcontrol.MotorController;
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
  private static final String kTokyoDrift = "Tokyo Drift";
  private static final String kHitchRoute = "Hitch Route";
  private static final String kFadeAway = "Fade Away";

  private static final String kHigh = "High";
  private static final String kMid = "Mid";
  private static final String kLow = "Low";
  // TODO: make a 'Tokyo Drift' option here!

  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();
  private String score_preset_selected;
  private final SendableChooser<String> score_preset_chooser = new SendableChooser<>();

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
    m_chooser.addOption("Tokyo Drift", kTokyoDrift);
    m_chooser.addOption("Hitch Route", kHitchRoute);
    m_chooser.addOption("Fade Away", kFadeAway);

    SmartDashboard.putData("Auto choices", m_chooser);

    score_preset_chooser.setDefaultOption("High", kHigh);
    score_preset_chooser.addOption("Mid", kMid);
    score_preset_chooser.addOption("Low", kLow);
    SmartDashboard.putData("Score Preset Choices", score_preset_chooser);

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
    
    // if(  /* check some limelight variable here */    ){
    //   ready = true;
    // }

    // tell the parent routine if we are ready to move on
    return ready;
  }
  
  public boolean sConeEl(){
    // flag indicating elevator height and extension are lined up
    boolean ready = false;

    // checking elevator encoder
    // checking extension encoder

    switch (score_preset_selected) {
      
      case kHigh:
        //reach elevator and extension for high cone
        //set ready to true once conditions are met

        break;

      case kMid:
        //reach elevator and extension for mid cone
        //set ready to true once conditions are met

        break;

      case kLow:
        //reach elevator and extension for low cone
        //set ready to true once conditions are met

        break;

      default:
        //high preset code, in case selector BREAKS!
        //set ready to true once conditions are met

        break;
    
    }

    return ready;
  }

  public boolean Score(){
    // flag indicating cone has been dropped
    boolean ready = false;

    // release cone motors until encoders read a certain value = cone is dropped
    // set ready to true once conditions are met

    return ready;
  }
  
  public boolean TokyoEscape(){
    // flag indicating 
    boolean ready = false;
    return ready;
  }

  public boolean HitchEscape(){
    // flag indicating 
    boolean ready = false;
    return ready;
 }

  public boolean FadeEscape(){
    // flag indicating 
    boolean ready = false;
    return ready;
  }

  public boolean TokyoDrift(){
    boolean ready = false;
    return ready;
  }

  public boolean HitchDrift(){
    boolean ready = false;
    return ready;
  }

  
  public boolean FadeDrift(){
    boolean ready = false;
    return ready;
  }

  public boolean Arrival(){
    boolean ready = false;
    return ready;
  }

  public boolean Gunit(){
    boolean ready = false;
    return ready;
  }

  public boolean Balance(){
    boolean ready = false;

 //IMPORTANT: WANTED TO ADD ALL CANSPARKS TOGETHER IN ONE VARIBLE BUT IT DID NOT WORK 

    //When pitch ~ 5 then stop
if(navx.getPitch() == 5){
  left_front.set(0);
  left_back.set(0); 
  right_front.set(0);
  right_back.set(0);
}
   
  //When pitch > 5 then move forward
if(navx.getPitch() > 5){
  left_front.set(1);
  left_back.set(1);
  right_front.set(1);
  right_back.set(1);
}
    
  //When pitch < 5 then move backward
if(navx.getPitch() < 5){
  left_front.set(-1);
  left_back.set(-1);
  right_front.set(-1);
  right_back.set(-1);
}

    return ready;
    //Focus on pitch when level value reads around 5
  }

  public boolean Generic_Backup(){
    boolean ready = false;
    return ready;
  }

  // This autonomous routine is for a start in front of a cone-scoring post
  // It scores a cone, then zooms around the charging station
  // It then drives us onto the charging station, keeping us there with a gyro/brake

  // This autonomous routine starts in the right position, scores a cone,
  // backs up past the charge station, strafes right, and drives back on it
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
        ready = Score();

      case 3:
        ready = TokyoEscape();

      case 4:
        ready = TokyoDrift();

      case 5: 
        ready = Arrival();

      case 6:
        ready = Gunit();

      case 7:
        ready = Balance();

      case 8:
        break;
    }
    // if an autonomous step is complete, move on to the next one!
    if(ready){
      ready = false;
      autoStep++;
    }
  }

  // This autonomous routine starts in the middle position, scores a cone,
  // backs up past the charge station, and drives back on it
  public void AutoHitchRoute(){
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
        ready = Score();

      case 3:
        ready = HitchEscape();

      case 4:
        ready = HitchDrift();

      case 5: 
        ready = Arrival();

      case 6:
        ready = Gunit();

      case 7:
        ready = Balance();

      case 8:
        break;
    }
    // if an autonomous step is complete, move on to the next one!
    if(ready){
      ready = false;
      autoStep++;
    }
  }

  // This autonomous routine starts in the left position, scores a cone,
  // backs up past the charge station, strafes to the left, and drives back on it
  public void AutoFadeAway(){
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
        ready = Score();

      case 3:
        ready = FadeEscape();

      case 4:
        ready = FadeDrift();

      case 5: 
        ready = Arrival();

      case 6:
        ready = Gunit();

      case 7:
        ready = Balance();

      case 8:
        break;
    }
    // if an autonomous step is complete, move on to the next one!
    if(ready){
      ready = false;
      autoStep++;
    }
  }


  // This autonomous routine starts anywhere in front of a cone scoring location
  // It drives forward, scores, backs out of community
  public void AutoDefault(){
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
        ready = Score();

      case 3:
        ready = Generic_Backup();

      case 8:
        break;
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
    score_preset_selected = score_preset_chooser.getSelected(); 

    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);
    System.out.println("Preset selected: " + score_preset_selected);
    
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
      
      case kTokyoDrift:
        // If we select 'Tokyo Drift' on Drivers' station, it will run this function!
        AutoTokyoDrift();        
      break;

      case kHitchRoute:
        AutoHitchRoute();
        break;

      case kFadeAway:
        AutoFadeAway();
        break;

      case kDefaultAuto:
        AutoDefault();
        break;
        

      default:
        AutoDefault();
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