package com.sheepit.client.hardware.gpu;

import java.util.List;

public interface GPULister {
	public abstract List<GPUDevice> getGpus();
	
	public abstract int getRecommendedRenderBucketSize(long memory);
	
	public abstract int getMaximumRenderBucketSize(long memory);
}
