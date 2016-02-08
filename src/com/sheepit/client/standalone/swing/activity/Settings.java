package com.sheepit.client.standalone.swing.activity;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.MalformedURLException;
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
import javax.swing.JTextField;
import com.sheepit.client.Configuration;
import com.sheepit.client.Configuration.ComputeType;
import com.sheepit.client.SettingsLoader;
import com.sheepit.client.hardware.cpu.CPU;
import com.sheepit.client.hardware.gpu.GPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.network.Proxy;
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
	private JTextField proxy;
	
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
		new SettingsLoader().merge(config);
		
		List<GPUDevice> gpus = GPU.listDevices();
		
		GridBagConstraints constraints = new GridBagConstraints();
		int columns = 4 + (gpus != null ? gpus.size() : 0);
		int currentRow = 0;
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		ImageIcon image = new ImageIcon(getClass().getResource("/title.png"));
		JLabel labelImage = new JLabel(image);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 3.0;
		constraints.gridwidth = columns - 2;
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(labelImage, constraints);
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		JLabel loginLabel = new JLabel("Login:");
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weighty = 0.0;
		constraints.gridwidth = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(loginLabel, constraints);
		
		login = new JTextField();
		login.setText(parent.getConfiguration().login());
		login.setColumns(20);
		login.addKeyListener(new CheckCanStart());
		constraints.gridwidth = columns - 3;
		constraints.gridx = 2;
		parent.getContentPane().add(login, constraints);
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		JLabel passwordLabel = new JLabel("Password:");
		constraints.weighty = 0.0;
		constraints.gridwidth = 1;
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(passwordLabel, constraints);
		
		password = new JPasswordField();
		password.setText(parent.getConfiguration().password());
		password.setColumns(10);
		password.addKeyListener(new CheckCanStart());
		constraints.gridwidth = columns - 3;
		constraints.gridx = 2;
		parent.getContentPane().add(password, constraints);
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		JLabel proxyLabel = new JLabel("Proxy:");
		proxyLabel.setToolTipText("http://login:password@host:port");
		constraints.gridwidth = 1;
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(proxyLabel, constraints);
		
		proxy = new JTextField();
		proxy.setToolTipText("http://login:password@host:port");
		proxy.setText(parent.getConfiguration().getProxy());
		proxy.addKeyListener(new CheckCanStart());
		constraints.gridwidth = columns - 3;
		constraints.gridx = 2;
		parent.getContentPane().add(proxy, constraints);
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		JLabel cacheLabel = new JLabel("Working directory:");
		constraints.gridwidth = 1;
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(cacheLabel, constraints);
		
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
		
		constraints.gridwidth = columns - 3;
		constraints.gridx = 2;
		parent.getContentPane().add(cacheDirWrapper, constraints);
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		JLabel computeMethodLabel = new JLabel("Use:");
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(computeMethodLabel, constraints);
		
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
		constraints.gridwidth = Math.max(1, columns - (gpus != null ? gpus.size() : 0) - 3);
		constraints.gridx = 2;
		parent.getContentPane().add(useCPU, constraints);
		
		constraints.gridwidth = 1;
		if (gpus != null) {
			for (int i=0; i < gpus.size(); i++) {
				GPUDevice gpu = gpus.get(i);
				JCheckBoxGPU gpuCheckBox = new JCheckBoxGPU(gpu);
				gpuCheckBox.setToolTipText(gpu.getCudaName());
				if (gpuChecked) {
					GPUDevice config_gpu = config.getGPUDevice();
					if (config_gpu != null && config_gpu.getCudaName().equals(gpu.getCudaName())) {
						gpuCheckBox.setSelected(gpuChecked);
					}
				}
				gpuCheckBox.addActionListener(new GpuChangeAction());
				constraints.gridx = i + 3;
				parent.getContentPane().add(gpuCheckBox, constraints);
				useGPUs.add(gpuCheckBox);
			}
		}
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		CPU cpu = new CPU();
		if (cpu.cores() > 1) { // if only one core is available, no need to show the choice
			cpuCores = new JSlider(1, cpu.cores());
			cpuCores.setMajorTickSpacing(1);
			cpuCores.setMinorTickSpacing(1);
			cpuCores.setPaintTicks(true);
			cpuCores.setPaintLabels(true);
			cpuCores.setValue(config.getNbCores() != -1 ? config.getNbCores() : cpuCores.getMaximum());
			JLabel coreLabel = new JLabel("CPU cores:");
			constraints.gridx = 1;
			constraints.gridy = currentRow;
			parent.getContentPane().add(coreLabel, constraints);
			constraints.gridwidth = columns - 3;
			constraints.gridx = 2;
			parent.getContentPane().add(cpuCores, constraints);
			
			parent.addPadding(1, ++currentRow, columns - 2, 1);
			++currentRow;
		}
		
		saveFile = new JCheckBox("Save settings", true);
		constraints.gridwidth = columns - 3;
		constraints.gridx = 2;
		constraints.gridy = currentRow;
		parent.getContentPane().add(saveFile, constraints);
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		autoSignIn = new JCheckBox("Auto sign in", config.getAutoSignIn());
		autoSignIn.addActionListener(new AutoSignInChangeAction());
		constraints.gridy = currentRow;
		parent.getContentPane().add(autoSignIn, constraints);
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		++currentRow;
		
		String buttonText = "Start";
		if (parent.getClient() != null) {
			if (parent.getClient().isRunning()) {
				buttonText = "Save";
			}
		}
		
		saveButton = new JButton(buttonText);
		checkDisplaySaveButton();
		saveButton.addActionListener(new SaveAction());
		constraints.gridwidth = columns - 2;
		constraints.gridx = 1;
		constraints.gridy = currentRow;
		parent.getContentPane().add(saveButton, constraints);
		
		parent.addPadding(1, ++currentRow, columns - 2, 1);
		parent.addPadding(0, 0, 1, currentRow + 1);
		parent.addPadding(columns - 1, 0, 1, currentRow + 1);
		
		
		if (haveAutoStarted == false && config.getAutoSignIn() && checkDisplaySaveButton()) {
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
		if (login.getText().isEmpty() || password.getPassword().length == 0 || Proxy.isValidURL(proxy.getText()) == false) {
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
			
			String proxyText = null;
			if (proxy != null) {
				try {
					Proxy.set(proxy.getText());
					proxyText = proxy.getText();
				}
				catch (MalformedURLException e1) {
					System.err.println("Error: wrong url for proxy");
					System.err.println(e);
					System.exit(2);
				}
			}
			
			parent.setCredentials(login.getText(), new String(password.getPassword()));
			
			String cachePath = null;
			if (config.getUserSpecifiedACacheDir() && config.getStorageDir() != null) {
				cachePath = config.getStorageDir().getAbsolutePath();
			}
			
			if (saveFile.isSelected()) {
				new SettingsLoader(login.getText(), new String(password.getPassword()), proxyText, method, selected_gpu, cpu_cores, cachePath, autoSignIn.isSelected(), GuiSwing.type).saveFile();
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
