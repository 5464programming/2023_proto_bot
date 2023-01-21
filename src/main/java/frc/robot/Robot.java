// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import org.photonvision.PhotonCamera;
// please bring me some pizza
//ZYXWVUTSRQPONMLKJIHGFEDCBA em htiw gnis uoy t'now emit txen sCBA ym wonk I woN
//import com.revrobotics.AnalogInput;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.MecanumDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import pabeles.concurrency.ConcurrencyOps.Reset;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.PS4Controller.Button;

import com.fasterxml.jackson.databind.util.RawValue;
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
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  //Motors
  CANSparkMax left_front = new CANSparkMax(2, MotorType.kBrushless);
  CANSparkMax right_front = new CANSparkMax(4, MotorType.kBrushless);
  CANSparkMax left_back = new CANSparkMax(1, MotorType.kBrushless);
  CANSparkMax right_back = new CANSparkMax(3, MotorType.kBrushless);

  CANSparkMax testcan = new CANSparkMax(3, MotorType.kBrushless);

  //Drive Train
  MecanumDrive drivetrain = new MecanumDrive(right_front, right_back, left_front, left_back);

  //Joysticks
  Joystick notjoystick = new Joystick(0);

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
  RelativeEncoder testmotor;
  double testsp; 

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);

    right_back.setInverted(false);
    right_front.setInverted(false);

    pnematic.enableDigital();
    // pnematic.disable(); 

    boolean enabled = pnematic.isEnabled();
    boolean pressureSwitch = pnematic.getPressureSwitchValue();
    double current = pnematic.getCurrent(); 

    //pcmsol.set(Value.kOff); 
    pcmright.set(Value.kForward); 
    pcmleft.set(Value.kForward);
    //pcmsol.set(Value.kReverse); 

    //Encodertest 
    testmotor = testcan.getEncoder();
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

    testsp = testmotor.getPosition();

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
    if (notjoystick.getRawButton(1)){
      if(pump=false){
        pump=true;
        pnematic.enableDigital();
      }
      if(pump=true){
        pump=false;
        pnematic.disable();
      }
    }

    ultracm=(sensordistant.getAverageVoltage())*(1024);
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
  drivetrain.driveCartesian(notjoystick.getRawAxis(1), notjoystick.getRawAxis(0), notjoystick.getRawAxis(4));

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