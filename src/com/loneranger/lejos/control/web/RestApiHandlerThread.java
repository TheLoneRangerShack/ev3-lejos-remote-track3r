package com.loneranger.lejos.control.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestApiHandlerThread implements Runnable {
	public static enum API_EVENT {
		FORWARD("/forward"), BACKWARD("/backward"), RIGHT("/right"), LEFT("/left"), STOP("/stop"), SPEEDUP(
				"/speedup"), SLOWDOWN("/slowdown"), BELL("/bell"), CALL("/call"), ALL(null);

		private String queryPath;

		private API_EVENT(String queryPath) {
			this.queryPath = queryPath;
		}

		public String getQueryPath() {
			return queryPath;
		}

		public static API_EVENT getEventFromQuery(String queryPath) {
			for (API_EVENT event : API_EVENT.values()) {
				if (event.getQueryPath().equals(queryPath)) {
					return event;
				}
			}
			return null;
		}
	}

	public static class RequestContext {
		private final String requestLine;

		public RequestContext(String requestLine) {
			this.requestLine = requestLine;
		}

		public String getRequestLine() {
			return requestLine;
		}
	}

	public static final int PORT = 80;
	public static final String GOOD_REQUEST = "HTTP/1.1 200 OK\r\n\r\nOK\r\n";
	public static final String BAD_REQUEST = "HTTP/1.1 501 Method Not Implemented\r\n\r\nFailed to process request\r\n";

	private Map<API_EVENT, List<Callback>> listeners;
	private final ServerSocket ss;

	public RestApiHandlerThread() throws IOException {
		this.ss = new ServerSocket(PORT);
		initListeners();
	}

	private void initListeners() {
		this.listeners = new HashMap<>(API_EVENT.values().length);
		for (API_EVENT event : API_EVENT.values()) {
			listeners.put(event, new ArrayList<Callback>());
		}
	}

	public void registerListener(API_EVENT event, Callback callback) {
		this.listeners.get(event).add(callback);
	}

	public void registerListener(Callback callback) {
		this.listeners.get(API_EVENT.ALL).add(callback);
	}

	synchronized public void closeServerSocket() {
		try {
			this.ss.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		BufferedReader br = null;
		PrintStream ps = null;

		try {

			while (true) {
				Socket sock = ss.accept();
				InputStream is = sock.getInputStream();
				OutputStream os = sock.getOutputStream();

				br = new BufferedReader(new InputStreamReader(is));
				ps = new PrintStream(os);

				// if there are multiple requests since the last process run,
				// process them one by one - so that we don't miss out on
				// anything
				while (true) {

					String cmd = br.readLine();
					if (cmd == null)
						break;

					//System.out.println("Received request: " + cmd);
					String[] tokens = cmd.split(" ");
					if (tokens.length <= 1 || !tokens[0].equals("GET")) {
						ps.println(BAD_REQUEST);
						break;
					}

					String requestedQueryPath = tokens[1];
					API_EVENT event = API_EVENT.getEventFromQuery(requestedQueryPath);
					if (event == null) {
						ps.println(BAD_REQUEST);
						break;
					}
					System.out.println("Picked up event: " + event);
					
					
					for (Callback callback : listeners.get(event)) {
						callback.handleEvent(event, new RequestContext(cmd));
					}

					// now call all the callbacks associated with the special
					// ALL event
					for (Callback callback : listeners.get(API_EVENT.ALL)) {
						callback.handleEvent(API_EVENT.ALL, new RequestContext(cmd));
					}

					// now call all the callbacks associated with these events//
					ps.println(GOOD_REQUEST);
		
				}
				ps.close();
				br.close();

			}
		} catch (IOException ie) {
			throw new IllegalStateException(ie);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (ps != null) {
				ps.close();
			}
		}
	}
}
