package com.github.dbadia.atomosphere.test;

public enum SqrlAuthenticationStatus {
	CORRELATOR_ISSUED, COMMUNICATING, ERROR_BAD_REQUEST, ERROR_SQRL_INTERNAL, AUTH_COMPLETE;

	public boolean isErrorStatus() {
		return this.toString().startsWith("ERROR_");
	}

}
