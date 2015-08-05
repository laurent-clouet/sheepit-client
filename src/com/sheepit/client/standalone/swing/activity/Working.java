package com.sheepit.client.standalone.swing.activity;

import java.awt.GridBagConstraints;
import java.awt.SystemTray;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import com.sheepit.client.Client;
import com.sheepit.client.Server;
import com.sheepit.client.standalone.GuiSwing;
import com.sheepit.client.standalone.GuiSwing.ActivityType;

public class Working implements Activity {
	GuiSwing parent;
	
	JLabel statusContent;
	JLabel renderedFrameContent;
	JLabel remainingFrameContent;
	JLabel lastRender;
	JLabel creditEarned;
	JButton pauseButton;
	
	public Working(GuiSwing parent_) {
		parent = parent_;
		
		statusContent = new JLabel("Init");
		renderedFrameContent = new JLabel("0");
		remainingFrameContent = new JLabel("0");
		creditEarned = new JLabel("");
		
		lastRender = new JLabel();
	}
	
	@Override
	public void show() {
		GridBagConstraints constraints = new GridBagConstraints();
		int currentRow = 0;
		
		parent.addPadding(1, ++currentRow, 2, 1);
		++currentRow;
		
		ImageIcon image = new ImageIcon(getClass().getResource("/title.png"));
		JLabel labelImage = new JLabel(image);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 3.0;
		constraints.gridwidth = 2;
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(labelImage, constraints);
		
		parent.addPadding(1, ++currentRow, 2, 1);
		++currentRow;
		
		JLabel statusLabel = new JLabel("Status:");
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weighty = 0.0;
		constraints.gridwidth = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(statusLabel, constraints);
		
		statusContent.setVerticalAlignment(JLabel.TOP);
		statusContent.setVerticalTextPosition(JLabel.TOP);
		constraints.gridx = 2;
		parent.getContentPane().add(statusContent, constraints);
		
		parent.addPadding(1, ++currentRow, 2, 1);
		++currentRow;
		
		JLabel creditsEarnedLabel = new JLabel("Credits earned:");
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(creditsEarnedLabel, constraints);
		
		constraints.gridx = 2;
		parent.getContentPane().add(creditEarned, constraints);
		
		parent.addPadding(1, ++currentRow, 2, 1);
		++currentRow;
		
		JLabel renderedFrameLabel = new JLabel("Rendered frames:");
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(renderedFrameLabel, constraints);
		
		constraints.gridx = 2;
		parent.getContentPane().add(renderedFrameContent, constraints);
		
		parent.addPadding(1, ++currentRow, 2, 1);
		++currentRow;
		
		JLabel remainingFrameLabel = new JLabel("Remaining frames:");
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(remainingFrameLabel, constraints);
		
		constraints.gridx = 2;
		parent.getContentPane().add(remainingFrameContent, constraints);
		
		parent.addPadding(1, ++currentRow, 2, 1);
		++currentRow;
		
		JLabel lastRenderedFrameLabel = new JLabel("Last rendered frame:");
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(lastRenderedFrameLabel, constraints);
		
		constraints.gridx = 2;
		parent.getContentPane().add(lastRender, constraints);
		
		parent.addPadding(1, ++currentRow, 2, 1);
		++currentRow;
		
		JButton settingsButton = new JButton("Settings");
		settingsButton.addActionListener(new SettingsAction());
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(settingsButton, constraints);
		
		pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new PauseAction());
		constraints.gridx = 2;
		parent.getContentPane().add(pauseButton, constraints);
		
		//Add hide button if os supports it
		if (SystemTray.isSupported()) {
			JButton hideButton = new JButton("Hide window");
			hideButton.addActionListener(new HideAction());
			constraints.gridx = 3;
			parent.getContentPane().add(hideButton, constraints);
		}
		
		parent.addPadding(1, ++currentRow, 2, 1);
		parent.addPadding(0, 0, 1, currentRow + 1);
		parent.addPadding(3, 0, 1, currentRow + 1);
	}
	
	public void setStatus(String msg_) {
		statusContent.setText("<html>" + msg_ + "</html>"); // html for the text wrapping
	}
	
	public void setRemainingFrame(int n) {
		remainingFrameContent.setText(String.valueOf(n));
	}
	
	public void setRenderedFrame(int n) {
		renderedFrameContent.setText(String.valueOf(n));
		showCreditEarned();
		showLastRender();
	}
	
	public void showLastRender() {
		Client client = parent.getClient();
		if (client != null) {
			Server server = client.getServer();
			if (server != null) {
				byte[] data = server.getLastRender();
				if (data != null) {
					InputStream is = new ByteArrayInputStream(data);
					try {
						BufferedImage image = ImageIO.read(is);
						lastRender.setIcon(new ImageIcon(image));
					}
					catch (IOException e) {
						System.out.println("Working::showLastRender() exception " + e);
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	public void showCreditEarned() {
		Client client = parent.getClient();
		if (client != null) {
			Server server = client.getServer();
			if (server != null) {
				String data = server.getCreditEarnedOnCurrentSession();
				if (data != null) {
					creditEarned.setText(data);
				}
			}
		}
	}
	
	class PauseAction implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Client client = parent.getClient();
			if (client != null) {
				if (client.isSuspended()) {
					pauseButton.setText("Pause");
					client.resume();
				}
				else {
					pauseButton.setText("Resume");
					client.suspend();
				}
			}
		}
	}
	
	class SettingsAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (parent != null) {
				parent.showActivity(ActivityType.SETTINGS);
			}
		}
	}
	
	class HideAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (parent != null) {
				parent.hideToTray();
			}
		}
	}
	
}
