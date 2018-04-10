package steam.boiler.core;

import steam.boiler.model.PhysicalUnits;

//import org.eclipse.jdt.annotation.NonNull;
//import org.eclipse.jdt.annotation.Nullable;

import steam.boiler.util.Mailbox;
import steam.boiler.util.SteamBoilerCharacteristics;
import steam.boiler.util.Mailbox.Message;
import steam.boiler.util.Mailbox.MessageKind;

public class SteamBoilerController {

	private Pump pumps[];
	private Mailbox.Mode mode;
	private double M1;
	private double M2;
	private double N1;
	private double N2;
	private double steam_V = 0;
	private double waterLevel = 0;
	private double evacuationRate;
	private double capacity;
	private double maximalSteamRate;
	private boolean arePhysicalUnitsReadv;
	private boolean isSteamBoilerWaiting;

	/**
	 * Construct a steam boiler controller for a given set of characteristics.
	 *
	 * @param configuration
	 *            The boiler characteristics to be used.
	 */
	public SteamBoilerController(SteamBoilerCharacteristics configuration) {
		initialize(configuration);

	}

	private void initialize(SteamBoilerCharacteristics configuration) {

		pumps = new Pump[configuration.getNumberOfPumps()];
		for (int index = 0; index < pumps.length; index++) {
			pumps[index] = new Pump();
		}
		for (int index = 0; index < configuration.getNumberOfPumps(); index++) {
			pumps[index].setPumpCapacity(configuration.getPumpCapacity(index));
		}

		mode = Mailbox.Mode.INITIALISATION;
		M2 = configuration.getMaximalLimitLevel();
		M1 = configuration.getMinimalLimitLevel();
		N1 = configuration.getMinimalLimitLevel();
		N2 = configuration.getMaximalNormalLevel();
		evacuationRate = configuration.getEvacuationRate();
		capacity = configuration.getCapacity();
		maximalSteamRate = configuration.getMaximualSteamRate();

		arePhysicalUnitsReadv = false;
		isSteamBoilerWaiting = false;

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
	public void clock(Mailbox incoming, Mailbox outgoing) {

		for (int index = 0; index < incoming.size(); index++) {
			checkMessage(incoming.read(index));
		}

		switch (mode) {
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

		}
		// Send an example message to illustrate the syntax

	}

	private void rescue(Mailbox outgoing) {

	}

	private void emergencyStop(Mailbox outgoing) {

	}

	private void degraded(Mailbox outgoing) {

	}

	private void normal(Mailbox outgoing) {

	}

	private void initialisation(Mailbox outgoing) {

		if (arePhysicalUnitsReadv) {
			isSteamBoilerWaiting = false;
			mode = Mailbox.Mode.NORMAL;
			outgoing.send(new Message(Mailbox.MessageKind.MODE_m, mode));
			return;
		}
		
		
		
		else if (isSteamBoilerWaiting) {
			
			if (steam_V != 0) {
				outgoing.send(new Message(Mailbox.MessageKind.MODE_m, Mailbox.Mode.EMERGENCY_STOP));
			}
			
			if (N1 < waterLevel && waterLevel < N2) {
				outgoing.send(new Message(Mailbox.MessageKind.PROGRAM_READY));
			} else {
				adjustWaterLevel(outgoing);
			}

		}
		outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.INITIALISATION));
	}

	private void adjustWaterLevel(Mailbox outgoing) {
		if (waterLevel < N1) {
			double cap = (N1 + N2 / 2) - waterLevel;
			cap /= (15 * pumps[0].getPumpCapacity());
			cap = Math.min(cap, pumps.length);
			openPumps(cap, outgoing);

		} else if (waterLevel > N2) {
			outgoing.send(new Message(Mailbox.MessageKind.VALVE));
		}
	}

	private void openPumps(double number, Mailbox outgoing) {
		for (int index = 0; index < number; index++) {
			pumps[index].setIsOn(true);
			outgoing.send(new Message(Mailbox.MessageKind.OPEN_PUMP_n, index));
		}

	}

	private void closeAllPumps(Mailbox outgoing) {
		for (int index = 0; index < pumps.length; index++) {
			pumps[index].setIsOn(false);
			outgoing.send(new Message(Mailbox.MessageKind.CLOSE_PUMP_n, index));
		}

	}

	private void checkMessage(Message message) {
		switch (message.getKind()) {
		case MODE_m:
			mode = message.getModeParameter();
			break;

		case STEAM_BOILER_WAITING:
			isSteamBoilerWaiting = true;
			break;

		case PHYSICAL_UNITS_READY:
			arePhysicalUnitsReadv = true;
			break;

		case PUMP_STATE_n_b:
			pumps[message.getIntegerParameter()].setIsOn(message.getBooleanParameter());
			break;

		case PUMP_CONTROL_STATE_n_b:
			pumps[message.getIntegerParameter()].setControllerOn(message.getBooleanParameter());
			break;

		case LEVEL_v:
			waterLevel = message.getDoubleParameter();
			break;

		case STEAM_v:
			steam_V = message.getDoubleParameter();
			break;

		default:
			break;

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
	public String getStatusMessage() {
		return "MY STATUS";
	}

}
