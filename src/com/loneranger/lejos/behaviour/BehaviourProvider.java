package com.loneranger.lejos.behaviour;

import java.io.File;

import com.loneranger.lejos.control.web.Callback;
import com.loneranger.lejos.control.web.RestApiHandlerThread.API_EVENT;
import com.loneranger.lejos.control.web.RestApiHandlerThread.RequestContext;

import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.subsumption.Behavior;

public class BehaviourProvider {
	public static class ForwardBackwardBehaviour implements Behavior, Callback {
		public static enum DIRECTION {
			FORWARD, BACKWARD;
		}

		private MovePilot pilot;
		volatile DIRECTION currentDirection = null;

		public ForwardBackwardBehaviour(MovePilot pilot) {
			this.pilot = pilot;
		}

		@Override
		public boolean takeControl() {
			return currentDirection != null;
		}

		@Override
		public void action() {
			//System.out.println("Entering FB action");
			if (currentDirection != null) {
				//System.out.println("Processing FB action");
				// blocking call as the suppress should suppress it and nothing
				// else
				LCD.clearDisplay();
				LCD.drawString(currentDirection.name(), 4, 4);

				switch (currentDirection) {
				case FORWARD: {
					pilot.travel(Double.NEGATIVE_INFINITY, false);
					break;
				}
				case BACKWARD: {
					pilot.travel(Double.POSITIVE_INFINITY, false);
					//System.out.println("Should not return right away 2");
					break;
				}
				}

			}

		}

		@Override
		public void suppress() {
			//System.out.println("Suppressing FB action");
			pilot.stop();
			currentDirection = null;
		}

		@Override
		public void handleEvent(API_EVENT event, RequestContext context) {
			//System.out.println("Handling event: " + event + " in forward backward");
			if (event == API_EVENT.FORWARD) {
				currentDirection = DIRECTION.FORWARD;
			} else if (event == API_EVENT.BACKWARD) {
				currentDirection = DIRECTION.BACKWARD;
			}
		}

	}

	public static class LeftRightBehaviour implements Behavior, Callback {
		public static enum DIRECTION {
			LEFT, RIGHT;
		}

		private MovePilot pilot;
		volatile DIRECTION currentDirection = null;

		public LeftRightBehaviour(MovePilot pilot) {
			this.pilot = pilot;
		}

		@Override
		public boolean takeControl() {
			return currentDirection != null;
		}

		@Override
		public void action() {
			//System.out.println("Entered LR action");
			if (currentDirection != null) {
				//System.out.println("Processing LR action");
				// blocking call as the suppress should suppress it and nothing
				// else
				LCD.clearDisplay();
				LCD.drawString(currentDirection.name(), 4, 4);

				double currentAngularSpeed = pilot.getAngularSpeed();
				pilot.setAngularSpeed(10);
				switch (currentDirection) {
				case LEFT: {
					while (currentDirection != null) {
						pilot.rotate(-90.0);
						// break;
					}
					break;
				}
				case RIGHT: {
					while (currentDirection != null) {
						pilot.rotate(90.0);
						// break;
					}
					break;
				}
				}
				
				//restore the original angular speed
				pilot.setAngularSpeed(currentAngularSpeed);

			}

		}

		@Override
		public void suppress() {
			//System.out.println("Suppressing LR action");
			pilot.stop();
			currentDirection = null;
		}

		@Override
		public void handleEvent(API_EVENT event, RequestContext context) {
			//System.out.println("Handling event " + event + " in leftright");
			if (event == API_EVENT.LEFT) {
				currentDirection = DIRECTION.LEFT;
			} else if (event == API_EVENT.RIGHT) {
				currentDirection = DIRECTION.RIGHT;
			}
		}

	}

	public static class StopBehaviour implements Behavior, Callback {
		private MovePilot pilot;
		volatile boolean isTriggered = false;

		public StopBehaviour(MovePilot pilot) {
			this.pilot = pilot;
		}

		@Override
		public boolean takeControl() {
			return isTriggered;
		}

		@Override
		public void action() {
			if (isTriggered) {
				// blocking call as the suppress should suppress it and nothing
				// else
				LCD.clearDisplay();
				LCD.drawString("STOP", 4, 4);

				pilot.stop();

				isTriggered = false;
			}

		}

		@Override
		public void suppress() {
			System.out.println("Suppressing stop action");
			isTriggered = false;
		}

		@Override
		public void handleEvent(API_EVENT event, RequestContext context) {
			//System.out.println("Handling event " + event + " in stop");
			if (event == API_EVENT.STOP) {
				isTriggered = true;
			}
		}

	}

	public static class CallBehaviour implements Behavior, Callback {

		volatile boolean isTriggered = false;

		@Override
		public boolean takeControl() {
			return isTriggered;
		}

		@Override
		public void action() {
			if (isTriggered) {
				Sound.playSample(new File("Betty4.wav"));
				//Sound.playSample(new File("Betty2.wav"));
				isTriggered = false;
			}
		}

		@Override
		public void suppress() {
			isTriggered = false;

		}
		
		@Override
		public void handleEvent(API_EVENT event, RequestContext context) {
			//System.out.println("Handling event " + event + " in stop");
			if (event == API_EVENT.CALL) {
				isTriggered = true;
			}
		}

	}

}
