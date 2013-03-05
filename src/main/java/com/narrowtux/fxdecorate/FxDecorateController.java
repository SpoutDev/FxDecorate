/*
 * This file is part of FxDecorate.
 *
 * Copyright (c) 2013 narrowtux <http://www.narrowtux.com/>
 * FxDecorate is licensed under the GNU Lesser General Public License.
 *
 * FxDecorate is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FxDecorate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.narrowtux.fxdecorate;

import java.util.LinkedList;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;

public class FxDecorateController {
	private LinkedList<Node> moveNodes = new LinkedList<Node>();
	private Node primaryMoveNode = null;
	private FxDecorateScene scene;
	private Node currentMoveNode = null;
	private Point2D moveStartPoint = null;

	FxDecorateController(FxDecorateScene scene) {
		this.scene = scene;
	}

	public void addMoveNode(final Node added) {
		if (primaryMoveNode == null) {
			primaryMoveNode = added;
		}
		moveNodes.add(added);
		added.setOnMousePressed(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				currentMoveNode = added;
				moveStartPoint = new Point2D(e.getScreenX(), e.getScreenY());
				e.consume();
			}
		});

		added.setOnMouseDragged(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				if (currentMoveNode == added) {
					Point2D delta = new Point2D(e.getScreenX() - moveStartPoint.getX(), e.getScreenY() - moveStartPoint.getY());
					double stageX = getScene().getWindow().getX();
					double stageY = getScene().getWindow().getY();

					stageX += delta.getX();
					stageY += delta.getY();

					Rectangle2D newBounds = new Rectangle2D(stageX, stageY, getScene().getWindow().getWidth(), getScene().getWindow().getHeight());

					// check screen bounds
					for (Screen screen : Screen.getScreensForRectangle(newBounds)) {
						// Limit dragging over menu bar
						if (screen.getVisualBounds().equals(Screen.getPrimary().getVisualBounds()) && OperatingSystem.getOS().isMac()) {
							if (stageY < screen.getVisualBounds().getMinY()) {
								stageY = screen.getVisualBounds().getMinY();
							}
						}
					}

					getScene().getWindow().setX(stageX);
					getScene().getWindow().setY(stageY);
					moveStartPoint = new Point2D(e.getScreenX(), e.getScreenY());
					e.consume();
				}
			}
		});

		added.setOnMouseReleased(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent e) {
				if (currentMoveNode == added) {
					currentMoveNode = null;
					// checkScreenEdges();
					e.consume();
				}
			}
		});
	}

	public void removeMoveNode(final Node removed) {
		moveNodes.remove(removed);
		removed.setOnMouseMoved(null);
		removed.setOnMousePressed(null);
		removed.setOnMouseReleased(null);
	}

	public FxDecorateScene getScene() {
		return scene;
	}

	public void centerOnScreen() {
		Screen primary = Screen.getPrimary();
		Rectangle2D bounds = primary.getVisualBounds();
		double width = getScene().getStage().getWidth();
		double height = getScene().getStage().getHeight();
		double x = bounds.getMinX() + (bounds.getWidth() - width) / 2d;
		double y = bounds.getMinY() + (bounds.getHeight() - height) / 2d;

		getScene().getWindow().setX(x);
		getScene().getWindow().setY(y);
	}

	/**
	 * checks if the primary move node is off screen and if so, resets the
	 * window so it is visible again.
	 */
	public void checkScreenEdges() {
		if (primaryMoveNode == null) {
			return;
		}

		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;

		for (Screen screen : Screen.getScreens()) {
			minX = Math.min(minX, screen.getVisualBounds().getMinX());
			minY = Math.min(minY, screen.getVisualBounds().getMinY());

			maxX = Math.max(maxX, screen.getVisualBounds().getMaxX());
			maxY = Math.max(maxY, screen.getVisualBounds().getMaxY());
		}

		Rectangle2D bounds = getScreenBounds(primaryMoveNode);
		Rectangle2D valid = bounds;
		System.out.println(bounds);
		if (!isValidBounds(bounds)) {
			System.out.println("Not valid");
			if (bounds.getMinY() < minY || bounds.getMaxY() > maxY) {
				int dir = 1;
				if (bounds.getMaxY() > maxY) {
					dir = -1;
				}
				for (double y = minX; y <= maxY && y >= minY; y += dir) {
					Rectangle2D test = cloneRect(bounds, null, y, null, null);
					if (isValidBounds(test)) {
						valid = test;
					}
				}
			}
		}
		if (valid != bounds) {

		}
	}

	void setResizeableHandle(final Node handle) {

	}

	/**
	 * Returns if the given rectangle has a valid position for a primary move
	 * node.<br/>
	 * Conditions are:
	 * <ul>
	 * <li>at least 10 vertical pixels must be visible</li>
	 * <li>the whole height must be visible</li>
	 * </ul>
	 * 
	 * @param bounds
	 *            the rectangle that should be checked
	 * @return
	 */
	public boolean isValidBounds(Rectangle2D bounds) {
		ObservableList<Screen> screens = Screen.getScreensForRectangle(bounds);
		if (screens.size() == 0) {
			return false;
		}
		for (Screen screen : Screen.getScreensForRectangle(bounds)) {
			Rectangle2D vis = screen.getVisualBounds();
			System.out.println("screen: " + vis);
			if (bounds.getMinY() < vis.getMinY() || bounds.getMaxY() > vis.getMaxY()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Manually sets the primary move node.<br/>
	 * It will be made sure that this node is always available to be dragged so
	 * the user can't move it off screen and have no possibility to drag it.
	 * 
	 * @param primaryMoveNode
	 */
	public void setPrimaryMoveNode(Node primaryMoveNode) {
		this.primaryMoveNode = primaryMoveNode;
	}

	public Node getPrimaryMoveNode() {
		return primaryMoveNode;
	}

	private static Rectangle2D getScreenBounds(Node node) {
		double windowX = node.getScene().getWindow().getX();
		double windowY = node.getScene().getWindow().getY();

		Point2D scenepos = node.localToScene(0, 0);

		windowX += scenepos.getX();
		windowY += scenepos.getY();

		return new Rectangle2D(windowX, windowY, node.getLayoutBounds().getWidth(), node.getLayoutBounds().getHeight());
	}

	private static Rectangle2D cloneRect(Rectangle2D c, Double x, Double y, Double width, Double height) {
		return new Rectangle2D(x != null ? x : c.getMinX(), y != null ? y : c.getMinY(), width != null ? width : c.getWidth(), height != null ? height : c.getHeight());
	}
}