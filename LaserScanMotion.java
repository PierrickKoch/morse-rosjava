package org.ros.tutorials.pubsub;

import org.apache.commons.logging.Log;
import org.ros.MessageListener;
import org.ros.Node;
import org.ros.NodeConfiguration;
import org.ros.NodeMain;
import org.ros.Publisher;
import org.ros.message.geometry_msgs.Twist;
import org.ros.message.sensor_msgs.LaserScan;

import com.google.common.base.Preconditions;

public class LaserScanMotion implements NodeMain {

	private Node node;
	private Publisher<Twist> publisher;

	@Override
	public void main(NodeConfiguration configuration) {
		Preconditions.checkState(node == null);
		Preconditions.checkNotNull(configuration);
		try {
			node = new Node("laser_cmd", configuration);
			Log log = node.getLog();
			log.info("init");
			publisher = node.createPublisher("/ATRV/Motion_Controller", Twist.class);
			node.createSubscriber("/ATRV/Sick", new MyListener(log), LaserScan.class);
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

	private class MyListener implements MessageListener<LaserScan> {

		private final Log log;

		public MyListener(Log log) {
			this.log = log;
		}

		@Override
		public void onNewMessage(LaserScan msg) {
			Twist cmd = new Twist();
			float[] ranges = msg.ranges;
			if (obstacleIsClose(ranges)) {
				cmd.angular.z = newAngle(ranges);
			} else {
				cmd.linear.x = 1;
			}
			publish(cmd);
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

		private int newAngle(float[] ranges) {
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
