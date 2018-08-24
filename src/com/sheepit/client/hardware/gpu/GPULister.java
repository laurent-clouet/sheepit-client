package com.sheepit.client.hardware.gpu;

import java.util.List;

public interface GPULister {
	public abstract List<GPUDevice> getGpus();
}
