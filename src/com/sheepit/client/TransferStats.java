package com.sheepit.client;

import lombok.AllArgsConstructor;

/****************
 * Holds the session traffic statistics. The constructor accepts two parameters:
 * @long bytes - bytes transferred in the session
 * @Job seconds - seconds spent transferring the data
 */
@AllArgsConstructor
public class TransferStats {
	private long bytes;
	private long millis;
	
	public TransferStats() {
		this.bytes = 0;
		this.millis = 0;
	}
	
	public void calc(long bytes, long millis) {
		this.bytes += bytes;
		this.millis += millis;
	}
	
	public String getSessionTraffic() {
		return Utils.formatDataConsumption(this.bytes);
	}
	
	public String getAverageSessionSpeed() {
		try {
			return Utils.formatDataConsumption((long) (this.bytes / (this.millis / 1000f)));
		} catch (ArithmeticException e) {	// Unlikely, but potential division by zero fallback if first transfer is done in zero millis
			return Utils.formatDataConsumption((long) (this.bytes / (0.1f)));
		}
	}
}
