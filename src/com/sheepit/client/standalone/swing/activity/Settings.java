package com.sheepit.client.standalone.swing.activity;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import com.sheepit.client.Configuration;
import com.sheepit.client.Configuration.ComputeType;
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
	
	JButton saveButton;
	
	public Settings(GuiSwing parent_) {
		parent = parent_;
		cacheDir = null;
		useGPUs = new LinkedList<JCheckBoxGPU>();
	}
	
	@Override
	public void show() {
		Configuration config = parent.getConfiguration();
		
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
		parent.getContentPane().add(login);
		
		n += sep;
		
		JLabel passwordLabel = new JLabel("Password:");
		passwordLabel.setBounds(start_label_left, n, 170, size_height_label);
		parent.getContentPane().add(passwordLabel);
		
		password = new JPasswordField();
		password.setBounds(start_label_right, n, end_label_right - start_label_right, size_height_label);
		password.setText(parent.getConfiguration().password());
		password.setColumns(10);
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
		else if (method == ComputeType.CPU_ONLY) {
			useCPU.setSelected(true);
			gpuChecked = false;
		}
		else if (method == ComputeType.GPU_ONLY) {
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
				gpuCheckBox.setSelected(gpuChecked);
				gpuCheckBox.setBounds(start_label_right, n, 200, size_height_label);
				gpuCheckBox.addActionListener(new GpuChangeAction());
				parent.getContentPane().add(gpuCheckBox);
				useGPUs.add(gpuCheckBox);
			}
		}
		
		n += sep;
		
		saveButton = new JButton("Start");
		saveButton.setBounds(start_label_right, n, 80, size_height_label);
		saveButton.addActionListener(new SaveAction());
		parent.getContentPane().add(saveButton);
		
	}
	
	public void checkDisplaySaveButton() {
		boolean selected = useCPU.isSelected();
		for (JCheckBoxGPU box : useGPUs) {
			if (box.isSelected()) {
				selected = true;
			}
		}
		saveButton.setEnabled(selected);
	}
	
	class ChooseFileAction implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
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
			
			ComputeType method = ComputeType.CPU_ONLY;
			if (useCPU.isSelected() && selected_gpu == null) {
				method = ComputeType.CPU_ONLY;
			}
			else if (useCPU.isSelected() == false && selected_gpu != null) {
				method = ComputeType.GPU_ONLY;
			}
			else if (useCPU.isSelected() && selected_gpu != null) {
				method = ComputeType.CPU_GPU;
			}
			config.setComputeMethod(method);
			
			if (selected_gpu != null) {
				config.setUseGPU(selected_gpu);
			}
			
			parent.setCredentials(login.getText(), new String(password.getPassword()));
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
}
