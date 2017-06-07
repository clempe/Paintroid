/**
 * Paintroid: An image manipulation application for Android.
 * Copyright (C) 2010-2015 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.paintroid.test.espresso.Integration;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.support.test.espresso.action.Swipe;
import android.util.SparseArray;

import org.catrobat.paintroid.MainActivity;
import org.catrobat.paintroid.PaintroidApplication;
import org.catrobat.paintroid.R;
import org.catrobat.paintroid.command.LayerBitmapCommand;
import org.catrobat.paintroid.command.UndoRedoManager;
import org.catrobat.paintroid.command.implementation.CommandManagerImplementation;
import org.catrobat.paintroid.datastructures.HistoryBitmap;
import org.catrobat.paintroid.datastructures.LimitedSizeQueue;
import org.catrobat.paintroid.test.espresso.util.ActivityHelper;
import org.catrobat.paintroid.test.espresso.util.base.UndoRedoTestRule;
import org.catrobat.paintroid.test.utils.PrivateAccess;
import org.catrobat.paintroid.tools.Layer;
import org.catrobat.paintroid.tools.ToolType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.TestCase.assertEquals;
import static org.catrobat.paintroid.PaintroidApplication.numUndoSaves;
import static org.catrobat.paintroid.PaintroidApplication.numUndoToolSaves;
import static org.catrobat.paintroid.PaintroidApplication.perspective;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.addLayer;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.convertFromCanvasToScreen;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.drawHorizontalLine;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.getCurrentLayer;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.getLayer;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.resetColorPicker;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.selectTool;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.waitMillis;
import static org.catrobat.paintroid.test.espresso.util.UiInteractions.swipeMode;
import static org.catrobat.paintroid.test.espresso.util.UiInteractions.touchAt;
import static org.catrobat.paintroid.test.espresso.util.UiInteractions.waitFor;
import static org.catrobat.paintroid.test.utils.PaintroidAsserts.assertBitmapEquals;


@RunWith(JUnit4.class)
public class UndoRedoBitmapTest {

    @Rule
    public UndoRedoTestRule<MainActivity> activityTestRule = new UndoRedoTestRule(MainActivity.class);

    private ActivityHelper activityHelper;

    private int displayWidth;
    private int displayHeight;
    private int lineLength;
    private LinkedList<Bitmap> bitmapHistory;
    private SparseArray<LimitedSizeQueue<HistoryBitmap>> historyQueue;
    private SparseArray<LimitedSizeQueue<HistoryBitmap>> savedBitmaps;
    private int drawCounter;

    private PointF pointOnScreenMiddle;
    private ArrayList<LayerBitmapCommand> mDrawBitmapCommandsAtLayer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        activityHelper = new ActivityHelper(activityTestRule.getActivity());
        PaintroidApplication.drawingSurface.destroyDrawingCache();

        displayWidth = activityHelper.getDisplayWidth();
        displayHeight = activityHelper.getDisplayHeight();
        lineLength = displayWidth + 200;
        drawCounter = 0;
        bitmapHistory = new LinkedList<>();

        pointOnScreenMiddle = new PointF(displayWidth / 2, displayHeight / 2);
        swipeMode = Swipe.FAST;
        resetColorPicker();

        UndoRedoManager undoRedoManager = UndoRedoManager.getInstance();
        historyQueue = (SparseArray<LimitedSizeQueue<HistoryBitmap>>) PrivateAccess.getMemberValue(UndoRedoManager.class, undoRedoManager, "undoArray");
        savedBitmaps = (SparseArray<LimitedSizeQueue<HistoryBitmap>>) PrivateAccess.getMemberValue(UndoRedoManager.class, undoRedoManager, "savedBitmaps");
        mDrawBitmapCommandsAtLayer = (ArrayList<LayerBitmapCommand>) PrivateAccess.getMemberValue(CommandManagerImplementation.class, PaintroidApplication.commandManager, "mDrawBitmapCommandsAtLayer");
        PaintroidApplication.numUndoToolSaves = 2;
        numUndoSaves = 2;
    }

    @After
    public void tearDown() {
        activityHelper = null;
        displayWidth = 0;
        displayHeight = 0;
        drawCounter = 0;
        pointOnScreenMiddle = null;
    }

    @Test
    public void checkDrawCounterSingleLayer() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        selectTool(ToolType.BRUSH);
        Layer layer = getLayer(0);
        int numDraw = 5;
        assertEquals(drawCounter, getLayerBitmapCommand(layer).getDrawingState());
        LayerBitmapCommand layerBitmapCommand = getLayerBitmapCommand(layer);

        for (int i = 0; i < numDraw; i++) {
            onView(isRoot()).perform(touchAt(pointOnScreenMiddle));
            pointOnScreenMiddle.x += 30;
            drawCounter++;
            assertEquals(drawCounter, layerBitmapCommand.getDrawingState());
        }

        for (int i = 0; i < numDraw; i++) {
            onView(withId(R.id.btn_top_undo)).perform(click());
            drawCounter--;
            assertEquals(drawCounter, layerBitmapCommand.getDrawingState());
        }
    }

    @Test
    public void checkDrawCounterMultipleLayer() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        selectTool(ToolType.BRUSH);
        int numDraw = 5;
        int numLayer = 3;
        int xSpacing = 30;

        for (int l = 0; l < numLayer; l++) {
            Layer layer = getLayer(l);
            assertEquals(drawCounter, getLayerBitmapCommand(layer).getDrawingState());
            LayerBitmapCommand layerBitmapCommand = getLayerBitmapCommand(layer);
            for (int i = 0; i < numDraw; i++) {
                float x = pointOnScreenMiddle.x + xSpacing * i;
                onView(isRoot()).perform(touchAt(x, pointOnScreenMiddle.y));
                drawCounter++;
                assertEquals(drawCounter, layerBitmapCommand.getDrawingState());
            }

            for (int i = 0; i < numDraw; i++) {
                onView(withId(R.id.btn_top_undo)).perform(click());
                drawCounter--;
                assertEquals(drawCounter, layerBitmapCommand.getDrawingState());
            }
            addLayer();
        }
    }

    @Test
    public void drawLinesAndUndo() throws NoSuchFieldException, IllegalAccessException {
        int ySpacing = 200;
        int yStart = 300;
        int xStart = 0;
        int xEnd = displayWidth + 200;
        int num_lines = 3;
        Layer layer = getLayer(0);

        bitmapHistory.add(layer.getImage().copy(Bitmap.Config.ARGB_8888, true));

        for (int i = 0; i < num_lines; i++) {
            int y = yStart + drawCounter * ySpacing;
            drawHorizontalLine(xStart, xEnd, y);
            bitmapHistory.add(layer.getImage().copy(Bitmap.Config.ARGB_8888, true));
            drawCounter++;
        }

        LimitedSizeQueue<HistoryBitmap> layer0Queue = historyQueue.get(layer.getLayerID());
        int expected = Math.min(drawCounter, numUndoSaves);
        assertEquals(expected, layer0Queue.size());
        assertBitmapEquals(bitmapHistory.get(drawCounter - 1), layer0Queue.getYoungest().getBitmap());

        for (int i = 0; i < num_lines; i++) {
            onView(withId(R.id.btn_top_undo)).perform(click());
            drawCounter--;
            assertBitmapEquals(bitmapHistory.get(drawCounter), layer.getImage());
        }
    }

    @Test
    public void drawLinesFillAndUndo() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        int ySpacing = 200;
        int yStart = 300;
        int xStart = 0;
        int xEnd = displayWidth + 200;
        int num_draws = 3;
        int y;
        Layer layer = getCurrentLayer();
        bitmapHistory.add(getCurrentLayer().getImageCopy());

        for (int i = 0; i < num_draws; i++) {
            y = yStart + i * ySpacing;
            drawHorizontalLineAndFillAbove(xStart, xEnd, y);
        }

        LimitedSizeQueue<HistoryBitmap> layer0Queue = historyQueue.get(layer.getLayerID());
        assertEquals(layer0Queue.getYoungest().getHistoryCount(), drawCounter-1);
        assertEquals(Math.min(numUndoSaves, num_draws), layer0Queue.size());
        assertEquals(Math.min(numUndoToolSaves, num_draws), savedBitmaps.get(layer.getLayerID()).size());


        while (drawCounter != 0) {
            onView(withId(R.id.btn_top_undo)).perform(click());
            drawCounter--;
            assertBitmapEquals(bitmapHistory.get(drawCounter), layer.getImage());
        }
    }


    private LayerBitmapCommand getLayerBitmapCommand(Layer layer) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        for (LayerBitmapCommand layerBitmapCommand : mDrawBitmapCommandsAtLayer) {
            if (layerBitmapCommand.getLayer().getLayerID() == layer.getLayerID()) {
                return layerBitmapCommand;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    private void drawHorizontalLineAndFillAbove(int xStart, int xEnd, int y) throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        drawHorizontalLine(xStart, xEnd, y);
        drawCounter++;
        bitmapHistory.add(getCurrentLayer().getImageCopy());
        selectTool(ToolType.FILL);
        PointF pointF = convertFromCanvasToScreen(new PointF(pointOnScreenMiddle.x, y - 100), perspective);
        onView(isRoot()).perform(touchAt(pointF));
        waitMillis(300);
        bitmapHistory.add(getCurrentLayer().getImageCopy());
        drawCounter++;
    }
}
