/*
 * Copyright (C) 2017 Laurent CLOUET
 * Author Laurent CLOUET <laurent.clouet@nopnop.net>
 *
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.sheepit.proxy.main;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import com.sheepit.proxy.Configuration;
import com.sheepit.proxy.Gui;
import com.sheepit.proxy.ProxyRunner;
import com.sheepit.proxy.SettingsLoader;

public class GuiSwing extends JFrame implements Gui {
	public static final String type = "swing";
	
	private static final String DUMMY_CACHE_DIR = "Auto detected";
	
	private JPanel panel;
	private ProxyRunner runner;
	
	private ThreadProxy threadProxy;
	
	private JLabel cacheDirText;
	private File cacheDir;
	private JFileChooser cacheDirChooser;
	
	private JLabel statusContent;
	private JTextField port;
	
	private JCheckBox saveFile;
	JButton saveButton;
	
	public GuiSwing() {
	}
	
	@Override
	public void status(String msg_) {
		statusContent.setText(msg_);
	}
	
	@Override
	public void error(String msg_) {
		statusContent.setText("Error: " + msg_);
	}
	
	@Override
	public void setProxyRunner(ProxyRunner runner_) {
		runner = runner_;
	}
	
	@Override
	public ProxyRunner getProxyRunner() {
		return runner;
	}
	
	@Override
	public void start() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
		
		URL iconUrl = getClass().getResource("/icon.png");
		if (iconUrl != null) {
			ImageIcon img = new ImageIcon(iconUrl);
			setIconImage(img.getImage());
		}
		
		setTitle("SheepIt Render Farm Proxy");
		setSize(420, 580);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		setContentPane(this.panel);
		panel.setBorder(new EmptyBorder(20, 20, 20, 20));
		
		new SettingsLoader().merge(runner.getConfiguration());
		
		GridBagConstraints constraints = new GridBagConstraints();
		int currentRow = 0;
		ImageIcon image = new ImageIcon(getClass().getResource("/title.png"));
		constraints.fill = GridBagConstraints.CENTER;
		
		JLabel labelImage = new JLabel(image);
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		panel.add(labelImage, constraints);
		
		currentRow++;
		
		JPanel status_panel = new JPanel(new GridLayout(1, 1));
		status_panel.setBorder(BorderFactory.createTitledBorder("Status"));
		
		statusContent = new JLabel("Initialization");
		status_panel.add(statusContent);
		
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(status_panel, constraints);
		
		// directory
		JPanel directory_panel = new JPanel(new GridLayout(1, 3));
		directory_panel.setBorder(BorderFactory.createTitledBorder("Cache"));
		JLabel cacheLabel = new JLabel("Working directory:");
		directory_panel.add(cacheLabel);
		String destination = DUMMY_CACHE_DIR;
		if (runner.getConfiguration().getUserSpecifiedACacheDir()) {
			destination = runner.getConfiguration().getCacheDir().getName();
		}
		
		JPanel cacheDirWrapper = new JPanel();
		cacheDirWrapper.setLayout(new BoxLayout(cacheDirWrapper, BoxLayout.LINE_AXIS));
		cacheDirText = new JLabel(destination);
		cacheDirWrapper.add(cacheDirText);
		
		cacheDirWrapper.add(Box.createHorizontalGlue());
		
		cacheDirChooser = new JFileChooser();
		cacheDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		JButton openButton = new JButton("...");
		openButton.addActionListener(new ChooseFileAction());
		cacheDirWrapper.add(openButton);
		
		directory_panel.add(cacheDirWrapper);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		
		panel.add(directory_panel, constraints);
		
		JPanel network_panel = new JPanel(new GridLayout(1, 2));
		network_panel.setBorder(BorderFactory.createTitledBorder("Network"));
		
		JLabel portLabel = new JLabel("Binding port:");
		port = new JTextField();
		port.setText(Integer.toString(runner.getConfiguration().getPort()));
		
		network_panel.add(portLabel);
		network_panel.add(port);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		panel.add(network_panel, constraints);
		
		JPanel general_panel = new JPanel(new GridLayout(1, 2));
		
		saveFile = new JCheckBox("Save settings", true);
		general_panel.add(saveFile);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		panel.add(general_panel, constraints);
		
		String buttonText = "Start";
		if (runner != null && runner.isRunning()) {
			buttonText = "Stop";
		}
		saveButton = new JButton(buttonText);
		checkDisplaySaveButton();
		saveButton.addActionListener(new SaveAction());
		currentRow++;
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		panel.add(saveButton, constraints);
		
		setVisible(true);
		panel.repaint();
	}
	
	@Override
	public void stop() {
		System.exit(0);
	}
	
	public boolean checkDisplaySaveButton() {
		if (port.getText().isEmpty()) {
			return false;
		}
		
		try {
			Integer.parseInt(port.getText().replaceAll(",", ""));
		}
		catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	class ChooseFileAction implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JOptionPane.showMessageDialog(panel, "<html>The working directory has to be dedicated directory. <br />Caution, everything not related to SheepIt-Renderfarm will be removed.<br />You should create a directory specifically for it.</html>", "Warning: files will be removed!", JOptionPane.WARNING_MESSAGE);
			int returnVal = cacheDirChooser.showOpenDialog(panel);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = cacheDirChooser.getSelectedFile();
				cacheDir = file;
				cacheDirText.setText(cacheDir.getName());
			}
		}
	}
	
	class SaveAction implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Configuration config = runner.getConfiguration();
			if (config == null) {
				return;
			}
			
			if (cacheDir != null) {
				File fromConfig = config.getCacheDir();
				if (fromConfig != null && fromConfig.getAbsolutePath().equals(cacheDir.getAbsolutePath()) == false) {
					config.setCacheDir(cacheDir);
				}
				else {
					// do nothing because the directory is the same as before
				}
			}
			
			int p = -1;
			try {
				p = Integer.parseInt(port.getText().replaceAll(",", ""));
			}
			catch (NumberFormatException e1) {
				return;
			}
			
			config.setPort(p);
			
			String cachePath = null;
			if (config.getUserSpecifiedACacheDir() && config.getCacheDir() != null) {
				cachePath = config.getCacheDir().getAbsolutePath();
			}
			
			if (saveFile.isSelected()) {
				new SettingsLoader(port.getText(), cachePath, GuiSwing.type).saveFile();
			}
			else {
				try {
					new File(new SettingsLoader().getFilePath()).delete();
				}
				catch (SecurityException e3) {
				}
			}
			
			if (runner != null) {
				if (runner.isRunning()) {
					saveButton.setText("Start");
					runner.stop();
					threadProxy = null;
				}
				else {
					saveButton.setText("Stop");
					
					// start it
					if (threadProxy == null || threadProxy.isAlive() == false) {
						threadProxy = new ThreadProxy();
						threadProxy.start();
					}
				}
			}
		}
	}
	
	public class ThreadProxy extends Thread {
		@Override
		public void run() {
			if (GuiSwing.this.runner != null) {
				GuiSwing.this.runner.run();
			}
		}
	}
}
