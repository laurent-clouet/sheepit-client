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
	private JTextField incompatibleProcess;
	
	private JCheckBox saveFile;
	private JCheckBox autoSignIn;
	JButton saveButton;
	
	private boolean haveAutoStarted;
	
	private JTextField tileSizeValue;
	private JLabel tileSizeLabel;
	private JCheckBox customTileSize;
	
	public Settings(GuiSwing parent_) {
		parent = parent_;
		cacheDir = null;
		useGPUs = new LinkedList<JCheckBoxGPU>();
		haveAutoStarted = false;
	}
	
	@Override
	public void show() {
		Configuration config = parent.getConfiguration();
		new SettingsLoader().merge(config);
		
		List<GPUDevice> gpus = GPU.listDevices();
		
		GridBagConstraints constraints = new GridBagConstraints();
		int currentRow = 0;
		ImageIcon image = new ImageIcon(getClass().getResource("/title.png"));
		constraints.fill = GridBagConstraints.CENTER;
		
		JLabel labelImage = new JLabel(image);
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		parent.getContentPane().add(labelImage, constraints);
		
		++currentRow;
		
		// authentication
		JPanel authentication_panel = new JPanel(new GridLayout(2, 2));
		authentication_panel.setBorder(BorderFactory.createTitledBorder("Authentication"));
		
		JLabel loginLabel = new JLabel("Username:");
		login = new JTextField();
		login.setText(parent.getConfiguration().login());
		login.setColumns(20);
		login.addKeyListener(new CheckCanStart());
		JLabel passwordLabel = new JLabel("Password:");
		password = new JPasswordField();
		password.setText(parent.getConfiguration().password());
		password.setColumns(10);
		password.addKeyListener(new CheckCanStart());
		
		authentication_panel.add(loginLabel);
		authentication_panel.add(login);
		
		authentication_panel.add(passwordLabel);
		authentication_panel.add(password);
		
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		parent.getContentPane().add(authentication_panel, constraints);
		
		// directory
		JPanel directory_panel = new JPanel(new GridLayout(1, 3));
		directory_panel.setBorder(BorderFactory.createTitledBorder("Cache"));
		JLabel cacheLabel = new JLabel("Working directory:");
		directory_panel.add(cacheLabel);
		String destination = DUMMY_CACHE_DIR;
		if (config.getUserSpecifiedACacheDir()) {
			destination = config.getStorageDir().getName();
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
		
		parent.getContentPane().add(directory_panel, constraints);
		
		// compute devices
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints compute_devices_constraints = new GridBagConstraints();
		JPanel compute_devices_panel = new JPanel(gridbag);
		
		compute_devices_panel.setBorder(BorderFactory.createTitledBorder("Compute devices"));
		
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
		
		compute_devices_constraints.gridx = 1;
		compute_devices_constraints.gridy = 0;
		compute_devices_constraints.fill = GridBagConstraints.BOTH;
		compute_devices_constraints.weightx = 1.0;
		compute_devices_constraints.weighty = 1.0;
		
		gridbag.setConstraints(useCPU, compute_devices_constraints);
		compute_devices_panel.add(useCPU);
		
		for (GPUDevice gpu : gpus) {
			JCheckBoxGPU gpuCheckBox = new JCheckBoxGPU(gpu);
			gpuCheckBox.setToolTipText(gpu.getCudaName());
			if (gpuChecked) {
				GPUDevice config_gpu = config.getGPUDevice();
				if (config_gpu != null && config_gpu.getCudaName().equals(gpu.getCudaName())) {
					gpuCheckBox.setSelected(gpuChecked);
				}
			}
			gpuCheckBox.addActionListener(new GpuChangeAction());
			
			compute_devices_constraints.gridy++;
			gridbag.setConstraints(gpuCheckBox, compute_devices_constraints);
			compute_devices_panel.add(gpuCheckBox);
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
			
			compute_devices_constraints.weightx = 1.0 / gpus.size();
			compute_devices_constraints.gridx = 0;
			compute_devices_constraints.gridy++;
			
			gridbag.setConstraints(coreLabel, compute_devices_constraints);
			compute_devices_panel.add(coreLabel);
			
			compute_devices_constraints.gridx = 1;
			compute_devices_constraints.weightx = 1.0;
			
			gridbag.setConstraints(cpuCores, compute_devices_constraints);
			compute_devices_panel.add(cpuCores);
		}
		
		// max ram allowed to render
		OS os = OS.getOS();
		int all_ram = os.getMemory();
		ram = new JSlider(0, all_ram);
		int step = 1000000;
		double display = (double)all_ram / (double)step;
		while (display > 10) {
			step += 1000000;
			display = (double)all_ram / (double)step;
		}
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		for (int g = 0; g < all_ram; g += step) {
			labelTable.put(new Integer(g), new JLabel("" + (g / 1000000)));
		}
		ram.setMajorTickSpacing(step);
		ram.setLabelTable(labelTable);
		ram.setPaintTicks(true);
		ram.setPaintLabels(true);
		ram.setValue(config.getMaxMemory() != -1 ? config.getMaxMemory() : os.getMemory());
		JLabel ramLabel = new JLabel("Memory:");
		
		compute_devices_constraints.weightx = 1.0 / gpus.size();
		compute_devices_constraints.gridx = 0;
		compute_devices_constraints.gridy++;
		
		gridbag.setConstraints(ramLabel, compute_devices_constraints);
		compute_devices_panel.add(ramLabel);
		
		compute_devices_constraints.gridx = 1;
		compute_devices_constraints.weightx = 1.0;
		
		gridbag.setConstraints(ram, compute_devices_constraints);
		compute_devices_panel.add(ram);
		
		parent.getContentPane().add(compute_devices_panel, constraints);
		
		// priority
		boolean high_priority_support = os.getSupportHighPriority();
		priority = new JSlider(high_priority_support ? -19 : 0, 19);
		priority.setMajorTickSpacing(19);
		priority.setMinorTickSpacing(1);
		priority.setPaintTicks(true);
		priority.setPaintLabels(true);
		priority.setValue(config.getPriority());
		JLabel priorityLabel = new JLabel(high_priority_support ? "Priority (High <-> Low):" : "Priority (Normal <-> Low):" );
		
		compute_devices_constraints.weightx = 1.0 / gpus.size();
		compute_devices_constraints.gridx = 0;
		compute_devices_constraints.gridy++;
		
		gridbag.setConstraints(priorityLabel, compute_devices_constraints);
		compute_devices_panel.add(priorityLabel);
		
		compute_devices_constraints.gridx = 1;
		compute_devices_constraints.weightx = 1.0;
		
		gridbag.setConstraints(priority, compute_devices_constraints);
		compute_devices_panel.add(priority);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		parent.getContentPane().add(compute_devices_panel, constraints);
		
		// other
		JPanel advanced_panel = new JPanel(new GridLayout(6, 2));
		advanced_panel.setBorder(BorderFactory.createTitledBorder("Advanced options"));
		
		JLabel proxyLabel = new JLabel("Proxy:");
		proxyLabel.setToolTipText("http://login:password@host:port");
		proxy = new JTextField();
		proxy.setToolTipText("http://login:password@host:port");
		proxy.setText(parent.getConfiguration().getProxy());
		proxy.addKeyListener(new CheckCanStart());
		
		advanced_panel.add(proxyLabel);
		advanced_panel.add(proxy);
		
		JLabel hostnameLabel = new JLabel("Computer name:");
		hostname = new JTextField();
		hostname.setText(parent.getConfiguration().getHostname());
		
		advanced_panel.add(hostnameLabel);
		advanced_panel.add(hostname);
		
		JLabel renderTimeLabel = new JLabel("Max time per frame (in minute):");
		int val = 0;
		if (parent.getConfiguration().getMaxRenderTime() > 0) {
			val = parent.getConfiguration().getMaxRenderTime() / 60;
		}
		renderTime = new JSpinner(new SpinnerNumberModel(val,0,1000,1));
		
		advanced_panel.add(renderTimeLabel);
		advanced_panel.add(renderTime);
		
		JLabel incompatibleProcessLabel = new JLabel("Pause when running:");
		incompatibleProcess = new JTextField();
		incompatibleProcess.setText(parent.getConfiguration().getIncompatibleProcessName());
		
		advanced_panel.add(incompatibleProcessLabel);
		advanced_panel.add(incompatibleProcess);
		
		JLabel customTileSizeLabel = new JLabel("Custom render tile size:");
		customTileSize = new JCheckBox("", config.getTileSize() != -1);
		advanced_panel.add(customTileSizeLabel);
		advanced_panel.add(customTileSize);
		
		customTileSize.addActionListener(new TileSizeChange());
		tileSizeLabel = new JLabel("Tile Size:");
		
		tileSizeValue = new JTextField();
		int fromConfig = parent.getConfiguration().getTileSize();
		if (fromConfig == -1) {
			if (parent.getConfiguration().getGPUDevice() != null) {
				fromConfig = parent.getConfiguration().getGPUDevice().getRecommandedTileSize();
			}
			else {
				fromConfig = 32;
			}
		}
		tileSizeValue.setText(Integer.toString(fromConfig));
		hideCustomTileSize(config.getTileSize() != -1, false);
		
		advanced_panel.add(tileSizeLabel);
		advanced_panel.add(tileSizeValue);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		parent.getContentPane().add(advanced_panel, constraints);
		
		// general settings
		JPanel general_panel = new JPanel(new GridLayout(1, 2));
		
		saveFile = new JCheckBox("Save settings", true);
		general_panel.add(saveFile);
		
		autoSignIn = new JCheckBox("Auto sign in", config.getAutoSignIn());
		autoSignIn.addActionListener(new AutoSignInChangeAction());
		general_panel.add(autoSignIn);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		parent.getContentPane().add(general_panel, constraints);
		
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
		
		if (haveAutoStarted == false && config.getAutoSignIn() && checkDisplaySaveButton()) {
			// auto start
			haveAutoStarted = true;
			new SaveAction().actionPerformed(null);
		}
	}
	
	public void hideCustomTileSize(boolean hidden, boolean displayWarning) {
		tileSizeValue.setVisible(hidden);
		tileSizeLabel.setVisible(hidden);
		if (customTileSize.isSelected() == true && displayWarning) {
			JOptionPane.showMessageDialog(parent.getContentPane(), "<html>These settings should only be changed if you are an advanced user.<br /> Improper settings may lead to invalid and slower renders!</html>", "Warning: Advanced Users Only!", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	public boolean checkDisplaySaveButton() {
		boolean selected = useCPU.isSelected();
		for (JCheckBoxGPU box : useGPUs) {
			if (box.isSelected()) {
				selected = true;
			}
		}
		if (login.getText().isEmpty() || password.getPassword().length == 0 || Proxy.isValidURL(proxy.getText()) == false) {
			selected = false;
		}
		
		if (customTileSize.isSelected()) {
			try {
				Integer.parseInt(tileSizeValue.getText().replaceAll(",", ""));
			}
			catch (NumberFormatException e) {
				selected = false;
			}
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
				File file = cacheDirChooser.getSelectedFile();
				cacheDir = file;
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
				if (box.equals(e.getSource()) == false) {
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
	
	class TileSizeChange implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			boolean custom = customTileSize.isSelected();
			hideCustomTileSize(custom, true);
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
				if (fromConfig != null && fromConfig.getAbsolutePath().equals(cacheDir.getAbsolutePath()) == false) {
					config.setCacheDir(cacheDir);
				}
				else {
					// do nothing because the directory is the same as before
				}
			}
			
			GPUDevice selected_gpu = null;
			for (JCheckBoxGPU box : useGPUs) {
				if (box.isSelected()) {
					selected_gpu = box.getGPUDevice();
				}
			}
			
			ComputeType method = ComputeType.CPU;
			if (useCPU.isSelected() && selected_gpu == null) {
				method = ComputeType.CPU;
			}
			else if (useCPU.isSelected() == false && selected_gpu != null) {
				method = ComputeType.GPU;
			}
			else if (useCPU.isSelected() && selected_gpu != null) {
				method = ComputeType.CPU_GPU;
			}
			config.setComputeMethod(method);
			
			if (selected_gpu != null) {
				config.setUseGPU(selected_gpu);
			}
			
			int cpu_cores = -1;
			if (cpuCores != null) {
				cpu_cores = cpuCores.getValue();
			}
			
			if (cpu_cores > 0) {
				config.setUseNbCores(cpu_cores);
			}
			
			int max_ram = -1;
			if (ram != null) {
				max_ram = ram.getValue();
			}
			
			if (max_ram > 0) {
				config.setMaxMemory(max_ram);
			}
			
			int max_rendertime = -1;
			if (renderTime != null) {
				max_rendertime = (Integer)renderTime.getValue() * 60;
				config.setMaxRenderTime(max_rendertime);
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
			
			String tile = null;
			if (customTileSize.isSelected() && tileSizeValue != null) {
				try {
					tile = tileSizeValue.getText().replaceAll(",", "");
					config.setTileSize(Integer.parseInt(tile));
				}
				catch (NumberFormatException e1) {
					System.err.println("Error: wrong tile format");
					System.err.println(e1);
					System.exit(2);
				}
			}
			else {
				config.setTileSize(-1); // no tile
			}
			
			parent.setCredentials(login.getText(), new String(password.getPassword()));
			
			String cachePath = null;
			if (config.getUserSpecifiedACacheDir() && config.getStorageDir() != null) {
				cachePath = config.getStorageDir().getAbsolutePath();
			}
			
			String hostnameText = null;
			if (hostname.getText() != null && hostname.getText().equals(parent.getConfiguration().getHostname()) == false) {
				hostnameText = hostname.getText();
			}
			
			if (saveFile.isSelected()) {
				new SettingsLoader(login.getText(), new String(password.getPassword()), proxyText, hostnameText, incompatibleProcess.getText(), method, selected_gpu, cpu_cores, max_ram, max_rendertime, cachePath, autoSignIn.isSelected(), GuiSwing.type, tile, priority.getValue()).saveFile();
			}
			else {
				try {
					new File(new SettingsLoader().getFilePath()).delete();
				}
				catch (SecurityException e3) {
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
