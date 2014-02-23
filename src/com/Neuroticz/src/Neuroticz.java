package com.Neuroticz.src;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.dylan.NNL.Input;
import me.dylan.NNL.NNetwork;
import me.dylan.NNL.HiddenNode;
import me.dylan.NNL.Node;
import me.dylan.NNL.Output;
import me.dylan.NNL.Synapse;
import me.dylan.NNL.Test.TestUtil;
import me.dylan.NNL.Utils.ArrayUtil;
import me.dylan.NNL.Utils.NetworkUtil;
import me.dylan.NNL.Utils.StringUtil;
import me.dylan.NNL.Utils.ThreadUtil;
import me.dylan.NNL.Visualizer.Display;

public class Neuroticz {
	Timer timer = new Timer();
	public static final int NETWORKS_PER_GENERATION = 3;
	public static final int NETWORK_DISPLAY_OFFSET_MULTIPLIER = 300;
	public static final int HORIZONTAL_NODE_SPACING = 4;
	public static final int VERTICAL_NODE_SPACING = 2;
	public static final int LEFT_NODE_SHIFT = -400;
	public static final int NETWORKS_TO_PICK_PER_GEN = 2;
	ArrayList<NNetwork> mostFit = new ArrayList<NNetwork>();
	ArrayList<NNetwork> allNetworks = new ArrayList<NNetwork>();
	String desiredOutput = "";

	public Neuroticz() {

		Display.showDisplay("Neuroticz Visualizer", new Dimension(700, 700),
				Color.BLACK);

		List<File> indata = FileUtil.compileLearningData(new File("recipes"));
		for (int i = 0; i < NETWORKS_PER_GENERATION; i++) {
			NNetwork initialNet = new NNetwork();
			initialNet.addOutputNodeToNetwork(new Output());
			//
			for (File trainingData : indata) {
				Input inputNode = new Input();
				BufferedReader fileIn = null;
				try {
					fileIn = new BufferedReader(new FileReader(trainingData));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				String inLine = "";
				try {
					while ((inLine = fileIn.readLine()) != null) {
						desiredOutput += inLine;
						if (!inLine.isEmpty()) {
							HiddenNode hiddenNode = NetworkUtil.createHidden(
									inLine, inputNode.getNodeVariety());
							initialNet.addHiddenNodeToNetwork(hiddenNode);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				initialNet.addInputNodeToNetwork(inputNode);

			}
			if (TestUtil.AnyNodesExist(initialNet)) {
				TestUtil.WhatNodesExist(initialNet);
			}
			System.out.println("Any hidden nodes have values assigned? "
					+ TestUtil.AHiddenNodeHasValue(initialNet));
			initialNet.randomizeConnections();
			allNetworks.add(initialNet);
		}
		// net.randomizeConnections();
		ThreadUtil.spinThreadForPool("mainLoop", new Runnable() {

			@Override
			public void run() {
				while (!Thread.interrupted()) {
					int netcount = 0;
					ArrayList<NNetwork> networksClone = (ArrayList<NNetwork>) allNetworks
							.clone();
					for (NNetwork net : networksClone) {

						netcount++;
						Display.setOffset(new Point(netcount
								* NETWORK_DISPLAY_OFFSET_MULTIPLIER, 0));
						doMainLoopTick(net);
						Display.setOffset(new Point(-netcount
								* NETWORK_DISPLAY_OFFSET_MULTIPLIER, 0));
						int matchPercentage = net
								.getNetworkSimilarityPercentage(desiredOutput);
						if (matchPercentage < 90) {
							if (!mostFit.isEmpty()) {
								// allNetworks.clear();
								if (matchPercentage > mostFit.get(0)
										.getNetworkSimilarityPercentage(
												desiredOutput)) {
									mostFit = ArrayUtil.shiftNetworkArray(
											mostFit, 1);
									mostFit.set(0, net);
									NNetwork parentNet1 = mostFit.get(0);
									NNetwork parentNet2 = mostFit.get(1);
									ArrayList<NNetwork> parentNetworks = new ArrayList<NNetwork>();
									parentNetworks.add(parentNet1);
									parentNetworks.add(parentNet2);
									for (int i = 0; i < NETWORKS_PER_GENERATION; i++) {
										allNetworks.add(NetworkUtil
												.breedNetworks(parentNetworks,
														80));
									}
								}
							} else {
								mostFit.addAll(allNetworks);
							}

						}
					}
				}
			}
		});
	}

	public void doMainLoopTick(NNetwork net) {

		// timer.start();
		Display.repaint();
		for (Input in : net.getInputNodesInNetwork()) {
			in.activateInputNode();
		}
		// timer.start();
		for (HiddenNode n : net.getHiddenNodesInNetwork()) {
			n.doTick();
		}
		// timer.end();
		// System.out.println("Hidden Milis: " + timer.getElapsedTimeMilis());
		// timer.end();
		// System.out.println(timer.getElapsedTimeMilis());
		timer.start();
		draw(net);
		timer.end();
		// System.out.println("Paint Milis: " + timer.getElapsedTimeMilis());
		System.out.println("Output: " + net.getNetworkOutput());
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		new Neuroticz();
	}

	public void draw(NNetwork network) {
		Display.repaint();

		int row = 0;
		drawSynapses(network);
		for (Input in : network.getInputNodesInNetwork()) {
			int x = LEFT_NODE_SHIFT + 130
					+ (row * Node.NODE_DRAW_SIZE * HORIZONTAL_NODE_SPACING);
			in.paint(x, 1 * (Node.NODE_DRAW_SIZE * 2) * VERTICAL_NODE_SPACING);
			row++;
		}
		int y = 1;
		row = 0;
		for (HiddenNode n : network.getHiddenNodesInNetwork()) {
			y += 1 /* + network.getHiddenNodesInNetwork().indexOf(n) % 4 */;
			int x = LEFT_NODE_SHIFT + 180
					+ (row * Node.NODE_DRAW_SIZE * HORIZONTAL_NODE_SPACING);
			n.paint(x, y * (Node.NODE_DRAW_SIZE * 2) * VERTICAL_NODE_SPACING);
			if (network.getHiddenNodesInNetwork().indexOf(n) % 10 == 0) {
				row++;
				y = 1;
			}
			// if(column >= 2) {
			// column+=10;
			// }

		}
		row = 0;
		for (Output out : network.getOutputNodesInNetwork()) {
			int x = LEFT_NODE_SHIFT + 180 + 200
					+ (row * Node.NODE_DRAW_SIZE * HORIZONTAL_NODE_SPACING);

			out.paint(x, Node.NODE_DRAW_SIZE * 20 * VERTICAL_NODE_SPACING);
			row++;
		}
	}

	public void drawSynapses(NNetwork net) {
		for (Synapse connection : net.getNetworkSynapses()) {
			// if (!connection.hasPaintedInTick()) {
			Point originDrawingPoint = connection.getConnectionOrigin().graphicsRepresentationObject
					.getPaintCoords();
			Point destinationDrawingPoint = connection
					.getConnectionDestination().graphicsRepresentationObject
					.getPaintCoords();

			Color displayColor = Display.getDisplayBackgroundColor();
			Display.setDisplayBackgroundColor(NetworkUtil
					.returnWeightColor(connection.getSynapseWeight()));
			Display.drawLine(originDrawingPoint.x, originDrawingPoint.y,
					destinationDrawingPoint.x, destinationDrawingPoint.y);
			Display.setDisplayBackgroundColor(displayColor);
			connection.setHasPaintedInTick(true);
			// } else {
			//
			// }

		}
	}
}