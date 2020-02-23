/*
 * Copyright (C) 2015 Laurent CLOUET
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

package com.sheepit.client.standalone.swing.activity;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.sheepit.client.Configuration;
import com.sheepit.client.Configuration.ComputeType;
import com.sheepit.client.SettingsLoader;
import com.sheepit.client.hardware.cpu.CPU;
import com.sheepit.client.hardware.gpu.GPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.network.Proxy;
import com.sheepit.client.os.OS;
import com.sheepit.client.standalone.GuiSwing;
import com.sheepit.client.standalone.swing.components.CollapsibleJPanel;

public class Settings implements Activity {
	private static final String DUMMY_CACHE_DIR = "Auto detected";
	
	private GuiSwing parent;
	
	private JTextField login;
	private JPasswordField password;
	private JLabel cacheDirText;
	private File cacheDir;
	private JFileChooser cacheDirChooser;
	private JCheckBox useCPU;
	private List<JCheckBoxGPU> useGPUs;
	private JSlider cpuCores;
	private JSlider ram;
	private JSpinner renderTime;
	private JSlider priority;
	private JTextField proxy;
	private JTextField hostname;
	
	private JCheckBox saveFile;
	private JCheckBox autoSignIn;
	JButton saveButton;
	
	private boolean haveAutoStarted;
	
	public Settings(GuiSwing parent_) {
		parent = parent_;
		cacheDir = null;
		useGPUs = new LinkedList<JCheckBoxGPU>();
		haveAutoStarted = false;
	}
	
	@Override
	public void show() {
		Configuration config = parent.getConfiguration();
		new SettingsLoader(config.getConfigFilePath()).merge(config);
		
		List<GPUDevice> gpus = GPU.listDevices(config);
		
		GridBagConstraints constraints = new GridBagConstraints();

		int currentRow = 0;
		ImageIcon image = new ImageIcon(getClass().getResource("/title.png"));
		constraints.fill = GridBagConstraints.CENTER;
		
		JLabel labelImage = new JLabel(image);
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		parent.getContentPane().add(labelImage, constraints);
		
		currentRow++;
		
		// authentication
		CollapsibleJPanel authPanel = new CollapsibleJPanel(new GridLayout(2, 2));
		authPanel.setBorder(BorderFactory.createTitledBorder("Authentication"));
		
		JLabel loginLabel = new JLabel("Username:");
		login = new JTextField();
		login.setText(parent.getConfiguration().getLogin());
		login.setColumns(20);
		login.addKeyListener(new CheckCanStart());

		JLabel passwordLabel = new JLabel("Password:");
		password = new JPasswordField();
		password.setText(parent.getConfiguration().getPassword());
		password.setColumns(10);
		password.addKeyListener(new CheckCanStart());
		
		authPanel.add(loginLabel);
		authPanel.add(login);
		
		authPanel.add(passwordLabel);
		authPanel.add(password);
		
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		parent.getContentPane().add(authPanel, constraints);
		
		// directory
		CollapsibleJPanel directoryPanel = new CollapsibleJPanel(new GridLayout(1, 3));
		directoryPanel.setBorder(BorderFactory.createTitledBorder("Cache"));

		JLabel cacheLabel = new JLabel("Working directory:");
		directoryPanel.add(cacheLabel);
		String destination = DUMMY_CACHE_DIR;
		if (config.isUserHasSpecifiedACacheDir()) {
			destination = config.getCacheDirForSettings().getName();
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
		
		directoryPanel.add(cacheDirWrapper);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		
		parent.getContentPane().add(directoryPanel, constraints);
		
		// compute devices
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints computeDevicesConstraints = new GridBagConstraints();
		CollapsibleJPanel computeDevicesPanel = new CollapsibleJPanel(gridbag);
		
		computeDevicesPanel.setBorder(BorderFactory.createTitledBorder("Compute devices"));
		
		ComputeType method = config.getComputeMethod();

		useCPU = new JCheckBox("CPU");
		boolean gpuChecked = false;
		
		if (method == ComputeType.CPU_GPU) {
			useCPU.setSelected(true);
			gpuChecked = true;
		}
		else if (method == ComputeType.CPU) {
			useCPU.setSelected(true);
			gpuChecked = false;
		}
		else if (method == ComputeType.GPU) {
			useCPU.setSelected(false);
			gpuChecked = true;
		}
		useCPU.addActionListener(new CpuChangeAction());
		
		computeDevicesConstraints.gridx = 1;
		computeDevicesConstraints.gridy = 0;
		computeDevicesConstraints.fill = GridBagConstraints.BOTH;
		computeDevicesConstraints.weightx = 1.0;
		computeDevicesConstraints.weighty = 1.0;
		
		gridbag.setConstraints(useCPU, computeDevicesConstraints);
		computeDevicesPanel.add(useCPU);
		
		for (GPUDevice gpu : gpus) {
			JCheckBoxGPU gpuCheckBox = new JCheckBoxGPU(gpu);
			gpuCheckBox.setToolTipText(gpu.getId());
			if (gpuChecked) {
				GPUDevice configGPU = config.getGPUDevice();
				if (configGPU != null && configGPU.getId().equals(gpu.getId())) {
					gpuCheckBox.setSelected(gpuChecked);
				}
			}
			gpuCheckBox.addActionListener(new GpuChangeAction());
			
			computeDevicesConstraints.gridy++;
			gridbag.setConstraints(gpuCheckBox, computeDevicesConstraints);
			computeDevicesPanel.add(gpuCheckBox);
			useGPUs.add(gpuCheckBox);
		}
		
		CPU cpu = new CPU();
		if (cpu.cores() > 1) { // if only one core is available, no need to show the choice
			double step = 1;
			double display = (double)cpu.cores() / step;
			while (display > 10) {
				step += 1.0;
				display = (double)cpu.cores() / step;
			}
			
			cpuCores = new JSlider(1, cpu.cores());
			cpuCores.setMajorTickSpacing((int)(step));
			cpuCores.setMinorTickSpacing(1);
			cpuCores.setPaintTicks(true);
			cpuCores.setPaintLabels(true);
			cpuCores.setValue(config.getNbCores() != -1 ? config.getNbCores() : cpuCores.getMaximum());
			JLabel coreLabel = new JLabel("CPU cores:");
			
			computeDevicesConstraints.weightx = 1.0 / gpus.size();
			computeDevicesConstraints.gridx = 0;
			computeDevicesConstraints.gridy++;
			
			gridbag.setConstraints(coreLabel, computeDevicesConstraints);
			computeDevicesPanel.add(coreLabel);
			
			computeDevicesConstraints.gridx = 1;
			computeDevicesConstraints.weightx = 1.0;
			
			gridbag.setConstraints(cpuCores, computeDevicesConstraints);
			computeDevicesPanel.add(cpuCores);
		}
		
		// max ram allowed to render
		OS os = OS.getOS();
		int allRAM = (int) os.getMemory();
		ram = new JSlider(0, allRAM);
		int step = 1000000;
		double display = (double)allRAM / (double)step;
		while (display > 10) {
			step += 1000000;
			display = (double)allRAM / (double)step;
		}
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		for (int g = 0; g < allRAM; g += step) {
			labelTable.put(g, new JLabel("" + (g / 1000000)));
		}
		ram.setMajorTickSpacing(step);
		ram.setLabelTable(labelTable);
		ram.setPaintTicks(true);
		ram.setPaintLabels(true);
		ram.setValue((int)(config.getMaxMemory() != -1 ? config.getMaxMemory() : os.getMemory()));
		JLabel ramLabel = new JLabel("Memory:");
		
		computeDevicesConstraints.weightx = 1.0 / gpus.size();
		computeDevicesConstraints.gridx = 0;
		computeDevicesConstraints.gridy++;
		
		gridbag.setConstraints(ramLabel, computeDevicesConstraints);
		computeDevicesPanel.add(ramLabel);
		
		computeDevicesConstraints.gridx = 1;
		computeDevicesConstraints.weightx = 1.0;
		
		gridbag.setConstraints(ram, computeDevicesConstraints);
		computeDevicesPanel.add(ram);
		
		parent.getContentPane().add(computeDevicesPanel, constraints);
		
		// priority
		boolean highPrioritySupport = os.getSupportHighPriority();
		priority = new JSlider(highPrioritySupport ? -19 : 0, 19);
		priority.setMajorTickSpacing(19);
		priority.setMinorTickSpacing(1);
		priority.setPaintTicks(true);
		priority.setPaintLabels(true);
		priority.setValue(config.getPriority());
		JLabel priorityLabel = new JLabel(highPrioritySupport ? "Priority (High <-> Low):" : "Priority (Normal <-> Low):" );
		
		computeDevicesConstraints.weightx = 1.0 / gpus.size();
		computeDevicesConstraints.gridx = 0;
		computeDevicesConstraints.gridy++;
		
		gridbag.setConstraints(priorityLabel, computeDevicesConstraints);
		computeDevicesPanel.add(priorityLabel);
		
		computeDevicesConstraints.gridx = 1;
		computeDevicesConstraints.weightx = 1.0;
		
		gridbag.setConstraints(priority, computeDevicesConstraints);
		computeDevicesPanel.add(priority);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		parent.getContentPane().add(computeDevicesPanel, constraints);
		
		// other
		CollapsibleJPanel advancedPanel = new CollapsibleJPanel(new GridLayout(3, 2));
		advancedPanel.setBorder(BorderFactory.createTitledBorder("Advanced options"));
		
		JLabel proxyLabel = new JLabel("Proxy:");
		proxyLabel.setToolTipText("http://login:password@host:port");
		proxy = new JTextField();
		proxy.setToolTipText("http://login:password@host:port");
		proxy.setText(parent.getConfiguration().getProxy());
		proxy.addKeyListener(new CheckCanStart());
		
		advancedPanel.add(proxyLabel);
		advancedPanel.add(proxy);
		
		JLabel hostnameLabel = new JLabel("Computer name:");
		hostname = new JTextField();
		hostname.setText(parent.getConfiguration().getHostname());
		
		advancedPanel.add(hostnameLabel);
		advancedPanel.add(hostname);
		
		JLabel renderTimeLabel = new JLabel("Max time per frame (in minute):");
		int val = 0;
		if (parent.getConfiguration().getMaxRenderTime() > 0) {
			val = parent.getConfiguration().getMaxRenderTime() / 60;
		}
		renderTime = new JSpinner(new SpinnerNumberModel(val,0,1000,1));
		
		advancedPanel.add(renderTimeLabel);
		advancedPanel.add(renderTime);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		parent.getContentPane().add(advancedPanel, constraints);
		advancedPanel.setCollapsed(true);
		
		// general settings
		JPanel generalPanel = new JPanel(new GridLayout(1, 2));
		
		saveFile = new JCheckBox("Save settings", true);
		generalPanel.add(saveFile);
		
		autoSignIn = new JCheckBox("Auto sign in", config.isAutoSignIn());
		autoSignIn.addActionListener(new AutoSignInChangeAction());
		generalPanel.add(autoSignIn);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		parent.getContentPane().add(generalPanel, constraints);
		
		String buttonText = "Start";
		if (parent.getClient() != null) {
			if (parent.getClient().isRunning()) {
				buttonText = "Save";
			}
		}
		saveButton = new JButton(buttonText);
		checkDisplaySaveButton();
		saveButton.addActionListener(new SaveAction());
		currentRow++;
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		parent.getContentPane().add(saveButton, constraints);
		
		if (!haveAutoStarted && config.isAutoSignIn() && checkDisplaySaveButton()) {
			// auto start
			haveAutoStarted = true;
			new SaveAction().actionPerformed(null);
		}
	}
	
	public boolean checkDisplaySaveButton() {
		boolean selected = useCPU.isSelected();
		for (JCheckBoxGPU box : useGPUs) {
			if (box.isSelected()) {
				selected = true;
			}
		}
		if (login.getText().isEmpty() || password.getPassword().length == 0 || !Proxy.isValidURL(proxy.getText())) {
			selected = false;
		}

		saveButton.setEnabled(selected);
		return selected;
	}
	
	class ChooseFileAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JOptionPane.showMessageDialog(parent.getContentPane(), "<html>The working directory has to be dedicated directory. <br />Caution, everything not related to SheepIt-Renderfarm will be removed.<br />You should create a directory specifically for it.</html>", "Warning: files will be removed!", JOptionPane.WARNING_MESSAGE);
			int returnVal = cacheDirChooser.showOpenDialog(parent.getContentPane());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				cacheDir = cacheDirChooser.getSelectedFile();
				cacheDirText.setText(cacheDir.getName());
			}
		}
	}
	
	class CpuChangeAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			checkDisplaySaveButton();
		}
	}
	
	class GpuChangeAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			for (JCheckBox box : useGPUs) {
				if (!box.equals(e.getSource())) {
					box.setSelected(false);
				}
			}
			checkDisplaySaveButton();
		}
	}
	
	class AutoSignInChangeAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (autoSignIn.isSelected()) {
				saveFile.setSelected(true);
			}
		}
	}
	
	class SaveAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (parent == null) {
				return;
			}
			
			Configuration config = parent.getConfiguration();
			if (config == null) {
				return;
			}
			
			if (cacheDir != null) {
				File fromConfig = config.getStorageDir();
				if (fromConfig != null && !fromConfig.getAbsolutePath().equals(cacheDir.getAbsolutePath())) {
					config.setCacheDir(cacheDir);
				}
				else {
					// do nothing because the directory is the same as before
				}
			}
			
			GPUDevice selectedGPU = null;
			for (JCheckBoxGPU box : useGPUs) {
				if (box.isSelected()) {
					selectedGPU = box.getGPUDevice();
				}
			}
			
			ComputeType method = ComputeType.CPU;

			if (useCPU.isSelected() && selectedGPU == null) {
				method = ComputeType.CPU;
			}
			else if (!useCPU.isSelected() && selectedGPU != null) {
				method = ComputeType.GPU;
			}
			else if (useCPU.isSelected() && selectedGPU != null) {
				method = ComputeType.CPU_GPU;
			}
			config.setComputeMethod(method);
			
			if (selectedGPU != null) {
				config.setGPUDevice(selectedGPU);
			}
			
			int cpuCores = -1;
			if (Settings.this.cpuCores != null) {
				cpuCores = Settings.this.cpuCores.getValue();
			}
			
			if (cpuCores > 0) {
				config.setNbCores(cpuCores);
			}
			
			long maxRAM = -1;
			if (ram != null) {
				maxRAM = ram.getValue();
			}
			
			if (maxRAM > 0) {
				config.setMaxMemory(maxRAM);
			}
			
			int maxRendertime = -1;
			if (renderTime != null) {
				maxRendertime = (Integer)renderTime.getValue() * 60;
				config.setMaxRenderTime(maxRendertime);
			}
			
			config.setUsePriority(priority.getValue());
			
			String proxyText = null;
			if (proxy != null) {
				try {
					Proxy.set(proxy.getText());
					proxyText = proxy.getText();
				}
				catch (MalformedURLException e1) {
					System.err.println("Error: wrong url for proxy");
					System.err.println(e1);
					System.exit(2);
				}
			}
			
			parent.setCredentials(login.getText(), new String(password.getPassword()));
			
			String cachePath = null;
			if (config.isUserHasSpecifiedACacheDir() && config.getCacheDirForSettings() != null) {
				cachePath = config.getCacheDirForSettings().getAbsolutePath();
			}
			
			String hostnameText = hostname.getText();
			if (hostnameText == null || hostnameText.isEmpty()) {
				hostnameText = parent.getConfiguration().getHostname();
			}
			
			if (saveFile.isSelected()) {
				parent.setSettingsLoader(new SettingsLoader(config.getConfigFilePath(), login.getText(), new String(password.getPassword()), proxyText, hostnameText, method, selectedGPU, cpuCores, maxRAM, maxRendertime, cachePath, autoSignIn.isSelected(), GuiSwing.type, priority.getValue()));
				
				// wait for successful authentication (to store the public key)
				// or do we already have one?
				if (parent.getClient().getServer().getServerConfig() != null && parent.getClient().getServer().getServerConfig().getPublickey() != null) {
					parent.getSettingsLoader().saveFile();
				}
			}
		}
	}
	
	class JCheckBoxGPU extends JCheckBox {
		private GPUDevice gpu;
		
		public JCheckBoxGPU(GPUDevice gpu) {
			super(gpu.getModel());
			this.gpu = gpu;
		}
		
		public GPUDevice getGPUDevice() {
			return gpu;
		}
	}
	
	public class CheckCanStart implements KeyListener {
		@Override
		public void keyPressed(KeyEvent arg0) {
		}
		
		@Override
		public void keyReleased(KeyEvent arg0) {
			checkDisplaySaveButton();
		}
		
		@Override
		public void keyTyped(KeyEvent arg0) {
		}
		
	}
}
