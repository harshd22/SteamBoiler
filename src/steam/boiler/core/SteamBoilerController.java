package steam.boiler.core;

//import org.eclipse.jdt.annotation.NonNull;
//import org.eclipse.jdt.annotation.Nullable;

import steam.boiler.util.Mailbox;
import steam.boiler.util.Mailbox.Message;
import steam.boiler.util.Mailbox.MessageKind;
import steam.boiler.util.SteamBoilerCharacteristics;

/**
 * .
 * @author Harshdeep Singh
 * Id - 300413169
 *
 */
public class SteamBoilerController {
  
  private Pump[] pumps;
  private Mailbox.Mode mode;
  private int openPumps = 0;
  private double m1;
  private double m2;
  private double n1;
  private double n2;
  private double steamV = 0;
  private double waterLevel = 0;
  private double tankCapacity;
  private boolean arePhysicalUnitsReadv;
  private boolean isValveOpen = false;

  /**
 * Construct a steam boiler controller for a given set of characteristics.
 *
 * @param configuration
 *            The boiler characteristics to be used.
 */
  public SteamBoilerController(SteamBoilerCharacteristics configuration) {
    initialize(configuration);
  }

  /**
   * Initialize all the fields and variables.
 * @param configuration
 *  The boiler characteristics to be used.
 */
  private void initialize(SteamBoilerCharacteristics configuration) {
    assert configuration != null;
    //setup the number of pumps used in program.
    this.setPumps(new Pump[configuration.getNumberOfPumps()]);
    for (int index = 0; index < this.getPumps().length; index++) {
      this.getPumps()[index] = new Pump();
    }
    for (int i = 0; i < configuration.getNumberOfPumps(); i++) {
      getPumps()[i].setPumpCapacity(configuration.getPumpCapacity(i));
    }
    this.setMode(Mailbox.Mode.INITIALISATION);

    setM1(configuration.getMinimalLimitLevel());
    setM2(configuration.getMaximalLimitLevel());
    setN1(configuration.getMinimalNormalLevel());
    setN2(configuration.getMaximalNormalLevel());
    setTankCapacity(configuration.getCapacity());
    setArePhysicalUnitsReadv(false);
  }

  /**
 * Process a clock signal which occurs every 5 seconds. This requires reading
 * the set of incoming messages from the physical units and producing a set of
 * output messages which are sent back to them.
 *
 * @param incoming
 *            The set of incoming messages from the physical units.
 * @param outgoing
 *            Messages generated during the execution of this method should be
 *            written here.
 */
  public void clock(final Mailbox incoming, final Mailbox outgoing) {
    assert incoming != null && outgoing != null;

    checkMessage(incoming, outgoing);
    checkMode(outgoing);
  }

  /**
   * This methods checks and calls the method for current mode.
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void checkMode(Mailbox outgoing) {
    switch (this.getMode()) {
      case INITIALISATION:
        initialisation(outgoing);
        break;
      case NORMAL:
        normal(outgoing);
        break;
      case DEGRADED:
        degraded(outgoing);
        break;
      case EMERGENCY_STOP:
        emergencyStop(outgoing);
        break;
      case RESCUE:
        rescue(outgoing);
        break;
      default:
        break;
    }
  }

  /**
   * This method checks about the messages received and update the variables with the 
   * values received in message.
 * @param incoming
 *     The set of incoming messages from the physical units.
 * @param outgoing
 *      Messages generated during the execution of this method should be
 *            written here.
 */
  private void checkMessage(final Mailbox incoming, final Mailbox outgoing) {

    for (int index = 0; index < incoming.size(); index++) {
      Message currentMessage = incoming.read(index);

      switch (currentMessage.getKind()) {
        //tells the program about the current mode of the boiler.
        case MODE_m:
          setMode(currentMessage.getModeParameter());
          break;
        //tells the program whether steam boiler is waiting or not.
        case STEAM_BOILER_WAITING:
          steamBoilerWaiting(outgoing);
          break;
        //tells the program whether physical units are ready or not.
        case PHYSICAL_UNITS_READY:
          setArePhysicalUnitsReadv(true);
          break;
        //turns the pump on or off.
        case PUMP_STATE_n_b:
          int intParameter = currentMessage.getIntegerParameter();
          boolean boolParameter = currentMessage.getBooleanParameter();
          getPumps()[intParameter].setIsOn(boolParameter);
          break;
        //turns the pump controller on or off.
        case PUMP_CONTROL_STATE_n_b:
          intParameter = currentMessage.getIntegerParameter();
          boolParameter = currentMessage.getBooleanParameter();
          getPumps()[intParameter].setControllerOn(boolParameter);
          break;
        //sets the water level .
        case LEVEL_v:
          setWaterLevel(currentMessage.getDoubleParameter());
          break;
        // sets the steam level.
        case STEAM_v:
          setsteamV(currentMessage.getDoubleParameter());
          break;
        default:
          break;

      }
    }
  }


  /**
   * When the water level sensor is broken the steam boiler mode shifts
   * to rescue mode and the water is maintained by calculating the evacuation 
   * rate and capacity of pumps.
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void rescue(Mailbox outgoing) {
    assert outgoing != null;
    //checks the failures.
    waterLevelCheck(outgoing);
    if (getsteamV() < 0 || getsteamV() >= getTankCapacity()) {
      emergencyStop(outgoing);
    }
  }

  private static void emergencyStop(Mailbox outgoing) {
    outgoing.send(new Message(Mailbox.MessageKind.MODE_m, Mailbox.Mode.EMERGENCY_STOP));
  }

  /**
   * When the Steam sensor is broken the Steam Boiler changes its mode
   * to degraded mode , and the normal functioning remains the same.
 * @param outgoing
 *     Messages generated during the execution of this method should be
 *            written here.
 */
  private void degraded(Mailbox outgoing) {
    assert outgoing != null;
    //checks for the failures
    waterLevelCheck(outgoing);
    checkWaterLevelFailure(outgoing);
    
    maintainWaterLevel(outgoing);

    assert getWaterLevel() > getM1() && getWaterLevel() < getM2();
  }

  /**
   * Normal mode maintains the water level between N1 and N2
   * and if any failure occurs the mode is changed.
 * @param outgoing
 *   Messages generated during the execution of this method should be
 *            written here.
 */
  private void normal(Mailbox outgoing) {
    assert outgoing != null;

    waterLevelCheck(outgoing);
    checkWaterLevelFailure(outgoing);
    checkSteamFailure(outgoing);
    if ((getsteamV() <= 0 || getsteamV() >= getTankCapacity())
         && (getWaterLevel() < 0 || getWaterLevel() >= getTankCapacity())) {
      emergencyStop(outgoing);
    }
    maintainWaterLevel(outgoing);
  }

  /**
   * This method is called to maintain the water level between N1 and N2.
 * @param outgoing
 *  Messages generated during the execution of this method should be
 *            written here.
 */
  private void maintainWaterLevel(Mailbox outgoing) {
    if (getWaterLevel() > getN2()) {
      openPumps(1, outgoing);
    } else if (getWaterLevel() < getN1()) {
      openPumps(w, outgoing);
      // maintain the water level between the midpoint of N1 and N2
    } else if (getWaterLevel() > (getN1() + (getN2() - getN1()) * (50.0 / 100.0))) {
      openPumps(getOpenedPumps() - 1, outgoing);
    } else if (getWaterLevel() < (getN1() + (getN2() - getN1()) * (50.0 / 100.0))) {
      openPumps(getOpenedPumps() + 1, outgoing);
    }
  }

  /**
   * When the program starts it enters into the initialisation mode
   * and maintains the water level for starting , if the water level is 
   * between the margins it sends the message for physical units ready  
   * and then program is shifted into Normal mode.
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void initialisation(Mailbox outgoing) {
    assert outgoing != null;
    //check failure.
    if (getWaterLevel() < 0 || getWaterLevel() > getTankCapacity()) {
      emergencyStop(outgoing);
    }
    
    if (isArePhysicalUnitsReadv()) {
      //close the valve if it is open.
      if (this.isValveOpen) {
        outgoing.send(new Message(Mailbox.MessageKind.VALVE));
        this.isValveOpen = false;
      }
      
      setMode(Mailbox.Mode.NORMAL);
      outgoing.send(new Message(Mailbox.MessageKind.MODE_m, getMode()));
      return;
    }
    //if physical units are not ready then just stay in initialisation mode.
    outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.INITIALISATION));
  }


  /**
   * It returns the number of pumps active at the moment.
 * @return
 *     The number of pumps active.
 */
  private int getOpenedPumps() {
    setOpenPumps(0);
    for (int index = 0; index < getPumps().length; index++) {
      if (getPumps()[index].isOn() && !getPumps()[index].isBroken()) {
        setOpenPumps(getOpenPumps() + 1);
      }
    }
    assert getOpenPumps() <= getPumps().length;
    assert getOpenPumps() > -1;
    return getOpenPumps();
  }

  /**
   * During initialisation mode when water level is less than 
   * N1 or greater than N2 , this method is called which helps in 
   * maintaining the water level by turning on the pumps or opening
   * the valve.
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void adjustWaterLevel(Mailbox outgoing) {
    assert getWaterLevel() <= getN1() || getWaterLevel() >= getN2();
    assert outgoing != null;
    
    //if water level is below the N1 then open pumps.
    if (getWaterLevel() < getN1()) {
      double cap = (getM1() + getN2() / 2) - getWaterLevel();
      cap /= (15 * getPumps()[0].getPumpCapacity());

      cap = Math.min(cap, getPumps().length);
      openPumps((int) cap, outgoing);
      //if water is more than N2 then open the valve until it comes between N1 and N2.
    } else if (getWaterLevel() >= getN2()) {
      outgoing.send(new Message(Mailbox.MessageKind.VALVE));
      this.isValveOpen = true;
      //if water level is equal to n1 then just open one pump.
    } else if (getWaterLevel() == getN1()) {
      openPumps(1, outgoing);
    }

    assert getWaterLevel() <= getN1() || getWaterLevel() >= getN2();
  }

  /**
   * This method opens the number of pumps needed.
   * 
 * @param number
 *    The number of pumps to be opened.
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void openPumps(int number, Mailbox outgoing) {
    assert number <= getPumps().length;

    closeAllPumps(outgoing);
    for (int index = 0; index < number; index++) {
      if (index < getPumps().length && !getPumps()[index].isBroken()) {
        getPumps()[index].setIsOn(true);
        outgoing.send(new Message(Mailbox.MessageKind.OPEN_PUMP_n, index));
      }
    }
  }

  /**
   * This method closes all the pumps.
   * 
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void closeAllPumps(Mailbox outgoing) {
    assert getOpenedPumps() > -1;
    assert outgoing != null;
    
    for (int index = 0; index < getPumps().length; index++) {
      getPumps()[index].setIsOn(false);
      outgoing.send(new Message(Mailbox.MessageKind.CLOSE_PUMP_n, index));
    }
    assert getOpenedPumps() == 0;
  }

  /**
   * If water level sensor is broken then mode changes to rescue mode.
   * 
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void checkWaterLevelFailure(Mailbox outgoing) {
    assert outgoing != null;

    if (getWaterLevel() < 0 || getWaterLevel() >= getTankCapacity()) {
      outgoing.send(new Message(Mailbox.MessageKind.LEVEL_FAILURE_DETECTION));
      outgoing.send(new Message(Mailbox.MessageKind.MODE_m, Mailbox.Mode.RESCUE));
    } else {
      return;
    }

    assert getWaterLevel() < 0 || getWaterLevel() >= getTankCapacity();
  }

  /**
   * If steam sensor is broken mode changes to degraded mode.
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void checkSteamFailure(Mailbox outgoing) {
    assert outgoing != null;

    if (getsteamV() < 0 || getsteamV() >= getTankCapacity()) {
      outgoing.send(new Message(Mailbox.MessageKind.STEAM_FAILURE_DETECTION));
      outgoing.send(new Message(Mailbox.MessageKind.MODE_m, Mailbox.Mode.DEGRADED));
    } else {
      return;
    }

    assert getsteamV() < 0 || getsteamV() >= getTankCapacity();
  }

  /**
   * Program goes into emergency mode if water is not in the bounds of 
   * M1 and M2.
 * @param outgoing
 *    Messages generated during the execution of this method should be
 *            written here.
 */
  private void waterLevelCheck(Mailbox outgoing) {
    assert outgoing != null;

    if (getWaterLevel() < getM1() || getWaterLevel() > getM2()) {
      emergencyStop(outgoing);
    } else {
      return;
    }

    assert getWaterLevel() < getM1() || getWaterLevel() > getM2();
  }

  /**
   * When the steam boiler is waiting this method is called to to check 
   * whether program is ready to enter the ready state or not , if not 
   * then the water level is adjusted.
 * @param outgoing
 * Messages generated during the execution of this method should be
 *            written here.
 */
  private void steamBoilerWaiting(Mailbox outgoing) {
    assert outgoing != null;
    //check failure.
    if (getsteamV() != 0) {
      outgoing.send(new Message(Mailbox.MessageKind.MODE_m, Mailbox.Mode.EMERGENCY_STOP));
    }
    //if water level is between n1 and n2 program is ready.
    if (getN1() < getWaterLevel() && getWaterLevel() < getN2()) {
      outgoing.send(new Message(Mailbox.MessageKind.PROGRAM_READY));
    } else {
      adjustWaterLevel(outgoing);
    }
  }

  /**
 * This message is displayed in the simulation window, and enables a limited
 * form of debug output. The content of the message has no material effect on
 * the system, and can be whatever is desired. In principle, however, it should
 * display a useful message indicating the current state of the controller.
 *
 * @return
 */
  public static String getStatusMessage() {
    return "MY STATUS"; //$NON-NLS-1$
  }

  // ---------------------------Getters And
  // Setters------------------------------
  public boolean isArePhysicalUnitsReadv() {
    return this.arePhysicalUnitsReadv;
  }

  public void setArePhysicalUnitsReadv(boolean arePhysicalUnitsReadv) {
    this.arePhysicalUnitsReadv = arePhysicalUnitsReadv;
  }

  public Mailbox.Mode getMode() {
    return this.mode;
  }

  public void setMode(Mailbox.Mode mode) {
    this.mode = mode;
  }

  public Pump[] getPumps() {
    return this.pumps;
  }

  public void setPumps(Pump[] pumps) {
    this.pumps = pumps;
  }

  public double getWaterLevel() {
    return this.waterLevel;
  }

  public void setWaterLevel(double waterLevel) {
    this.waterLevel = waterLevel;
  }

  public double getsteamV() {
    return this.steamV;
  }

  public void setsteamV(double steamV) {
    this.steamV = steamV;
  }

  public double getTankCapacity() {
    return this.tankCapacity;
  }

  public void setTankCapacity(double tankCapacity) {
    this.tankCapacity = tankCapacity;
  }

  public double getN2() {
    return this.n2;
  }

  public void setN2(double n2) {
    this.n2 = n2;
  }

  public double getN1() {
    return this.n1;
  }

  public void setN1(double n1) {
    this.n1 = n1;
  }

  public double getM2() {
    return this.m2;
  }

  public void setM2(double m2) {
    this.m2 = m2;
  }

  public double getM1() {
    return this.m1;
  }
  
  public void setM1(double m1) {
    this.m1 = m1;
  }

  public int getOpenPumps() {
    return this.openPumps;
  }

  public void setOpenPumps(int openPumps) {
    this.openPumps = openPumps;
  }

}