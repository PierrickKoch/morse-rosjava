package org.ros.tutorials.pubsub;

import org.apache.commons.logging.Log;
import org.ros.DefaultNode;
import org.ros.MessageListener;
import org.ros.Node;
import org.ros.NodeConfiguration;
import org.ros.NodeMain;
import org.ros.Publisher;
import org.ros.message.geometry_msgs.Twist;
import org.ros.message.sensor_msgs.LaserScan;
import org.ros.message.sensor_msgs.Image;

import com.google.common.base.Preconditions;

public class LaserScanMotion implements NodeMain {

	private Node node;
	private Publisher<Twist> publisher;

	@Override
	public void main(NodeConfiguration configuration) {
		Preconditions.checkState(node == null);
		Preconditions.checkNotNull(configuration);
		try {
			node = new DefaultNode("laser_cmd", configuration);
			Log log = node.getLog();
			log.info("init");
			publisher = node.createPublisher("/ATRV/Motion_Controller", Twist.class);
			node.createSubscriber("/ATRV/Sick", new LaserListener(log), LaserScan.class);
			node.createSubscriber("/ATRV/Odometry", new OdometryListener(log), Twist.class);
			node.createSubscriber("/ATRV/CameraMain", new CameraListener(log), Image.class);
			log.info("ready");
		} catch (Exception e) {
			if (node != null) {
				node.getLog().fatal(e);
			} else {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void shutdown() {
		node.shutdown();
		node = null;
	}

	private void publish(Twist cmd) {
		publisher.publish(cmd);
	}

  // Odometry 
	private class OdometryListener implements MessageListener<Twist> {

		private final Log log;

		public OdometryListener(Log log) {
			this.log = log;
		}

		@Override
		public void onNewMessage(Twist msg) {
		  // cf http://www.ros.org/doc/api/geometry_msgs/html/msg/Twist.html
		  // http://www.openrobots.org/morse/doc/latest/user/sensors/odometry.html
		  log.info("Odometry sensor:" + msg);
		}
  }
  // Camera 
	private class CameraListener implements MessageListener<Image> {

		private final Log log;

		public CameraListener(Log log) {
			this.log = log;
		}

		@Override
		public void onNewMessage(Image msg) {
		  // cf http://www.ros.org/doc/api/sensor_msgs/html/msg/Image.html
		  // http://www.openrobots.org/morse/doc/latest/user/sensors/camera.html
		  log.info("Camera sensor: new img");
		}
  }
  // Laser 
	private class LaserListener implements MessageListener<LaserScan> {

		private final Log log;

		public LaserListener(Log log) {
			this.log = log;
		}

		@Override
		public void onNewMessage(LaserScan msg) {
		  // cf http://www.ros.org/doc/api/sensor_msgs/html/msg/LaserScan.html
		  // http://www.openrobots.org/morse/doc/latest/user/sensors/sick.html
			Twist cmd = new Twist();
			float[] ranges = msg.ranges;
			if (obstacleIsClose(ranges)) {
				cmd.angular.z = newAngularVelocity(ranges);
			} else {
				cmd.linear.x = 1;
			}
			publish(cmd);
			// http://www.openrobots.org/morse/doc/latest/user/actuators/v_omega.html
		}

		private boolean obstacleIsClose(float[] ranges) {
			for (int i = middle(ranges) - 15; i <= middle(ranges) + 15; i++) {
				if (ranges[i] < 2.0) {
					return true;
				}
			}
			return false;
		}

		private int middle(float[] ranges) {
			return ranges.length / 2;
		}

		private int newAngularVelocity(float[] ranges) {
			double midA = 0, midB = 0;
			// we look to the left and to the right and decide which ones has
			// more space
			for (int i = 0; i < middle(ranges); i++) {
				midA += ranges[i];
			}
			for (int i = middle(ranges); i < ranges.length; i++) {
				midB += ranges[i];
			}
			log.info("A:" + midA + ", B:" + midB);
			if (midA > midB) {
				return -1;
			} else {
				return 1;
			}
		}
	}
}
