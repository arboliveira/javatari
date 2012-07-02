// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package pc.room;

import parameters.Parameters;
import pc.cartridge.ROMLoader;
import pc.controls.AWTConsoleControls;
import pc.room.settings.SettingsDialog;
import pc.savestate.FileSaveStateMedia;
import pc.screen.DesktopScreenWindow;
import pc.screen.Screen;
import pc.speaker.Speaker;
import utils.Terminator;
import atari.cartridge.Cartridge;
import atari.console.Console;
import atari.network.ClientConsole;
import atari.network.RemoteReceiver;
import atari.network.RemoteTransmitter;
import atari.network.ServerConsole;

public class Room {
	
	Room() {
		super();
	}

	public void powerOn() {
		screen.powerOn();
	 	speaker.powerOn();
	 	if (currentConsole.cartridgeSocket().inserted() != null) currentConsole.powerOn();
	}

	public void powerOff() {
	 	currentConsole.powerOff();
	 	speaker.powerOff();
		screen.powerOff();
	}

	public Console currentConsole() {
		return currentConsole;
	}

	public Console standaloneCurrentConsole() {
		if (currentConsole != standaloneConsole) throw new IllegalStateException();
		return standaloneConsole;
	}

	public ServerConsole serverCurrentConsole() {
		if (currentConsole != serverConsole) throw new IllegalStateException();
		return serverConsole;
	}

	public ClientConsole clientCurrentConsole() {
		if (currentConsole != clientConsole) throw new IllegalStateException();
		return clientConsole;
	}

	public Screen screen() {
		return screen;
	}

	public Speaker speaker() {
		return speaker;
	}
	
	public AWTConsoleControls controls() {
		return controls;
	}
	
	public FileSaveStateMedia stateMedia() {
		return stateMedia;
	}

	public boolean isStandaloneMode() {
		return currentConsole == standaloneConsole;
	}

	public boolean isServerMode() {
		return currentConsole == serverConsole;
	}
	
	public boolean isClientMode() {
		return currentConsole == clientConsole;
	}
	
	public void morphToStandaloneMode() {
		if (isStandaloneMode()) return;
		powerOff();
		Cartridge lastCartridge = isClientMode() ? null : currentConsole.cartridgeSocket().inserted();
		if (standaloneConsole == null) buildAndPlugStandaloneConsole();
		else plugConsole(standaloneConsole);
		if (lastCartridge != null) currentConsole.cartridgeSocket().insert(lastCartridge, false);
		powerOn();
	}

	public void morphToServerMode() {
		if (isServerMode()) return;
		powerOff();
		Cartridge lastCartridge = isClientMode() ? null : currentConsole.cartridgeSocket().inserted();
		if (serverConsole == null) buildAndPlugServerConsole();
		else plugConsole(serverConsole);
		if (lastCartridge != null) currentConsole.cartridgeSocket().insert(lastCartridge, false);
		powerOn();
	}

	public void morphToClientMode() {
		if (isClientMode()) return;
		powerOff();
		if (clientConsole == null) buildAndPlugClientConsole();
		else plugConsole(clientConsole);
		powerOn();
	}

	public void destroy() {
		powerOff();
		if (standaloneConsole != null) standaloneConsole.destroy();
		if (serverConsole != null) serverConsole.destroy();
		if (clientConsole != null) clientConsole.destroy();
		screen.destroy();
		speaker.destroy();
		currentRoom = null;
	}
	
	protected void buildPeripherals() {
		// PC interfaces for Video, Audio, Controls, Cartridge and SaveState
		if (screen != null) throw new IllegalStateException();
		screen = buildScreenPeripheral();
		speaker = new Speaker();
		controls = new AWTConsoleControls(screen.monitor());
		controls.addInputComponents(screen.controlsInputComponents());
		stateMedia = new FileSaveStateMedia();
	}

	protected Screen buildScreenPeripheral() {
		return new DesktopScreenWindow();
	}

	private void plugConsole(Console console) {
		if (currentConsole == console) return;
		currentConsole = console;
		screen.connect(currentConsole.videoOutput(), currentConsole.controlsSocket(), currentConsole.cartridgeSocket());
		speaker.connect(currentConsole.audioOutput());
		controls.connect(currentConsole.controlsSocket());
		stateMedia.connect(currentConsole.saveStateSocket());
	}
	
	private void insertCartridgeProvided() {
		if (Parameters.mainArg == null) return;
		Cartridge cart = ROMLoader.load(Parameters.mainArg);
		if (cart != null) currentConsole.cartridgeSocket().insert(cart, false);
		else Terminator.terminate();	// Error loading Cartridge
	}

	private Console buildAndPlugStandaloneConsole() {
		if (standaloneConsole != null) throw new IllegalStateException();
		standaloneConsole = new Console();
		plugConsole(standaloneConsole);
		return standaloneConsole;
	}

	private ServerConsole buildAndPlugServerConsole() {
		if (serverConsole != null) throw new IllegalStateException();
		RemoteTransmitter remoteTransmitter = new RemoteTransmitter();
		serverConsole = new ServerConsole(remoteTransmitter);
		plugConsole(serverConsole);
		return serverConsole;
	}
	
	private ClientConsole buildAndPlugClientConsole() {
		RemoteReceiver remoteReceiver = new RemoteReceiver();
		clientConsole = new ClientConsole(remoteReceiver);
		plugConsole(clientConsole);
		return clientConsole;
	}	

	
	public static Room currentRoom() {
		return currentRoom;
	}

	public static Room buildStandaloneRoom() {
		if (currentRoom != null) throw new IllegalStateException("Room already built");
		currentRoom = new Room();
		currentRoom.buildPeripherals();
		currentRoom.buildAndPlugStandaloneConsole();
		currentRoom.insertCartridgeProvided();
		return currentRoom;
	}

	public static Room buildServerRoom() {
		if (currentRoom != null) throw new IllegalStateException("Room already built");
		currentRoom = new Room();
		currentRoom.buildPeripherals();
		currentRoom.buildAndPlugServerConsole();
		currentRoom.insertCartridgeProvided();
		return currentRoom;
	}

	public static Room buildClientRoom() {
		if (currentRoom != null) throw new IllegalStateException("Room already built");
		currentRoom = new Room();
		currentRoom.buildPeripherals();
		// Automatically adjust interface for Multiplayer Client operation
		currentRoom.controls().p1ControlsMode(true);
		currentRoom.screen().monitor().setCartridgeChangeEnabled(false);
		currentRoom.buildAndPlugClientConsole();
		// Insert no Cartridge
		return currentRoom;
	}

	public static Room buildAppletRoom() {
		if (currentRoom != null) throw new IllegalStateException("Room already built");
		currentRoom = new AppletRoom();
		currentRoom.buildPeripherals();
		currentRoom.buildAndPlugStandaloneConsole();
		currentRoom.insertCartridgeProvided();
		return currentRoom;
	}

	public static void openCurrentRoomSettings() {
		if (settingsDialog == null) settingsDialog = new SettingsDialog(currentRoom);
		settingsDialog.setVisible(true);
	}

	
	private Console currentConsole;
	private Console	standaloneConsole;
	private ServerConsole serverConsole;
	private ClientConsole clientConsole;

	private Screen screen;
	private Speaker speaker;
	private AWTConsoleControls controls;
	private FileSaveStateMedia stateMedia;
	
	private static Room currentRoom;
	private static SettingsDialog settingsDialog;
		
}
