// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package pc.screen;

import general.av.video.VideoSignal;

import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import atari.cartridge.CartridgeSocket;
import atari.controls.ConsoleControlsSocket;

public class ScreenWithConsolePanel extends JPanel {

	public ScreenWithConsolePanel(boolean screenFixedSize, boolean showConsolePanel) {
		super();
		screenPanel = new ScreenPanel();
		screenPanel.screen().setFixedSize(screenFixedSize);
		screenPanel.screen().addControlInputComponent(this);
		screenPanel.consoleControls().addInputComponents(this);
		if (showConsolePanel) consolePanel = new ConsolePanel(screenPanel.screen(), null);
		setup();
	}

	public void connect(VideoSignal videoSignal, ConsoleControlsSocket controlsSocket, CartridgeSocket cartridgeSocket) {
		screenPanel.connect(videoSignal, controlsSocket, cartridgeSocket);
		if (consolePanel != null) consolePanel.connect(controlsSocket);
	}
	
	public void powerOn() {
		screenPanel.powerOn();
	}
	
	public void powerOff() {
		screenPanel.powerOff();
	}
	
	private void setup() {
		screenPanel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (e.getComponent() == screenPanel) validate();
			}});		
		setLayout(new FlowLayout(
				FlowLayout.CENTER,
				0, 0
		));
		setOpaque(false);
		add(screenPanel);
		if (consolePanel != null) add(consolePanel);
		validate();
	}

	public ScreenPanel screenPanel;
	public ConsolePanel consolePanel;


	private static final long serialVersionUID = 1L;

}
