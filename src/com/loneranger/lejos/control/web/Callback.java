package com.loneranger.lejos.control.web;

import com.loneranger.lejos.control.web.RestApiHandlerThread.API_EVENT;
import com.loneranger.lejos.control.web.RestApiHandlerThread.RequestContext;

public interface Callback {
	public void handleEvent(API_EVENT event, RequestContext context);

}
