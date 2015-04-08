package com.sheepit.client.standalone.swing.activity;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import com.sheepit.client.Configuration;
import com.sheepit.client.Configuration.ComputeType;
import com.sheepit.client.SettingsLoader;
import com.sheepit.client.hardware.gpu.GPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
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
		
		int size_height_label = 24;
		int start_label_left = 109;
		int start_label_right = 265;
		int end_label_right = 490;
		int n = 10;
		int sep = 45;
		
		ImageIcon image = new ImageIcon(getClass().getResource("/title.png"));
		JLabel labelImage = new JLabel(image);
		labelImage.setBounds(600 / 2 - 265 / 2, n, 265, 130 + n);
		n = labelImage.getHeight();
		parent.getContentPane().add(labelImage);
		
		n += 40;
		
		JLabel loginLabel = new JLabel("Login:");
		loginLabel.setBounds(start_label_left, n, 170, size_height_label);
		parent.getContentPane().add(loginLabel);
		
		login = new JTextField();
		login.setBounds(start_label_right, n, end_label_right - start_label_right, size_height_label);
		login.setText(parent.getConfiguration().login());
		login.setColumns(20);
		login.addKeyListener(new CheckCanStart());
		parent.getContentPane().add(login);
		
		n += sep;
		
		JLabel passwordLabel = new JLabel("Password:");
		passwordLabel.setBounds(start_label_left, n, 170, size_height_label);
		parent.getContentPane().add(passwordLabel);
		
		password = new JPasswordField();
		password.setBounds(start_label_right, n, end_label_right - start_label_right, size_height_label);
		password.setText(parent.getConfiguration().password());
		password.setColumns(10);
		password.addKeyListener(new CheckCanStart());
		parent.getContentPane().add(password);
		
		n += sep;
		
		JLabel cacheLabel = new JLabel("Working directory");
		cacheLabel.setBounds(start_label_left, n, 240, size_height_label);
		parent.getContentPane().add(cacheLabel);
		
		String destination = DUMMY_CACHE_DIR;
		if (config.getUserSpecifiedACacheDir()) {
			destination = config.getStorageDir().getName();
		}
		
		cacheDirText = new JLabel(destination);
		cacheDirText.setBounds(start_label_right, n, 240, size_height_label);
		parent.getContentPane().add(cacheDirText);
		
		cacheDirChooser = new JFileChooser();
		cacheDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		JButton openButton = new JButton("...");
		openButton.addActionListener(new ChooseFileAction());
		openButton.setBounds(end_label_right - 50, n, 50, size_height_label);
		parent.getContentPane().add(openButton);
		
		n += sep;
		
		JLabel computeMethodLabel = new JLabel("Use");
		computeMethodLabel.setBounds(start_label_left, n, 240, size_height_label);
		parent.getContentPane().add(computeMethodLabel);
		
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
		
		int size = 60;
		useCPU.addActionListener(new CpuChangeAction());
		useCPU.setBounds(start_label_right, n, size, size_height_label);
		parent.getContentPane().add(useCPU);
		
		List<GPUDevice> gpus = GPU.listDevices();
		if (gpus != null) {
			for (GPUDevice gpu : gpus) {
				n += 20;
				JCheckBoxGPU gpuCheckBox = new JCheckBoxGPU(gpu);
				gpuCheckBox.setToolTipText(gpu.getCudaName());
				if (gpuChecked) {
					GPUDevice config_gpu = config.getGPUDevice();
					if (config_gpu != null && config_gpu.getCudaName().equals(gpu.getCudaName())) {
						gpuCheckBox.setSelected(gpuChecked);
					}
				}
				gpuCheckBox.setBounds(start_label_right, n, 200, size_height_label);
				gpuCheckBox.addActionListener(new GpuChangeAction());
				parent.getContentPane().add(gpuCheckBox);
				useGPUs.add(gpuCheckBox);
			}
		}
		
		n += sep;
		
		saveFile = new JCheckBox("Save settings", true);
		saveFile.setBounds(start_label_right, n, end_label_right - start_label_right, size_height_label);
		parent.getContentPane().add(saveFile);
		
		n += 20;
		
		autoSignIn = new JCheckBox("Auto sign in", config.getAutoSignIn());
		autoSignIn.setBounds(start_label_right, n, end_label_right - start_label_right, size_height_label);
		autoSignIn.addActionListener(new AutoSignInChangeAction());
		parent.getContentPane().add(autoSignIn);
		
		n += sep;
		
		saveButton = new JButton("Start");
		saveButton.setBounds(start_label_right, n, 80, size_height_label);
		saveButton.addActionListener(new SaveAction());
		parent.getContentPane().add(saveButton);
		
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
		if (login.getText().isEmpty() || password.getPassword().length == 0) {
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
					System.out.println("Activity::Settings::handle do not dir since it did not change (dir: " + cacheDir + ")");
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
			
			parent.setCredentials(login.getText(), new String(password.getPassword()));
			
			String cachePath = null;
			if (config.getUserSpecifiedACacheDir() && config.getStorageDir() != null) {
				cachePath = config.getStorageDir().getAbsolutePath();
			}
			
			if (saveFile.isSelected()) {
				new SettingsLoader(login.getText(), new String(password.getPassword()), method, selected_gpu, cachePath, autoSignIn.isSelected(), GuiSwing.type).saveFile();
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
		}
		
		@Override
		public void keyTyped(KeyEvent arg0) {
			checkDisplaySaveButton();
		}
		
	}
}
