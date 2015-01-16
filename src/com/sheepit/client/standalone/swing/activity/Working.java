package com.sheepit.client.standalone.swing.activity;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
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
	
	public Working(GuiSwing parent_) {
		parent = parent_;
		
		statusContent = new JLabel("Init");
		renderedFrameContent = new JLabel("0");
		remainingFrameContent = new JLabel("0");
		
		lastRender = new JLabel();
	}
	
	@Override
	public void show() {
		int n = 10;
		int size_height_label = 24;
		int sep = 45;
		int start_label_left = 109;
		int start_label_right = 280;
		int end_label_right = 490;
		
		ImageIcon image = new ImageIcon(getClass().getResource("/title.png"));
		JLabel labelImage = new JLabel(image);
		labelImage.setBounds(600 / 2 - 265 / 2, n, 265, 130 + n);
		n = labelImage.getHeight();
		parent.getContentPane().add(labelImage);
		
		n += 40;
		
		JLabel statusLabel = new JLabel("Status:");
		statusLabel.setBounds(start_label_left, n, 240, size_height_label);
		parent.getContentPane().add(statusLabel);
		
		statusContent.setBounds(start_label_right, n, 600 - 20 - start_label_right, size_height_label);
		parent.getContentPane().add(statusContent);
		
		n += sep;
		
		JLabel renderedFrameLabel = new JLabel("Rendered Frame:");
		renderedFrameLabel.setBounds(start_label_left, n, 240, size_height_label);
		parent.getContentPane().add(renderedFrameLabel);
		
		renderedFrameContent.setBounds(start_label_right, n, end_label_right - start_label_right, size_height_label);
		parent.getContentPane().add(renderedFrameContent);
		
		n += sep;
		
		JLabel remainingFrameLabel = new JLabel("Remaining Frame:");
		remainingFrameLabel.setBounds(start_label_left, n, 240, size_height_label);
		parent.getContentPane().add(remainingFrameLabel);
		
		remainingFrameContent.setBounds(start_label_right, n, end_label_right - start_label_right, size_height_label);
		parent.getContentPane().add(remainingFrameContent);
		
		n += sep;
		
		JLabel lastRenderedFrameLabel = new JLabel("Last rendered frame:");
		lastRenderedFrameLabel.setBounds(start_label_left, n, 240, size_height_label);
		parent.getContentPane().add(lastRenderedFrameLabel);
		
		lastRender.setBounds(start_label_right, n, 200, 112);
		parent.getContentPane().add(lastRender);
		
		JButton settingsButton = new JButton("Settings");
		settingsButton.setBounds(220, 500, 100, 25);
		settingsButton.addActionListener(new SettingsAction());
		parent.getContentPane().add(settingsButton);
		
		JButton pauseButton = new JButton("Pause");
		pauseButton.setBounds(330, 500, 80, 25);
		pauseButton.addActionListener(new PauseAction());
		parent.getContentPane().add(pauseButton);
	}
	
	public void setStatus(String msg_) {
		statusContent.setText(msg_);
	}
	
	public void setRemainingFrame(int n) {
		remainingFrameContent.setText(String.valueOf(n));
	}
	
	public void setRenderedFrame(int n) {
		renderedFrameContent.setText(String.valueOf(n));
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
	
	class PauseAction implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Client client = parent.getClient();
			if (client != null) {
				client.suspend();
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
	
}
