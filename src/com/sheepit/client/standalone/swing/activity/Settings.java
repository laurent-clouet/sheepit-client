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
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.SpinnerNumberModel;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

import com.sheepit.client.Configuration;
import com.sheepit.client.Configuration.ComputeType;
import com.sheepit.client.SettingsLoader;
import com.sheepit.client.hardware.cpu.CPU;
import com.sheepit.client.hardware.gpu.GPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.hardware.gpu.GPULister;
import com.sheepit.client.hardware.gpu.nvidia.Nvidia;
import com.sheepit.client.hardware.gpu.opencl.OpenCL;
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
	private JLabel renderbucketSizeLabel;
	private JSlider renderbucketSize;
	private JSlider cpuCores;
	private JSlider ram;
	private JSpinner renderTime;
	private JSlider priority;
	private JTextField proxy;
	private JTextField hostname;
	
	private ButtonGroup themeOptionsGroup;
	private JRadioButton lightMode;
	private JRadioButton darkMode;
	
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
	
	@Override public void show() {
		Configuration config = parent.getConfiguration();
		new SettingsLoader(config.getConfigFilePath()).merge(config);
		
		applyTheme(config.getTheme());    // apply the proper theme (light/dark)
		
		List<GPUDevice> gpus = GPU.listDevices(config);
		useGPUs.clear();    // Empty the auxiliary list (used in the list of checkboxes)
		
		GridBagConstraints constraints = new GridBagConstraints();
		int currentRow = 0;
		ImageIcon image = new ImageIcon(getClass().getResource("/sheepit-logo.png"));
		constraints.fill = GridBagConstraints.CENTER;
		
		JLabel labelImage = new JLabel(image);
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		parent.getContentPane().add(labelImage, constraints);
		
		++currentRow;
		
		constraints.gridy = currentRow;
		parent.getContentPane().add(new JLabel(" "), constraints);    // Add a separator between logo and first panel
		
		currentRow++;
		
		// authentication
		CollapsibleJPanel authentication_panel = new CollapsibleJPanel(new GridLayout(2, 2));
		authentication_panel.setBorder(BorderFactory.createTitledBorder("Authentication"));
		
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
		
		authentication_panel.add(loginLabel);
		authentication_panel.add(login);
		
		authentication_panel.add(passwordLabel);
		authentication_panel.add(password);
		
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		parent.getContentPane().add(authentication_panel, constraints);
		
		// Theme selection panel
		CollapsibleJPanel themePanel = new CollapsibleJPanel(new GridLayout(1, 3));
		themePanel.setBorder(BorderFactory.createTitledBorder("Theme"));
		
		themeOptionsGroup = new ButtonGroup();
		
		lightMode = new JRadioButton("Light");
		lightMode.setActionCommand("light");
		lightMode.setSelected(config.getTheme().equals("light"));
		lightMode.addActionListener(new ApplyThemeAction());
		
		darkMode = new JRadioButton("Dark");
		darkMode.setActionCommand("dark");
		darkMode.setSelected(config.getTheme().equals("dark"));
		darkMode.addActionListener(new ApplyThemeAction());
		
		themePanel.add(lightMode);
		themePanel.add(darkMode);
		
		// Group both radio buttons to allow only one selected
		themeOptionsGroup.add(lightMode);
		themeOptionsGroup.add(darkMode);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		
		parent.getContentPane().add(themePanel, constraints);
		
		// directory
		CollapsibleJPanel directory_panel = new CollapsibleJPanel(new GridLayout(1, 3));
		directory_panel.setBorder(BorderFactory.createTitledBorder("Cache"));
		JLabel cacheLabel = new JLabel("Working directory:");
		directory_panel.add(cacheLabel);
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
		
		directory_panel.add(cacheDirWrapper);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		
		parent.getContentPane().add(directory_panel, constraints);
		
		// compute devices
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints compute_devices_constraints = new GridBagConstraints();
		CollapsibleJPanel compute_devices_panel = new CollapsibleJPanel(gridbag);
		
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
		
		if (gpus.size() > 0) {
			renderbucketSizeLabel = new JLabel("Renderbucket size:");
			renderbucketSize = new JSlider();
			renderbucketSize.setMajorTickSpacing(1);
			renderbucketSize.setMinorTickSpacing(1);
			renderbucketSize.setPaintTicks(true);
			renderbucketSize.setPaintLabels(true);
			
			renderbucketSizeLabel.setVisible(false);
			renderbucketSize.setVisible(false);
			
			for (GPUDevice gpu : gpus) {
				JCheckBoxGPU gpuCheckBox = new JCheckBoxGPU(gpu);
				gpuCheckBox.setToolTipText(gpu.getId());
				if (gpuChecked) {
					GPUDevice config_gpu = config.getGPUDevice();
					if (config_gpu != null && config_gpu.getId().equals(gpu.getId())) {
						gpuCheckBox.setSelected(gpuChecked);
						renderbucketSizeLabel.setVisible(true);
						renderbucketSize.setVisible(true);
					}
				}
				gpuCheckBox.addActionListener(new GpuChangeAction());
				
				compute_devices_constraints.gridy++;
				gridbag.setConstraints(gpuCheckBox, compute_devices_constraints);
				compute_devices_panel.add(gpuCheckBox);
				useGPUs.add(gpuCheckBox);
			}
			
			// Initialisation values will apply if we are not  able to detect the proper GPU technology or
			// because is a new one (different from CUDA and OPENCL). In that case, move into a safe position
			// of 32x32 pixel render bucket and a maximum of 128x128 pixel for the "unknown GPU"
			int maxRenderbucketSize = 128;
			int recommendedBucketSize = 32;
			
			if (config.getComputeMethod() == ComputeType.GPU || config.getComputeMethod() == ComputeType.CPU_GPU) {
				GPULister gpu;
				
				if (config.getGPUDevice().getType().equals("CUDA")) {
					gpu = new Nvidia();
					maxRenderbucketSize = gpu.getMaximumRenderBucketSize(config.getGPUDevice().getMemory());
					recommendedBucketSize = gpu.getRecommendedRenderBucketSize(config.getGPUDevice().getMemory());
				}
				else if (config.getGPUDevice().getType().equals("OPENCL")) {
					gpu = new OpenCL();
					maxRenderbucketSize = gpu.getMaximumRenderBucketSize(config.getGPUDevice().getMemory());
					recommendedBucketSize = gpu.getRecommendedRenderBucketSize(config.getGPUDevice().getMemory());
				}
			}
			
			buildRenderBucketSizeSlider(maxRenderbucketSize, config.getRenderbucketSize() != -1 ?
					((int) (Math.log(config.getRenderbucketSize()) / Math.log(2))) - 5 :
					((int) (Math.log(recommendedBucketSize) / Math.log(2))) - 5);
			
			compute_devices_constraints.weightx = 1.0 / gpus.size();
			compute_devices_constraints.gridx = 0;
			compute_devices_constraints.gridy++;
			
			gridbag.setConstraints(renderbucketSizeLabel, compute_devices_constraints);
			compute_devices_panel.add(renderbucketSizeLabel);
			
			compute_devices_constraints.gridx = 1;
			compute_devices_constraints.weightx = 1.0;
			
			gridbag.setConstraints(renderbucketSize, compute_devices_constraints);
			compute_devices_panel.add(new JLabel(" "), compute_devices_constraints);        // Add a space between lines
			compute_devices_panel.add(renderbucketSize);
		}
		
		CPU cpu = new CPU();
		if (cpu.cores() > 1) { // if only one core is available, no need to show the choice
			double step = 1;
			double display = (double) cpu.cores() / step;
			while (display > 10) {
				step += 1.0;
				display = (double) cpu.cores() / step;
			}
			
			cpuCores = new JSlider(1, cpu.cores());
			cpuCores.setMajorTickSpacing((int) (step));
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
			compute_devices_panel.add(new JLabel(" "), compute_devices_constraints);        // Add a space between lines
			compute_devices_panel.add(cpuCores);
		}
		
		// max ram allowed to render
		OS os = OS.getOS();
		int all_ram = (int) os.getMemory();
		ram = new JSlider(0, all_ram);
		int step = 1000000;
		double display = (double) all_ram / (double) step;
		while (display > 10) {
			step += 1000000;
			display = (double) all_ram / (double) step;
		}
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		for (int g = 0; g < all_ram; g += step) {
			labelTable.put(new Integer(g), new JLabel("" + (g / 1000000)));
		}
		ram.setMajorTickSpacing(step);
		ram.setLabelTable(labelTable);
		ram.setPaintTicks(true);
		ram.setPaintLabels(true);
		ram.setValue((int) (config.getMaxMemory() != -1 ? config.getMaxMemory() : os.getMemory()));
		JLabel ramLabel = new JLabel("Memory:");
		
		compute_devices_constraints.weightx = 1.0 / gpus.size();
		compute_devices_constraints.gridx = 0;
		compute_devices_constraints.gridy++;
		
		gridbag.setConstraints(ramLabel, compute_devices_constraints);
		compute_devices_panel.add(ramLabel);
		
		compute_devices_constraints.gridx = 1;
		compute_devices_constraints.weightx = 1.0;
		
		gridbag.setConstraints(ram, compute_devices_constraints);
		compute_devices_panel.add(new JLabel(" "), compute_devices_constraints);        // Add a space between lines
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
		JLabel priorityLabel = new JLabel(high_priority_support ? "Priority (High <-> Low):" : "Priority (Normal <-> Low):");
		
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
		CollapsibleJPanel advanced_panel = new CollapsibleJPanel(new GridLayout(3, 2));
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
		renderTime = new JSpinner(new SpinnerNumberModel(val, 0, 1000, 1));
		
		advanced_panel.add(renderTimeLabel);
		advanced_panel.add(renderTime);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		parent.getContentPane().add(advanced_panel, constraints);
		advanced_panel.setCollapsed(true);
		
		// general settings
		JPanel general_panel = new JPanel(new GridLayout(1, 2));
		
		saveFile = new JCheckBox("Save settings", true);
		general_panel.add(saveFile);
		
		autoSignIn = new JCheckBox("Auto sign in", config.isAutoSignIn());
		autoSignIn.addActionListener(new AutoSignInChangeAction());
		general_panel.add(autoSignIn);
		
		currentRow++;
		constraints.gridx = 0;
		constraints.gridy = currentRow;
		constraints.gridwidth = 2;
		parent.getContentPane().add(general_panel, constraints);
		
		currentRow++;
		constraints.gridy = currentRow;
		parent.getContentPane().add(new JLabel(" "), constraints);    // Add a separator between last checkboxes and button
		
		currentRow++;
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
		
		if (haveAutoStarted == false && config.isAutoSignIn() && checkDisplaySaveButton()) {
			// auto start
			haveAutoStarted = true;
			new SaveAction().actionPerformed(null);
		}
	}
	
	private void buildRenderBucketSizeSlider(int maxRenderbucketSize, int selectedBucketSize) {
		Hashtable<Integer, JLabel> renderbucketSizeTable = new Hashtable<Integer, JLabel>();
		
		// We "take logs" to calculate the exponent to fill the slider. The logarithm, or log, of a number reflects
		// what power you need to raise a certain base to in order to get that number. In this case, as we are
		// offering increments of 2^n, the formula will be:
		//
		//            log(tile size in px)
		// exponent = --------------------
		//                  log(2)
		//
		int steps = (int) (Math.log(maxRenderbucketSize) / Math.log(2));
		
		for (int i = 5; i <= steps; i++) {
			renderbucketSizeTable.put((i - 5), new JLabel(String.format("%.0f", Math.pow(2, i))));
		}
		
		renderbucketSize.setMinimum(0);
		renderbucketSize.setMaximum(renderbucketSizeTable.size() - 1);
		renderbucketSize.setLabelTable(renderbucketSizeTable);
		renderbucketSize.setValue(selectedBucketSize);
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
		
		saveButton.setEnabled(selected);
		return selected;
	}
	
	private void applyTheme(String theme_) {
		try {
			if (theme_.equals("light")) {
				UIManager.setLookAndFeel(new FlatLightLaf());
			}
			else if (theme_.equals("dark")) {
				UIManager.setLookAndFeel(new FlatDarkLaf());
			}
			
			// Apply the new theme
			FlatLaf.updateUI();
		}
		catch (UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
	}
	
	class ChooseFileAction implements ActionListener {
		
		@Override public void actionPerformed(ActionEvent arg0) {
			JOptionPane.showMessageDialog(parent.getContentPane(),
					"<html>The working directory has to be dedicated directory. <br />Caution, everything not related to SheepIt-Renderfarm will be removed.<br />You should create a directory specifically for it.</html>",
					"Warning: files will be removed!", JOptionPane.WARNING_MESSAGE);
			int returnVal = cacheDirChooser.showOpenDialog(parent.getContentPane());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = cacheDirChooser.getSelectedFile();
				cacheDir = file;
				cacheDirText.setText(cacheDir.getName());
			}
		}
	}
	
	class CpuChangeAction implements ActionListener {
		
		@Override public void actionPerformed(ActionEvent e) {
			checkDisplaySaveButton();
		}
	}
	
	class GpuChangeAction implements ActionListener {
		
		@Override public void actionPerformed(ActionEvent e) {
			renderbucketSizeLabel.setVisible(false);
			renderbucketSize.setVisible(false);
			
			int counter = 0;
			for (JCheckBox box : useGPUs) {
				if (!box.isSelected()) {
					box.setSelected(false);
				}
				else {
					GPULister gpu;
					int maxRenderbucketSize = 128;        // Max default render bucket size
					int recommendedBucketSize = 32;        // Default recommended render bucket size
					
					if (useGPUs.get(counter).getGPUDevice().getType().equals("CUDA")) {
						gpu = new Nvidia();
						maxRenderbucketSize = gpu.getMaximumRenderBucketSize(useGPUs.get(counter).getGPUDevice().getMemory());
						recommendedBucketSize = gpu.getRecommendedRenderBucketSize(useGPUs.get(counter).getGPUDevice().getMemory());
					}
					else if (useGPUs.get(counter).getGPUDevice().getType().equals("OPENCL")) {
						gpu = new OpenCL();
						maxRenderbucketSize = gpu.getMaximumRenderBucketSize(useGPUs.get(counter).getGPUDevice().getMemory());
						recommendedBucketSize = gpu.getRecommendedRenderBucketSize(useGPUs.get(counter).getGPUDevice().getMemory());
					}
					
					buildRenderBucketSizeSlider(maxRenderbucketSize, ((int) (Math.log(recommendedBucketSize) / Math.log(2))) - 5);
					
					renderbucketSizeLabel.setVisible(true);
					renderbucketSize.setVisible(true);
				}
				
				// Simulate a radio button behavior with check buttons while only 1 GPU
				// can be selected at a time
				if (box.equals(e.getSource()) == false) {
					box.setSelected(false);
				}
				
				counter++;
			}
			checkDisplaySaveButton();
		}
	}
	
	class AutoSignInChangeAction implements ActionListener {
		
		@Override public void actionPerformed(ActionEvent e) {
			if (autoSignIn.isSelected()) {
				saveFile.setSelected(true);
			}
		}
	}
	
	class ApplyThemeAction implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) {
			applyTheme(themeOptionsGroup.getSelection().getActionCommand());
		}
	}
	
	class SaveAction implements ActionListener {
		
		@Override public void actionPerformed(ActionEvent e) {
			if (parent == null) {
				return;
			}
			
			Configuration config = parent.getConfiguration();
			if (config == null) {
				return;
			}
			
			if (themeOptionsGroup.getSelection().getActionCommand() != null)
				config.setTheme(themeOptionsGroup.getSelection().getActionCommand());
			
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
				config.setGPUDevice(selected_gpu);
			}
			
			int renderbucket_size = -1;
			if (renderbucketSize != null) {
				renderbucket_size = (int) Math.pow(2, (renderbucketSize.getValue() + 5));
			}
			
			int cpu_cores = -1;
			if (cpuCores != null) {
				cpu_cores = cpuCores.getValue();
			}
			
			if (cpu_cores > 0) {
				config.setNbCores(cpu_cores);
			}
			
			long max_ram = -1;
			if (ram != null) {
				max_ram = ram.getValue();
			}
			
			if (max_ram > 0) {
				config.setMaxMemory(max_ram);
			}
			
			int max_rendertime = -1;
			if (renderTime != null) {
				max_rendertime = (Integer) renderTime.getValue() * 60;
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
				parent.setSettingsLoader(
						new SettingsLoader(config.getConfigFilePath(), login.getText(), new String(password.getPassword()), proxyText, hostnameText, method,
								selected_gpu, renderbucket_size, cpu_cores, max_ram, max_rendertime, cachePath, autoSignIn.isSelected(), GuiSwing.type,
								themeOptionsGroup.getSelection().getActionCommand(),    // selected theme
								priority.getValue()));
				
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
		
		@Override public void keyPressed(KeyEvent arg0) {
		}
		
		@Override public void keyReleased(KeyEvent arg0) {
			checkDisplaySaveButton();
		}
		
		@Override public void keyTyped(KeyEvent arg0) {
		}
		
	}
}
