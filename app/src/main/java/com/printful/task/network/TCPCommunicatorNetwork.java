package com.printful.task.network;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TCPCommunicatorNetwork {
	private static TCPCommunicatorNetwork uniqInstance;
	private static String serverHost;
	private static int serverPort;
	private static List<TCPListener> allListeners;
	private static BufferedWriter out;
	private static BufferedReader in;
	private static Socket s;
	private static Handler UIHandler;
	private TCPCommunicatorNetwork()
	{
		allListeners = new ArrayList<TCPListener>();
	}
	public static TCPCommunicatorNetwork getInstance()
	{
		if(uniqInstance==null)
		{
			uniqInstance = new TCPCommunicatorNetwork();
		}
		return uniqInstance;
	}
	public  TCPWriterErrors init(String host, int port)
	{
		setServerHost(host);
		setServerPort(port);
		InitTCPClientTask task = new InitTCPClientTask();
		task.execute();
		return TCPWriterErrors.OK;
	}
	public static  TCPWriterErrors writeToSocket(final String obj, Handler handle, Context context)
	{
		UIHandler=handle;
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try
				{
			        String outMsg = obj + System.getProperty("line.separator");
			        out.write(outMsg);
			        out.flush();
				}
				catch(Exception e)
				{
					UIHandler.post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
						}
					});
				}
			}

		};
		Thread thread = new Thread(runnable);
		thread.start();
		return TCPWriterErrors.OK;

	}

	public static void addListener(TCPListener listener)
	{
		allListeners.clear();
		allListeners.add(listener);
	}
	public static void closeStreams()
	{
		try
		{
			s.close();
			in.close();
			out.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	public static String getServerHost() {
		return serverHost;
	}
	public static void setServerHost(String serverHost) {
		TCPCommunicatorNetwork.serverHost = serverHost;
	}
	public static int getServerPort() {
		return serverPort;
	}
	public static void setServerPort(int serverPort) {
		TCPCommunicatorNetwork.serverPort = serverPort;
	}


	public static class InitTCPClientTask extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			try
			{
				s = new Socket(getServerHost(), getServerPort());
		         in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		         out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		         for(TCPListener listener:allListeners)
			        	listener.onTCPConnectionStatusChanged(true);
		        while(true)
		        {
		        	String inMsg = in.readLine();
		        	if(inMsg!=null)
		        	{
				        for(TCPListener listener:allListeners)
				        	listener.onTCPMessageReceived(inMsg);
		        	}
		        }

		    } catch (UnknownHostException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }

			return null;

		}

	}
	public enum TCPWriterErrors{UnknownHostException,IOException,otherProblem,OK}
}
