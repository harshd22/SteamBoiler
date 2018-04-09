package steam.boiler.core;

//import org.eclipse.jdt.annotation.NonNull;
//import org.eclipse.jdt.annotation.Nullable;

import steam.boiler.util.Mailbox;
import steam.boiler.util.SteamBoilerCharacteristics;
import steam.boiler.util.Mailbox.Message;
import steam.boiler.util.Mailbox.MessageKind;
import steam.boiler.util.Mailbox.Mode;

public class SteamBoilerController {

  /**
   * Construct a steam boiler controller for a given set of characteristics.
   *
   * @param configuration
   *          The boiler characteristics to be used.
   */
  public SteamBoilerController(SteamBoilerCharacteristics configuration) {

  }

  /**
   * Process a clock signal which occurs every 5 seconds. This requires reading the set of incoming
   * messages from the physical units and producing a set of output messages which are sent back to
   * them.
   *
   * @param incoming
   *          The set of incoming messages from the physical units.
   * @param outgoing
   *          Messages generated during the execution of this method should be written here.
   */
  public void clock(Mailbox incoming, Mailbox outgoing) {
    // Send an example message to illustrate the syntax
    outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.INITIALISATION));
  }


  /**
   * This message is displayed in the simulation window, and enables a limited form of debug output.
   * The content of the message has no material effect on the system, and can be whatever is
   * desired. In principle, however, it should display a useful message indicating the current state
   * of the controller.
   *
   * @return
   */
  public String getStatusMessage() {
    return "MY STATUS";
  }

}
