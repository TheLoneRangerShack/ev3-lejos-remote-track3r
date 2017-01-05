package com.loneranger.lejos.control.web;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.loneranger.lejos.arbitrator.ImprovedArbitrator;
import com.loneranger.lejos.behaviour.BehaviourProvider;
import com.loneranger.lejos.control.web.RestApiHandlerThread.API_EVENT;

import lejos.hardware.Button;
import lejos.hardware.Key;
import lejos.hardware.KeyListener;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.subsumption.Behavior;

public class ApiControlledTrack3r {
	public static boolean endOfProgram = false;
	
	// standard track3r wheel dimensions in mm
	public static Wheel TRACK3R_LEFT_WHEEL = WheeledChassis.modelWheel(new EV3LargeRegulatedMotor(MotorPort.C), 30)
			.offset(97.5).gearRatio(1);
	public static Wheel TRACK3R_RIGHT_WHEEL = WheeledChassis.modelWheel(new EV3LargeRegulatedMotor(MotorPort.B), 30)
			.offset(-97.5).gearRatio(1);
	public static Chassis TRACK3R = new WheeledChassis(new Wheel[] { TRACK3R_LEFT_WHEEL, TRACK3R_RIGHT_WHEEL },
			WheeledChassis.TYPE_DIFFERENTIAL);

	private MovePilot track3rPilot;
	private EV3IRSensor infraredSensor;

	public ApiControlledTrack3r() {
		track3rPilot = new MovePilot(TRACK3R);
		infraredSensor = new EV3IRSensor(SensorPort.S1);

	}

	public MovePilot getPilot() {
		return track3rPilot;
	}

	public EV3IRSensor getInfraredSensor() {
		return infraredSensor;
	}

	public static class AbortBehaviour implements Behavior {
		volatile private boolean isTriggered = false;
		private final MovePilot pilot;
		private final ExecutorService apiHandler;
		private final RestApiHandlerThread apiThread;

		public AbortBehaviour(MovePilot pilot, ExecutorService apiHandler, RestApiHandlerThread apiThread) {
			this.pilot = pilot;
			this.apiHandler = apiHandler;
			this.apiThread = apiThread;
		}

		@Override
		public boolean takeControl() {
			return isTriggered;
		}

		@Override
		public void action() {
			pilot.stop();
			apiThread.closeServerSocket();
			apiHandler.shutdownNow();
			endOfProgram = true;
		}

		@Override
		public void suppress() {
			// should never be suppressed as this is the highest priority
			// action//
			this.isTriggered = false;
		}

		public void setEscapePressed() {
			this.isTriggered = true;
		}

	}

	public static class FallbackBehaviour implements Behavior {

		volatile private boolean isSuppressed = false;
		
		@Override
		public boolean takeControl() {
			return true;
		}

		@Override
		public void action() {
			while(!isSuppressed && !endOfProgram) {
				Thread.yield();
			}
			
			isSuppressed = false;
			
		}

		@Override
		public void suppress() {
			isSuppressed = true;
			
		}

	}

	// public static class DetectObstacleBehaviour implements Behavior {
	//
	// EV3IRSensor infraredSensor;
	// SampleProvider
	// public DetectObstacleBehaviour(EV3IRSensor infraredSensor) {
	// this.infraredSensor = infraredSensor;
	// }
	//
	// @Override
	// public boolean takeControl() {
	//
	// }
	//
	// @Override
	// public void action() {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void suppress() {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// }

	// public static class FallbackBehaviour implements Behavior {
	// @Override
	// public boolean takeControl() {
	// return true;
	// }
	//
	// @Override
	// public void action() {
	// while (true) {
	// Sound.twoBeeps();
	// try {
	// Thread.sleep(1000);
	// } catch (InterruptedException e) {
	// break;
	// }
	// }
	// }
	//
	// @Override
	// public void suppress() {
	// // just stop the sound is all
	//
	// }
	//
	// }

	public static void main(String[] args) throws IOException {
		ApiControlledTrack3r parent = new ApiControlledTrack3r();
		Behavior stop = new BehaviourProvider.StopBehaviour(parent.getPilot());
		Behavior forwardBackward = new BehaviourProvider.ForwardBackwardBehaviour(parent.getPilot());
		Behavior leftRight = new BehaviourProvider.LeftRightBehaviour(parent.getPilot());
		Behavior call = new BehaviourProvider.CallBehaviour();
		Behavior fallback = new FallbackBehaviour();

		// first the API handler that uses a separate low priority thread to
		// listen to HTTP requests on port 80
		ExecutorService threadPool = Executors.newFixedThreadPool(1);
		RestApiHandlerThread handler = new RestApiHandlerThread();
		handler.registerListener(API_EVENT.FORWARD, (Callback) forwardBackward);
		handler.registerListener(API_EVENT.BACKWARD, (Callback) forwardBackward);
		handler.registerListener(API_EVENT.LEFT, (Callback) leftRight);
		handler.registerListener(API_EVENT.RIGHT, (Callback) leftRight);
		handler.registerListener(API_EVENT.STOP, (Callback) stop);
		handler.registerListener(API_EVENT.CALL, (Callback) call);

		final AbortBehaviour abort = new AbortBehaviour(parent.getPilot(), threadPool, handler);
		Button.ESCAPE.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(Key k) {
				LCD.clearDisplay();
				LCD.drawString("Escape Button", 4, 4);
				abort.setEscapePressed();
			}

			@Override
			public void keyReleased(Key k) {
				// do nothing for now

			}

		});

		// FallbackBehaviour waitBehaviour = new FallbackBehaviour();

		// configure our behaviour arbiter
		Behavior[] bArray = { fallback, call, forwardBackward, leftRight, stop, abort };
		ImprovedArbitrator arbitrator = new ImprovedArbitrator(bArray, true);

		threadPool.submit(handler);
		arbitrator.go();
		//arbitrator.stop();

		Sound.beepSequenceUp();
		LCD.refresh();
		// start our listener//

	}
}
