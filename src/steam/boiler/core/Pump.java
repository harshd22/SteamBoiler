package steam.boiler.core;

/**
 * This class represents the pump the its functioning.
 * @author Harsh
 *
 */
public class Pump {

  private boolean isOn;
  private boolean isControllerOn; 
  private double pumpCapacity;
  private boolean isBroken;
  
  /**
 * Pump constructor it initialisis all the variables.
 */
  public Pump() {
    this.isOn = false;
    this.isControllerOn = false;
    this.pumpCapacity = 0;
    this.isBroken = false;
  }

  //----Getters and Setters-------

  public boolean isOn() {
    return this.isOn;
  }

  public boolean isControllerOn() {
    return this.isControllerOn;
  }

  public double getPumpCapacity() {
    return this.pumpCapacity;
  }

  public void setIsOn(boolean isOn) {
    this.isOn = isOn;
  }

  public void setControllerOn(boolean isControllerOn) {
    this.isControllerOn = isControllerOn;
  }

  public void setPumpCapacity(double pumpCapacity) {
    this.pumpCapacity = pumpCapacity;
  }

  public boolean isBroken() {
    return this.isBroken;
  }

  public void setOn(boolean isOn) {
    this.isOn = isOn;
  }

  public void setBroken(boolean isBroken) {
    this.isBroken = isBroken;
  }

}

