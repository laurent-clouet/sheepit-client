package com.sheepit.client.exception;

/****
 * This exception will be raised when the server returns a job that has been previously blocked by the user
 */
public class FermeExceptionJobBlockedByUser extends FermeException {
	public FermeExceptionJobBlockedByUser() {
		super();
	}
	
	public FermeExceptionJobBlockedByUser(String message_) {
		super(message_);
	}
}
