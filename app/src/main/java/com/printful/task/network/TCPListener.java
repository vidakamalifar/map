package com.printful.task.network;

public interface TCPListener {
	public void onTCPMessageReceived(String message);
	public void onTCPConnectionStatusChanged(boolean isConnectedNow);
}
